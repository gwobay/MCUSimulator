package com.intelligent7.controllersimulator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//import com.volunteer.dataprocessor.BodyLocator;

public class ChatHost extends Thread {
	
/*
 * 1. assert is member (cid and regid)
 * 2. check if room number is in the messsage
 * 3. 
 */
	String myRoomNumber;
	ChatMaster chatMaster;
	
	String anchor_cid;
	String roomTitle;
	
	ReentrantReadWriteLock wrLock;
	
	int chatterCount;
	class Chatter {
		IOSocket channel;
		FixDataBundle mFixData;
		public DataInputStream inChannel;
		public DataOutputStream outChannel;
		
		public Chatter(IOSocket skt, FixDataBundle fixData)
		{
			channel=skt;
			try {
				inChannel=new DataInputStream(skt.getInputStream());
				outChannel=new DataOutputStream(skt.getOutputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mFixData=fixData;			
		}
	}
	
	void processFirstMessage()
	{
		// send invitation by notification 
		// indicate new join
		// put message to other chatter for new chatter
	}
	
	void close()
	{
		chatMaster.closeChatRoom(myRoomNumber);
	}
	
	Vector<Chatter> chatters;
	public ChatHost(FixDataBundle fixData, IOSocket skt, int roomNumber, ChatMaster mst)
	{
		chatters=new Vector<Chatter>();
		Chatter aChatter=new Chatter(skt, fixData);
		chatters.add(aChatter);
		chatterCount=1;
		myRoomNumber="Room"+roomNumber;
		chatMaster=mst;
		roomTitle="";
		String lastName=fixData.getCommand(49);
		if (lastName != null) roomTitle += lastName;
		String firstName=fixData.getCommand(50);
		if (firstName != null) roomTitle += firstName;
		anchor_cid=fixData.getCommand(186);
		wrLock=new ReentrantReadWriteLock();
	}

	public boolean hasPermission(String newCid)
	{
		//try {
			return true;// no data check staff here
						// BodyLocator.checkIfRelated(anchor_cid, newCid, 2);
		//} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
		//return false;
	}
	void broadCastNewChatterMsg(FixDataBundle fixData)
	{
		String command="35=addChatter|170=chatroom|";
		String lastName=fixData.getCommand(49);
		if (lastName != null) command += ("49="+lastName+"|");
		String firstName=fixData.getCommand(50);
		if (firstName != null) command += ("50="+firstName+"|");
		String citizen_id=fixData.getCommand(186);
		if (citizen_id != null) command += ("286="+citizen_id+"|");
		String badge_id=fixData.getCommand(112);
		if (badge_id != null) command += ("49="+badge_id+"|");
		for (int i=0; i<chatters.size(); i++)
		{
			chatters.get(i).channel.sendSocketText(command);
		}
	}
	
	public void broadCastBossMsg(String msg)
	{
		String command="35=broadcast|170=chatroom|";		
		for (int i=0; i<chatters.size(); i++)
		{
			chatters.get(i).channel.sendSocketText(command+msg);
		}		
	}
	
	public void addChatter(FixDataBundle fixData, IOSocket skt)
	{
		Chatter aChatter=new Chatter(skt, fixData);
		synchronized(wrLock){
			chatters.add(aChatter);
		}
		chatterCount++;	
		broadCastNewChatterMsg(fixData);
		//here have to call low level send for each skt
	}
	
	void iDispatchMsg(int i, byte[] msg)
	{
		int[] iBads=new int[chatters.size()];
		int pBad=-1;
		iBads[0]=0;
		for (int k=0; k<chatters.size(); k++)
		{	
			if (k==i) continue;
			try {
				synchronized(wrLock){
					chatters.get(k).outChannel.write(msg);
				}
			} catch (IOException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				iBads[++pBad]=k;
			}						
		}
		if (pBad >= 0)
			synchronized(wrLock){
		for (int k=pBad; k >=0; k--)
		{
			chatters.remove(iBads[k]);
		}
			}
	}
	
	public void run()
	{
		while (chatters.size() > 0)
		{
			int[] iBads=new int[chatters.size()];
			int pBad=-1;
	//cycle all chatter to forward message to all other chatters in this room (from processing socket member, i.e., chater
			for (int i=0; i<chatters.size(); i++)
			{
				Chatter aLine;
				synchronized(wrLock){
					aLine=chatters.get(i);
				}
				byte[] readBytes=null;
				int iRead=0;
				try {
					int iThere=aLine.inChannel.available();
					if (iThere > 0)
						readBytes=new byte[iThere];
					iRead=aLine.inChannel.read(readBytes);
					if (iRead > 0) {
						iDispatchMsg(i, readBytes);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					iBads[++pBad]=i;				
				}						
			}
			if (pBad >= 0)
				synchronized(wrLock){
			for (int k=pBad; k >=0; k--)
			{
				chatters.remove(iBads[k]);
			}
				}
		}
		if (chatterCount==0)
			close();
		return;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
