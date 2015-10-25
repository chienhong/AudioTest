package com.example.audiotest.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.audiotest.config.UDPTestConfig;

import android.util.Log;

public class UDPSender implements Runnable {

	private static AtomicInteger mPacketNumber = new AtomicInteger(0);
	private static final String LOG_TAG = "UDPSender";
	private static int UDP_SERVER_PORT = 1024;
	private static String mSendIPAddr = null;
	private static DatagramSocket mDatagramSocket = null;


	private static InetAddress mInetTargetHost = null;
	
	public UDPSender( String sendIP, int serverPort) {
		UDP_SERVER_PORT = serverPort;
		mSendIPAddr = sendIP;
        mPacketNumber.set(0);
        
        initUDPSocket();
	}
	
	private void initUDPSocket(){
		
		if( null != mDatagramSocket){
			closeUDPSocket();
    	}
		
		try {
    		//Create for any port
			mDatagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mDatagramSocket = null;
			return;
		}
		
		if( null == mInetTargetHost ){
			try {
				mInetTargetHost = InetAddress.getByName(mSendIPAddr);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				//return;
				closeUDPSocket();
			}
		}
	}
	
	public void stop(){
		closeUDPSocket();
	}
	
	public void sendAudioData(byte[] data){
		int totalLength = 10 + data.length;
		byte[] msgToSend = getAudioData(data, totalLength);
		startSendAudioPacket(msgToSend, totalLength);
	}
	
	public byte[] getAudioData(byte[] data, int totalLength){
		
		int iPayLoadLength = totalLength;
		byte[] packetData = new byte[totalLength];
		//2 Bytes of OPCode reserved: 'OP'
        packetData[0] = 'O';
        packetData[1] = 'P';
		
        //payload length
		int length = iPayLoadLength-6;
        packetData[2] = (byte) (length >>> 24);
        packetData[3] = (byte) (length >>> 16);
        packetData[4] = (byte) (length >>> 8);
        packetData[5] = (byte) (length >>> 0);

        int iNumber = 0;
		iNumber = mPacketNumber.addAndGet(1);
		//4 bytes for packet number
		packetData[9] = (byte) (iNumber >>> 0);
		packetData[8] = (byte) ( iNumber >>> 8);
		packetData[7] = (byte) ( iNumber >>> 16);
		packetData[6] = (byte) ( iNumber >>> 24);
		
		if(data.length > 0){
			System.arraycopy(data, 0, packetData, 10, data.length);
		}
		return packetData;
	}


	public void startSendAudioPacket( byte[] packet_data, int packet_length ) {
        
        DatagramPacket datagramPacket = new DatagramPacket(packet_data, packet_length, mInetTargetHost, UDP_SERVER_PORT);
        try {
			mDatagramSocket.send(datagramPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.d(LOG_TAG, "startSendMsg error.");
			e.printStackTrace();
		}

	}
	
	public void closeUDPSocket(){
		
		mDatagramSocket.close();
		mDatagramSocket = null;
		
		mPacketNumber.set(0);
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
