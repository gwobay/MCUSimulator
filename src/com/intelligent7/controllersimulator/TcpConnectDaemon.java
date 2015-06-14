/**
 * 
 */
package com.intelligent7.controllersimulator;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

/**
 * @author eric
 *
 */
public class TcpConnectDaemon extends Thread
{
//this socket always has id either for SIM or PHONE
	//an instance is created whenever server accept
	//an client socket which is aliased as mySocket
	//then spawn a writeThread (which will poll the outboundQ,
	//this thread (act as readThread) then loop to read and dump data to peer;
	//then join write thread
	//socket owner should take care of data processing
	
	String myName;
	String mHost;
	int mPort;
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
	ArrayBlockingQueue<String> outBoundDataDataQ ;
	ArrayBlockingQueue<String> socketInDataQ ;
	HashMap<String, ArrayBlockingQueue<String> > friendQ ;
	//PostOffice myPostOffice;
	final int Q_SIZE=20;

	int mHeartBeatInterval;
	//can be changed in file
	int myMODE;
	public static int MODE_REPEAT=1440;
	public static int MODE_ONCE=1;
	public static int MODE_COUNT=10;

	Vector<DataUpdateListener> sniffers;
	
	/*
	 * interface for owner to update my name
	 * like a tag
	 * this is for realtime staff
	 * as of now, Post Office scheme will be used for this
	 */
	public interface DataUpdateListener //must be a data switch board
	{
		public void engineSocketSignOn(String name, TcpConnectDaemon who);
		public void engineSocketQuit(String who);
		public void engineSocketAddPeers(String controller, String phones);
		public void peerSocketDataReady(String myName, String data);// later if for byte[] data);
		public void peerSocketDataReady(String myName, String data, String peerName);// later if for byte[] data);
	}

	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private TcpConnectDaemon(){};

	public TcpConnectDaemon(String server, int port)
	{
		mHost=server;
		mPort=port;
		mySocket=null;
		outBoundDataDataQ=null;
		socketInDataQ=null;
		init();
	}

	public TcpConnectDaemon(Socket clientSkt) {
		// TODO Auto-generated constructor stub
		if (clientSkt == null) return;
		mySocket = new SimpleSocket(clientSkt);
		outBoundDataDataQ=null;
		socketInDataQ=null;
		init();
	}

	static String myToken="";
	//TODO : update following data from saved 
	static String myNickName="";
	static String mySimIccId="";
	static String acceptedPhones="*";
	String myPin;

	private void init()
	{
		//if (mySocket==null) return;
		//myName=null;
		//mContext=this;
		if (mySocket != null)
		clientAddr=mySocket.getInetAddress();
		
		myMODE=MODE_REPEAT;
		mHeartBeatInterval=60*1000;
		if (outBoundDataDataQ==null)
		outBoundDataDataQ = new ArrayBlockingQueue<String>(Q_SIZE, true);
		if (socketInDataQ==null)
			socketInDataQ = new ArrayBlockingQueue<String>(Q_SIZE, true);
		//socketOutDataQ = new ArrayBlockingQueue<String>(Q_SIZE, true);
		//friendQ=new HashMap<String, ArrayBlockingQueue<String> >();
		//sniffers=new Vector<DataUpdateListener>();
		if (log==null)
		log=Logger.getAnonymousLogger();
		myPin="0000";
		mySimIccId=SimulationStarter.getResource("SIM_ICCID");
		acceptedPhones=SimulationStarter.getResource("APP_PHONES");
		if (mySimIccId!=null)
			myToken="<"+mySimIccId+">";
	}

	public void setModeInterval(int mode, int interval) //in milliseconds
	{
		myMODE=mode;
		mHeartBeatInterval=interval;
	}
	public void attachToStarter(SimulationStarter who)
	{
		//mContext=who;
	}
	public void setOutDataQ(ArrayBlockingQueue<String> newQ)
	{
		outBoundDataDataQ=newQ;
	}

	public ArrayBlockingQueue<String> getOutDataQ()
	{

		if (outBoundDataDataQ==null)
			outBoundDataDataQ=new ArrayBlockingQueue<String>(Q_SIZE, true);
		return outBoundDataDataQ;
	}

	//public void setMyPostOffice(PostOffice new1)
	//{
		//myPostOffice=new1;
	//}

