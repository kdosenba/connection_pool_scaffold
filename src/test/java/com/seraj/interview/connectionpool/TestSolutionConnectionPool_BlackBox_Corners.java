package com.seraj.interview.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;

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
public class TestSolutionConnectionPool_BlackBox_Corners extends
		EasyMockSupport {

	// The class under test
	private BlockingConnectionPool classUnderTest;
	@Mock
	private Connection connection;

	@Before
	public void setUp() {
		classUnderTest = new BlockingConnectionPool();
	}

	/**
	 * Verify that an {@link IllegalArgumentException} is thrown when null is
	 * provided as the argument to
	 * {@link BlockingConnectionPool#releaseConnection(Connection)}
	 * 
	 * @throws SQLException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testReleaseConnection_null() throws SQLException {
		classUnderTest.releaseConnection(null);
	}

	// *******************************************************************
	// CORNER CASE : Not From Pool
	// Description : Releasing a connection that was not originally part of the
	// pool.

	/**
	 * With a connection pool that contains less connections than the configured
	 * max, releasing a connection not originally from the pool will add it into
	 * the pool and log the addition.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testReleaseConnection_notFromPool_belowMax()
			throws SQLException {
		replayAll();
		classUnderTest.releaseConnection(connection);
		verifyAll();

		// Verify the number of connections increased.
	}

	/**
	 * With a connection pool that has its max number of connections, releasing
	 * a connection not originally from the pool will NOT add the connection to
	 * the pool but will rather close the connection and log the case.
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testReleaseConnection_notFromPool_atMax() throws SQLException {
		connection.close();
		replayAll();

		classUnderTest.releaseConnection(connection);
		verifyAll();

		// Verify the number of connections does not increase.
	}

	// CORNER CASE : Not From Pool
	// *******************************************************************

}
