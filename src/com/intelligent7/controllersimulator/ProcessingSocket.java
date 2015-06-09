/**
 * 
 */
package com.intelligent7.controllersimulator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author eric
 *
 */
public class ProcessingSocket extends Thread 
{

	IOSocket mySocket;
	InputStream in;
	OutputStream out;
	Vector<Byte> data;
	byte[] inDataBuffer;
	boolean zipped_channel;
	String readPage;
	int totalRead;
	int myDatabaseId;
	 InetAddress  clientAddr;
	Logger log;
	ArrayBlockingQueue<byte[]> socketOutDataQ ;
	final int Q_SIZE=200;
	Vector<FixLineListener> sniffers;
	
	/**
	 * 
	 */
	
	public ProcessingSocket(Socket clientSkt) {
		// TODO Auto-generated constructor stub
		if (clientSkt == null) return;
		mySocket = new IOSocket(clientSkt);
		clientAddr=clientSkt.getInetAddress();
		socketOutDataQ = new ArrayBlockingQueue<byte[]>(Q_SIZE, true);
		sniffers=new Vector<FixLineListener>();
		log=Logger.getAnonymousLogger();
	}
	
	public void setZippedFlag(boolean T_F) { mySocket.setZippedFlag(T_F);}

	void dropToSniffers(String fixLine)
	{
		int ix0=fixLine.indexOf("201=");
		if (ix0 < 0) return;
		for (int i=0; i< sniffers.size(); i++)
		{
			final FixLineListener aF=sniffers.get(i);
			final String dropData=fixLine;
			if (aF != null)
			{
				new Thread(new Runnable(){
					public void run() {
						aF.onNewFixData(dropData);
					}
				}).start();
				
			}
			log.info("Broadcasted-> "+fixLine);
		}
	}
	
	public void addFixSniffer(FixLineListener sniffer)
	{
		if (sniffers==null) sniffers=new Vector<FixLineListener>();
		sniffers.add(sniffer);
	}
	
	MessageParser mParser;
	boolean stopFlag;
	boolean imChatLine;
	FixDataBundle mFDB=null;
	void processData()//byte[] readData)
	{
		byte[] readData=null;
		long timeEnd=(new Date()).getTime()+3000;
		while (readData==null || readData.length < 1)
		{
			if (readData==null && mySocket.isSktClosed())
				{
				stopFlag=true;
				return;
				}
			readData=mySocket.readStreamData();
			if (readData==null && mySocket.getReadFlag() <0)
				{
					stopFlag=true;
					try {
						sleep(30*1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mySocket.close();
					break;
				}
			if ((new Date()).getTime() > timeEnd) break;
		}
		if (readData==null || readData.length < 1) return;
		
		FixDataBundle aFDB=new FixDataBundle(readData);
		mFDB=aFDB;
		log.info("got : "+aFDB.getFixLine());
		
		String command=aFDB.getCommand(201); //also the list in 220='citizen_id','citizen_id','',...
		if (command != null && command.equalsIgnoreCase("broadcast"))
		{
			String temp=aFDB.getFixLine();
			int i0=temp.indexOf("170=");
			if (i0>0) {
				temp=aFDB.getFixLine().substring(i0);
			}
			dropToSniffers(temp);
		}
		
		String table=aFDB.getCommand(170);
		if (table != null && table.equalsIgnoreCase("chatroom"))
		{
			ChatMaster.plugInChatLine(aFDB, mySocket);
			log.info("Sent to Chatroom");
			imChatLine=true; //switch in/out stream to Chatmaster's room
			stopFlag=true;
			return;
		}
		
		
		//mParser.setMessage(readData, readData.length);
		//mParser.setDbClientId(myDatabaseId);
		mParser.process(aFDB, socketOutDataQ);
		
		
		return;
		/*Vector<String> dV=mParser.getDbResponseToClient();
		for (int i=0; i<dV.size();i++)
		{
			mySocket.sendSocketText(dV.get(i));
		}*/
		
	}
	public void startChat()
	{
		
	}
	public void dumpResponse()
	{
		while (mySocket.isSktConnected() && mySocket.hasOutStream() ||
				socketOutDataQ.size() > 0)
		{			
			byte[] socketData=null;
			try {
				if (socketOutDataQ.size() > 0) 
					socketData=socketOutDataQ.poll(200, TimeUnit.MILLISECONDS);
				else
					socketData = socketOutDataQ.take();
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
			if (socketData != null)
			{
				int iTry = 0;
				String data=new String(socketData);
				while (!mySocket.sendSocketData(socketData)){
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						if (iTry++ > 10) 
							{
								log.info("failed to send :"+data);
								return;
							}
					}
				}
				log.info("resp: "+data);
			}		
		}
			
	}
	
	public void run()
	{
		mParser=new MessageParser();
		stopFlag=false;
		imChatLine=false;
		Thread respThd=new Thread(){
			public void run()
			{
				dumpResponse();
			}
		};
		
		respThd.start();
		
		while (mySocket.isSktConnected() && mySocket.hasInStream())
		{
			if (stopFlag) break;
			processData();//readBytes);
		}
		if (imChatLine)
			{
				startChat();
			}
			try {
				respThd.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (mParser != null)
			mParser.finish();
			if (mFDB != null) mFDB.cleanUp();
			if (mySocket!=null && !imChatLine)
				mySocket.close();
			System.out.println("I am done with processing and closed at "+
								DateFormat.getTimeInstance().format(new Date()));		
	
			log.info("server socket closed connection with "+clientAddr);
	}
	/**
	 * @param arg0
	 */
	public ProcessingSocket(Runnable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public ProcessingSocket(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ProcessingSocket(ThreadGroup arg0, Runnable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ProcessingSocket(ThreadGroup arg0, String arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ProcessingSocket(Runnable arg0, String arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 */
	public ProcessingSocket(ThreadGroup arg0, Runnable arg1, String arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public ProcessingSocket(ThreadGroup arg0, Runnable arg1, String arg2,
			long arg3) {
		super(arg0, arg1, arg2, arg3);
		// TODO Auto-generated constructor stub
	}

}
