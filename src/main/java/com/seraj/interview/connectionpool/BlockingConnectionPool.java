package com.seraj.interview.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.opower.connectionpool.ConnectionPool;
import com.seraj.interview.configuration.Configurable;
import com.seraj.interview.configuration.ConfigurationReader;

/**
 * A Thread-safe implementation of {@link ConnectionPool} backed by a
 * {@link BlockingQueue}.
 * 
 * @author Seraj Dosenbach
 *
 */
public class BlockingConnectionPool implements ConnectionPool {

	// ************************
	// Required Fields, use setter to initialize
	// ************************
	/**
	 * The factory used to create new connections for the pool. This is a
	 * required field.
	 */
	private ConnectionFactory connectionFactory;

	// ************************
	// Configurable properties
	// ************************
	@Configurable
	private int maxPoolSize = Integer.MAX_VALUE;
	@Configurable
	private TimeUnit timeUnits = TimeUnit.MILLISECONDS;
	@Configurable
	private long borrowTimeoutInterval = 500;
	@Configurable
	private long leaseTerm = -1;
	@Configurable
	private int validationTimeoutInSeconds = 2;

	// ************************
	// Internal fields
	// ************************
	private static final Logger LOG = Logger
			.getLogger(BlockingConnectionPool.class);
	// Current size of the pool
	private AtomicInteger size = new AtomicInteger(0);
	// The list of connections sitting idle.
	private BlockingQueue<Connection> idleConnections = new LinkedBlockingQueue<Connection>();
	// The map of connections on lease and the system time when they were
	// leased.
	private ConcurrentMap<Connection, Long> leasedConnectionStartTime = new ConcurrentHashMap<Connection, Long>();

	// ************************
	// Daemon threads
	// ************************

	/**
	 * Create a new Blocking {@link ConnectionPool}.
	 */
	public BlockingConnectionPool() {
		runDeamonThreads();
	}

	/**
	 * Create and configure a new Blocking {@link ConnectionPool}. </p> The
	 * configurable properties include:
	 * <ul>
	 * <li><b>maxPoolSize</b> = The maximum number of connections to be held by
	 * this connection pool. <i>Default size is Integer.MAX_VALUE
	 * (unbounded).</i></li>
	 * <li><b>timeUnits</b> = The time unit context for all time based
	 * configurations; unless explicitly stated otherwise. <i>Default value is
	 * MILLISECONDS</i></li>
	 * <li><b>borrowTimeoutInterval</b> = The length of time a Thread will wait,
	 * each try, before timing out while waiting for a connection. <i>Default
	 * value is 500.</i></li>
	 * <li><b>leaseTerm</b> = The length of time a connection can be leased for.
	 * Once the lease time has expired on a given connection it will be reaped
	 * and closed. If configured to '-1' then connections are leased
	 * indefinitely. <i>Default value is '-1'.</i></li>
	 * <li><b>validationTimeoutInSeconds</b> = The length of time to wait while
	 * validating the state of a given connection. This field is always in
	 * Seconds. <i>Default value is 2.</i></li>
	 * </ul>
	 * 
	 * </p> <b>Note:</b> The timeUnits field configuration must match exactly an
	 * identifier used to declare an enum constant of {@link TimeUnit}.
	 * 
	 * @param properties
	 *            The java {@link Properties} used to configure this
	 *            {@link ConnectionPool}.
	 */
	public BlockingConnectionPool(Properties properties) {
		ConfigurationReader.loadConfigurations(properties,
				BlockingConnectionPool.class, this);
		runDeamonThreads();
	}

