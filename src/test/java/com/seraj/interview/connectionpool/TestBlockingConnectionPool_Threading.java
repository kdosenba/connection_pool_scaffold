package com.seraj.interview.connectionpool;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class TestBlockingConnectionPool_Threading extends EasyMockSupport {

	// The class under test
	private BlockingConnectionPool classUnderTest;
	@Mock
	private ConnectionFactory mockFactory;
	@Mock
	private Connection mockConnection;
	private static final int VALIDATION_TIMEOUT_VALUE = 3342;
	private Callable<Connection> threadGetTask = new Callable<Connection>() {

		@Override
		public Connection call() throws Exception {
			return classUnderTest.getConnection();
		}
	};

	/**
	 * Initialize the pool to max size of 1.
	 */
	@Before
	public void setUp() {
		Properties properties = new Properties();
		properties.setProperty("maxPoolSize", "" + 1);
		properties.setProperty("validationTimeoutInSeconds", ""
				+ VALIDATION_TIMEOUT_VALUE);
		classUnderTest = new BlockingConnectionPool(properties);
		classUnderTest.setConnectionFactory(mockFactory);
	}

	/**
	 * Test that a Thread will block if there are no available connections in a
	 * size bound pool. This also tests that the pool can be bound in size.
	 * 
	 * @throws SQLException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testGetConnection_blockWhenNotAvailable() throws SQLException,
			InterruptedException, ExecutionException {
		expect(mockFactory.newConnection()).andReturn(mockConnection);
		expect(mockConnection.isValid(VALIDATION_TIMEOUT_VALUE))
				.andReturn(true);
		replayAll();

		Connection connection = classUnderTest.getConnection();
		assertSame("The expected conneciton was not returned.", mockConnection,
				connection);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Connection> taskFuture = executor.submit(threadGetTask);
		Thread.sleep(1000);
		assertFalse("The second call did not block.", taskFuture.isDone());

		classUnderTest.releaseConnection(connection);
		Connection secondConnection = taskFuture.get();

		verifyAll();
		assertSame("The connection was not recycled.", connection,
				secondConnection);
	}
}
