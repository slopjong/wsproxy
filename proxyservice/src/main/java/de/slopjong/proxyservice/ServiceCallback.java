package de.slopjong.proxyservice;

import java.util.ArrayList;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;

public class ServiceCallback
implements AxisCallback
{	
	private ArrayList<String> triple;
	private ArrayList<OMElement> resultList;
	private ResponseQueue queue;
	
	public ServiceCallback(String porttype, String action, String endpoint, ArrayList<OMElement> results, ResponseQueue queue)
	{
		triple = new ArrayList<String>();
		triple.add(porttype);
		triple.add(action);
		triple.add(endpoint);
		
		resultList = results;
		this.queue = queue;
	}
	
	public void onComplete()
	{
		System.out.println("complete");
	}

	public void onError(Exception arg0) 
	{
		System.out.println("error");
	}

	public void onFault(MessageContext arg0) 
	{		        		
		System.out.println("fault");
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
		
		queue.add(triple);
		resultList.add(methodReturn);
	}
}
