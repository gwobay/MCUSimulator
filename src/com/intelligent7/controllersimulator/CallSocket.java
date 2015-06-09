package com.intelligent7.controllersimulator;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Vector;

public class CallSocket extends IOSocket implements Runnable 
{ //this is a simple one round-trip socket
String mHost;
int mPort;
//InputStream mIn;
//OutputStream mOut;
byte[] mCallerDataBuffer;
int callerDataLength;
byte[] mSocketDataBuffer;
byte[] mSocketPartialData;
Vector<byte[]> callerReadBuffer;
int socketDataLength;
int callerReadStarts;
int callerReadLength;
int newReadStarts;
int newReadLength;
SocketAddress mAddress;
boolean hasMore; //for mutiple rows communications
Boolean isSocketDataReady;//=new Boolean(false);
	public CallSocket (String h, int p) throws UnknownHostException, IOException
	{
		super(h, p);
		
		mIn=null;;
		mOut=null;			
		callerDataLength=0;
		socketDataLength=0;
		newReadStarts=0;
		newReadLength=0;
		callerReadLength=0;
		hasMore=false;
		
		// shared with caller thread
		isSocketDataReady=new Boolean(false);
		mCallerDataBuffer=new byte[4000];
		mSocketDataBuffer=new byte[40000]; //data are compressed
		mSocketPartialData=new byte[40000]; // uncompressed data for client
		

	}
	
	
	void connect() throws IOException
	{
		super.connect(getLocalSocketAddress());
		
			mIn=mSocket.getInputStream();
			mOut=mSocket.getOutputStream();						
		
        try
        {
        	mSocket.setSoTimeout(50);
        	//mSocket.setTcpNoDelay(true);
        }
        catch (SocketException e){ }
		return;
	}
	

	// app to server always singleton message so put before run
	public void setCallerBuffer(byte[] dataBuf, int buf_len)
	{
		mCallerDataBuffer=new byte[buf_len];
		callerDataLength=buf_len;
		System.arraycopy(dataBuf, 0,  mCallerDataBuffer, 0, buf_len);
	}
	public Vector<byte[]> getSocketDataBuffer()
	{
		synchronized (isSocketDataReady){
			while (!isSocketDataReady)
			{
				try {isSocketDataReady.wait(100);}
					catch(InterruptedException e){}
			}
		}
		return callerReadBuffer;
	}
	
	public void run()
	{
		int failedCount=0;
		try {
			connect(); } catch (IOException e){e.printStackTrace(); return;}
		while (!sendRawByte(mCallerDataBuffer, 0, callerDataLength) && ++failedCount < 3)
		{
			if (mSocket.isClosed())
			{
				try {
				connect(); } catch (IOException e){e.printStackTrace();return;}
			}
		}
		hasMore=true;
		if (failedCount > 2) {return;}
		failedCount=0;

		byte[] oneLineBuffer=new byte[40000];
	synchronized (isSocketDataReady){
			isSocketDataReady=false;
			
			//do multiple read
		while (hasMore && ++failedCount < 3)
		{	
			byte[] data=readStreamData();
			if (data != null) callerReadBuffer.add(data);
			else break;
			hasMore=hasLeftOver();
		}
			isSocketDataReady=true;
			isSocketDataReady.notifyAll();	
	}
		//wait for data got read
		try {Thread.sleep(100);}catch(InterruptedException e){}
		try {
		mOut.close();
		mIn.close(); } catch(IOException e){}

	}
}
