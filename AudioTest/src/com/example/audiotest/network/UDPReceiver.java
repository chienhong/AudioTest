package com.example.audiotest.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.audiotest.config.UDPTestConfig;

import android.os.Handler;
import android.util.Log;

public class UDPReceiver {

	private static int mUDPListeningPort = 0;
	private static final String LOG_TAG = "UDPReciever";
	private AtomicBoolean mIsRunning = new AtomicBoolean(false);
	private static DatagramSocket datagramSocket = null;
	private int mTestMode = UDPTestConfig.UI_TEST_MODE_SINGLE_WAY;
	
	private IUDPReceiveInterface mUDPRecvIntf;
	
	public UDPReceiver(int listenPort, int testMode, IUDPReceiveInterface udpRecvIntf) {
		mUDPListeningPort = listenPort;
		mTestMode = testMode;
		mUDPRecvIntf = udpRecvIntf;
	}

	public void start( final Handler msgHandler ) {

		mIsRunning.set(true);
        Thread background=new Thread(new Runnable() {
            public void run() {
            	datagramSocket = null;
                try {
                    //String data;
                    byte[] receiveData = new byte[1024];
                    DatagramPacket dataPacket = new DatagramPacket(receiveData, receiveData.length);
                    
                    if( null == datagramSocket ){
                    	datagramSocket = new DatagramSocket(mUDPListeningPort);
                    	Log.d(LOG_TAG, String.format("datagramSocket: broadcase=%b", datagramSocket.getBroadcast()));
                    }
                    
                    while(mIsRunning.get()) {
                        datagramSocket.receive(dataPacket);
                        parseData( receiveData, dataPacket.getLength(), dataPacket.getAddress(), dataPacket.getPort() );
                    }
                }
                catch (Throwable t) {
                	Log.d(LOG_TAG,"catch exception: "+t.getMessage().toString());
                }
            }
        });
        background.start();
	}
	
	public void parseData(byte data[], int length, InetAddress targetAddress, int targetPort){
		//2 Bytes of OPCode reserved: 'OP'
		byte[] bOPCode = new byte[2];
		bOPCode[0] = data[0];
		bOPCode[1] = data[1];
		
		//4 Bytes of content data length
		int iLength = 0;
		iLength = (int) data[2];
		iLength = (iLength <<8) + ((int) data[3]);
		iLength = (iLength <<8) + ((int) data[4]);
		iLength = (iLength <<8) + ((int) data[5]);

		//4 Bytes of packet SEQNO: sequence number from 0
		int iSeqNo = 0;
		iSeqNo = ((int)data[6] &0xFF);
		iSeqNo = (iSeqNo << 8) + ((int) data[7] &0xFF);
		iSeqNo = (iSeqNo << 8) + ((int) data[8] &0xFF);
		iSeqNo = (iSeqNo << 8) + ((int) data[9] &0xFF);
		
		if(null != mUDPRecvIntf){
			int dataLength = length - 10;
			if(dataLength > 0){
				byte[] destination = new byte[dataLength];

				System.arraycopy(data, 10, destination, 0, dataLength);
				mUDPRecvIntf.RecvData(destination);
			}
		}
		
	}
	
	public void closeUDPSocket(){
        if( null != datagramSocket ){
        	datagramSocket.close();
        	datagramSocket = null;
        }
	}
	
	public void stop(){
		mIsRunning.set(false);
		closeUDPSocket();
	}
	
}
