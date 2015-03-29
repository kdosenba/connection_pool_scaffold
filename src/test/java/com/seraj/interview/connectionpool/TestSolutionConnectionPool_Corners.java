package com.seraj.interview.connectionpool;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.opower.connectionpool.ConnectionPool;

/**
 * Test corner cases with black box knowledge of {@link ConnectionPool}
 * 
 * @author Seraj Dosenbach
 * 
 */
@RunWith(EasyMockRunner.class)
public class TestSolutionConnectionPool_Corners extends
		EasyMockSupport {

	// The class under test
	private BlockingConnectionPool classUnderTest;
	@Mock
	private ConnectionFactory mockFactory;
	@Mock
	private Connection mockConnection;
	private static final int VALIDATION_TIMEOUT_VALUE = 1233;

	/**
	 * Initialize the pool with properties.
	 */
	@Before
	public void setUp() {
		Properties properties = new Properties();
		properties.setProperty("validationTimeoutInSeconds", ""
				+ VALIDATION_TIMEOUT_VALUE);
		classUnderTest = new BlockingConnectionPool(properties);
		classUnderTest.setConnectionFactory(mockFactory);
	}

	/**
	 * Verify that an {@link IllegalArgumentException} is thrown when null is
	 * provided as the argument to
	 * {@link BlockingConnectionPool#releaseConnection(Connection)}
	 */
	@Test
	public void testReleaseConnection_null() {
		try {
			classUnderTest.releaseConnection(null);
			fail("No exception was thrown for a null argument.");
		} catch (SQLException exception) {
			assertTrue("The cause was not as expected; an illegal argument.",
					exception.getCause() instanceof IllegalArgumentException);
		}
	}

	/**
	 * Verify that an {@link IllegalArgumentException} is thrown when a
	 * {@link Connection} not associated with the pool is provided as the
	 * argument to {@link BlockingConnectionPool#releaseConnection(Connection)}
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testReleaseConnection_notFromPool() throws SQLException {
		mockConnection.close();
		replayAll();

		try {
			classUnderTest.releaseConnection(mockConnection);
			fail("No exception was thrown for a null argument.");
		} catch (SQLException exception) {
			assertTrue("The cause was not as expected; an illegal argument.",
					exception.getCause() instanceof IllegalArgumentException);
		}

		verifyAll();
	}

	/**
	 * Verify that an {@link IllegalArgumentException} is thrown when a
	 * {@link Connection} not associated with the pool is provided as the
	 * argument to {@link BlockingConnectionPool#releaseConnection(Connection)}
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testReleaseConnection_closedConnection() throws SQLException {
		Connection firstConnection = createMock(Connection.class);
		Connection secondConnection = createMock(Connection.class);
		expect(mockFactory.newConnection()).andReturn(firstConnection);
		expect(firstConnection.isValid(VALIDATION_TIMEOUT_VALUE)).andReturn(
				false);
		firstConnection.close();
		expect(mockFactory.newConnection()).andReturn(secondConnection);
		replayAll();

		Connection connection1 = classUnderTest.getConnection();
		assertSame("The first connection was not returned.", firstConnection,
				connection1);
		classUnderTest.releaseConnection(connection1);
		Connection connection2 = classUnderTest.getConnection();
		assertSame("The second connection was not returned.", secondConnection,
				connection2);

		verifyAll();
	}

}
