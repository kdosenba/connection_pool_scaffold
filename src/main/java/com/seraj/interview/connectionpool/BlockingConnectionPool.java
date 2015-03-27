package com.seraj.interview.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.opower.connectionpool.ConnectionPool;

/**
 * A Thread-safe implementation of {@link ConnectionPool} backed by a
 * {@link BlockingQueue}.
 * 
 * @author Seraj Dosenbach
 *
 */
public class BlockingConnectionPool implements ConnectionPool {

	/**
	 * The factory used to create new connections for the pool.
	 */
	private ConnectionFactory connectionFactory;

	/**
	 * The list of connections sitting idle. The default implementation is an
	 * unbounded thread-safe {@link LinkedBlockingQueue}.
	 */
	private BlockingQueue<Connection> idleConnections = new LinkedBlockingQueue<Connection>();

	public BlockingConnectionPool() {

	}

	/**
	 * An overloaded constructor used to initialize the
	 * {@link BlockingConnectionPool} with a non-default implementation of a
	 * {@link BlockingQueue}.
	 * 
	 * @param idleConnectionQueue
	 */
	public BlockingConnectionPool(BlockingQueue<Connection> idleConnectionQueue) {
		idleConnections = idleConnectionQueue;
	}

	/**
	 * If an idle connections exist, it will be returned. Otherwise, if capacity
	 * exists in the pool a new connection is created and returned. Else, the
	 * functionality is dependent on the idle queue implementation.
	 * 
	 * @return A {@link Connection}, either created new to meet the demand, or
	 *         recycled from a previously released connection.
	 * @throws SQLException
	 */
	@Override
	public Connection getConnection() throws SQLException {
		if (idleConnections.isEmpty()) {
			Connection connection = getConnectionFactory().newConnection();
			idleConnections.add(connection);
		}
		Connection connection = idleConnections.poll();
		return connection;
	}

	/**
	 * Releasing a connection places it back into the pool so that it can be
	 * reused at a future call to {@link ConnectionPool#getConnection()}.
	 * 
	 * @throws IllegalArgumentException
	 *             When the connection is null.
	 */
	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		if (connection == null) {
			throw new IllegalArgumentException(
					"Null connections are not accepted.");
		}
		if (idleConnections.remainingCapacity() == Integer.MAX_VALUE) {
			connection.close();
		} else {
			idleConnections.add(connection);
		}
	}

	/**
	 * @return the connectionFactory
	 */
	public ConnectionFactory getConnectionFactory() {
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
