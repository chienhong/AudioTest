package com.example.audiotest.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

public class AudioRecvDelayTime {
	private static final String LOG_TAG = "UDPReceiverDelayTime";
	
	private static final int MAX_QUEUE_SIZE = 3000;
	private static final String LOG_FOLDER = "audio_log";
	private static final String LOG_FILE_NAME = "RecvTimeLog.log";
	
	private ArrayList<Long> mRecvDelayTimeList = new ArrayList<Long>(MAX_QUEUE_SIZE);
	
	public AudioRecvDelayTime(){

	}
	
	public long getRecvDelayTime(){
		long delayTime = 0;
		if(mRecvDelayTimeList.size() > 0){
			delayTime = mRecvDelayTimeList.get(0);
			mRecvDelayTimeList.remove(0);
		}
		return delayTime;
	}
	
	public void readLogFile(){
		File logFolder = Environment.getExternalStoragePublicDirectory(LOG_FOLDER);
		if (!logFolder.exists()) logFolder.mkdirs();
		
		File logFile = new File(logFolder, LOG_FILE_NAME);
		if(!logFile.exists()){
			Log.e(LOG_TAG, String.format("Log file not exsit: %s/%s", LOG_FOLDER, LOG_FILE_NAME));
			return;
		}
		BufferedReader in = null;

        try {
        	String line = "";
            in = new BufferedReader(new FileReader(logFile));
            while ((line = in.readLine()) != null){
            	//stringBuilder.append(line);
            	if(line.length() < 1){
            		continue;
            	}
            	if(line.charAt(0) != 'a'){
            		String[] intervalList = line.split(",");
            		for(int index=0; index<intervalList.length; index++){
            			mRecvDelayTimeList.add(Long.valueOf(intervalList[index]));
            		}
            	}
            }
            in.close();
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
	}
	
	public void printLogList(){
		String strLogList = "";
		for(int index=0; index < mRecvDelayTimeList.size(); index++){
			if(0 == ((index+1)%25)){
				Log.d(LOG_TAG, String.format("RecvDelayTime: %s", strLogList));
				strLogList = "";
			}
			else{
				strLogList = strLogList + Long.valueOf(mRecvDelayTimeList.get(index)) + ",";
			}
		}
	}
}
