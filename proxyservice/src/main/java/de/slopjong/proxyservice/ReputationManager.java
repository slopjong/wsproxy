package de
.slopjong.proxyservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class ReputationManager 
{
	public ArrayList<String> getEndpoints(String porttype)
    {	
    	// See [0] for the sqlite driver usage
    	
    	// load the sqlite-JDBC driver using the current class loader
        try 
        {
			Class.forName("org.sqlite.JDBC");
		} 
        catch (ClassNotFoundException e1) 
		{
			e1.printStackTrace();
		}
        
        Connection connection = null;
        try
        {
			// create a database connection
			connection = DriverManager.getConnection("jdbc:sqlite:store.db");
			  
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);  // set timeout to 30 sec.
			  
			ResultSet rs = statement.executeQuery("select * from services where porttype='" + porttype + "'");
	      	
			ArrayList<String> endpoints = new ArrayList<String>();
	      	
			// this initializes a reversed list
	      	while(rs.next())
				endpoints.add(rs.getString("endpoint"));
	      	
	      	return endpoints;
        }
        catch(SQLException e)
        {
          // if the error message is "out of memory", 
          // it probably means no database file is found
          System.err.println(e.getMessage());
        }
        finally
        {
          try
          {
            if(connection != null)
              connection.close();
          }
          catch(SQLException e)
          {
            // connection close failed.
            System.err.println(e);
          }
        }
        
        return null;
    }
}

/*
 [0] http://www.xerial.org/trac/Xerial/wiki/SQLiteJDBC#Usage
*/