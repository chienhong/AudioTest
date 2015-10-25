package com.example.audiotest.module;

import android.util.Log;


public class AecmWrapper {

	private static final String LOG_TAG = "AecmWrapper";
	
	public static final int AEC_PROCESS_SAMPLE_COUNT_80 = 80;
	public static final int AEC_PROCESS_SAMPLE_COUNT_160 = 160;
	
	private int mSampleRate = 0;
	private boolean mIsAecmInit = false;
	private int mProcessSampleCount = 80;
	
	static{
		try{
			System.loadLibrary("webrtc_aecm");
		}
		catch(UnsatisfiedLinkError ule){
			ule.printStackTrace();
		}
	}
	
	public native int aecmInit(int samplerate);
	public native int aecmDestroy();
	public native int aecmBufferFarend(short[] farEnd, int sampleCount);
	public native int aecmAECProcess(short[] nearEnd, short[] output, int sampleCount, int delayTimeMs);
	public native int aecmAECProcessWithNS(short[] nearEndNoise, short[] nearEndClean, short[] output, int sampleCount, int delayTimeMs);
	public native int aecmSetProcessSampleCount(int sampleCount);
	
	public AecmWrapper(int sampleRate, int processSampleCount){
		int result = aecmInit(sampleRate);
		mSampleRate = sampleRate;
		mProcessSampleCount = processSampleCount;
		if(0 != result){
			//Log.e(LOG_TAG, "Init AECM failed.");
			Log.d(LOG_TAG, String.format("aecmInit failed with samplerate=%d, result=%d", sampleRate, result));
			mIsAecmInit = false;
		}
		else{
			Log.d(LOG_TAG, String.format("aecmInit success with samplerate=%d, result=%d", sampleRate, result));
			mIsAecmInit = true;
			/*
			if(sampleRate == 8000){
				mProcessSampleCount = 80;
			}
			else{
				mProcessSampleCount =160;
			}*/

			aecmSetProcessSampleCount(mProcessSampleCount);
		}
	}
	
	public boolean getIsAecmInitSuccess(){
		return mIsAecmInit;
	}
	
	public void destroyAEC(){
		int result = aecmDestroy();
		if(0 == result){
			Log.d(LOG_TAG, String.format("aecmDestroy success result=%d", result));
		}
		else{
			Log.d(LOG_TAG, String.format("aecmDestroy failed result=%d", result));
		}
	}
	
}
