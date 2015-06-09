/**
 * 
 */
package com.intelligent7.controllersimulator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
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
public class ChatProcessingSocket extends Thread 
{

	IOSocket mySocket;
	InputStream inChannel;
	OutputStream outChannel;
	Vector<Byte> data;
	//byte[] inDataBuffer;
	//boolean zipped_channel;
	//String readPage;
	//int totalRead;
	//int myDatabaseId;
	 InetAddress  clientAddr;
	Logger log;
	//ArrayBlockingQueue<byte[]> socketOutDataQ ;
	//final int Q_SIZE=200;
	//Vector<FixLineListener> sniffers;
	/**
	 * 
	 */
	public ChatProcessingSocket(Socket clientSkt) {
		// TODO Auto-generated constructor stub
		if (clientSkt == null)
			{
			mySocket=null;
			return;
			}
		mySocket = new IOSocket(clientSkt);
		try {
			mySocket.mSocket.setSoTimeout(1000);
		} catch (SocketException e) {
			e.printStackTrace();
			System.out.println("Failed to set socket timeout "+
						e.getMessage());
		}
		clientAddr=clientSkt.getInetAddress();
		//socketOutDataQ = new ArrayBlockingQueue<byte[]>(Q_SIZE, true);
		//sniffers=new Vector<FixLineListener>();
		log=Logger.getAnonymousLogger();
	}
	
	public void setZippedFlag(boolean T_F) { mySocket.setZippedFlag(T_F);}

	boolean stopFlag;
	boolean imChatLine;
	FixDataBundle mFDB=null;
	void processData()//byte[] readData)
	{
		byte[] readData=null;
		long timeEnd=(new Date()).getTime()+5000;
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
		
		String table=aFDB.getCommand(170);
		if (table != null && table.equalsIgnoreCase("chatroom"))
		{
			//when comes in this line must be invited
			//new open chat room request should come in main
			//server port
			//this line only handle (all the following has room number
			//			1. invitation request
			//          2. invitation response
			//			3. join request by supervisor
			//          4. broadcasting by supervisor
			//  so line should be plugged in directly to chat room host
			//confirm the new join is legal 
			//(1) get room opener cid and use it to find the relation
			//(2) call room host's newChatter listener
			//
			String command=aFDB.getCommand(35);
			if (command.equalsIgnoreCase("broadcast"))
			{
				ChatMaster.broadCastBossMsg(aFDB.fixLine);
			}
			String room=aFDB.getCommand(34);
			if (room == null) {
				log.warning("Missing room number for chat request");
						return;
			}
			int roomNumber=Integer.parseInt(room);
			ChatHost aHost=ChatMaster.getChatRoom(roomNumber);
			if (aHost.hasPermission(aFDB.getCommand(186)))
			{
				aHost.addChatter(aFDB, mySocket);	
				//
				//ChatMaster.plugInChatLine(aFDB, mySocket);
				log.info("Sent to Chatroom #"+room);
			}
			imChatLine=true; //switch in/out stream to Chatmaster's room
			//stopFlag=true;
			//return;
		}
		stopFlag=true;
		
		return;
		
	}
	
	public void run()
	{
		stopFlag=false;
		imChatLine=false;
		
		while (mySocket.isSktConnected() && mySocket.hasInStream())
		{
			if (stopFlag) break;
			processData();//readBytes);
		}
		
		if (imChatLine)
		{
			try {
				sleep(3*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			if (mFDB != null) mFDB.cleanUp();
			if (mySocket!=null && !imChatLine)
				mySocket.close();
			System.out.println("I am done with processing and closed at "+
								DateFormat.getTimeInstance().format(new Date()));		
			if (!imChatLine)
			log.info("server socket closed connection with "+clientAddr);
	}
	/**
	 * @param arg0
	 */
	public ChatProcessingSocket(Runnable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public ChatProcessingSocket(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ChatProcessingSocket(ThreadGroup arg0, Runnable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ChatProcessingSocket(ThreadGroup arg0, String arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public ChatProcessingSocket(Runnable arg0, String arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 */
	public ChatProcessingSocket(ThreadGroup arg0, Runnable arg1, String arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public ChatProcessingSocket(ThreadGroup arg0, Runnable arg1, String arg2,
			long arg3) {
		super(arg0, arg1, arg2, arg3);
		// TODO Auto-generated constructor stub
	}

}