	public void addDataToOutQ(String msg)
	{
		final String outMsg=msg;
		final Thread toWakeUp=this;
		getOutDataQ();
		new Thread(new Runnable(){
			public void run(){
				
				if (outBoundDataDataQ.size() > Q_SIZE){
					log.warning("Warning : too many msg in my Q");
					try {
						outBoundDataDataQ.take();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					outBoundDataDataQ.put(outMsg);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//setHasCommand(true);
				wakeUp4Command();
			}
		}).start();
	}
	//inbound msg is Qed in the switchBoard who has Hash<String, Vector<String>>
	//when Qed will be dropped to the new EngineSocket when updateName is called

	boolean socketSendData(String socketData){
		int iTry = 0;
		String data=new String(socketData);
		while (!mySocket.sendText(socketData)){
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if (mySocket.isClosed() || iTry++> 10 )
				{
					log.info("failed to send :"+data);
					return false;
				}
			}
		}
		log.info("sent: " + socketData);
		return true;
	}

	boolean sleepForCommand(ArrayList<String> toSend, long milliseconds){
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block 
			if (hasCommand) {
				while (outBoundDataDataQ.size() > 0){
					try {
					toSend.add(outBoundDataDataQ.take());
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				if (toSend.size() >0)
				return true;
			}
			e.printStackTrace();
			//break;
		}
		return false;
	}
	
	public void sendDataToServer()
	{
		//move data to my send poll
		if (mySocket==null) return;
		if (!mySocket.isSktConnected() || !mySocket.hasOutStream()) return;
		ArrayList<String> toSend=new ArrayList<String>();
		toSend.add(myToken);
		while (outBoundDataDataQ.size() > 0){
			try {
			toSend.add(outBoundDataDataQ.take());
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		while (mySocket!= null && mySocket.isSktConnected() && mySocket.hasOutStream() )
				//outBoundDataDataQ.size() > 0)
		{			

			if (toSend.size() > 0)
			{
				String socketData=toSend.remove(0);
				String msg=socketData;
				if (socketData.indexOf("<") < 0 )
					msg=socketData+myToken;

				if (!socketSendData(msg+"$")) break;

				//if (socketData.charAt(0) != '<')
				//saveDataToDb(socketData);
				try {
					System.out.println("Sent at "+new Date().getTime());
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();}
			}
			else //no more data for server
			{
				if (sleepForCommand(toSend, 500)) continue;
				//send last token
				socketSendData(myToken+"$");
				System.out.println("Sent at "+new Date().getTime());
				if (sleepForCommand(toSend, 500)) continue;				
				//mySocket.close(); just break out don't close so that other thread can still use the socket
				break;
			}
		}
		
		if (toSend.size()>0){
			//put them back to Q; need to take of sequence issue
			for (int i=0; i<toSend.size(); i++){
				try {
				outBoundDataDataQ.put(toSend.get(i));
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		toSend.clear();
		toSend=null;
	}
	boolean hasCommand;
	public void wakeUp4Command()
	{
		hasCommand=true;
		if (writeThread.isAlive())
		writeThread.interrupt();
		else
			startWriteThread();			
	}
	Thread writeThread;
	void startWriteThread()
	{
		writeThread=new Thread(){
			public void run()
			{
				sendDataToServer();
			}
		};
		writeThread.start();
	}

	
	//------------------------------------------------

	boolean stopFlag;

	void readAndDispatchData()//byte[] readData)
	{
		byte[] readData=null;
		long timeEnd=(new Date()).getTime()+5*1000;
		while (readData==null || readData.length < 1)
		{
			if (readData==null && mySocket.isSktClosed())
				{
				stopFlag=true;
				return;
				}
			
			readData=mySocket.getStreamData();
			
			if (readData==null)
			{
				if (mySocket.getReadFlag() <0)				
				{
					stopFlag=true;
					try {
						sleep(2*1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mySocket.close();
					break;
				}
				if ((new Date()).getTime() > timeEnd) {
					log.info("No Data from server for 5 secs ");
					stopFlag=true;
				break;
				}
			}
		}
		if (readData==null || readData.length < 1) return;
		
		String sData=new String(readData);
		log.info("got : "+sData);

		
		//data from server to app should always msg@time-stamp<iccid>$
		//data from server to mcu should always msg<phone>$
		
		//put in log(database) and create notification
		//format "msg"+"<sender, backup-sender@receiver>$" i.e., "...<phone1, phone2@receiver>" from phone 
		//and only <sim number> from control
		int iUser=sData.indexOf('<');
		int idx=sData.indexOf('>');
		if (iUser < 0 || idx < 0){
			log.warning(sData+" !!Bad data format: missing '<', unknown sender");
			return;
		}

		int i0x=iUser;
		String sender=sData.substring(i0x+1,  idx);
		String msg=sData.substring(0, i0x);
		if (sender.equalsIgnoreCase("000000"))
		{
			//new host and port for next connection
			//save them to shared pref
			//change the value of
			String[] terms=msg.split("-"); //M6-ip-port
			mHost=terms[1];
			mPort=Integer.parseInt(terms[2]);
			log.info("New ip-port : "+msg);
			return;
		}
		else if (acceptedPhones.charAt(0) != '*' && acceptedPhones.indexOf(sender) <0){
			log.warning(sData + " !!Mismatch data : wrong sender " + sender + " not in " + acceptedPhones);
			socketSendData("S999<"+mySimIccId+">");
			return;
		}
		

		idx=msg.indexOf('@');
		if (idx > 0){
			String senderTime=msg.substring(idx+1);
			log.warning(sData + "delayed by " + (new Date().getTime() - Integer.parseInt(senderTime)));
			String tmp=msg.substring(0, idx);
			msg=tmp;
		}	
		
		if (msg.charAt(1) != '2' && msg.charAt(1)!='3'){
			try {
				long iSleep=Math.round(Math.random()*1000)%10;
		    	
				sleep(iSleep*1000); //only pin or phone change will be return immediately
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
			long iSuccess=Math.round(Math.random()*1000)%2;
	    	String retCode="S";
	    	String resp=(new DecimalFormat("0").format(iSuccess));
	    	int inCode=msg.charAt(1)-'0';
	    	switch(inCode)
	    	{
	    	case 1: //warmer
	    		retCode += ""+msg.charAt(1)+msg.charAt(4)+resp;
	    		break;
	    	case 2: //pin
	    		retCode += ""+msg.charAt(1)+'0'+resp;
	    		if (iSuccess==0){
	    			String[] terms=msg.split("-");
	    			myPin=terms[2];
	    		}
	    		break;
	    	case 3: //pin
	    		retCode += ""+msg.charAt(1)+'0'+resp;
	    		if (iSuccess==0){
	    			acceptedPhones="";
	    			String[] terms=msg.split("-");
	    			if (terms[2].length() > 1)
	    			acceptedPhones +=terms[2];
	    			if (terms[3].length() > 1)
		    			acceptedPhones += (","+terms[3]);
	    		}
	    		break;
	    	case 4: //stop engine or air cond
	    		retCode += ""+msg.charAt(1)+msg.charAt(4)+resp;
	    		break;
	    		
	    	case 5: //start engine 
	    		retCode += ""+msg.charAt(1)+"0"+resp;
	    		
	    		break;
	    		
	    	}
	    	//addDataToOutQ(retCode+myToken);

	    	socketSendData(retCode+myToken);
	    	if (inCode==5 && iSuccess==0){
	    		try {
	    			long iSleep=Math.round(Math.random()*1000)%50+10;
					sleep(iSleep*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		addDataToOutQ("S502"+myToken);
	    	}
			
			return;
	
		
	}
	
	void setHasCommand(boolean T_F)
	{
		hasCommand=T_F;
	}
	
	public void setStopFlag(boolean T_F){
		imDone=!T_F;
	}
	boolean imDone;
	int iSuccessful=0;
	public void run()
	{
		writeThread=null;
		//mParser=new MessageParser();
		boolean wait30=false;
		imDone=false;
		//imChatLine=false;
		do {
			if (wait30)
			{
				try {
					sleep(30*000);
				} catch(InterruptedException e){

				}
			}
			boolean iCanStart=false;
			//if (myName.charAt(0) != '-' &&
				//mySimIccId .charAt(0) != '-')
			//iCanStart=true;
			
			myToken="<" + mySimIccId+">";

			try {
				mySocket = new SimpleSocket(mHost, mPort);
				//outBoundDataDataQ.put(myToken);
				// //token was add to by send thread to make sure it sends first
				//init();
				iSuccessful++;

			}
			catch (UnknownHostException e) {
				log.warning("failed to connect to server at "+mPort+"@"+mHost);
				wait30=true;
				continue;
			} catch (IOException b) {
				wait30=true;
				continue;
			}
			if (mySocket==null) return;			
			

			startWriteThread();

			stopFlag=false;
			log.info("daemon connected at " +
					DateFormat.getTimeInstance().format(new Date()));
			while (mySocket.isSktConnected() && mySocket.hasInStream()) {
				if (stopFlag) break;
				readAndDispatchData();//readBytes);
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
			if (mySocket != null)//&& !imChatLine)
				mySocket.close();
			log.info("daemon socket closed connection with " + clientAddr);
			log.info("Daemon is done with processing and closed at " +
					DateFormat.getTimeInstance().format(new Date()));
			log.info("will restart connection in "+mHeartBeatInterval/1000+" seconds");
			if (iSuccessful > myMODE) return;
			hasCommand=false;
			try {
				Thread.sleep(mHeartBeatInterval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				if (!hasCommand) {
					e.printStackTrace();
					log.warning("!!Woken up for unknown reason!!");
					break;
				}
			}

		} while (!imDone);
		System.out.println("Daemon finishes "+iSuccessful+" jobs and gone");
	}
	/* databse staff starts here
	*
	
	Context mContext;
	protected DbProcessor mDbExcutor;
	protected SQLiteDatabase mDb;
	protected ContentValues mContentValues;
	protected String criteria;   // for rawQuery you can use ... where abc > ? and efg < ? and those ? can be replaced by
	protected String[] criteria_values;
	Vector<ContentValues> mAllRecords;
	//for Cursor
	protected Vector<String> listColumns;//={"rowid _id", "visit_date", "full_name"}; id is mandatory for simpleCursorAdapter
	protected Vector<String> showColumns;//={"visit_date", "full_name"};
	String mTableName;

	String createTableSql()
	{
		String sql="create table if not exists event_records ";
		sql += "( event_time numeric not null,";
		//sql += " message text unique not null,";
		sql += " message text,";
		sql += " i_o char(1),";
		sql += " sender char(20),";
		sql += " receiver char(20));";
		//sql += " event_description text,";
		//sql += " status char(8) not null default 'new',";
		//sql += " participants char(8) default '0' );";//new, cancle, read, join
		return sql;
	}


	void getAllRecords()
	{
		String sql="select * from event_records order by event_date desc";
		mAllRecords=getAllListRecord(DbProcessor.getRecordsFromSql(mContext, sql));
	}


	void initDB()
	{
		//setConstants();
		mDbExcutor=null;
		startDb();
		mTableName="event_records";

		if (!DbProcessor.ifTableExists(mContext, mTableName))
		{
			DbProcessor.createTable(mContext, createTableSql());
			//checkForServerData();
		}
		//else getAllRecords();
	}
	public void startDb()
	{
		if (mContext==null) {
			log.warning("Context is not set! cannot open database");
			return;
		}
		try {
			if (mDbExcutor == null || !mDbExcutor.getDb().isOpen() )
				mDbExcutor=new DbProcessor(mContext, "engine_auto_db");
			mDb=mDbExcutor.getDb();
		} catch (SQLiteException e){}

		//{Toast.makeText(getActivity(), e.getMessage()+" CAll 0986056745", Toast.LENGTH_LONG).show();}

	}

	void confirmTableExist(String table)
	{
		if (!DbProcessor.ifTableExists(mContext, mTableName))
		{
			DbProcessor.createTable(mContext, createTableSql());
			//checkForServerData();
		}
	}
	void doInsert(String tableName, ContentValues aRow)
	{
		//startDb();
		confirmTableExist(tableName);
		String eMsg="";
		try {
			DbProcessor.insertTable(mContext, tableName, null, aRow);//mContentValues);
		} catch (SQLiteConstraintException e){ log.warning(e.getMessage());}
		catch (SQLiteException e){ log.warning(e.getMessage());}
		//if (eMsg.length() >0)
		//{Toast.makeText(mActivity, eMsg, Toast.LENGTH_SHORT).show();}
	}

	void saveDataToDb(String data, String sender, String receiver, String io){
		confirmTableExist("event_records");
		String sql="insert into event_records ";
		long when=new Date().getTime();
		String message=data;
		int i0x=data.indexOf("@");
		if (i0x > 0){
			when = Long.parseLong(data.substring(i0x+1));
			message=data.substring(0, i0x);
		}
		sql += ("(event_time, message, sender, receiver, i_o) values ("+when+", '"+
					message+"', '"+sender+"', '"+receiver+"', '"+io+"' );"	);
		try {
			DbProcessor.insertTable(mContext, sql);
		}catch (SQLiteException e){
			log.warning("failed to insert data "+data+" for "+e.getMessage());
		}
	}

	void saveDataToDb(String data)
	{
		saveDataToDb(data,myName, mySimIccId, "O");
	}
	String fromSpecifedFieldsToSqlCriteria(HashMap<String, String> specifiedFields)
	{
		if (specifiedFields==null || specifiedFields.size()<1) return "";
		String criteria=" where ";
		Set<String> keys=specifiedFields.keySet();
		Iterator<String> itr=keys.iterator();
		boolean once=true;
		while (itr.hasNext())
		{
			if (!once){ criteria += " and ";} else once=false;
			String key= itr.next();
			criteria += key;
			criteria += "='";
			criteria += specifiedFields.get(key);
			criteria += "'";
		}
		return criteria;
	}

	HashMap<String, String> fromContentValuesToMap(ContentValues aC)
	{
		if (aC==null || aC.size()<1) return null;
		HashMap<String, String> aMap=new HashMap<String, String>();
		Set<String> keys=aC.keySet();
		Iterator<String> itr=keys.iterator();
		while (itr.hasNext())
		{
			String key= itr.next();
			aMap.put(key, aC.getAsString(key));
		}
		return aMap;
	}
	ContentValues fromMapToContentValues(HashMap<String, String> aC)
	{
		if (aC==null || aC.size()<1) return null;
		ContentValues oC=new ContentValues();
		Set<String> keys=aC.keySet();
		Iterator<String> itr=keys.iterator();
		while (itr.hasNext())
		{
			String key= itr.next();
			if (key.compareToIgnoreCase("table_name")==0) continue;
			oC.put(key, aC.get(key));
		}
		return oC;
	}

	HashMap<String, String> selectedValues;
	Vector<ContentValues> getAllListRecord(Cursor aCursor)
	{
		if (aCursor.getCount() <1)
		{
			aCursor.close();
			//Toast.makeText(getContext(), "NO RECORD TO SHOW", Toast.LENGTH_LONG).show();
		}
		log.info("dbDATA" +"CALLED BY"+this.getId());
		Vector<ContentValues> allRows=new Vector<ContentValues>();
		//firstNew=null;
		int selectedId=-1;
		int iNew=-1;
		String[] heads=aCursor.getColumnNames();
		aCursor.moveToFirst();
		while (!aCursor.isAfterLast())
		{
			ContentValues aC= new ContentValues();
			for (int k=0; k<aCursor.getColumnCount(); k++)
			{
				switch (aCursor.getType(k))
				{
					case Cursor.FIELD_TYPE_NULL:
						aC.put(heads[k], "0"); break;
					case Cursor.FIELD_TYPE_INTEGER:
						aC.put(heads[k], ""+aCursor.getInt(k) ); break;
					case Cursor.FIELD_TYPE_FLOAT:
						aC.put(heads[k], ""+aCursor.getFloat(k) ); break;
					case Cursor.FIELD_TYPE_STRING:
						aC.put(heads[k], aCursor.getString(k) ); break;
					case Cursor.FIELD_TYPE_BLOB:
						aC.put(heads[k], new String(aCursor.getBlob(k)) ); break;
					default:
						aC.put(heads[k],  "0");
						break;
				}
			}
			String dd=aC.getAsString("event_time");
			String tt=aC.getAsString("message");
			String status=aC.getAsString("sender");
			log.info("dbDATA" + dd + tt );
			if (selectedValues != null && selectedId<0){
				String uiDD=selectedValues.get("event_time");
				if (dd.equalsIgnoreCase(uiDD) &&
						tt.equalsIgnoreCase(selectedValues.get("message")))
					selectedId=aCursor.getPosition();
			}


			allRows.add(aC);
			aCursor.moveToNext();
		}
		aCursor.close();

		return allRows;
	}
	

	/**
	 * @param arg0
	 */
	public TcpConnectDaemon(Runnable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public TcpConnectDaemon(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public TcpConnectDaemon(ThreadGroup arg0, Runnable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public TcpConnectDaemon(ThreadGroup arg0, String arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public TcpConnectDaemon(Runnable arg0, String arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 */
	public TcpConnectDaemon(ThreadGroup arg0, Runnable arg1, String arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 */
	public TcpConnectDaemon(ThreadGroup arg0, Runnable arg1, String arg2,
							long arg3) {
		super(arg0, arg1, arg2, arg3);
		// TODO Auto-generated constructor stub
	}

}
