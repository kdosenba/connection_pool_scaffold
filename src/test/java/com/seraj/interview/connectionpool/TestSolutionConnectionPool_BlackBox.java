package com.seraj.interview.connectionpool;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.opower.connectionpool.ConnectionPool;

/**
 * Test expectations given black box knowledge of {@link ConnectionPool}
 * 
 * @author Seraj Dosenbach
 * 
 */
@RunWith(EasyMockRunner.class)
public class TestSolutionConnectionPool_BlackBox extends EasyMockSupport {

	// The class under test
	@TestSubject
	private BlockingConnectionPool classUnderTest = new BlockingConnectionPool();
	@Mock
	private ConnectionFactory mockFactory;
	@Mock
	private Connection mockConnection;

	/**
	 * Verify that a {@link Connection} taken from the
	 * {@link BlockingConnectionPool}, when the pool is empty, is valid.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testGetConnection_initialGet() throws SQLException {
		expect(mockFactory.newConnection()).andReturn(mockConnection);
		replayAll();

		Connection connection = classUnderTest.getConnection();

		verifyAll();
		assertSame("The expected conneciton was not returned.", mockConnection,
				connection);
	}

	/**
	 * Verify that a {@link Connection} taken from the
	 * {@link BlockingConnectionPool}, when connections already exist in the
	 * pool, is valid.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testGetConnection_nonInitialGet() throws SQLException {
		replayAll();
		BlockingQueue<Connection> queue = new LinkedBlockingQueue<Connection>();
		queue.add(mockConnection);
		// classUnderTest = new BlockingConnectionPool(queue);

		Connection connection = classUnderTest.getConnection();

		verifyAll();
		assertSame("The expected conneciton was not returned.", mockConnection,
				connection);
	}

	/**
	 * Verify that a {@link Connection} taken from the
	 * {@link BlockingConnectionPool}, after already existing connections in the
	 * pool are returned, is valid.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testGetConnection_gettingAllIdleConnections()
			throws SQLException {
		expect(mockFactory.newConnection()).andReturn(mockConnection);
		int numInitialConnections = 5;
		BlockingQueue<Connection> queue = new LinkedBlockingQueue<Connection>();
		for (int i = 0; i < numInitialConnections; ++i) {
			queue.add(createMock(Connection.class));
		}
		replayAll();

		// classUnderTest = new BlockingConnectionPool(queue);
		classUnderTest.setConnectionFactory(mockFactory);

		List<Connection> returnedConnections = new ArrayList<Connection>();
		for (int i = 0; i < numInitialConnections; ++i) {
			returnedConnections.add(classUnderTest.getConnection());
		}
		Connection connection = classUnderTest.getConnection();

		verifyAll();
		assertTrue("Some initially queueed connection was not returned.",
				returnedConnections.containsAll(queue));
		assertSame("The expected conneciton was not returned.", mockConnection,
				connection);
	}

	/**
	 * Test that a {@link Connection} released back into the
	 * {@link BlockingConnectionPool} is not terminated and remains valid.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testReleaseConnection() throws SQLException {
		expect(mockFactory.newConnection()).andReturn(mockConnection);
		replayAll();

		Connection connection = classUnderTest.getConnection();
		classUnderTest.releaseConnection(connection);
		verifyAll();
	}

	/**
	 * Test that a {@link Connection} released back into the
	 * {@link BlockingConnectionPool} is recycled when the next
	 * {@link ConnectionPool#getConnection()} method is called.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testRecycleConnection() throws SQLException {
		expect(mockFactory.newConnection()).andReturn(mockConnection);
		replayAll();

		Connection connection1 = classUnderTest.getConnection();
		classUnderTest.releaseConnection(connection1);
		Connection connection2 = classUnderTest.getConnection();

		verifyAll();
		assertSame("The connection was not recycled.", connection1, connection2);
	}

}