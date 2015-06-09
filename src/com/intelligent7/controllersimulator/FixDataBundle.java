package com.intelligent7.controllersimulator;

import java.util.HashMap;

public class FixDataBundle {
	//String command; //tag 10 and tag 35 are command related, 10=OPEN or 35=CREATE
						// 170=table (agenda, profile, ..., chatroom, 
						// 35=broadcast, then 128=deliver to, (all then read from table, otherwise comma delimited
	String fixLine;
	byte[] streamData;
	HashMap<String, String> dataRow;
	public FixDataBundle()
	{
		//command=null;
		fixLine=null;
		streamData=null;
		dataRow=null;
	}
	
	public FixDataBundle(byte[] fixData)
	{
		//command=null;
		fixLine=null;
		streamData=null;
		dataRow=null;
		if (fixData==null) return;
		int iLen=fixData.length;
		int length=iLen;// format change to accomodate big file -- fixData[0]*127+fixData[1];
		//if (length+2 > iLen) return;
		fixLine=new String(fixData, 0, length);
		String[] atoms=fixLine.split("\\|");
		if (atoms.length > 0){
			dataRow=new HashMap<String, String>();
			for (int i=0; i<atoms.length; i++)
			{
				String[] tokens=atoms[i].split("=");
				if (tokens.length<2) continue;
				dataRow.put(tokens[0],  tokens[1]);
			}
		}
		
		if (fixData.length <= 2+length) return;
		//streamData=java.util.Arrays.copyOfRange(fixData, 2+length, fixData.length-2-length);
		streamData=java.util.Arrays.copyOfRange(fixData, 0, fixData.length);
	}
	
	public String getFixLine()
	{
		return fixLine;
	}
	
	public String getCommand(int i)
	{
		if (dataRow==null)
		return null;
		return dataRow.get(""+i);
	}
	
	public byte[] getStream()
	{
		return streamData;
	}
	
	public void cleanUp()
	{
		if (dataRow != null) dataRow.clear();
		dataRow=null;
		if (streamData != null) streamData=null;
	}

}
