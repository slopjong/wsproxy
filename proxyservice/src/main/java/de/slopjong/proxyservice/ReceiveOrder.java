package de.slopjong.proxyservice;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * ReceiveOrder
 * @author slopjong
 *
 */
public class ReceiveOrder
{
	/**
	 * The list of services which successfully returned a result after the execution.
	 */
	private ArrayList<ArrayList<String>> success_list;
	
	/**
	 * The list of services which did not successfully return a result after the execution
	 * due to a network issue, a bad deployment or for some other reason
	 */
	private ArrayList<ArrayList<String>> fail_list;
	
	private int max;
	private ProxyService proxy;
	
	static Logger logger = Logger.getLogger("de.slopjong.proxyservice.ResponseQueue");
	
	public ReceiveOrder(int max, ProxyService proxy)
	{
		logger.info("Created response queue");
		this.max = max;
		this.proxy = proxy;
		this.success_list = new ArrayList<ArrayList<String>>();
		this.fail_list = new ArrayList<ArrayList<String>>();
	}
	
	/**
	 * Add the triple to the service list which executed successfully.
	 * 
	 * @param triple
	 * @return
	 */
	public boolean addGood(ArrayList<String> triple)
	{		
		return add(success_list, triple);
	}
	
	/**
	 * Add the triple to the service list which didn't execute successfully.
	 * 
	 * @param triple
	 * @return
	 */
	public boolean addBad(ArrayList<String> triple)
	{		
		return add(fail_list, triple);
	}

	/**
	 * Add the triple to the list. Checks if the total amount of services got already processed and
	 * notifies the reputation manager accordingly.
	 * 
	 * @param list
	 * @param triple
	 * @return
	 */
	private boolean add(ArrayList<ArrayList<String>> list, ArrayList<String> triple)
	{
		boolean success = list.add(triple);
		
		int amountAddedServices = success_list.size() + fail_list.size();
		
		if (max == amountAddedServices)
			proxy.responseReady(this);
		
		return success;
	}
	
	/**
	 * Returns a list of services with the "bad" ones behind the "good" ones.
	 * @return the services
	 */
	public ArrayList<ArrayList<String>> servicesList()
	{
		// create a new list and put the "bad" services at the end
		ArrayList<ArrayList<String>> services = new ArrayList<ArrayList<String>>();
		services.addAll(success_list);
		services.addAll(fail_list);
		
		return services;
	}

}
