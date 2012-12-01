package de.slopjong.proxyservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Logger;

public class ReputationManager 
{
	static Logger logger = Logger.getLogger("de.slopjong.proxyservice.ReputationManager");
	
	public ReputationManager() 
	{
		logger.info("Reputation manager instantiated.'");
	}
	
	public ArrayList<String> getEndpoints(String porttype, String action)
    {
		logger.info("PortType is '"+ porttype +"' and Action is '"+ action +"'");
		
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
			
			String query =
					" SELECT endpoint FROM (SELECT endpoint,AVG(reputation) AS rep FROM deployments " +
					" INNER JOIN actions ON deployments.action_id=actions.action_id " +
					" INNER JOIN endpoints ON deployments.endpoint_id=endpoints.endpoint_id " +
					" INNER JOIN reputations ON deployments.deployment_id=reputations.deployment_id " +
					" GROUP BY endpoint " +
					" ORDER BY rep DESC) ";
			
			logger.info("Executing the SQL query: "+ query);
			
			ResultSet rs = statement.executeQuery(query);
			
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
	
	public void calculateReputation(ResponseQueue queue)
	{
		logger.info("Calculating the reputation");
	}
}

/*
 [0] http://www.xerial.org/trac/Xerial/wiki/SQLiteJDBC#Usage
*/