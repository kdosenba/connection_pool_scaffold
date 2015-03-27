package com.seraj.interview.connectionpool;

import org.junit.Before;

public class TestSolutionConnectionPool_Threading {

	// The class under test
	private BlockingConnectionPool classUnderTest;

	@Before
	public void setUp() {
		classUnderTest = new BlockingConnectionPool();
	}

}
