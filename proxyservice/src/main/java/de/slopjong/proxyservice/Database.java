package de.slopjong.proxyservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class Database 
{
	
	static Logger logger = Logger.getLogger("de.slopjong.proxyservice.Databaser");
	
	public Connection connection;
	
	public void disconnect()
	{
		disconnect(connection, null, null);
	}
	
	private void disconnect(Connection connection, Statement statement, ResultSet results)
	{		
        try
        {
			if(connection != null)
			{
				connection.close();
				connection = null;
			}
          
	        if(statement != null)
	        	statement.close();
	        
	        if(results != null)
	        	results.close();
        }
        catch(SQLException e)
        {
          logger.info("Could not disconnect the resources");
        }		
	}
	
	private void loadDriver()
	{
		// TODO: load the driver on service start and unload it on service shutdown
    	// load the sqlite-JDBC driver using the current class loader
		// See [0] for the sqlite driver usage
		try 
        {
			// TODO: check if the class was already loaded in another ClassLoader. 
			//            Otherwise the following error appears:
			//                         > java.lang.UnsatisfiedLinkError: 
			//                         > Native Library /tmp/sqlite-3.7.2-libsqlitejdbc.so already loaded in another classloader
			//            See [2]
			Class.forName("org.sqlite.JDBC");
		} 
        catch (ClassNotFoundException e1) 
		{
			logger.info(e1.getMessage());
		}	
	}
	
	
	private void connect() 
	{		
		try
		{
			// try to connect to the database
			connection = DriverManager.getConnection("jdbc:sqlite:store.db");
		}
        catch(SQLException e)
        {
		  // if the error message is "out of memory", 
		  // it probably means no database file is found
		  logger.info("Could not connect ");
        }	
	}
	
	/**
	 * Executes a SQL statement. Before the query is processed the SQL driver will be loaded
	 * and a new connection will be created. Finally all the resources such as statement, 
	 * resultset and connection will be freed.
	 * 
	 * @param query
	 * @return
	 */
	public List<HashMap<String,Object>> query(String query) 
	{
		// TODO: load the driver and create the connection once when the service is starting
		loadDriver();
		connect();
		
		ResultSet resultSet = null;
		Statement statement = null;
	
		// if there's no connection return an empty recordset
		if(connection == null)
		{
			logger.info("There's no database connection");
			return new ArrayList<HashMap<String,Object>>();
		}
		
        try
        {  
			statement = connection.createStatement();
			statement.setQueryTimeout(30);  // set timeout to 30 sec.
						
			resultSet = statement.executeQuery(query);
        }
        catch(SQLException e)
        {
          // if the error message is "out of memory", 
          // it probably means no database file is found
          logger.info("Could not execute the statement");
        }
		
        List<HashMap<String,Object>> ret = convertResultSetToList(resultSet);
        
        // TODO: free the connection on service shutdown
        // close the statement and resultset, closing the connection is not enough as mentioned in [4]
        disconnect(connection, statement, resultSet);
        
        return ret;
	}
	
	// borrowed from [3]
	/**
	 * Converts the ResultSet object into a list of HashMap objects.
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private List<HashMap<String,Object>> convertResultSetToList(ResultSet rs)
	{
	    ResultSetMetaData md;
	    List<HashMap<String,Object>> list = new ArrayList<HashMap<String,Object>>();
	    
		try 
		{
			md = rs.getMetaData();
			int columns = md.getColumnCount();
		 
		    while (rs.next()) {
		        HashMap<String,Object> row = new HashMap<String, Object>(columns);
		        for(int i=1; i<=columns; ++i) {
		            row.put(md.getColumnName(i),rs.getObject(i));
		        }
		        list.add(row);
		    }
		} 
		catch (SQLException e) 
		{
			logger.info("Could not convert ResultMap to List<HashMap<String,Object>>");
		}
	    
	    return list;
	}
	
}

/*  [0] http://www.xerial.org/trac/Xerial/wiki/SQLiteJDBC#Usage
 *  [2] http://stackoverflow.com/questions/482633/in-java-is-it-possible-to-know-whether-a-class-has-already-been-loaded
 *  [3] http://stackoverflow.com/a/10213258
 *  [4] http://stackoverflow.com/questions/103938/resultset-not-closed-when-connection-closed
 */