package com.example.audiotest.config;

public class UDPTestConfig {

	private static UDPTestConfig SINGLETON_INSTANCE = null;
	
	public static final int UI_TEST_MODE_SINGLE_WAY = 1;
	public static final int UI_TEST_MODE_ROUND_WAY = 2;
	
	public static int UDP_LISTEN_PORT = 1024;
	public static int UDP_TARGET_PORT = 1024;
	public static String mTargetIPAddress = "192.168.0.108";
	public static int mTestMode = UI_TEST_MODE_SINGLE_WAY;

	private int mUDPSendDelayRate = 0;
	private int mUDPSendDelayTime = 40;
	private int mUDPRecvDelayRate = 0;
	private int mUDPRecvDelayTime = 40;
	private int mSendDelayBias = 0;
	private int mRecvDelayBias = 0;
	private int mInterruptDelayTime = 2000;
	
	
	private UDPTestConfig() {
		SINGLETON_INSTANCE = this;
	}

	public static UDPTestConfig GetInstance() {
		if (SINGLETON_INSTANCE == null){
			SINGLETON_INSTANCE = new UDPTestConfig();
		}

		return SINGLETON_INSTANCE;
	}
	
	public void setTargetIPAddress(String strTargetIP)
	{
		mTargetIPAddress = strTargetIP;
	}
	
	public String getTargetIPAddress(){
		return mTargetIPAddress;
	}
	
	public void setUDPListenPort(int port){
		UDP_LISTEN_PORT = port;
	}
	
	public int getUDPListenPort(){
		return UDP_LISTEN_PORT;
	}
	
	public void setUDPTargetPort(int port){
		UDP_TARGET_PORT = port;
	}
	
	public int getUDPTargetPort(){
		return UDP_TARGET_PORT;
	}
	
	public int getUDPSendDelayRate(){
		return mUDPSendDelayRate;
	}
	
	public void setUDPSendDelayRate(int delayRate){
		mUDPSendDelayRate = delayRate;
	}
	
	public int getUDPSendDelayTime(){
		return mUDPSendDelayTime;
	}
	
	public void setUDPSendDelayTime(int delayTime){
		mUDPSendDelayTime = delayTime;
	}
	
	public int getUDPRecvDelayRate(){
		return mUDPRecvDelayRate;
	}
	
	public void setUDPRecvDelayRate(int delayRate){
		mUDPRecvDelayRate = delayRate;
	}
	
	public int getUDPRecvDelayTime(){
		return mUDPRecvDelayTime;
	}
	
	public void setUDPRecvDelayTime(int delayTime){
		mUDPRecvDelayTime = delayTime;
	}
	
	public int getSendDelayBias(){
		return mSendDelayBias;
	}
	
	public void setSendDelayBias(int bias){
		mSendDelayBias = bias;
	}
	
	public int getRecvDelayBias(){
		return mRecvDelayBias;
	}
	
	public void setRecvDelayBias(int bias){
		mRecvDelayBias = bias;
	}
	
	public int getInterruptDelayTime(){
		return mInterruptDelayTime;
	}
	
	public void setInterruptDelayTime(int time){
		mInterruptDelayTime = time;
	}
	
}
