package com.seraj.interview.connectionpool;

import java.sql.Connection;

/**
 * An interface defining a factory class used to create {@link Connection}s.
 * 
 * @author Seraj Dosenbach
 *
 */
public interface ConnectionFactory {

	/**
	 * Creates a new valid connection.
	 * 
	 * @return a new valid connection ready to use.
	 */
	public Connection newConnection();

}
