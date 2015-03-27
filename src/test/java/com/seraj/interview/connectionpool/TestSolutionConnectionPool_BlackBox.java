package com.seraj.interview.connectionpool;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertSame;

import java.sql.Connection;
import java.sql.SQLException;

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

	// List<Connection> mockConnections;

	// @Before
	// public void initializeConnections() {
	// mockConnections = new ArrayList<Connection>();
	// for (int i = 0; i < 5; ++i) {
	// mockConnections.add(createNiceMock(Connection.class));
	// }
	// }

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
	 * {@link ConnectionPool#getConnection()} is called.
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