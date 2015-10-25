package com.example.audiotest.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
//import com.sun.media.sound.WaveFileWriter;

import com.example.audiotest.module.WavFile.WavInfo;

import android.util.Log;

public class WaveReader {

	private final String LOG_TAG = "WaveReader";
	
	private final static int MAX_QUEUE_SIZE = 5000;
	private BlockingQueue<short[]> mAudioRawData = new ArrayBlockingQueue<short[]>(MAX_QUEUE_SIZE);
	
	private String mWaveSource = "";
	private int mAudioBufferSize = 0;
	private short[] mAudioBuffer = null;
	private int mCurrentIndex = 0;
	private AtomicBoolean mIsRunning = new AtomicBoolean();
	
	private WavFile mWaveFile = null;
	private WavInfo mWaveInfo = null;
	
	public WaveReader(String fileAtPath, int audioBufferSize){
		mWaveSource = fileAtPath;
		mAudioBufferSize = audioBufferSize*2;
		mAudioBuffer = new short[mAudioBufferSize];
		mIsRunning.set(false);
	}
	
	public void startWaveReader(){
		
		(new Thread(){
			@Override
			public void run(){
				readWaveData();
			}
			
		}).start();
		
	}
	
	public short[] getWaveData(){
		short[] shortData = null;
		if(mAudioRawData.size() > 0){
			try {
				shortData = mAudioRawData.take();
				Log.d(LOG_TAG, "Read data from wave reader.");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return shortData;
	}
	
	public void readWaveData(){
		File file = new File(mWaveSource);
		try {
			InputStream inputStream = new FileInputStream(file);
			//mWaveFile = new WavFile();
			mWaveInfo = WavFile.readHeader(inputStream);
			
			
			Log.d(LOG_TAG, String.format("WaveInfo: samplerate=%d, channels=%d, " +
					"dataSize=%d", mWaveInfo.mSampleRate, mWaveInfo.mChannels, mWaveInfo.mDataSize));
		 
			int index = 0;
			int maxLength = mWaveInfo.mDataSize - mAudioBufferSize +1;
			for(index = 0; index < maxLength; index += mAudioBufferSize){
				ByteBuffer buffer = ByteBuffer.allocate(mAudioBufferSize);
			    buffer.order(ByteOrder.LITTLE_ENDIAN);
				inputStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());
				
				
				short[] shorts = new short[mAudioBufferSize/2];
    			// to turn bytes to shorts as either big endian or little endian. 
    			ByteBuffer.wrap(buffer.array()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
    			mAudioRawData.add(shorts);
    			
    			//Thread.sleep(5);
			}
			
			inputStream.close();
			
			Log.d(LOG_TAG, String.format("Total audio frames are read: %d", mAudioRawData.size()));
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
