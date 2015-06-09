package com.intelligent7.controllersimulator;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
/*
 * no data related as of now
 * 

import com.kou.utilities.DataProcessor.DataReadyListener;
import com.volunteer.dataprocessor.Agenda;
import com.volunteer.dataprocessor.BaseProcessor;
import com.volunteer.dataprocessor.Profile;
import com.volunteer.dataprocessor.Regid;



 */
/**
 * @author eric
 *
 * use 0x01 as field separator and = as key-value separator
 * 
 
 */
public class MessageParser 
//implements DataProcessor.DataReadyListener
 {

	
	static final Object[][][] page_tags={
		{{170,"volunteer"},{ 55,"regid"},{186, "citizen_id"},
			{49,"last_name"},{ 50,"first_name"},
			{176,"address_street_and_number"},{166,"address_city"},
			{75,"birth_date"},{181,"mobile_number"},
			{111,"rank"},{177,"team_id"},
		{178, "profile_photo_uri"}},
		
		{{170,"commitment"},{35,"msgType"},{ 11,"vid"},
			{186, "citizen_id"},
			{151, "visits_per_week"}, 
			{152, "hours_per_visit"}, 
			{153, "raises_per_week"}, 
			{154, "hours_per_raise"}, 
			{155, "events_per_week"}, 			 
			{156, "hours_per_event"}, 
			{157, "supports_per_week"}, 
			{158, "hours_per_support"}, 
			{159, "specialty_list"},{75,"starting_date"}, {58, "specialty"}},

	{{170,"visited"},{35,"msgType"},{ 11,"vid"},{186, "citizen_id"},{75,"act_date"},
			{179,"fullname"},{181,"mobile_number"},{64,"next_schedule_date"},
				{ 178,"voter_rating"}},

	{{170,"fund_raised"},{35,"msgType"},{ 11,"vid"},{186, "citizen_id"},{75,"act_date"},
			{179,"fullname"},{181,"mobile_number"},{186,"receipt_number"},
				{ 119,"amount"}},

	{{170,"reminder"},{35,"msgType"},{ 60,"act_time"},{75,"act_date"},
				{186, "citizen_id"},{179,"title"},{176,"location_street"},
				{166,"location_city"},{ 149,"url"},{181,"contact_number"}},


	{{170,"agenda"},{35,"msgType"},{ 186,"citizen_id"},
					{151, "event_date"}, 			 
					{152, "event_time"}, 
					{153, "event_title"}, 
					{154, "location"}, 
					{155, "city"}, 
					{156, "event_host"}, 
					{157, "contact_number"}, 
					{158, "description"}},

	{{170,"candidate_photo"},{35,"msgType"},{75,"act_date"},
				{179,"title"},{58,"description"},{ 149,"url"}},
				
	{{170,"candidate_vocal"},{35,"msgType"},{75,"act_date"},
					{179,"title"},{58,"description"},{ 149,"url"}},
					
	{{170,"team"},{35,"msgType"},{ 55,"mentor_id"},{177,"team_id"},{ 11,"member_id"}},
		
			{{170, "api_regids"},{179, "api_name"}, {55, "regid"}}
	};
	
	static String[] getUniqueKeyField(String tblName)
	{
		switch (tblName)
		{
			case "profile":
				return new String[]{ "citizen_id"};
				
			case "commitment":
				return new String[]{"vid","starting_date"};

			case "visited":
				return new String[]{"act_date","fullname"};

			case "fund_raised":
				return new String[]{"vid", "citizen_id","act_date",
					"fullname","receipt_number","amount"};

			case "agenda" :
				return new String[]{"event_date","event_title"};
				
			case "agenda_participant" :
				return new String[]{"event_date","event_title", "citizen_id"};

			case "candidate_vocal" :
				return new String[]{"act_date","title"};
				
			case "team" :
				return new String[]{"member_id"};
				
			case "cid_regids" :
				return new String[]{"regid"};
			default:
				break;
		}
		return null;
	}
	
	static String[] getIntFields(String tblName)
	{
		switch (tblName)
		{
			case "profile":
				return new String[]{ "photo_size"};
				
			case "commitment":
				return new String[]{};

			case "visited":
				return new String[]{};

			case "fund_raised":
				return new String[]{"amount"};

			case "agenda" :
				return new String[]{"participants"};
				
			case "agenda_participant" :
				return new String[]{"participants"};

			case "candidate_vocal" :
				return new String[]{};
				
			case "team" :
				return new String[]{};
				
			case "cid_regids" :
				return new String[]{};
			default:
				break;
		}
		return null;
	}

	static String getBlobField(String tblName)
	{
		switch (tblName)
		{
			case "profile":
				return "photo";
				
			case "commitment":
				return null;

			case "visited":
				return null;

			case "fund_raised":
				return null;

			case "agenda" :
				return null;
				
			case "agenda_participant" :
				return null;

			case "candidate_vocal" :
				return null;
				
			case "team" :
				return null;
				
			case "cid_regids" :
				return null;
			default:
				break;
		}
		return null;
	}

	/*
	void setTagRefByName(String tblName)
	{
		int i=0;
		for (i=0; i< page_tags.length; i++)
		{
			String pageName=(String)page_tags[i][0][1];
		if (tblName.toUpperCase().equals(pageName.toUpperCase()))
				break;
		}
		tagNumber.clear();
		for (int k=0; k<page_tags[i].length; k++)
			tagNumber.put((String)page_tags[i][k][1], 
					       (Integer)page_tags[i][k][0]);
		tagNumber.put("memo",  58);
		tagNumber.put("table_name",  170);
	}
	
	void addDBData(RowStruct dbData, int idx)
	{
		if (tagData[idx].length() > 0) dbData.put(tagName[idx], tagData[idx]);
	}
	String getDBData(RowStruct dbData, String key)
	{
		return (String)dbData.get(key);
	}

	void saveDBData(RowStruct dbData)
	{
		
	}
	void readDBData(String criteria, RowStruct dbData)
	{
		
	}
	
	public Vector<RowStruct > getResponseTable()
	{
		return outTable;
	}

	public String convertRowToLine(RowStruct aRow)
	{
		String fixLine="";
		Iterator<String> itr=aRow.keySet().iterator();
		while (itr.hasNext())
		{
			String fld=itr.next();
			String value=(String)aRow.get(fld); 
			if (value.length() < 1) continue;
			fixLine += tagNumber.get(fld);
			fixLine += "=";
			fixLine += value;
			fixLine += "|";
		}
		return fixLine;		
	}
	
	public String composeFixText(RowStruct aRow, String tbName)
	{
		setTagRefByName(tbName);
		aRow.put("table_name", tbName);
		return convertRowToLine(aRow)+"170="+tbName+"|";
	}
	void processNewRegistration() 
	{ 
		RowStruct dbData=readRow;
		
		int iType=0;
		String sType=(String)readRow.get("msgType");
		if (sType != null)
		iType=Integer.parseInt(sType);
		//to create criteria \' should used to quote the tag-data
		RowStruct criteria=new RowStruct();
		criteria.put("citizen_id", "='"+tagData[186]+"'"); 
		//need to add \' for where clause
		String command="select count(*) from volunteer where citizen_id ='"+tagData[186]+"'";
		mDbProcessor=MainServer.getDbProcessor();
		db_clientId=mDbProcessor.registerClient(this);
		//mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, dbData, criteria));
		mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, null, null));
		
		Vector<RowStruct > resp=getDbResponse();
		
		RowStruct dataRow=resp.get(0);
		String none="NO DATA";
		if (dataRow != null) none=(String)dataRow.get(dataRow.keySet().iterator().next());
		
		if (none==null || none.indexOf("NO DATA") > -1 || Integer.parseInt(none)==0) //new one
		{
			dbData.remove("vid");
			dbData.put("regid", tagData[55]);
			mDbProcessor.putInstruction(db_clientId, new DB_Instruction("insert volunteer", dbData, null));
		} else if (readRow.get("vid")!=null || iType==1) 
		{
			command="update volunteer";
			mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, dbData, criteria));			
		}
		resp=getDbResponse();
		dataRow=resp.get(0);
		none=(String)dataRow.get(dataRow.keySet().iterator().next());
		if (none.indexOf("NO DATA") < 0)
		{ //this should have vid	
		//DataProcessor.putInstruction(db_clientId, new DB_Instruction("select volunteer", null, criteria));
		//resp=DataProcessor.clientReadResponse(db_clientId);			 
		}
		setTagRefByName("volunteer");
		for (int i=0; i<resp.size(); i++)
		{
			putSocketDataInQ(convertRowToLine(resp.get(i)).getBytes());
		}
	}
	
	void processNewCommitment()
	{
		RowStruct dbData=readRow;
		String command="select count(*) from commitment where vid ="+tagData[11];
		mDbProcessor=MainServer.getDbProcessor();
		db_clientId=mDbProcessor.registerClient(this);
		//mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, dbData, criteria));
		mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, null, null));
		
		Vector<RowStruct > resp=getDbResponse();
		RowStruct dataRow=resp.get(0);
		String none="NO DATA";
		if (dataRow != null) none=(String)dataRow.get(dataRow.keySet().iterator().next());
		
		if (none==null || none.indexOf("NO DATA") > -1 || Integer.parseInt(none)==0) //new one
		{
			mDbProcessor.putInstruction(db_clientId, new DB_Instruction("insert commitment", dbData, null));
		} else if (readRow.get("vid")!=null) 
		{
			RowStruct criteria=new RowStruct();
			criteria.put("vid", "="+tagData[11]); 
			
			command="update commitment";
			mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, dbData, criteria));			
		}		
		resp=getDbResponse();
		setTagRefByName("commitment");
		for (int i=0; i<resp.size(); i++)
		{
			putSocketDataInQ(convertRowToLine(resp.get(i)).getBytes());
		}
	}

		
void processNewInterview()
{

		RowStruct dbData=new RowStruct();
		
		addDBData(dbData, 11); //vid
		addDBData(dbData, 55);	//regid
		addDBData(dbData, 179); //fullname
		addDBData(dbData, 152); //head count
		addDBData(dbData, 176); //street
		addDBData(dbData, 177); //number
		addDBData(dbData, 181); //phone
		addDBData(dbData, 178); //rating
		addDBData(dbData, 166); //city
		addDBData(dbData, 64);  //next schedule date
		addDBData(dbData, 75);  //date
		mDbProcessor=MainServer.getDbProcessor();
		db_clientId=mDbProcessor.registerClient(this);
mDbProcessor.putInstruction(db_clientId, new DB_Instruction("insert visited", dbData, null));
		Vector<RowStruct > resp=getDbResponse();
		setTagRefByName("visited");
		for (int i=0; i<resp.size(); i++)
		{
			putSocketDataInQ(convertRowToLine(resp.get(i)).getBytes());
		}
}

	void processAgendaRequest()
	{
		//RowStruct dbData=new RowStruct();
		
		mDbProcessor=MainServer.getDbProcessor();
		db_clientId=mDbProcessor.registerClient(this);
		
		RowStruct dbData=readRow;
		String bossRemark=(String)dbData.remove("BOSS");
		//int iType=0;
		//leave it as String e.g. : resend, delet, ...
		String sType=(String)readRow.get("msgType");
		
		//iType=Integer.parseInt(sType);
		//to create criteria \' should used to quote the tag-data
		RowStruct criteria=new RowStruct();
		criteria.put("event_date", "='"+tagData[151]+"'"); 
		criteria.put("event_title", "='"+tagData[153]+"'"); 
		//need to add \' for where clause
		String command="select count(*) from agenda where event_title ='"+tagData[153]+"'";
		command += " and event_date ='"+tagData[151]+"'";
		
		//mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, dbData, criteria));
		mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, null, null));
		
		Vector<RowStruct > resp=getDbResponse();
		RowStruct dataRow=resp.get(0);
		String none="NO DATA";
		if (dataRow != null) none=(String)dataRow.get(dataRow.keySet().iterator().next());
		
		if (none==null || none.indexOf("NO DATA") > -1 || Integer.parseInt(none)==0) //new one
		{
			mDbProcessor.putInstruction(db_clientId, new DB_Instruction("insert agenda", dbData, null));
		} else if (bossRemark.equalsIgnoreCase("MODIFY")) 
		{
			command="update agenda";
			mDbProcessor.putInstruction(db_clientId, new DB_Instruction(command, dbData, criteria));			
		}
		resp=getDbResponse();
		if (resp == null || resp.size() < 1) return;
		dataRow=resp.get(0);
		none=(String)dataRow.get(dataRow.keySet().iterator().next());
		if (none.indexOf("NO DATA") < 0)
		{ //this should have vid	
		//DataProcessor.putInstruction(db_clientId, new DB_Instruction("select volunteer", null, criteria));
		//resp=DataProcessor.clientReadResponse(db_clientId);			 
		}
		setTagRefByName("agenda");
		for (int i=0; i<resp.size(); i++)
		{
			putSocketDataInQ(convertRowToLine(resp.get(i)).getBytes());
		}

	}
	
	void processNewRaisedFund()
	{
		RowStruct dbData=new RowStruct();
		
		addDBData(dbData, 11); //vid
		addDBData(dbData, 55);	//regid
		addDBData(dbData, 179); //fullname
		//addDBData(dbData, 152); //head count
		addDBData(dbData, 176); //street
		addDBData(dbData, 177); //number
		addDBData(dbData, 181); //phone
		addDBData(dbData, 166); //city
		addDBData(dbData, 186);  //receipt
		addDBData(dbData, 75);  //date
		addDBData(dbData, 119);  //amount
		mDbProcessor=MainServer.getDbProcessor();
		db_clientId=mDbProcessor.registerClient(this);
mDbProcessor.putInstruction(db_clientId, new DB_Instruction("insert raised_fund", dbData, null));
		//thread will be block between put and read here
		Vector<RowStruct > resp=getDbResponse();
		setTagRefByName("raised_fund");
		for (int i=0; i<resp.size(); i++)
		{
			putSocketDataInQ(convertRowToLine(resp.get(i)).getBytes());
		}		
	}
	
	void processNewVoiceRequest()
	{
		RowStruct dbData=new RowStruct();

		addDBData(dbData, 11); //vid
		addDBData(dbData, 55);	//regid
		addDBData(dbData, 75);  //date; from this date on
		mDbProcessor=MainServer.getDbProcessor();
		db_clientId=mDbProcessor.registerClient(this);

	}
	*/
	/*void processRegidRegister()
	{
		RowStruct dbData=new RowStruct();

		addDBData(dbData, 55);	//regid
		addDBData(dbData, 179);  //api name
		addDBData(dbData, 186);  //api name
		mDbProcessor=MainServer.getDbProcessor();
		db_clientId=mDbProcessor.registerClient(this);
		dbData.put("table_name", "cid_regids");
mDbProcessor.putInstruction(db_clientId, "cid_regids"
				new DB_Instruction("insert cid_regids", dbData, null));
	}
	*/

	//BaseProcessor myProcessor;
	void processMessage(FixDataBundle aFDB, ArrayBlockingQueue<byte[]> resultQ)
	{
		/*
		String tableName=aFDB.getCommand(170);
		
		switch(tableName){
		case ("profile"):
			Profile pP=new Profile(aFDB, resultQ);
			myProcessor=pP;
			pP.processRequest();
			//processNewRegistration();
			break;
		case "commitment":
			//processNewCommitment();
			break;
		case "visited": //new Exec
			//processNewInterview();
			break;
		case "agenda":
			Agenda aP=new Agenda(aFDB, resultQ);
			myProcessor=aP;
			aP.processRequest();
			//processAgendaRequest();
			break;
		case "raised_fund":
			//processNewRaisedFund();
			break;
		case "candidate_volca":
			//processNewVoiceRequest();
			break;
		case "team":
			//processNewVoiceRequest();
			break;
		case "blue_pint":
			//processNewVoiceRequest();
			break;
		case "api_regids":
			Regid gP=new Regid(aFDB, resultQ);
			myProcessor=gP;
			gP.processRequest();
			
			//processRegidRegister();
			break;
		default:
			break;
		//new: J (advertise) will be used to send new agenda back
			//tableName="event_list
		}
		//mDbProcessor.clientQuit(db_clientId);	
		 * */	
		
	}
	
	/*
	void setTagRefByNumber(String tblName)
	{
		int i=0;
		for (i=0; i< page_tags.length; i++)
		{
			String pageName=(String)page_tags[i][0][1];
		if (tblName.toUpperCase().equals(pageName.toUpperCase()))
				break;
		}
		//tagNumber.clear();
		for (int k=0; k<page_tags[i].length; k++)
		{
			tagName[(Integer)page_tags[i][k][0]]=(String)page_tags[i][k][1]; 
		}	
		tagName[58]="memo";
	}
	
	int[] getFixMessageTags(String inText)
	{
		int[] tags=new int[200];
		tags[0]=0;
		for (int i=0; i<200; i++)
		{	tagData[i]=""; }
		int k=1; // first element reserved for 170 -> table_name
		if (inText.charAt(inText.length()-1) != '|') inText += '|';	
		int i0=0, iEq=0;
		for (int ix=0; ix < inText.length(); ix++)
		{
			if (inText.charAt(ix)=='=') iEq=ix;
			else if (inText.charAt(ix)== '|')
			{
				if (iEq > i0)
				{
				try {
					String tagNo=inText.substring(i0, iEq);
						i0=Integer.parseInt(tagNo);
						if (i0 > 199){							
						tagData[0]=inText.substring(++iEq, ix);
						continue; //rout to client indicator 200=
						
						}
						tagData[i0]=inText.substring(++iEq, ix);
						if (i0==170) tags[0]=170;
						else
							tags[k++]=i0;				
					} catch(NumberFormatException e){return null;}
				}
				i0=ix+1;
			}
		}
		return tags;	
	}

	public Vector<String> getDbResponseToClient()
	{
		return outMessageList;
	}
	
	public RowStruct lineToRow(String aLine)
	{
		int[] tags=getFixMessageTags(aLine);
		if (tags == null ) return null;
		if (tags[0]==0) return null;
		setTagRefByNumber(tagData[170]);
		tagName[170]="table_name";
		RowStruct data=new RowStruct();
		int i=0; 
		do
		{
			data.put(tagName[tags[i]], tagData[tags[i]]);
		}while (tags[++i]> 0);
		if (tagData[0].length() > 0)
		{
			data.put("BOSS", tagData[0]);
		}
		return data;
	}
	
	public RowStruct parseFixMessage(String inText)
	{
		if (inText.charAt(inText.length()-1) != '|') inText += "|";
		return lineToRow(inText); //no data check here, checked in client
	}
	*/
	public void process(FixDataBundle aFDB, ArrayBlockingQueue<byte[]> resultQ)
	{
		//RowStruct readRow=parseFixMessage(fixLine);
		//new change on May25, 2014
		processMessage(aFDB, resultQ);
	}
	void startJob()
	{
		//String inText=new String(inData, 0, dataLength, Charset.forName("UTF-8"));
		//readRow=parseFixMessage(inText);
		//processMessage();
	}
	public void finish()
	{
		//myProcessor.finish();
		/*
		if (socketDataQ != null)
		socketDataQ.clear();
		if (outMessageList != null)
			outMessageList.clear();
		outMessageList=null;
		socketDataQ=null;
		
		tableName=null;
		db_command=null;
		outMessageList=null;
		outTable=null;
		outData=null;
		inData=null;
		readRow=null;
		tagData=null;	
		if (tagNumber != null) tagNumber.clear();
		tagNumber=null;
		tagName=null;
		mDbProcessor=null;*/
	}
	/*
	void sendDataToSocket()
	{
		for (int i=0; i<outMessageList.size(); i++)
			System.out.println("My id "+db_clientId+" : "+outMessageList.get(i));
	}
	
	public static void main(String[] args)
	{
		DataProcessor aProcess=DataProcessor.getInstance(false);
		long stopAt=(new Date()).getTime()+60*10*1000;
		aProcess.setStopTime(stopAt);
		aProcess.start();
		//MessageParser aP=new MessageParser(null, 0);
		//aP.startJob();
		final String testData="35=0|55=sdafabdjads1536|49=郭|50=豐永|176=中山路621巷1號|166=苗栗市|186=K120009376|181=0986056745|170=volunteer";
		
		//t1.setData(testData);
		Thread t1=new Thread(new Runnable(){
			
			public void run()
			{
				byte[] data=testData.getBytes();
				MessageParser aP=new MessageParser(data, data.length);
				aP.setDbClientId(MainServer.getDbProcessor().registerClient(aP));
				aP.startJob();
			}
		});
		t1.start();
		try { t1.join();} catch (InterruptedException e){}
		final String testLine="35=1|186=K120009376|75=1955-11-24|170=volunteer|";
		Thread t2=new Thread(new Runnable(){
			public void run()
			{
				byte[] data=testLine.getBytes();
				MessageParser aP=new MessageParser(data, data.length);
				aP.setDbClientId(MainServer.getDbProcessor().registerClient(aP));
				aP.startJob();
			}
		});
		t2.start();
		try {
			//t1.join();
			t2.join(); } catch (InterruptedException e){}
		
		aProcess.interrupt();
	}
	*/
}
