package com.example.audiotest.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

//Audio Test Control:
//	Simulate Audio Packet Lost Behavior
public class AudioRecvSeqNo {
	private static final String LOG_TAG = "AudioRecvSeqNo";
	private static final String LOG_FOLDER = "audio_log";
	private static final String LOG_FILE_NAME = "RecvSeqNo.log";
	
	@SuppressLint("UseSparseArrays") 
	private HashMap<Integer, Integer> mRecvSeqNoMap = new HashMap<Integer, Integer>();
	
	public AudioRecvSeqNo(){
		
	}
	
	public void clearSeqNoMap(){
		mRecvSeqNoMap.clear();
	}
	
	public boolean isContainsSeqNo(int seqNo){
		if(mRecvSeqNoMap.containsKey(seqNo)){
			return true;
		}
		return false;
	}
	
	public void readSeqNoProfile(){
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
            	if(line.charAt(0) != 'l'){
            		String[] seqNoList = line.split(",");
            		for(int index=0; index<seqNoList.length; index++){
            			int keyValue = Integer.valueOf(seqNoList[index]);
            			mRecvSeqNoMap.put(keyValue, keyValue);
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
	
	
}
