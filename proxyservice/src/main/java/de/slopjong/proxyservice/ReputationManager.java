package de.slopjong.proxyservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class ReputationManager 
{
	static Logger logger = Logger.getLogger("de.slopjong.proxyservice.ReputationManager");
	
	// the maximum amount of reputations to hold in the database
	final int maxSavedReputations = 10;
	Database database = new Database();
	
	
	public ReputationManager() 
	{
		logger.info("Instantiating the reputation manager ...'");
	}
	
	
	/**
	 * Get a list of endpoints of the given porttype and action.
	 * 
	 * @param porttype
	 * @param action
	 * @return list of Axis2 service endpoint URLs
	 */
	public ArrayList<String> getEndpoints(String porttype, String action)
    {
		logger.info("PortType is '"+ porttype +"' and Action is '"+ action +"'");

		String query =
				" SELECT endpoint FROM (SELECT endpoint,AVG(reputation) AS rep FROM deployments " +
				" INNER JOIN actions ON deployments.action_id=actions.action_id " +
				" INNER JOIN endpoints ON deployments.endpoint_id=endpoints.endpoint_id " +
				" INNER JOIN reputations ON deployments.deployment_id=reputations.deployment_id " +
				" GROUP BY endpoint " +
				" ORDER BY rep DESC) ";
		
		logger.info("Executing the SQL query: "+ query);
        
		List<HashMap<String,Object>> results = database.query(query);
		
		// a size of 0 can mean that there are no reputations
		if ( results.size() == 0)
		{
			logger.fine("No endpoints ordered by their reputation");
			
			query =
					" SELECT endpoint FROM deployments " +
					" INNER JOIN actions ON deployments.action_id=actions.action_id " +
					" INNER JOIN endpoints ON deployments.endpoint_id=endpoints.endpoint_id " +
					" GROUP BY endpoint ";
			
			results = database.query(query);
			
		}
		
		ArrayList<String> endpoints = new ArrayList<String>();
      	
		if(results != null)
      	{
			for( HashMap<String,Object> row : results)
	      	{
	      		String endpoint = row.get("endpoint").toString();
	      		endpoints.add(endpoint);	
	      		logger.fine("Endpoint added: " + endpoint);
	      	}
      	}
		else
			logger.info("The database didn't return any results for the endpoints");
      	
      	return endpoints;
    }
	
	
	/**
	 * Calculates the reputation for the passed endpoints
	 * @param queue
	 */
	public void calculateReputation(ReceiveOrder responseOrder)
	{
		logger.info("Calculating the reputation");
		
		ArrayList<ArrayList<String>> queue = responseOrder.servicesList();
		
		// See [1]
		Collections.reverse(queue);
		
		int amount = queue.size();
		
		logger.info("Got a list with " + amount + " endpoints");
		
		// iterate over the triples (porttype,action,endpoint)
		for( int rank=1; rank<=amount; rank++)
		{
			// because the list got reversed 1 means the worst rank and rank=size means the best
			float reputation = ( (float) rank) / amount;
			ArrayList<String> triple = queue.get(rank-1);
			String porttype = triple.get(0);
			String action = triple.get(1);
			String endpoint = triple.get(2);
			
			if(! setReputation(porttype, action, endpoint, reputation))
				logger.info("Reputation (" + reputation + ") for " + endpoint + " could not be set");
		}
	}
	
	/**
	 * Coordinates the insertion of the reputations. If the maximum amount of reputations, that should
	 * not exceed, has been reached the oldest reputation is deleted from the database.
	 * 
	 * @param porttype
	 * @param action
	 * @param endpoint
	 * @param reputation
	 * @return
	 */
	private boolean setReputation(String porttype, String action, String endpoint, float reputation)
	{
		if(reputation < 0 || reputation > 1)
			return false;
		
		int deploymentId = getServiceDeploymentId(porttype, action, endpoint);
		int amount = getAmountReputations(deploymentId);
		
		logger.info("Set reputation for deployment ID '" + deploymentId + 
				"' which has " + amount + " reputations");
		
		// a deployment ID of -1 which means an error
		if ( deploymentId == -1 )
			return false;
		
		if(amount == maxSavedReputations)
			removeOldestReputation(deploymentId);
		
		return saveNewReputation(deploymentId, reputation);
	}
	
	
	/**
	 * Removes the oldest reputation from the reputations table for the passed deployment ID
	 * @param deploymentId
	 */
	private void removeOldestReputation(int deploymentId)
	{
		String subquery = 
				" SELECT reputation_id FROM reputations " +
				" WHERE deployment_id=1 " +
				" ORDER BY datetime ASC " +
				" LIMIT 1; ";
		
		String query = 
				" DELETE FROM reputations " +
				" WHERE deployment_id=" + deploymentId;
		
		database.query(query);
	}

	
	/**
	 * Stores a new reputation for the passed deployment ID
	 * 
	 * @param deploymentId
	 * @param reputation
	 */
	private boolean saveNewReputation(int deploymentId, float reputation)
	{
		
		String query = 
				" INSERT INTO reputations (deployment_id,datetime,reputation) " +
				" VALUES(" + deploymentId + ",datetime('now','localtime')," + reputation + ")";
		
		database.query(query);
		
		return true;
	}
	
	
	/**
	 * Removes the oldest reputation from the reputations table for the passed deployment ID
	 * @param porttype
	 * @param action
	 * @param endpoint
	 */
	private int getServiceDeploymentId(String porttype, String action, String endpoint)
	{
		String query =
				" SELECT deployment_id FROM deployments " +
				" INNER JOIN actions ON deployments.action_id=actions.action_id " +
				" INNER JOIN endpoints ON deployments.endpoint_id=endpoints.endpoint_id " +
				" WHERE endpoint='"+ endpoint +"' AND action='"+ action +"' AND porttype='"+ porttype + "'";
		
		List<HashMap<String,Object>> results = database.query(query);
		int deploymentId = -1;
		
		// we expect exactly one row
		if(results == null)
		{
			logger.info("The database didn't return any results");
		}
		else if(results.size() == 1)
		{
			HashMap<String,Object> row = results.get(0);
			String id = row.get("deployment_id").toString();
			deploymentId = Integer.parseInt(id);
		}
		else if(results.size() > 1)
			logger.info("Only one ID was expected but got more");
		else if(results.size() == 0)
			logger.info("An ID was expected but got none");
		
		return deploymentId;
	}
	
	
	/**
	 * Returns the amount of saved reputations for the passed deployment ID
	 * @param deploymentId
	 */
	private int getAmountReputations(int deploymentId)
	{
		String query =
				" SELECT COUNT(reputation) as amount FROM reputations " +
				" WHERE deployment_id=" + deploymentId;
		
		List<HashMap<String,Object>> results = database.query(query);
		
		int amount = 0;
		
		if ( results != null && results.size() == 1 )
		{
			HashMap<String,Object> row = results.get(0);
			String id = row.get("amount").toString();
			amount = Integer.parseInt(id);
		};
		
		return amount;
	}
	
	
}

/*  [1] http://www.java-examples.com/reverse-order-all-elements-java-arraylist-example
 */