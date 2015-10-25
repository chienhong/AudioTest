package com.example.audiotest.module;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;


public class FrameAmplitudeDiffCounter {

	private static final String LOG_TAG = "FrameAmplitudeDiffCounter";
	
	private static final int MAX_LIST_SIZE = 2000;
	private static final String LOG_FOLDER = "audio_log";
	
	private ArrayList<Short> mFrameLastAmplitudeCh1 = new ArrayList<Short>(MAX_LIST_SIZE);
	private ArrayList<Short> mFrameLastAmplitudeCh2 = new ArrayList<Short>(MAX_LIST_SIZE);
	
	private int mCh1PeekDiffValue = -1;
	private int mCh2PeekDiffValue = -1;
	private int mDiffAmplitudeCount100 = 0;		//diff value below 100
	private int mDiffAmplitudeCount200 = 0;		//diff value between 101 ~ 200
	private int mDiffAmplitudeCount500 = 0;		//diff value between 201 ~ 500
	private int mDiffAmplitudeCount1000 = 0; 	//diff value between 501 ~ 1000
	private int mDiffAmplitudeCount2500 = 0;	//diff value between 1001 ~ 2500
	private int mDiffAmplitudeCount5000 = 0;	//diff value between 2501 ~ 5000
	private int mDiffAmplitudeCountAbove = 0;	//diff value above 3000
	
	public FrameAmplitudeDiffCounter(){
		
	}
	
	public void addFrameLastAmplitude(short ch1LastValue, short ch2LastValue){
		if(mFrameLastAmplitudeCh1.size() < MAX_LIST_SIZE){
			mFrameLastAmplitudeCh1.add(ch1LastValue);
		}
		
		if(mFrameLastAmplitudeCh2.size() < MAX_LIST_SIZE){
			mFrameLastAmplitudeCh2.add(ch2LastValue);
		}
	}
	
	private void recordDiffValueCount(int diffValue){
		if(diffValue <= 100){
			mDiffAmplitudeCount100++;
		}
		else if((diffValue > 100)
			&& (diffValue <= 200)){
			mDiffAmplitudeCount200++;
		}
		else if((diffValue > 200)
			&& (diffValue <= 500)){
			mDiffAmplitudeCount500++;
		}
		else if((diffValue > 500)
			&& (diffValue <= 1000)){
			mDiffAmplitudeCount1000++;
		}
		else if((diffValue > 1000)
			&& (diffValue <= 2500)){
			mDiffAmplitudeCount2500++;
		}
		else if((diffValue > 2500)
			&& (diffValue < 5000)){
			mDiffAmplitudeCount5000++;
		}
		else{
			mDiffAmplitudeCountAbove++;
		}
	}
	
