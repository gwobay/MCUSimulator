package com.intelligent7.controllersimulator;

import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.io.*;
import java.lang.Thread;
import java.util.logging.Logger;

public class PostOfficeServer extends Thread
{
	public PostOfficeServer()
	{
		super();
	}

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

int mPort;
long mStopTime;
Logger log;	
public PostOffice myPostOffice;
	void init()
	{
		//mGcmSender=new SendGcmMessager();
		log=Logger.getAnonymousLogger();
		readFromResourceFile("CommanderResource", config);
		String sPort=config.get("CMD_PORT");
		if (sPort == null) {
			log.warning("Missing port information");
			System.exit(-1);
		}
		setPort(Integer.parseInt(sPort));
		setStopTime(0);
		
		myPostOffice=new PostOffice();
		myPostOffice.start();
		
	}

	public void setPort(int p)
	{
		mPort=p;
	}
	public void setStopTime(long t)
	{
		mStopTime=t;;
	}

	boolean stopNow;

    public void run()
    {
    	
    	long stopAt=(new Date()).getTime()+60*1000*60*8;

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(mPort);
            System.out.println("listening on port: "+mPort);
        } catch (IOException e) {
            System.err.println("Could not listen on port: "+mPort);
            System.exit(1);
        }
        
        try {
			serverSocket.setSoTimeout(5*60*1000);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        stopNow=false;
        while (!stopNow)
        {
        	Socket newUser=null;
        	try {
        			newUser = serverSocket.accept();
            } catch (SocketTimeoutException e) {
        		//stopNow=true;
        		System.err.print("alive!!");
        		//break;
        	} catch (IOException e) {
                	System.err.println("Accept failed.");
                	System.err.println(e.getMessage());
                	//if (newUser==null) continue;
                }
        	
        	if (newUser!=null){
        
	        System.out.println("Got call from "+newUser.getRemoteSocketAddress().toString());
	        
	        myPostOffice.addNewSocket(newUser);
        	}
        	
        } //while ((new Date()).getTime() < mStopTime || mStopTime==0);

                try {
                        serverSocket.close();
                        if (myPostOffice != null)
                        {
                        	myPostOffice.shutdown();
                        	myPostOffice.interrupt(); 
                        	try {
								myPostOffice.join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                        }
                } catch (IOException e) {}
        return ;
    }

    public static void main(String[] args)
    {
        long stopAt=(new Date()).getTime()+60*1000*60*8;
            	stopAt=0;
 
        PostOfficeServer aServer=new PostOfficeServer();
        aServer.init();
        aServer.start();
                
        try {           		
            aServer.join();
            //Thread.sleep(stopAt - (new Date()).getTime());
       }catch(InterruptedException e){
    	  aServer.interrupt();
    		   try { 
				aServer.join();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    	   }
       }
            	
 
}




