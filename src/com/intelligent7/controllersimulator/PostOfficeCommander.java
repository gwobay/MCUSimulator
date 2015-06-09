package com.intelligent7.controllersimulator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;

//import com.kou.utilities.ChatMaster;
//import com.kou.utilities.MainServer;

public class PostOfficeCommander {
	
	
	//private static final Executor threadPool = Executors.newFixedThreadPool(5);

	public static void readFromResourceFile(String fileName, HashMap<String, String> params)
	{
		  
	  BufferedReader reader=null;
	  try {
		  reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
	  
		  String aLine;
		  while ((aLine=reader.readLine())!=null)
		  {
			  if (aLine.length() < 2) continue;
			  int i0=0; while (aLine.charAt(i0)<= ' ') i0++;
			  int iE=i0; while (++iE<aLine.length() && aLine.charAt(iE) > ' ');
			  String key=aLine.substring(i0, iE).toUpperCase();
			  i0=iE; 
			  while (aLine.charAt(i0)!= '=' && ++i0 <aLine.length());
			  iE=++i0; 
			  while (++iE<aLine.length() && aLine.charAt(iE) > ' ');
			  String value=aLine.substring(i0, iE);//no case change .toUpperCase();
			  if (key.length() > 0)
				  params.put(key, value);
		  }
	    
	  } catch (IOException e) {
		  e.printStackTrace();
	    System.out.println("Could not read file "+fileName);
	  } finally {
	    try {
	      reader.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	System.out.println("Exception closing "+fileName);
	    }
	  }
	    
	}
	
	private static HashMap<String, String> config=new HashMap<String, String>();
	  
	MainServer mServer; //for receive command from commander
	//SendGcmMessager mGcmSender; //for message to GCM
	ChatMaster mChatMaster; 
	void init()
	{
		//mGcmSender=new SendGcmMessager();
		
		readFromResourceFile("CommanderResource", config);
		mServer=new MainServer();
		String sPort=config.get("CMD_PORT");
		if (sPort == null) System.exit(-1);
		mServer.setPort(Integer.parseInt(sPort));
		mServer.setStopTime(0);
		//mServer.addFIXDataSniffer(mGcmSender);
		mServer.start();
		//mServer=new MainServer();
		sPort=config.get("CHAT_PORT");
		if (sPort != null) {
			if (mChatMaster==null) mChatMaster=new ChatMaster();
			mChatMaster.setPort(Integer.parseInt(sPort));
			mChatMaster.setStopTime(0);
			mChatMaster.start();
		}
	}

	void joinThreads()
	{
		try {
		if (mServer != null) mServer.join();
		//if (mGcmSender != null) mGcmSender.join();
		} catch (InterruptedException e){
			e.printStackTrace();
		}
		
		mServer.setStopTime(1);
		//mGcmSender.setStopTime(1);
	}

	public static void main(String[] args) {
		PostOfficeCommander aCmd=new PostOfficeCommander();
		aCmd.init();
		aCmd.joinThreads();		
	}

}