	public String GetCurrentTime(String szFormat) {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
			simpleDateFormat.applyPattern(szFormat);
			return simpleDateFormat.format(System.currentTimeMillis());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public void saveCh1AmplitudeDiffResult(){
		File logFolder = Environment.getExternalStoragePublicDirectory(LOG_FOLDER);
		if (!logFolder.exists()) logFolder.mkdirs();
		String newFileName = GetCurrentTime("yyyy-MM-dd_HH-mm-ss");
		
		File logfile = new File(logFolder, String.format("FrameAmplitudeDiffCh1_%s.log", newFileName));
		
		try {
			FileOutputStream output = new FileOutputStream(logfile, true);
			PrintWriter pwrite = new PrintWriter(output);
			long totalDiffValue = 0;
			int avgDiff = 0;
			int preValue = 0;
			synchronized(mFrameLastAmplitudeCh1) {
				if(mFrameLastAmplitudeCh1.size() > 0){
					preValue = mFrameLastAmplitudeCh1.get(0);
				}
				int diffValue = 0;
				for(int index=0; index < mFrameLastAmplitudeCh1.size(); index++) {
					int value = mFrameLastAmplitudeCh1.get(index);
					diffValue = value - preValue;
					pwrite.write(String.format("%d,", diffValue));
					preValue = value;
					totalDiffValue += diffValue;
					recordDiffValueCount(Math.abs(diffValue));
				}
				if(mFrameLastAmplitudeCh1.size() > 0){
					avgDiff = (int) totalDiffValue/mFrameLastAmplitudeCh1.size();
					pwrite.write(String.format("\nAverage Audio Frame Amplitude Diff Value=%d\n", avgDiff));
				}
				pwrite.write(String.format("Diff Amplitude 0-100 Count=%d\n", mDiffAmplitudeCount100));
				pwrite.write(String.format("Diff Amplitude 101-200 Count=%d\n", mDiffAmplitudeCount200));
				pwrite.write(String.format("Diff Amplitude 201-500 Count=%d\n", mDiffAmplitudeCount500));
				pwrite.write(String.format("Diff Amplitude 501-1000 Count=%d\n", mDiffAmplitudeCount1000));
				pwrite.write(String.format("Diff Amplitude 1001-2500 Count=%d\n", mDiffAmplitudeCount2500));
				pwrite.write(String.format("Diff Amplitude 2501-5000 Count=%d\n", mDiffAmplitudeCount5000));
				pwrite.write(String.format("Diff Amplitude above 5001 Count=%d\n", mDiffAmplitudeCountAbove));
				
				mFrameLastAmplitudeCh1.clear();
			}
			pwrite.flush();
			pwrite.close();
			Log.d(LOG_TAG, String.format("Logs are written to %s", logfile.getAbsolutePath()));
		} catch (FileNotFoundException e) {
			//Log.e(LOG_TAG, "Failed to write log file.");
			e.printStackTrace();
		}
	}
	
	public void saveCh2AmplitudeDiffResult(){
		File logFolder = Environment.getExternalStoragePublicDirectory(LOG_FOLDER);
		if (!logFolder.exists()) logFolder.mkdirs();
		String newFileName = GetCurrentTime("yyyy-MM-dd_HH-mm-ss");
		
		File logfile = new File(logFolder, String.format("FrameAmplitudeDiffCh2_%s.log", newFileName));
		
		try {
			FileOutputStream output = new FileOutputStream(logfile, true);
			PrintWriter pwrite = new PrintWriter(output);
			long totalDiffValue = 0;
			int avgDiff = 0;
			int preValue = 0;
			synchronized(mFrameLastAmplitudeCh2) {
				if(mFrameLastAmplitudeCh2.size() > 0){
					preValue = mFrameLastAmplitudeCh2.get(0);
				}
				int diffValue = 0;
				for(int index=0; index < mFrameLastAmplitudeCh2.size(); index++) {
					int value = mFrameLastAmplitudeCh2.get(index);
					diffValue = value - preValue;
					pwrite.write(String.format("%d,", diffValue));
					preValue = value;
					totalDiffValue += diffValue;
					recordDiffValueCount(Math.abs(diffValue));
				}
				if(mFrameLastAmplitudeCh2.size() > 0){
					avgDiff = (int) totalDiffValue/mFrameLastAmplitudeCh2.size();
					pwrite.write(String.format("\nAverage Audio Frame Amplitude Diff Value=%d\n", avgDiff));
				}
				pwrite.write(String.format("Diff Amplitude 0-100 Count=%d\n", mDiffAmplitudeCount100));
				pwrite.write(String.format("Diff Amplitude 101-200 Count=%d\n", mDiffAmplitudeCount200));
				pwrite.write(String.format("Diff Amplitude 201-500 Count=%d\n", mDiffAmplitudeCount500));
				pwrite.write(String.format("Diff Amplitude 501-1000 Count=%d\n", mDiffAmplitudeCount1000));
				pwrite.write(String.format("Diff Amplitude 1001-2500 Count=%d\n", mDiffAmplitudeCount2500));
				pwrite.write(String.format("Diff Amplitude 2501-5000 Count=%d\n", mDiffAmplitudeCount5000));
				pwrite.write(String.format("Diff Amplitude above 5001 Count=%d\n", mDiffAmplitudeCountAbove));
				mFrameLastAmplitudeCh2.clear();
			}
			pwrite.flush();
			pwrite.close();
			Log.d(LOG_TAG, String.format("Logs are written to %s", logfile.getAbsolutePath()));
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "Failed to write log file.");
		}
	}
	
	private void resetCounter(){
		mDiffAmplitudeCount100 = 0;
		mDiffAmplitudeCount200 = 0;
		mDiffAmplitudeCount500 = 0;
		mDiffAmplitudeCount1000 = 0;
		mDiffAmplitudeCount2500 = 0;
		mDiffAmplitudeCount5000 = 0;
		mDiffAmplitudeCountAbove = 0;
	}
	
	public void saveAmplitudeDiffResult(){
		resetCounter();
		saveCh1AmplitudeDiffResult();
		mFrameLastAmplitudeCh1.clear();
		resetCounter();
		saveCh2AmplitudeDiffResult();
		mFrameLastAmplitudeCh2.clear();
	}
	
	
}
