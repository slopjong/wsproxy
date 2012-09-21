package de.slopjong.proxyclient;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
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
        RPCServiceClient client;
        
		String endpoint = "http://localhost:8080/services/ProxyService";
		String argument = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><apply><times/><cn>13</cn><cn>2</cn></apply></math>";
    	String action = "urn:calculate";
    	
		try {
			client = createServiceClient(endpoint);
		} catch (AxisFault e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
    	
        // setting the soap action
        Options opts = client.getOptions();
        opts.setAction(action);
        
        // create the method call object for the actual web service
        OMFactory factory = OMAbstractFactory.getOMFactory();
    	OMNamespace omNs = factory.createOMNamespace("http://mathservice.slopjong.de", "ns"); 
    	OMElement method = factory.createOMElement("calculate", omNs); 
    	OMElement value = factory.createOMElement("math", omNs); 
    	value.setText(argument);
    	method.addChild(value);
    	
    	opts.setTo( new EndpointReference(endpoint) );
        
    	Object proxy_args[] = null;
    	
        try
        {        	     
        	OMElement result = client.invokeBlocking(new QName("execute"), proxy_args);
        }
        catch ( AxisFault e )
        {
        	e.printStackTrace();       
        }
	}

    /**
     * Creates a service client which allows 20 parallel connections.
     * 
     * @return
     * @throws AxisFault
     */
    private static RPCServiceClient createServiceClient(String endpoint)
	throws AxisFault
	{
		try
        {
        	RPCServiceClient client = new RPCServiceClient();
        	client.getOptions().setTo(new EndpointReference(endpoint));
        	
        	//client.getOptions().setProperty(HTTPConstants.REUSE_HTTP_CLIENT, "true");
        	 	
        	// See [0]
        	ConfigurationContext configurationContext = client.getServiceContext()
        														.getConfigurationContext();

        	// increase the MAX possible parallel connections
        	MultiThreadedHttpConnectionManager conmgr = new MultiThreadedHttpConnectionManager();
        	conmgr.getParams().setDefaultMaxConnectionsPerHost(20);
        	HttpClient httpclient = new HttpClient(conmgr);
        	configurationContext.setProperty(HTTPConstants.CACHED_HTTP_CLIENT, httpclient);
			
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
