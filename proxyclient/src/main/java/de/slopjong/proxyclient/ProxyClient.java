package de.slopjong.proxyclient;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.rpc.client.RPCServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

/**
 * The proxy client.
 * @author Romain Schmitz
 *
 */
public class ProxyClient
{	
	public static void main(String args[])
	{
        ServiceClient client;
        
        // proxy relevant data
		String endpoint = "http://localhost:8080/services/ProxyService";
    	String action = "urn:execute";
    	
    	// payload of the actual web service
    	String argument = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><apply><times/><cn>13</cn><cn>2</cn></apply></math>";
    	
		try {
			client = createServiceClient();
		} catch (AxisFault e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
    	
        // setting the soap action
        Options opts = client.getOptions();
        opts.setAction(action);
        opts.setTo( new EndpointReference(endpoint) );
        
        OMFactory factory = OMAbstractFactory.getOMFactory();
        
        // create the method call object for the proxy web service 
        OMNamespace proxyNs = factory.createOMNamespace("http://proxyservice.slopjong.de", "ns");
        OMElement executeMethod = factory.createOMElement("execute", proxyNs);
    	OMElement method = createPayload(argument, factory, proxyNs);
    	executeMethod.addChild(method);
    	
    	Object proxy_args[] = null;
    	
        try
        {        	     
        	OMElement result = client.sendReceive(method);
        	System.out.println(result.getFirstElement().toString());
        }
        catch ( AxisFault e )
        {
        	e.printStackTrace();       
        }
	}

	private static OMElement createPayload(String argument, OMFactory factory,
			OMNamespace proxyNs) 
	{
		OMElement payload = factory.createOMElement("payload", proxyNs);
		
		// create the method call for the actual web service 
    	OMElement method = factory.createOMElement("calculate", null); 
    	
    	// the web service argument
    	OMElement value = factory.createOMElement("math", null);
    	value.setText(argument); 
    	method.addChild(value);
    	payload.addChild(method);
    	
    	// the web service namespace
    	OMElement wsns = factory.createOMElement("namespace", null); 
    	wsns.setText("http://mathservice.slopjong.de"); 
    	payload.addChild(wsns);
    	
    	// the action of the web service called by the proxy
    	OMElement saction = factory.createOMElement("action", null); 
    	saction.setText("urn:calculate"); 
    	payload.addChild(saction);
    	
    	OMElement portType = factory.createOMElement("porttype", null);
    	portType.setText("MathServicePortType");
    	payload.addChild(portType);
    	
		return payload;
	}

    /**
     * Creates a service client which allows 20 parallel connections.
     * 
     * @return
     * @throws AxisFault
     */
    private static ServiceClient createServiceClient()
	throws AxisFault
	{
		try
        {
        	ServiceClient client = new ServiceClient();
        	
        	//client.getOptions().setProperty(HTTPConstants.REUSE_HTTP_CLIENT, "true");
        	 	
        	// See [0]
        	ConfigurationContext configurationContext = client.getServiceContext()
        														.getConfigurationContext();

        	// increase the MAX possible parallel connections
        	//*
        	MultiThreadedHttpConnectionManager conmgr = new MultiThreadedHttpConnectionManager();
        	conmgr.getParams().setDefaultMaxConnectionsPerHost(20);
        	HttpClient httpclient = new HttpClient(conmgr);
        	configurationContext.setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpclient);
        	//*/
			
        	return client;
        }
        catch ( AxisFault e )
        { 
        	throw new AxisFault( "Could not create the service client. Reason: "+ e.getMessage() );
        }
	}
}

/*
 [0] http://axis.apache.org/axis2/java/core/docs/http-transport.html#setting_cached_httpclient_object
*/