	/**
	 * Gets a {@link Connection} from the connection pool. If an idle
	 * {@link Connection} exists, use it. Otherwise, if space is available in
	 * the pool a new {@link Connection} is created and returned. Else, block
	 * until an available {@link Connection} is available to borrow.
	 * 
	 * @return A {@link Connection} from the connection pool.
	 * @throws SQLException
	 *             Thrown if the method is interrupted.
	 */
	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = idleConnections.poll();
		while (connection == null) {
			connection = tryCreateNewConnection();
			connection = connection == null ? tryBorrowConnection()
					: connection;
		}
		leasedConnectionStartTime.putIfAbsent(connection,
				System.currentTimeMillis());
		return connection;
	}

	/**
	 * Releases a {@link Connection} back into the connection pool. If the
	 * {@link Connection} is part of the pool, revoke the lease and recycle the
	 * connection.
	 * 
	 * @param connection
	 *            The {@link Connection} being released back into the connection
	 *            pool.
	 * @throws SQLException
	 *             Thrown when the {@link Connection} is unknown, or a failure
	 *             to determine the validity of the connection.
	 */
	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		throwExceptionIfUnknown(connection);
		leasedConnectionStartTime.remove(connection);
		tryRecycleConnection(connection);
	}

	private void runDeamonThreads() {

	}

	/**
	 * Thread-safe implementation to create a new {@link Connection} when space
	 * is available in the connection pool.
	 * 
	 * @return A connection if spaces is available, null otherwise.
	 */
	private Connection tryCreateNewConnection() {
		Connection connection = null;
		if (size.get() < maxPoolSize) {
			// Check if multiple threads got past the first if statement.
			if (size.incrementAndGet() > maxPoolSize) {
				// Yep! more than one slipped in.
				size.decrementAndGet();
			} else {
				connection = getConnectionFactory().newConnection();
				LOG.debug("New connection added to the pool.");
			}
		}
		return connection;
	}

	/**
	 * A blocking attempt at acquiring an available {@link Connection} from the
	 * idle list. The wait time for this blocking wait is configurable.
	 * 
	 * @return A borrowed connection if available, null otherwise.
	 * @throws SQLException
	 *             Thrown if the blocking wait is interrupted.
	 */
	private Connection tryBorrowConnection() throws SQLException {
		Connection connection;
		try {
			connection = idleConnections.poll(borrowTimeoutInterval, timeUnits);
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new SQLException("Pool get connection interupted.", e);
		}
		return connection;
	}

	/**
	 * Attempt to reuse the {@link Connection} in the pool. If the
	 * {@link Connection} is no longer valid or the idle list can not accept the
	 * {@link Connection} then the {@link Connection} is closed.
	 * 
	 * @param connection
	 *            The {@link Connection} to place into the idle list.
	 * @throws SQLException
	 *             Thrown if some error occurs during validation or closing of
	 *             the {@link Connection}.
	 */
	private void tryRecycleConnection(Connection connection)
			throws SQLException {
		if (!connection.isValid(validationTimeoutInSeconds)) {
			size.decrementAndGet();
			connection.close();
			LOG.info("Connection from Thread["
					+ Thread.currentThread().getName()
					+ "] is no longer valid. Resource is being released from the pool.");
		}
		// If connection is not be retained, close it and adjust the pool size.
		// Otherwise, the connection is now ready to be reused.
		else if (!idleConnections.offer(connection)) {
			size.decrementAndGet();
			connection.close();
		}
	}

	/**
	 * If the {@link Connection} is not associated with the pool throw an
	 * {@link SQLException} to that affect.
	 * 
	 * @param connection
	 *            The {@link Connection} to validate.
	 * @throws SQLException
	 *             Thrown if the {@link Connection} is null or not associated
	 *             with this pool.
	 */
	private void throwExceptionIfUnknown(Connection connection)
			throws SQLException {
		if (connection == null) {
			IllegalArgumentException exception = new IllegalArgumentException(
					"A null connection is not valid.");
			throw new SQLException(exception);
		} else if (!leasedConnectionStartTime.containsKey(connection)) {
			connection.close();
			IllegalArgumentException exception = new IllegalArgumentException(
					"The connection is not recognized by the pool.");
			throw new SQLException(exception);
		}
	}

	/**
	 * @return the connectionFactory
	 */
	private ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	/**
	 * @param connectionFactory
	 *            the connectionFactory to set
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

}
