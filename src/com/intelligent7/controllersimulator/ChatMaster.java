package com.intelligent7.controllersimulator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class ChatMaster extends Thread {

	/* accept fixLine and socket's in/out stream
	/* check fixLine if room number is given
	 * if none then check if member (cid and regid)
	 * 							if yes open new chat host with room number;
	 * if has room number, forward to chat host
	 */
	
	private static class Envelope
	{
		FixDataBundle instructions;
		IOSocket dropLine;
		
		public Envelope(FixDataBundle inst, IOSocket skt)
		{
			instructions=inst;
			dropLine=skt;//new IOSocket(skt);
		}
	}
	
	static ArrayBlockingQueue<Envelope> dropBox=new ArrayBlockingQueue<Envelope>(10, true);
	static boolean isMember(String fixLine)
	{
		//get cid and regid
		return true;
	}
	static WriteLock wLock;
	public static void plugInChatLine(FixDataBundle data, IOSocket newSkt)
	{
		
			try {
				dropBox.put(new Envelope(data, newSkt ));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
/*			if (room < 0)
			{
				if (!isMember(fixLine)) return;
				ChatHost aRoom=new ChatHost(fixLine, newSkt);
				aRoom.start();
				return;
			}
			ChatHost aRoom=ChatHost.getChatRoom(room);
			aRoom.addChatter(fixLine, newSkt);*/
		
		return;
	}
	
	static int roomNumber=0;
	
	static HashMap<String, ChatHost> openRoom=new HashMap<String, ChatHost>();
	public static ChatHost getChatRoom(int room)
	{
		if (openRoom.size() < 1) return null;
		return openRoom.get("Room"+room);
	}
	
	public static void broadCastBossMsg(String fixLine)
	{
		Iterator<String> itr=openRoom.keySet().iterator();
		while (itr.hasNext())
		{
			String key=itr.next();
			ChatHost aHost=openRoom.get(key);
			aHost.broadCastBossMsg(fixLine);			
		}
	}
	public void closeChatRoom(String roomNumber)
	{
		openRoom.remove(roomNumber);
	}

	public void run()
	{
		Thread listenThread=new Thread(new Runnable(){
			public void run()
			{
				doListen();
			}
		});
		listenThread.start();
		
		long stopAt=(new Date()).getTime()+60*1000*60*24*6;

		do
		{
			try {
				Envelope aRequest =	dropBox.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}  while ((new Date()).getTime() < mStopTime || mStopTime==0);

		try {
			listenThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
int mPort;
long mStopTime;
	public ChatMaster()
	{
		super();
		mStopTime=0;
	}
	
	public void setPort(int p)
	{
		mPort=p;
	}
	public void setStopTime(long t)
	{
		mStopTime=t;;
	}

	public void doListen()
    {
    	
    	long stopAt=(new Date()).getTime()+60*1000*60*24*6;

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(mPort);
            System.out.println("chat listening on port: "+mPort);
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
        	
        	ChatProcessingSocket aChat=new ChatProcessingSocket(clientSocket);

        	aChat.start();
        } while ((new Date()).getTime() < mStopTime || mStopTime==0);

                try {
                        serverSocket.close();
                } catch (IOException e) {}
        return ;
    }

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
