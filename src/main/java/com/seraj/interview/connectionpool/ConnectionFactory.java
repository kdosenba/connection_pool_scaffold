package com.seraj.interview.connectionpool;

import java.sql.Connection;

public interface ConnectionFactory {

	/**
	 * Creates a new valid connection.
	 * 
	 * @return a new valid connection ready to use.
	 */
	public Connection newConnection();

}
