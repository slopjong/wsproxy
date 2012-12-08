package de.slopjong.proxyservice;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;

public class ServiceCallback
implements AxisCallback
{	
	private ArrayList<String> triple;
	private ArrayList<OMElement> resultList;
	private ReceiveOrder order;
	
	static Logger logger = Logger.getLogger("de.slopjong.proxyservice.ServiceCallback");
	
	public ServiceCallback(String porttype, String action, String endpoint, ArrayList<OMElement> results, ReceiveOrder queue)
	{
		logger.info("Creating callback for a service invocation...");
		triple = new ArrayList<String>();
		triple.add(porttype);
		triple.add(action);
		triple.add(endpoint);
		
		resultList = results;
		this.order = queue;
	}
	
	public void onComplete()
	{
		System.out.println("complete");
		order.addGood(triple);
	}

	public void onError(Exception arg0) 
	{
		System.out.println("error");
		order.addBad(triple);
	}

	public void onFault(MessageContext arg0) 
	{		        		
		System.out.println("fault");
		order.addBad(triple);
	}

	public void onMessage(MessageContext arg0) 
	{		 
		// TODO: some of the following lines triggers onError
		//EndpointReference ref = arg0.getFrom();
		//System.out.println(ref.getAddress());
		//String soapAction = arg0.getSoapAction(); // urn:calculateResponse
		//String wsaAction = arg0.getWSAAction(); // calculateResponse
		//Options options = arg0.getOptions();
		
		OMElement serviceResponse = arg0.getEnvelope().getBody().getFirstElement();
		OMElement methodReturn = serviceResponse.getFirstElement();
		
		resultList.add(methodReturn);
	}
	
}
