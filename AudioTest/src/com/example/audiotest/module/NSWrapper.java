package com.example.audiotest.module;

public class NSWrapper {

	private static final String LOG_TAG = "NSWrapper";
	
	public static final int NS_MODE_MILD = 0;
	public static final int NS_MODE_MEDIUM = 1;
	public static final int NS_MODE_AGGRESSIVE = 2;

	static{
		try{
			System.loadLibrary("webrtc_nsx");
		}
		catch(UnsatisfiedLinkError ule){
			ule.printStackTrace();
		}
	}
	
	public native int nsInit(int samplerate);
	public native int nsDestroy();
	public native int nsProcess(short[] speechFrame, short[] speechFrameHB, short[] outputFrame, short[] outputFrameHB, int sampleCount);
	public native int nsSetPolicy(int nsMode);

	private boolean mIsNSInit = false;
	
	public NSWrapper(int samplerate, int nsMode){
		int result = nsInit(samplerate);
		if(0 == result){
			mIsNSInit = true;
			nsSetPolicy(nsMode);
		}
		else{
			mIsNSInit = false;
		}
	}
	
	public boolean getIsNSInit(){
		return mIsNSInit;
	}
	
}
