package com.example.audiotest.module;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

/**
 * Audio Data Logger
 * 	Logging encoded/decoded audio data size
 *
 */
public class AudioDataLogger {

	private static final String LOG_TAG = "AudioDataLogger";
	private static final String LOG_FOLDER = "audio_log";
	
	public static final int MODE_ENC_DATA = 1;	//send mode
	public static final int MODE_DEC_DATA = 2;	//recv mode
	private static final int MAX_LIST_SIZE = 1000;
	
	private AudioConfig mAudioConfig = null;
	private OpusConfig mOpusConfig = null;
	
	private int mMode = 0;
	private static ArrayList<Integer> mEncDataSizeList = null;
	private int mEncOriDataSize = 0;
	private static ArrayList<Integer> mDecDataSizeList = null;
	private int mDecOriDataSize = 0;
	
	public AudioDataLogger(int mode, int oriDataSize) {
		mMode = mode;
		if(MODE_ENC_DATA == mMode){
			mEncDataSizeList = new ArrayList<Integer>(MAX_LIST_SIZE);
			mEncOriDataSize = oriDataSize;
		}
		else if(MODE_DEC_DATA == mMode){
			mDecDataSizeList = new ArrayList<Integer>(MAX_LIST_SIZE);
			mDecOriDataSize = oriDataSize;
		}
		mAudioConfig = AudioConfig.GetInstance();
		mOpusConfig = OpusConfig.GetInstance();
	}
	
	public void recordEncDataSize(int size){
		if(null != mEncDataSizeList){
			if(mEncDataSizeList.size() < MAX_LIST_SIZE){
				mEncDataSizeList.add(size);
			}
		}
	}
	
	public void recordDecDataSize(int size){
		if(null != mDecDataSizeList){
			if(mDecDataSizeList.size() < MAX_LIST_SIZE){
				mDecDataSizeList.add(size);
			}
		}
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
	
	public void saveEncDataSize(){
		File logFolder = Environment.getExternalStoragePublicDirectory(LOG_FOLDER);
		if (!logFolder.exists()) logFolder.mkdirs();
		//File logfile = new File(logFolder, String.format("EncDataSize_%tc.log", new Date()));
		
		String newFileName = getCurrentTime("yyyy-MM-dd_HH-mm-ss");
		
		if(mAudioConfig.getAutoRename() 
				|| mAudioConfig.getOpusRename()
				|| mAudioConfig.getAudioEffectRename()
				|| mAudioConfig.getAGCRename()
				|| mAudioConfig.getAECRename()
				|| mAudioConfig.getNSRename()
				|| mAudioConfig.getPacketLostRename()){
			newFileName = AudioTestFileName.getFileName();
		}
		
		File logfile = new File(logFolder, String.format("EncDataLog_%s.log", newFileName));
		
		try {
			FileOutputStream output = new FileOutputStream(logfile, true);
			PrintWriter pwrite = new PrintWriter(output);
			long totalValue = 0;
			int average = 0;
			synchronized(mEncDataSizeList) {
				pwrite.write(String.format("Original Enc Data Size=%d\n", mEncOriDataSize));
				for(int index=0; index < mEncDataSizeList.size(); index++) {
					int value = mEncDataSizeList.get(index);
					pwrite.write(String.format("%d,", value));
					totalValue += value;
				}
				if(mEncDataSizeList.size() > 0){
					average = (int) totalValue/mEncDataSizeList.size();
					pwrite.write(String.format("\nAverage Enc Data Size=%d\n", average));
				}
				mEncDataSizeList.clear();
			}
			pwrite.flush();
			pwrite.close();
			Log.d(LOG_TAG, String.format("Logs are written to %s", logfile.getAbsolutePath()));
		} catch (FileNotFoundException e) {
			Log.d(LOG_TAG, "Failed to write log file.");
		}
	}
	
	public void saveDecDataSize(){
		File logFolder = Environment.getExternalStoragePublicDirectory(LOG_FOLDER);
		if (!logFolder.exists()) logFolder.mkdirs();
		//File logfile = new File(logFolder, String.format("DecDataSize_%tc.log", new Date()));
		String newFileName = getCurrentTime("yyyy-MM-dd_HH-mm-ss");
		File logfile = new File(logFolder, String.format("DecDataLog_%s.log", newFileName));
		
		try {
			FileOutputStream output = new FileOutputStream(logfile, true);
			PrintWriter pwrite = new PrintWriter(output);
			long totalValue = 0;
			int average = 0;
			synchronized(mDecDataSizeList) {
				pwrite.write(String.format("Original Dec Data Size=%d\n", mDecOriDataSize));
				for(Integer log: mDecDataSizeList) {
					totalValue += log.intValue();
					pwrite.write(String.format("%d\n", log.intValue()));
				}
				if(mDecDataSizeList.size() > 0){
					average = (int) totalValue/mDecDataSizeList.size();
					pwrite.write(String.format("Average Dec Data Size=%d\n", average));
				}
				mDecDataSizeList.clear();
			}
			pwrite.flush();
			pwrite.close();
			Log.d(LOG_TAG, String.format("Logs are written to %s", logfile.getAbsolutePath()));
		} catch (FileNotFoundException e) {
			Log.d(LOG_TAG, "Failed to write log file.");
		}
	}
	
}
