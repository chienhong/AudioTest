package com.example.audiotest.module;

import android.util.Log;


public class AGCWrapper {

	private static final String LOG_TAG = "AGCWrapper";
	
	public static final int AGC_MODE_UNCHANGED = 1;
	public static final int AGC_MODE_ADAPTIVE_ANALOG = 2;
	public static final int AGC_MODE_ADAPTIVE_DIGITAL = 3;
	public static final int AGC_MODE_FIXED_DIGITAL = 4;
	
	static{
		try{
			System.loadLibrary("webrtc_agc");
		}
		catch(UnsatisfiedLinkError ule){
			ule.printStackTrace();
		}
	}
	
	public native int agcInit(int samplerate, int agcMode, int minVolumeLevel, int maxVolumeLevel);
	public native int agcDestroy();
	public native int agcProcess(short[] inNear, short[] inNearH, short[] output, short[] outputH, int samples, int inMicLevel, int[] outMicLevel, int hasEcho);
	public native int agcAddFarend(short[] inFarEnd, int inputSize);
	public native int agcSetAGCConfig(int targetLvDbfs, int compressionGaindB, int limiterEnable);
	public native int agcSetProcessSampleTime(int procTimeMs);
	
	private int mSampleRate = 0;
	private boolean mIsAGCInit = false;
	private int mAGCMode = 0;
	
	public AGCWrapper(int samplerate, int agcMode, int minMicLevel, int maxMicLevel){
		int result = agcInit(samplerate, agcMode, minMicLevel, maxMicLevel);
		mSampleRate = samplerate;
		mAGCMode = agcMode;
		if(0 == result){
			mIsAGCInit = true;
		}
		else{
			Log.d(LOG_TAG, "AGC init failed.");
			mIsAGCInit = false;
		}
	}
	
	public boolean getIsAGCInit(){
		return mIsAGCInit;
	}
	
}
