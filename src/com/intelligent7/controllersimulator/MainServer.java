package com.intelligent7.controllersimulator;

import java.net.*;
import java.util.Date;
import java.util.Vector;
import java.io.*;
import java.lang.Thread;


public class MainServer extends Thread
{
	
	//static Vector<DataProcessor> allDbReader=new Vector<DataProcessor>();
	static int readerCount=0;
	//static Vector<DataProcessor> allDbProcessor=new Vector<DataProcessor>();
	static int processorCount=0;
	/* public static DataProcessor getDbProcessor()
	{
		if (allDbProcessor.size()>0)
		return allDbProcessor.get(processorCount++ % allDbProcessor.size());
		
			DataProcessor aProcessor=DataProcessor.getInstance(false);
			aProcessor.start();
			return aProcessor;
		
	}
	public static DataProcessor getDbReader()
	{
		if ( allDbReader.size() > 0)
		return allDbReader.get(readerCount++ % allDbReader.size());
		DataProcessor aProcessor=DataProcessor.getInstance(true);
		aProcessor.start();
		return aProcessor;		
	}  */
int mPort;
long mStopTime;
	public MainServer()
	{
		super();
	}
	
	public void setPort(int p)
	{
		mPort=p;
	}
	public void setStopTime(long t)
	{
		mStopTime=t;;
	}

	FixLineListener mFixSniffer=null;
    public void run()
    {
    	
    	long stopAt=(new Date()).getTime()+60*1000*60*8;
/*        for (int i=0; i<DataProcessor.MAXCOUNT; i++)
        {
        	DataProcessor aProcessor=DataProcessor.getInstance(i<4);
        	if (i<4) allDbReader.add(aProcessor);
        	else allDbProcessor.add(aProcessor);
        	aProcessor.setStopTime(stopAt);
        	aProcessor.start();
        }*/
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(mPort);
            System.out.println("listening on port: "+mPort);
        } catch (IOException e) {
            System.err.println("Could not listen on port: "+mPort);
            System.exit(1);
        }

        do
        {
        	Socket clientSocket=null;
        	try {
        			clientSocket = serverSocket.accept();
            } catch (IOException e) {
                	System.err.println("Accept failed.");
                	if (clientSocket==null) continue;
                }
        
        System.out.println("Got call from "+clientSocket.getRemoteSocketAddress().toString());
        	
        	ProcessingSocket aProcess=new ProcessingSocket(clientSocket);
        	try {
        	for (int i=0; i<sniffers.size(); i++)
        	{
        		aProcess.addFixSniffer(sniffers.get(i));
        	}
        	} catch (NullPointerException e){}
        	aProcess.start();
        } while ((new Date()).getTime() < mStopTime || mStopTime==0);

                try {
                        serverSocket.close();
                } catch (IOException e) {}
        return ;
    }

    Vector<FixLineListener> sniffers;
    public void addFIXDataSniffer(FixLineListener sniffer)
    {
    	if (sniffers==null) sniffers=new Vector<FixLineListener>();
    	sniffers.add(sniffer);
    }
    public static void main(String[] args)
    {
    	int iPort=9696;
        if (args.length > 0)
                {
                	iPort=Integer.parseInt(args[0]);
                }
        long stopAt=(new Date()).getTime()+60*1000*60*8;
            	stopAt=0;
        //Vector<DataProcessor> allDbProcessor=new Vector<DataProcessor>();
            	/*
        for (int i=0; i<DataProcessor.MAXCOUNT; i++)
        {
        	DataProcessor aProcessor=DataProcessor.getInstance(i<4);
        	if (i<4) allDbReader.add(aProcessor);
        	else allDbProcessor.add(aProcessor);
        	aProcessor.setStopTime(stopAt);
        	aProcessor.start();
        }
            	*/
        MainServer aServer=new MainServer();
        aServer.setPort(iPort);
        aServer.setStopTime(stopAt);
        aServer.start();
                
        try {
            		
            aServer.join();
            /*
            for (int i=0; i<DataProcessor.MAXCOUNT; i++)
            {
            	DataProcessor aDbP=allDbProcessor.get(i);
            	aDbP.join();
            }*/
            Thread.sleep(stopAt - (new Date()).getTime());
       }catch(InterruptedException e){}
            	//aDbProcessor.interrupt();
            	aServer.interrupt();
    }
}




