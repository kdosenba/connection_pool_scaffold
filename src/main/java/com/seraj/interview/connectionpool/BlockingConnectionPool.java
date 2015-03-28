package com.seraj.interview.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.opower.connectionpool.ConnectionPool;
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
	private int maxPoolSize = Integer.MAX_VALUE;
	private TimeUnit timeUnits = TimeUnit.MILLISECONDS;
	private long borrowTimeoutInterval = 500;
	private long leaseTerm = -1;
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
	// The list of connection on lease.
	private BlockingQueue<Connection> leasedConnections = new LinkedBlockingQueue<Connection>();

	// ************************
	// Daemon threads
	// ************************

	public BlockingConnectionPool() {
		runDeamonThreads();
	}

	public BlockingConnectionPool(Properties properties) {
		ConfigurationReader.loadConfigurations(properties,
				BlockingConnectionPool.class, this);
		runDeamonThreads();
	}

	/**
	 * If an idle connections exist, it will be returned. Otherwise, a new
	 * connection is created and returned.
	 * 
	 * @return A {@link Connection}, either created new to meet the demand, or
	 *         recycled from a previously released connection.
	 * @throws SQLException
	 */
	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = idleConnections.poll();
		while (connection == null) {
			connection = tryCreateNewConnection();
			connection = connection == null ? tryBorrowConnection()
					: connection;
		}
		leasedConnections.offer(connection);
		return connection;
	}

	/**
	 * Releasing a connection places it back into the pool so that it can be
	 * reused at a future call to {@link ConnectionPool#getConnection()}. If
	 * there is no space available in the pool to accept the connection, then
	 * the connection will be closed and ignored.
	 * 
	 * @throws IllegalArgumentException
	 *             When the connection is null.
	 */
	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		throwExceptionIfUnknown(connection);
		leasedConnections.remove(connection);
		tryRecycleConnection(connection);
	}

	private void runDeamonThreads() {

	}

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

	private void tryRecycleConnection(Connection connection)
			throws SQLException {
		if (!connection.isValid(validationTimeoutInSeconds)) {
			LOG.info("Connection from Thread["
					+ Thread.currentThread().getName()
					+ "] is no longer valid. Resource is being released from the pool.");
		}
		idleConnections.offer(connection);
	}

	private void throwExceptionIfUnknown(Connection connection)
			throws SQLException {
		if (connection == null) {
			IllegalArgumentException exception = new IllegalArgumentException(
					"A null connection is not valid.");
			throw new SQLException(exception);
		} else if (!leasedConnections.contains(connection)) {
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
