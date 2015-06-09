package com.intelligent7.controllersimulator;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

//import android.util.Log;

public class IOSocket extends Socket {
	protected int timeWait;
//connection/close will use super or by child itself
	protected InputStream mIn;
	protected OutputStream mOut;
	protected boolean zipped_channel;
	protected byte[] readLeft;
	//protected int left_from;
	protected int left_length;
	
	protected int channel_type;
	public static final int KEEP_ALIVE=0x0001;
		protected long heart_beat_interval; //0 means no need
		protected long last_beat;
	public static final int STOP_AT=0x0002;
		protected long stop_time;
	public static final int FOREVER=0x0000; //default
		protected boolean kill_flag;
	
	public static final int ZIPPED = 0x000F;
	
	public static final int BAD_READ=-1;
	public static final int BAD_STREAM=-9;
	
	protected int readFlag;
	protected int writeFlag;
	protected Socket mSocket;

	static Logger log=Logger.getAnonymousLogger();
	
	public IOSocket() {
		// TODO Auto-generated constructor stub
		super();
		mSocket=this;
		channel_type = ZIPPED;
		zipped_channel=true;
	}
	
	public IOSocket(String host, int port) throws UnknownHostException, IOException
	{
		super(host, port);	
		mSocket=this;
/*        try
        {
        	//mSocket.setSoTimeout(2000);
        	//mSocket.setTcpNoDelay(true);
        }
        catch (SocketException e){ }*/
		try {
			if (isConnected())
			{
				mIn=mSocket.getInputStream();
				mOut=mSocket.getOutputStream();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		channel_type=FOREVER|ZIPPED;
		zipped_channel=true;
		readFlag=1;
		writeFlag=1;
	}

	public void setDelayReadTime(int milli)
	{
		timeWait=milli;
	}
	public IOSocket(Socket s)
	{
		mSocket=s;

/*        try
        {
        	mSocket.setSoTimeout(timeWait);
        	//mSocket.setTcpNoDelay(true);
        }
        catch (SocketException e){ }*/
		try {
			mIn=mSocket.getInputStream();
			mOut=mSocket.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		channel_type=FOREVER|ZIPPED;
		zipped_channel=true;
		readFlag=1;
		writeFlag=1;
	}
	
	public boolean isSktConnected()
	{
		return mSocket.isConnected();
	}
	
	public boolean isSktClosed()
	{
		return mSocket.isClosed();
	}

	protected boolean hasInStream()
	{
		return (mIn != null);
	}
	
	protected boolean hasOutStream()
	{
		return (mOut != null);
	}
	
	public void close()
	{
		try 
		{
			if (mIn!= null) mIn.close();
			mIn=null;
			if (mOut != null) mOut.close();
			mOut=null;
			mSocket.shutdownInput();
			mSocket.shutdownOutput();
			mSocket.close();
			readLeft=null;
		} catch (IOException e){}
		catch (NullPointerException e){}
	}
	// -------------------------------- I/O --------------------------//
	
	public void setZippedFlag(boolean T_F) {zipped_channel=T_F;
					channel_type |= ZIPPED;
	}
	
	// ----------------- READ -------------------------//
	static final int maxRead=20000;
	int readRawData(byte[] dataBuffer)
	{
		int iRead;
		try{	
			if (mIn == null) mIn=mSocket.getInputStream();
			iRead=mIn.read(dataBuffer, 0, dataBuffer.length);
			if (iRead < 0) {
				readFlag=BAD_READ; 
				// mIn.close();
				//	mIn=null;
			}
		   }
			catch(SocketTimeoutException e){return 0;}
			catch (IOException e)
			{
				return BAD_STREAM;
			}
	
		return iRead;
	}
	
	byte[] unZipRawData(byte[] zipped, int from, int len)
	{
		byte[] unZipped=null;
		int iLen;
		try {
		ByteArrayInputStream zippedData=
				new ByteArrayInputStream(zipped, from, len); 
		GZIPInputStream gzipLine=new GZIPInputStream(zippedData, len+1);
		unZipped=new byte[10*len];
		iLen=gzipLine.read(unZipped, 0, 10*len);
		gzipLine.close();
		//String inflated=new String(unZipData, 0, iL, "UTF-8");
		
		//readPage=inflated.toUpperCase();
		//inflated.length();
								
		} catch (IOException e){
		log.warning("Bad Zipped Data");
	             zipped_channel=false;
	             channel_type &= ~ZIPPED;
			return null;
	         }
		byte[] retB=new byte[iLen];
		System.arraycopy(unZipped, 0, retB, 0, iLen);
		log.info("zipped : "+len+" unZipped:"+iLen);
		return retB;
	}

	byte[] merge2(byte[] h, byte[] t, int h_offset, int t_copyLength)
	{
		byte[] ret=h;
		if (t_copyLength + h_offset > h.length) 
			{
				ret=new byte[t_copyLength + h_offset];
				System.arraycopy(h, 0, ret, 0, h_offset);
			}
		System.arraycopy(t, 0, ret, h_offset, t_copyLength);
		return ret;
	}
	
	int getLength(byte[] pt)
	{
		return pt[0]+pt[1]*127+pt[2]*127*127+pt[3]*127*127*127;
		
	}

	public byte[] readStreamData()
	{
		byte[] tmp=readLeft, tmp1=null;
		boolean needToRead=true;
		int iLen=0;
		if (left_length < 4){ tmp1=new byte[maxRead]; left_length=0;}
		else
		{
			iLen=getLength(readLeft);
			if (iLen < left_length) 
				{
					tmp=readLeft;
					needToRead=false;
				}
			else tmp1=new byte[iLen-left_length+4+4];
		}
		int iHad=left_length;
		if (needToRead)
		{
			iHad=readRawData(tmp1);
			if (iHad<0) { readFlag=iHad; 						
						return null;
					}
			if (left_length > 0) tmp=merge2(readLeft, tmp1, left_length, iHad);
			else tmp=tmp1;
			
			iHad += left_length;
		}
		if (iHad < 4) 
		{
			left_length=0;
			return 	null;
		}
		iLen=getLength(tmp);
		byte[] dBuf=tmp;
		if (iHad <= iLen+4)
		{
			if (iLen+4 <= tmp.length ) dBuf=tmp;
			else
			{
				dBuf=new byte[iLen+4];
				System.arraycopy(tmp, 0, dBuf, 0, iHad);
				tmp=null;
			}
			//tmp1=null;
			byte[] more=new byte[iLen-iHad+4];
			while (iLen+4 > iHad)
			{
				int i2=readRawData(more);
				if (i2 < 1)  
					{
							//put dBuf in leftover
							readLeft=dBuf;
							left_length=iHad;
							return	null;
					}
				if (iLen + 4 >= iHad+i2)
				{
					System.arraycopy(more, 0, dBuf, iHad, i2);
					iHad += i2;
				}
				else
				{
					left_length=(iHad+i2)-(iLen+4);
					readLeft=new byte[left_length];
					//System.arraycopy(org, 0, tmp, 0, i);
					System.arraycopy(more, 0, dBuf, iHad, (iLen+4)-iHad);
					more=null;
					iHad += i2;
					break;
				}
			}
		} 
		else
		{
			left_length=iHad-iLen-4;
			readLeft=new byte[left_length];
			System.arraycopy(tmp, 4+iLen, readLeft, 0, left_length);
		}
		log.info("expected:"+iLen+" got:"+iHad);
		if (zipped_channel || (channel_type & ZIPPED) > 0)
		{
			return unZipRawData(dBuf, 4, iLen);
		}
		byte[] retB=new byte[iHad];
		System.arraycopy(dBuf, 4, retB, 0, iLen);
		return retB;
	}
	
	public boolean hasLeftOver()
	{
		return (left_length > 0);
	}
	
	public int getReadFlag()
	{
		return readFlag;
	}
	// ------------------------------------ SEND ----------------//
	
    byte[] getZipped(byte[] inData, int from, int length)
    {
       if (from+length > inData.length) return null;
            
       byte[] outData;
       try {
              ByteArrayOutputStream byteStream=
                              new ByteArrayOutputStream(length);//outData, from, length);
              GZIPOutputStream gzipLine=new GZIPOutputStream(byteStream);
                    gzipLine.write(inData, from, length);
                    gzipLine.flush();
                    gzipLine.finish();
                    outData=byteStream.toByteArray();
                    //gzipLine.close();
                    //String inflated=new String(unZipData, 0, iL, "UTF-8");
       	} catch (IOException e){ 
       			writeFlag=BAD_STREAM; return null;}
       return outData;
    }

	protected boolean sendRawByte(byte[] b, int from, int length)
	{
		byte[] b4={(byte)(length % 127), (byte)(length / 127), 
					(byte)(length / (127*127)), (byte)(length /(127*127*127)) };
				
		try {
			if (mOut==null) mOut=mSocket.getOutputStream();
			
			if (zipped_channel || (channel_type & ZIPPED) > 0)
			{
				byte[] zipped=getZipped(b, from, length);
				if (zipped == null) return false;
				int zLen=zipped.length;
				//Log.i("IOSKT", "data:"+length+" ziped:"+zLen);
				log.info("data:"+length+" ziped:"+zLen);
				b4[0]=(byte)(zLen % 127);zLen /= 127;
				b4[1]=(byte)(zLen % 127);zLen /= 127;
				b4[2]=(byte)(zLen % 127);zLen /= 127;
				b4[3]=(byte)(zLen % 127);
				mOut.write(b4, 0, 4);
				mOut.write(zipped, 0, zipped.length);				
			} else {mOut.write(b4, 0, 4); mOut.write(b);}
			mOut.flush();			
		} catch (IOException e){
			writeFlag=BAD_STREAM; 
			return false;
			}
		return true;
	}
	
	public boolean sendSocketText(String text)
	{
		byte[] b=text.getBytes();
		return sendRawByte(b, 0, b.length);
	}

	public boolean sendSocketData(byte[] b)
	{
		return sendRawByte(b, 0, b.length);
	}

	public IOSocket(Proxy arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public IOSocket(SocketImpl impl) throws SocketException {
		super(impl);
		// TODO Auto-generated constructor stub
	}


	public IOSocket(InetAddress address, int port) throws IOException {
		super(address, port);
		// TODO Auto-generated constructor stub
	}

	public IOSocket(String host, int port, InetAddress localAddr, int localPort)
			throws IOException {
		super(host, port, localAddr, localPort);
		// TODO Auto-generated constructor stub
	}

	public IOSocket(InetAddress address, int port, InetAddress localAddr,
			int localPort) throws IOException {
		super(address, port, localAddr, localPort);
		// TODO Auto-generated constructor stub
	}

}
