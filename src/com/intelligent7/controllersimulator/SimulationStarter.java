/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intelligent7.controllersimulator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

//import com.google.android.gms.gcm.GoogleCloudMessaging;



/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class SimulationStarter extends Thread {
    //public static final int NOTIFICATION_ID = 1;
    public static int NOTIFICATION_ID;
    public static final String DAEMON_COMMAND="COMMAND";
    public static final ReentrantLock fileLock=new ReentrantLock(); 
    
    Logger log;
    public SimulationStarter() {
        super();
    }
    public static final String TAG = "EAS JOB";

    //@Override
   // protected void onHandleIntent(Intent intent) {
        //onBind(intent);
        //Bundle extras = intent.getExtras();
        //GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        //String messageType = null;//gcm.getMessageType(intent);

        //if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.

            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                for (int i = 0; i < 5; i++) {
                    Log.i(TAG, "Working... " + (i + 1)
                            + "/5 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                }
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.

            } */
           // sendNotification("Received: " + extras.toString());
           // Log.i(TAG, "Received: " + extras.toString());
        //}
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        //EASBroadcastReceiver.completeWakefulIntent(intent);
   // }

    static TcpConnectDaemon mDaemon=null;
    ArrayBlockingQueue<String> outBoundMailBox;
  
    
    public static final String SERVER_IP="server_ip";
    public static final String SERVER_PORT="server_port";
    void startDaemon()
    {
        mDaemon=null;

        mDaemon=new TcpConnectDaemon(mHost, Integer.parseInt(mPort));
        mDaemon.setModeInterval(TcpConnectDaemon.MODE_REPEAT, 60*1000);
        Vector<String> keep=null;
        if (outBoundMailBox!= null && outBoundMailBox.size() > 0)
        {
            keep=new Vector<String>();
            while (outBoundMailBox.size()> 0)
            {
                try {
                    keep.add(outBoundMailBox.take());
                } catch(InterruptedException e){}
            }
        }
        outBoundMailBox=mDaemon.getOutDataQ();
        if (keep!=null && keep.size() > 0)
        {
            for (int i=0; i<keep.size(); i++){
                try {
                    outBoundMailBox.put(keep.get(i));
                } catch(InterruptedException e){}
            }
            keep.clear();
            keep=null;
        }
        //mDaemon.attachToService(this);
        mDaemon.start();
        // need start schedule too; MainActivity.N_BOOT_PARAMS, nBootParam); //HH:MM-on minutes-off minutes-cycle last for minutes
        // MainActivity.ONE_BOOT_PARAMS, bootParam); //  yy/mm/dd:hh:mm-last for minutes
    }

    private static HashMap<String, String> mcuDictionary;
    static public HashMap<String, String> getMcuCodeDictionary(){

        if (mcuDictionary==null) buildCodeDictionary();
        return mcuDictionary;
    }

    public static String getChinese(String code){
        String retS="";
        if (mcuDictionary==null) buildCodeDictionary();
        return retS+mcuDictionary.get(code);
    }
    private static void buildCodeDictionary() {
        mcuDictionary=new HashMap<String, String>();
        mcuDictionary.put("M1-00","冷气启动");
        mcuDictionary.put("M1-01","暖气启动");
        mcuDictionary.put("M2","车载机密码更换");
        mcuDictionary.put("M3","手机號码设定");
        mcuDictionary.put("M4-00","立即关闭引擎");
        mcuDictionary.put("M4-01","立即关闭冷气");
        mcuDictionary.put("M5","立即启动");
        //mcuDictionary.put("M5","立即启动");

        mcuDictionary.put("S110", "暖气设定成功");

        mcuDictionary.put("S111", "暖气设定失败");

        mcuDictionary.put("S100", "冷氣設定成功");

        mcuDictionary.put("S101", "冷氣設定失敗");

        mcuDictionary.put("S200", "车载机密码设定成功");

        mcuDictionary.put("S201", "车载机密码设定失败");

        mcuDictionary.put("S300", "手机号码设定成功");

        mcuDictionary.put("S301", "手机号码设定失败");

        mcuDictionary.put("S400", "引擎已关闭");

        mcuDictionary.put("S401", "引擎由车主启动,不能关闭");

        mcuDictionary.put("S410", "冷氣已關閉");

        mcuDictionary.put("S411", "冷氣關閉失敗");

        mcuDictionary.put("S500", "引擎已启动");

        mcuDictionary.put("S501", "引擎启动失败");

        mcuDictionary.put("S502", "引擎启动成功");

        mcuDictionary.put("S503", "偷车");

        mcuDictionary.put("S504", "暖车失败");

        mcuDictionary.put("S505", "暖车完毕");

        mcuDictionary.put("S999", "手机号码未授权");
    }


    public static String getResponse(String inCode){
    	long iSuccess=Math.round(Math.random()*1000)%2;
    	String retCode="S"+inCode.charAt(1)+(new DecimalFormat("00").format(iSuccess)); 
    	return retCode;
    }
    //static //String header=MainActivity.getFileHeader();
    
    
        boolean isWakenByReset=false;
        public void refresh(){

        }
        long start_time;
        long on_time;
        long off_time;
        long end_time;
        void readParameter()
        {

        }
        public void setResetStatus(boolean ya)
        {
            isWakenByReset=ya;
        }
        
        public static void readFromResourceFile(String fileName, HashMap<String, String> params)
    	{
    		  
    	  BufferedReader reader=null;
    	  try {
    		  reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
    	  
    		  String aLine;
    		  while ((aLine=reader.readLine())!=null)
    		  {
    			  if (aLine.length() < 2) continue;
    			  int i0=0; while (aLine.charAt(i0)<= ' ') i0++;
    			  int iE=i0; while (++iE<aLine.length() && aLine.charAt(iE) > ' ');
    			  String key=aLine.substring(i0, iE).toUpperCase();
    			  i0=iE; 
    			  while (aLine.charAt(i0)!= '=' && ++i0 <aLine.length());
    			  iE=++i0; 
    			  while (++iE<aLine.length() && aLine.charAt(iE) > ' ');
    			  String value=aLine.substring(i0, iE);//no case change .toUpperCase();
    			  if (key.length() > 0)
    				  params.put(key, value);
    		  }
    	    
    	  } catch (IOException e) {
    		  e.printStackTrace();
    	    System.out.println("Could not read file "+fileName);
    	  } finally {
    	    try {
    	      reader.close();
    	    } catch (IOException e) {
    	    	e.printStackTrace();
    	    	System.out.println("Exception closing "+fileName);
    	    }
    	  }
    	    
    	}
    	
    	private static HashMap<String, String> config=new HashMap<String, String>();
    	
    	public static String getResource(String key){
    		return config.get(key);
    	}
    	String SIM_ICCID;
    String mHost;
    String mPort;
    int iPort;
    long mStopTime;
    public PostOffice myPostOffice;
    	void init()
    	{
    		//mGcmSender=new SendGcmMessager();
    		log=Logger.getAnonymousLogger();
    		readFromResourceFile("SimulatorResource", config);
    		mHost=config.get("CMD_HOST");
    		if (mHost == null) {
    			log.warning("Missing server ip information");
    			System.exit(-1);
    		}
    		 mPort=config.get("CMD_PORT");
    		if (mPort == null) {
    			log.warning("Missing port information");
    			System.exit(-1);
    		}
    		SIM_ICCID=config.get("SIM_ICCID");
    		if (SIM_ICCID == null) {
    			log.warning("Missing SIM_ICCID information");
    			System.exit(-1);
    		}
    		System.out.println("Connecting to "+mHost+"/"+mPort+" with "+SIM_ICCID);
    		setPort(Integer.parseInt(mPort));
    		setStopTime(0);
    		
    		//myPostOffice=new PostOffice();
    		//myPostOffice.start();
    		
    	}

    	public void setPort(int p)
    	{
    		iPort=p;
    	}
    	public void setStopTime(long t)
    	{
    		mStopTime=t;;
    	}

    	boolean stopNow;

        public void run()
        {
        	boolean notStop=true;
        	while (notStop){
        		init();
        		startDaemon();
        		if (mDaemon != null && mDaemon.isAlive())
				try {
					mDaemon.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					notStop=false;
					
					e.printStackTrace();
					if (mDaemon != null && mDaemon.isAlive()){
						mDaemon.setStopFlag(true);
						try {
							mDaemon.join();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
        	}
        }
        public static void main(String[] args)
        {
        	SimulationStarter aQA=new SimulationStarter();
        	aQA.start();
        	try {
        		aQA.join();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
}
