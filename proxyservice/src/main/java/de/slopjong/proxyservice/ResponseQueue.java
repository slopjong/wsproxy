package de.slopjong.proxyservice;

import java.util.ArrayList;

public class ResponseQueue
{
	private ArrayList<ArrayList<String>> triples;
	private int max;
	private ProxyService proxy;
	
	public ResponseQueue(int max, ProxyService proxy)
	{
		System.out.println("Created response queue");
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
