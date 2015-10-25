package com.example.audiotest.module;

public class VADWrapper {

	private static final String LOG_TAG = "VADWrapper";
	
	public static final int VAD_SEND_SIDE = 1;
	public static final int VAD_RECV_SIZE = 2;
	
	static{
		try{
			System.loadLibrary("webrtc_vad");
		}
		catch(UnsatisfiedLinkError ule){
			ule.printStackTrace();
		}
	}
	
	public native int vadInit();
	public native void vadDestroy();
	public native int vadProcess(int sampleRate, short[] audioFrame, int frameLength, int side);
	
	private boolean mIsVADInit = false;
	
	public VADWrapper(){
		mIsVADInit = false;
		int result = vadInit();
		if(0 == result){
			mIsVADInit = true;
		}
		else{
			android.util.Log.d(LOG_TAG, String.format("VAD Init failed, result=%d", result));
		}
	}
	
	public boolean getIsVADInit(){
		return mIsVADInit;
	}
	
}
