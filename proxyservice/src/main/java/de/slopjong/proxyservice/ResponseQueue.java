package de.slopjong.proxyservice;

import java.util.ArrayList;
import java.util.logging.Logger;

public class ResponseQueue
{
	private ArrayList<ArrayList<String>> triples;
	private int max;
	private ProxyService proxy;
	
	static Logger logger = Logger.getLogger("de.slopjong.proxyservice.ResponseQueue");
	
	public ResponseQueue(int max, ProxyService proxy)
	{
		logger.info("Created response queue");
		this.max = max;
		this.proxy = proxy;
		this.triples = new ArrayList<ArrayList<String>>();
	}
	
	public void add(ArrayList<String> triple)
	{
		triples.add(triple);
		if (max == triples.size())
			proxy.responseReady(this);
	}
	
	public ArrayList<ArrayList<String>> responseList()
	{
		return triples;
	}
}
