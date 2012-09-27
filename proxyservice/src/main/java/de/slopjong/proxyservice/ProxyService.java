package de.slopjong.proxyservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

/**
 * The proxy service.
 * @author Romain Schmitz
 *
 */
public class ProxyService
{	
    public OMElement execute(OMElement wsmethod, OMElement namespace, OMElement action, OMElement porttype)
    throws AxisFault
    {   
    	// replace the namespace with the correct web service namespace
    	// TODO: check if this substitution is really required. Maybe the this
    	//       is the result of a former misbehaviour of the client?
    	//       => try to set the namespace in the client
    	OMFactory factory = OMAbstractFactory.getOMFactory();
    	OMNamespace wsns = factory.createOMNamespace(namespace.getText(), "ns");
    	wsmethod.setNamespace(wsns);
    	
        ServiceClient client = createServiceClient();	
    	
    	final ArrayList<OMElement> resultList = new ArrayList<OMElement>();
    	
    	for(String endpoint : getEndpoints(porttype.getText()) )
    	{  
	        Options opts = client.getOptions();
	        opts.setTo( new EndpointReference(endpoint) );
	       
	        opts.setAction(action.getText());
	        
	        try
	        {        	     
	        	// the OMElement needs to be cloned because after the first
	        	// usage it will be damaged
		        client.sendReceiveNonBlocking(wsmethod.cloneOMElement(), new AxisCallback() {
					
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
		        		
		        		resultList.add(methodReturn);
		        	}
				});
		       
		        client.cleanupTransport();
		        //client.cleanup();
	        }
	        catch ( AxisFault e )
	        {
	        	throw new AxisFault( "Something went wrong while executing the service and cleaning up:" + e.getMessage() );       
	        }
	        
    	}
    	
        // TODO: how to do a passive event loop?
        while(resultList.isEmpty())
        {
        	try
        	{
        		Thread.currentThread().sleep(1);
    		}
    		catch(Exception e)
    		{
    			e.printStackTrace();
    		}
        }
        
        return resultList.get(0);
    }
    
    /**
     * Creates a service client which allows 20 parallel connections.
     * 
     * @return
     * @throws AxisFault
     */
    private ServiceClient createServiceClient()
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
    
    private ArrayList<String> getEndpoints(String porttype)
    {	
    	// See [1] for the sqlite driver usage
    	
    	// load the sqlite-JDBC driver using the current class loader
        try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
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
 [0] http://axis.apache.org/axis2/java/core/docs/http-transport.html#setting_cached_httpclient_object
 [1] http://www.xerial.org/trac/Xerial/wiki/SQLiteJDBC#Usage
*/