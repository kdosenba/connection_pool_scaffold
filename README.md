# Opower Connection Pool Interview Solution

This is my (Seraj Dosenbach) solution to the interview challenge given by Opower.

## Using this project

The basic structure of the project is as was provided. I modified the pom.xml to use java 1.7 and EasyMock 3.3.1. 

    mvn compile      # compiles your code in src/main/java
    mvn test-compile # compile test code in src/test/java
    mvn test         # run tests in src/test/java for files named Test*.java


[maven]:http://maven.apache.org/

## About the solution

The solution follows the bean design approach. The approach was chosen to simplify testing and to focus on 
the algorithm at hand, connection pooling, rather than the multitudinous facets of creating connections and 
the many variants on how to store the connections. Therefore, to use the connection pool in the real world 
one would have to instantiate an instance of the pool and provide, via the setters, all the required fields.

The choice of writing a blocking connection pool stemmed from two factors:
  1 - Without a connection its rather pointless to proceed with 


Since the goal was to write a connection pool, not create a connection, I used the factory design pattern to 
abstract the creation of Connections behind an interface. A user of the connection pool can then provide a 
configured connection factory that meets the needs of the system.

## Extensibility 

The classes found in the configuration package where thrown together to achieve a specific purpose. However,
a useful, more dynamic and extensible, feature can be extracted from those classes. For example, a reader
interface could be created to allow for multiple reader types each with the capacity to read from a different
source. Also the reader could become decoupled from the Annotation which it searches for by providing the 
specific annotation as a parameter to the method.

Since the focus of the project was on the connection pool algorithm, only a trivial version of the @Configuration
annotation was designed. A more robust version may take advantage of annotation fields to specify default,
min, and max values for the field being configured. The reader then can use those parameters to better
configure the object. 




Since a bean approach was used it can be cumbersome to setup a functional instance of the pool. While this 
approach is convenient in a Spring environment (or similar) with the aid of dependency injection, static 
convenience create methods can be added to the ConnectionPool interface or overloaded constructors added to 
the concrete implementation itself to ensure all necessary fields are provided when setting up a new instance
of the pool.  


