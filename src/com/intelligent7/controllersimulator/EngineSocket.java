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
import java.util.HashMap;
import java.util.Iterator;

import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author eric
 *
 */
public class EngineSocket extends Thread 
{
//this socket always has id either for SIM or PHONE
	//an instance is created whenever server accept
	//an client socket which is aliased as mySocket
	//then spawn a writeThread (which will poll the outboundQ,
	//this thread (act as readThread) then loop to read and dump data to peer;
	//then join write thread
	//socket owner should take care of data processing
	
	String myName;
	SimpleSocket mySocket;
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
	ArrayBlockingQueue<String> socketOutDataQ ;
	ArrayBlockingQueue<String> socketInDataQ ;
	HashMap<String, ArrayBlockingQueue<String> > friendQ ;
	PostOffice myPostOffice;
	final int Q_SIZE=20;


	Vector<DataUpdateListener> sniffers;
	
	/*
	 * interface for owner to update my name
	 * like a tag
	 * this is for realtime staff
	 * as of now, Post Office scheme will be used for this
	 */
	public interface DataUpdateListener //must be a data switch board
	{
		public void engineSocketSignOn(String name, EngineSocket who);
		public void engineSocketQuit(String who);
		public void engineSocketAddPeers(String controller, String phones);
		public void peerSocketDataReady(String myName, String data);// later if for byte[] data);
		public void peerSocketDataReady(String myName, String data, String peerName);// later if for byte[] data);
	}


	
	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private EngineSocket(){};
	
	public EngineSocket(Socket clientSkt) {
		// TODO Auto-generated constructor stub
		if (clientSkt == null) return;
		myName=null;
		mySocket = new SimpleSocket(clientSkt);
		clientAddr=clientSkt.getInetAddress();
		socketInDataQ = new ArrayBlockingQueue<String>(Q_SIZE, true);
		//socketOutDataQ = new ArrayBlockingQueue<String>(Q_SIZE, true);
		friendQ=new HashMap<String, ArrayBlockingQueue<String> >();

		sniffers=new Vector<DataUpdateListener>();
		log=Logger.getAnonymousLogger();
	}
	
	public void setOutDataQ(ArrayBlockingQueue<String> newQ)
	{
		socketOutDataQ=newQ;
	}
	
	public void setMyPostOffice(PostOffice new1)
	{
		myPostOffice=new1;
	}	
	
	void setFriendMailBox()
	{
		
	}
	
	void doFirstSignOn(String info){ //info= "phone1, phone2!controller"
		String[] users=info.split("!");
		myName=users[0];
		if (users.length == 1)
		{
			socketOutDataQ=myPostOffice.getMailBox(info);
			startWriteThread();
			//no need to wake-up post, the mail for MCU is always in its box
			String temp=myPostOffice.getPhones(myName);
			if (temp==null) return;
			String[] phones=temp.split(",");
			for (int i=0; i<phones.length; i++){
				friendQ.put(phones[i], myPostOffice.getMailBox(phones[i]));
			}		
			return;
		}
		myPostOffice.updateRelationBook(users[1],  users[0]);			
		String[] phones=users[0].split(",");
		myName=phones[0];
		socketOutDataQ=myPostOffice.getMailBox(myName);	
		startWriteThread();
		friendQ.put(users[1], myPostOffice.getMailBox(users[1]));
		//have to wake-up PostMan to deliver unknown receiver mail to me 
		myPostOffice.interrupt();				
	}
	
	public void setZippedFlag(boolean T_F) { mySocket.setZippedFlag(T_F);}

	void dropToPeer(String fixLine)
	{
		for (int i=0; i< sniffers.size(); i++)
		{
			final DataUpdateListener aF=sniffers.get(i);
			final String dropData=fixLine;
			if (aF != null)
			{
				new Thread(new Runnable(){
					public void run() {
						aF.peerSocketDataReady(myName, dropData);
					}
				}).start();
				
			}
			//log.info("Broadcasted-> "+fixLine);
		}
	}




