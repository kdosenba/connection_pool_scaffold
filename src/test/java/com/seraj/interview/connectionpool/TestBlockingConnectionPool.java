package com.seraj.interview.connectionpool;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.opower.connectionpool.ConnectionPool;

/**
 * Test standard expectations of a {@link ConnectionPool}
 * 
 * @author Seraj Dosenbach
 * 
 */
@RunWith(EasyMockRunner.class)
public class TestBlockingConnectionPool extends EasyMockSupport {

	// The class under test
	@TestSubject
	private BlockingConnectionPool classUnderTest = new BlockingConnectionPool();
	@Mock
	private ConnectionFactory mockFactory;
	@Mock
	private Connection mockConnection;

	/**
	 * Verify that a {@link Connection} taken from the
	 * {@link BlockingConnectionPool} is valid.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testGetConnection() throws SQLException {
		expect(mockFactory.newConnection()).andReturn(mockConnection);
		replayAll();

		Connection connection = classUnderTest.getConnection();

		verifyAll();
		assertSame("The expected conneciton was not returned.", mockConnection,
				connection);
	}

	/**
	 * Verify that multiple {@link Connection}s can be returned from the
	 * {@link BlockingConnectionPool}.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testGetConnection_multiples() throws SQLException {
		int numInitialConnections = 5;
		List<Connection> newConnections = new ArrayList<Connection>();
		for (int i = 0; i < numInitialConnections; ++i) {
			newConnections.add(createMock(Connection.class));
			expect(mockFactory.newConnection())
					.andReturn(newConnections.get(i));
		}
		replayAll();

		List<Connection> returnedConnections = new ArrayList<Connection>();
		for (int i = 0; i < numInitialConnections; ++i) {
			returnedConnections.add(classUnderTest.getConnection());
		}

		verifyAll();
		assertTrue("All connections were retrieved.",
				returnedConnections.containsAll(newConnections));
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
		expect(mockConnection.isValid(2)).andReturn(true);
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
		expect(mockConnection.isValid(2)).andReturn(true);
		replayAll();

		Connection connection1 = classUnderTest.getConnection();
		classUnderTest.releaseConnection(connection1);
		Connection connection2 = classUnderTest.getConnection();

		assertSame("The connection was not recycled.", connection1, connection2);
	}

}