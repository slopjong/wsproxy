package de.slopjong.proxyservice;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;

/**
 * The proxy service.
 * @author Romain Schmitz
 *
 */
public class ProxyService
{	
    public void execute()
    throws AxisFault
    {   
    	String argument = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><apply><times/><cn>13</cn><cn>2</cn></apply></math>";
    	//String endpoint = "http://93.92.148.20:8080/services/MathService";
    	String action = "urn:calculate";
    	
    	String endpoints[] = {
    			"http://93.92.148.20:8080/services/MathService",
    			"http://131.246.103.5:8989/services/MathService",
    			"http://get-corporate.com:8080/services/MathService"
    	};
    	
        ServiceClient client = null;
    	OMFactory factory = OMAbstractFactory.getOMFactory();
    	

    	
    	/*
    	AxisService service = new AxisService();
    	AxisEndpoint endpoint = new AxisEndpoint();
    	endpoint.setEndpointURL("http://93.92.148.20:8080/services/MathService");
    	service.addEndpoint("1", endpoint);
    	*/
    	

    	for(String endpoint : endpoints)
    	{
        	// create the method call object
        	OMNamespace omNs = factory.createOMNamespace("http://mathservice.slopjong.de", "ns"); 
        	OMElement method = factory.createOMElement("calculate", omNs); 
        	OMElement value = factory.createOMElement("math", omNs); 
        	value.setText(argument); 
        	method.addChild(value);
        	
    		System.out.println(endpoint);
    		
	        try
	        {
	        	client = new ServiceClient();
		        client.cleanupTransport();
		        client.cleanup();
	        }
	        catch ( AxisFault e )
	        { 
	        	throw new AxisFault( "Could not create the service client. Reason: "+ e.getMessage() );
	        }
	         
	        // getting & setting the endpoint reference
	        Options opts = new Options();
	        opts.setTo( new EndpointReference(endpoint) );
	        opts.setAction(action);
	        client.setOptions( opts );
	        
	        try
	        {        	        	
		        client.sendReceiveNonBlocking(method, new AxisCallback() {
					
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
		        		EndpointReference ref = arg0.getFrom();
		        		System.out.println(ref.getAddress());
		        		//String soapAction = arg0.getSoapAction(); // urn:calculateResponse
		        		//String wsaAction = arg0.getWSAAction(); // calculateResponse
		        		//Options options = arg0.getOptions();
		        		OMElement serviceResponse = arg0.getEnvelope().getBody().getFirstElement();
		        		OMElement methodReturn = serviceResponse.getFirstElement();
		        		
		        		System.out.println(methodReturn.getText());
		        	}
				});
		        
	        }
	        catch ( AxisFault e )
	        {
	        	throw new AxisFault( "Something went wrong while executing the service and cleaning up:" + e.getMessage() );       
	        }
    	}
    }
}