	void dropToPeer(String receiver, String fixLine)
	{
		for (int i=0; i< sniffers.size(); i++)
		{
			final DataUpdateListener aF=sniffers.get(i);
			final String dropData=fixLine;
			final String toWhom=receiver;
			if (aF != null)
			{
				new Thread(new Runnable(){
					public void run() {
						aF.peerSocketDataReady(myName, dropData, toWhom);
					}
				}).start();
				
			}

		}
	}

	void addMyPeers(String controller, String phones)
	{
		for (int i=0; i< sniffers.size(); i++)
		{
			final DataUpdateListener aF=sniffers.get(i);
			final String car_controller=controller;
			final String peers=phones;
			if (aF != null)
			{
				new Thread(new Runnable(){
					public void run() {
						aF.engineSocketAddPeers(car_controller, peers);
					}
				}).start();
				
			}
		}
	}
	void updateMyName()
	{		
		for (int i=0; i< sniffers.size(); i++)
		{
			final DataUpdateListener aF=sniffers.get(i);
			final String dropData=myName;

			final EngineSocket me=this;

			if (aF != null)
			{
				new Thread(new Runnable(){
					public void run() {
						aF.engineSocketSignOn(dropData, me);
					}
				}).start();
				
			}
			log.info("Got socket name -> "+myName);
		}
	}
	public void addSwitchServer(DataUpdateListener sniffer)
	{
		if (sniffers==null) sniffers=new Vector<DataUpdateListener>();
		sniffers.add(sniffer);
	}
	
	//------------ socket data processing -----------
	void putMsgInFriendQ(String msg){
		Iterator<String> itr=friendQ.keySet().iterator();
		while (itr.hasNext())
		{
			String key=itr.next();
			ArrayBlockingQueue<String> aFriend=friendQ.get(key);
			if (aFriend != null){
				aFriend.add(msg);
			}
		}
	}
	//------------------------------------------------
	void startWriteThread()
	{
		writeThread=new Thread(){
			public void run()
			{
				dumpResponse();
			}
		};
		writeThread.start();
	}

	
	boolean stopFlag;

