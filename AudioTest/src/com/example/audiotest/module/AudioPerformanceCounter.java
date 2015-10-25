package com.example.audiotest.module;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

public class AudioPerformanceCounter {

	private static final String LOG_TAG = "AudioPerformanceCounter";
	private static final int MAX_LIST_SIZE = 5000;
	private static final String LOG_FOLDER = "audio_log";
	public static final String LOG_FILE_NAME = "db_%s.log";
	
	private ArrayList<Integer> mMaxAmplitudeList = new ArrayList<Integer>(MAX_LIST_SIZE);
	private ArrayList<Integer> mDBValueList = new ArrayList<Integer>(MAX_LIST_SIZE);
	private int mMaxAmplitude = 0;
	private int mMaxDBValue = -200;
	private int mLastDBValue = 0;
	
	private static final int AVG_DB_ARRAY_SIZE = 15;
	private int[] mAvgDBArray = new int[AVG_DB_ARRAY_SIZE];
	private int mAvgDBIndex = 0;
	private int mAvgDBValue = 0;
	private int mTotalDBValue = 0;
	
	public AudioPerformanceCounter(){
		
		for(int index=0; index < AVG_DB_ARRAY_SIZE; index++){
			mAvgDBArray[index] = 0;
		}
	}
	
	public void addAmplitude(int value){
		countDBValue(value);
		if(value > mMaxAmplitude){
			mMaxAmplitude = value;
		}
		if(MAX_LIST_SIZE > mMaxAmplitudeList.size()){
			mMaxAmplitudeList.add(value);
		}
	}
	
	public int getMaxAmplitude(){
		return mMaxAmplitude;
	}
	
	public int getMaxDBValue(){
		return mMaxDBValue;
	}
	
	private void countDBValue(int value){
		//EX: 20*log10 (21188/32767)
		// amplitude: 1024 := -30dB
		// amplitude: 2048 := -24dB
		double dbLogValue1 = Math.log10(value*10000/32767);		
		double dbLogValue = (dbLogValue1 - 4)*20;
		
		int dbValue = (int) dbLogValue;

		if(dbValue > mMaxDBValue){
			mMaxDBValue = dbValue;
		}
		mLastDBValue = dbValue;
		
		countAvgDBValue(mLastDBValue);
		
		if(MAX_LIST_SIZE > mDBValueList.size()){
			if(dbValue > -120){
				mDBValueList.add(dbValue);
			}
		}
	}
	
	private void countAvgDBValue(int dbValue){
		if(mAvgDBIndex < AVG_DB_ARRAY_SIZE){
			mAvgDBArray[mAvgDBIndex] = dbValue;
			mTotalDBValue += dbValue;
			//android.util.Log.d(LOG_TAG, String.format("AudioPerformanceCounter mTotalDBValue=%d, dbValue=%d", mTotalDBValue, dbValue));
		}
		else{
			mAvgDBValue = mTotalDBValue/AVG_DB_ARRAY_SIZE;

			mAvgDBIndex = 0;
			mAvgDBArray[mAvgDBIndex] = dbValue;
			mTotalDBValue = dbValue;			
		}
		mAvgDBIndex++;
	}
	
	public int getAvgDBValue(){
		int avgValue = mAvgDBValue;
		if(0 != mAvgDBValue){
			mAvgDBValue = 0;
		}
		return avgValue;
	}
	
	public int getLastDBValue(){
		return mLastDBValue;
	}
	
	private String getCurrentTime(String szFormat){
		SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat();
		
		try {
			sSimpleDateFormat.applyPattern(szFormat);
			return sSimpleDateFormat.format(System.currentTimeMillis());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public void writeLogFile(){
		File logFolder = Environment.getExternalStoragePublicDirectory(LOG_FOLDER);
		if (!logFolder.exists()) logFolder.mkdirs();
		
    	String strTime = getCurrentTime("yyyy-MM-dd HH-mm-ss-SSS");
    	
		File logfile = new File(logFolder, String.format(LOG_FILE_NAME, strTime));
		try {
			FileOutputStream output = new FileOutputStream(logfile, true);
			PrintWriter pwrite = new PrintWriter(output);
			synchronized(mDBValueList) {
				long average = 0;
				for(Integer log: mDBValueList){
					pwrite.write(log.toString() + ",");
					average += log;
				}
				average = average/mDBValueList.size();
				pwrite.write("\naverate:" + average + "\n");
				mDBValueList.clear();
			}
			pwrite.flush();
			pwrite.close();
			Log.d(LOG_TAG, String.format("DB logs are written to %s", logfile.getAbsolutePath()));
		} catch (FileNotFoundException e) {
			Log.d(LOG_TAG, "Failed to write log file.");
		}
	}
}