	void processData()//byte[] readData)
	{
		if (mySocket.isSktClosed())
		{
			stopFlag=true;
			return;
		}
		byte[] readData=null;
		long timeEnd=(new Date()).getTime()+3000;
		while (readData==null || readData.length < 1)
		{
			if (readData==null && mySocket.isSktClosed())
				{
				stopFlag=true;
				return;
				}
			readData=mySocket.getStreamData();
			if (readData==null && mySocket.getReadFlag() <0)
				{
					stopFlag=true;
					try {
						sleep(1*1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//mySocket.close(); //maybe still writing
					break;
				}
			if ((new Date()).getTime() > timeEnd) break;
		}
		if (readData==null || readData.length < 1) return;
		
		String sData=new String(readData);
		log.info("got : "+sData);
				//format "msg"+"<sender, backup-sender!receiver>$" i.e., "...<phone1, phone2!receiver>" from phone 
		//and only <sim number> from control
		int iUser=sData.indexOf('<');
		int idx=sData.indexOf('>');
		if (iUser < 0 || idx < 0){
			System.out.println(sData+" !!Bad data format: missing '<', unknown sender");
			return;
		}
		
		if (sData.charAt(0)=='<')
		{
			String name=sData.substring(1,  idx);
			if (myName==null){
				doFirstSignOn(name);
			}
				return;
		} 

			int i0x=iUser;
			String sender=sData.substring(i0x+1,  idx);
			
			if (myName==null){
				doFirstSignOn(sender);
			}
			//time zone must be set in server
			
			String msgToMcu=(sData.substring(0, i0x)+"<"+myName+">$");
			String msgToApp=(sData.substring(0, i0x)+"@"+(new Date()).getTime()+"<"+myName+">$");
			
			idx=sender.indexOf('!');
			if (idx < 0) {
				//dropToPeer(msg);
				if (friendQ.size()==0)
				{
					myPostOffice.putNewMail(new PostOffice.UnknowReceiverMail(myName, msgToApp));
					return;
				}
				putMsgInFriendQ(msgToApp);
			}
			else {
				//ArrayBlockingQueue<String> myFriendQ=null;
				String myFriend=sender.substring(idx+1);
				if (friendQ.size()==0) friendQ.put(myFriend, myPostOffice.getMailBox(myFriend));
				friendQ.get(myFriend).add(msgToMcu);				
				//dropToPeer(sender.substring(idx+1), msg);
			}

			//dropToPeer(sData);
		

		/*if (sData.charAt(0)=='<')
		{
			int idx=sData.indexOf('>');
			if (idx < 0) 
				{
				System.out.println("Bad data format: unknown sender");
				return;
				
				}
			String name=sData.substring(1,  idx);
			if (myName==null){
				myName=name;
				updateMyName();
			}
		} else {
			int i0x=sData.indexOf('<');
			int idx=sData.indexOf('>');
			if (i0x < 0 || idx < 0) 
			{
				System.out.println("Bad data format: unknown sender");
				return;
			}
			String sender=sData.substring(i0x,  idx);
			idx=sender.indexOf('-');
			if (idx < 0) dropToPeer(sData.substring(0,  i0x)+"$");
			else {
				String receiver=sender.substring(idx+1);
				dropToPeer(receiver, sData.substring(0,  i0x)+"$");
			}

			dropToPeer(sData);
		}
		*/
		//log.info("got : "+sData);


		
	}
	public void startChat()
	{
		
	}
	
	public void putOutBoundMsg(String msg)
	{
		if (socketOutDataQ.size() > Q_SIZE){
			System.out.println("Warning : too many msg in my Q");
			return;
		}
		try {
			socketOutDataQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//inbound msg is Qed in the switchBoard who has Hash<String, Vector<String>>
	//when Qed will be dropped to the new EngineSocket when updateName is called
	
	public void dumpResponse()
	{
		boolean hasMore=true;
		while (mySocket.isSktConnected() && mySocket.hasOutStream() ||
				socketOutDataQ.size() > 0)
		{			
			String socketData=null;
			try 
			{
				if (socketOutDataQ.size() > 0) 
				{
					hasMore=true;
					socketData=socketOutDataQ.poll(100, TimeUnit.MILLISECONDS);
				}
				else 
				{
					hasMore=false;
					socketData = socketOutDataQ.poll(3000, TimeUnit.MILLISECONDS);//socketOutDataQ.take();
					}
			} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();					
				}
			
			if (socketData != null)
			{
				int iTry = 0;
				hasMore=true;
				String data=new String(socketData);
				while (!mySocket.sendText(socketData)){
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						if (mySocket.isClosed() || iTry++ > 10) 
							{
								log.info("failed to send :"+data);
								return;
							}
					}
				}
				log.info("resp: "+data);
			}
			if (!hasMore) return;//should send a bye signal out
		}
			
	}
	
	Thread writeThread;
	public void run()
	{
		writeThread=null;
		//mParser=new MessageParser();
		stopFlag=false;
		//imChatLine=false;		
				
		while (mySocket.isSktConnected() && mySocket.hasInStream())
		{
			if (stopFlag) break;
			processData();//readBytes);
		}

		if (writeThread != null)
			try {
				writeThread.join();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//if (mParser != null)
			//mParser.finish();
			//if (mFDB != null) mFDB.cleanUp();
			if (mySocket!=null )//&& !imChatLine)
				mySocket.close();
			System.out.println("I am done with processing and closed at "+
								DateFormat.getTimeInstance().format(new Date()));		
	
			log.info("server socket closed connection with "+clientAddr);
	}
	/**
	 * @param arg0
	 */
	public EngineSocket(Runnable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public EngineSocket(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public EngineSocket(ThreadGroup arg0, Runnable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public EngineSocket(ThreadGroup arg0, String arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public EngineSocket(Runnable arg0, String arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 */
	public EngineSocket(ThreadGroup arg0, Runnable arg1, String arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public EngineSocket(ThreadGroup arg0, Runnable arg1, String arg2,
			long arg3) {
		super(arg0, arg1, arg2, arg3);
		// TODO Auto-generated constructor stub
	}

}
