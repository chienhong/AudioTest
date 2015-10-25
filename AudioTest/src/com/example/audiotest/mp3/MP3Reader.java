package com.example.audiotest.mp3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.util.Log;

@SuppressLint("InlinedApi") 
public class MP3Reader {

	private final String LOG_TAG = "MP3Reader";
	
	private final static int MAX_QUEUE_SIZE = 5000;
	private BlockingQueue<short[]> mAudioRawData = new ArrayBlockingQueue<short[]>(MAX_QUEUE_SIZE);
	
	private static int TIMEOUT_US = -1;
	private MediaCodec mMediaCodec;
	private MediaExtractor mExtractor;

	private MediaFormat format;
	private ByteBuffer[] codecInputBuffers;
	private ByteBuffer[] codecOutputBuffers;
	private Boolean sawInputEOS = false;
	private Boolean sawOutputEOS = false;
	private BufferInfo info;
	
	private AtomicBoolean mIsRunning = new AtomicBoolean();
	private Thread mInputThread = null;
	private Thread mOutputThread = null;
	
	private String mMP3Source = "";
	private int mAudioBufferSize = 0;
	private short[] mAudioBuffer = null;
	private int mCurrentIndex = 0;
	
	public MP3Reader(String fileAtPath, int audioBufferSize){
		mMP3Source = fileAtPath;
		mAudioBufferSize = audioBufferSize;
		mAudioBuffer = new short[mAudioBufferSize];
	}
	
	public void stopMP3Reader(){
		mIsRunning.set(false);
		if(null != mExtractor){
			//mInputThread.interrupt();
			//mOutputThread.interrupt();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			mMediaCodec.flush();
			
			mExtractor.release();
			mMediaCodec.release();
		}
		mAudioRawData.clear();
	}
	
	public void startMP3Reader(){
		if(mIsRunning.get()){
			return;
		}
		mIsRunning.set(true);

	    mExtractor = new MediaExtractor();

	    try {
	        mExtractor.setDataSource(mMP3Source);
	    } catch (IOException e) {
	    }

	    format = mExtractor.getTrackFormat(0);
	    String mime = format.getString(MediaFormat.KEY_MIME);
	    int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

	    Log.i(LOG_TAG, "===========================");
	    Log.i(LOG_TAG, "fileAtPath "+mMP3Source);
	    Log.i(LOG_TAG, "mime type : "+mime);
	    Log.i(LOG_TAG, "sample rate : "+sampleRate);
	    Log.i(LOG_TAG, "===========================");

	    mMediaCodec = MediaCodec.createDecoderByType(mime);
	    mMediaCodec.configure(format, null , null , 0);
	    mMediaCodec.start();

	    codecInputBuffers = mMediaCodec.getInputBuffers();
	    codecOutputBuffers = mMediaCodec.getOutputBuffers();

	    mExtractor.selectTrack(0); 

	    info = new BufferInfo();

	    mInputThread = new Thread(){
	    	@Override
	    	public void run(){
	    		input();
	    	}
	    };
	    mInputThread.start();
	    
	    mOutputThread = new Thread(){
	    	@Override
	    	public void run(){
	    		output();
	    	}
	    };
	    mOutputThread.start();

	}
	
	public short[] getMP3Data(){
		short[] shortData = null;
		if(mAudioRawData.size() > 0){
			try {
				shortData = mAudioRawData.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return shortData;
	}
	
	private void output()
    {
		
    	while(mIsRunning.get()){
	        final int res = mMediaCodec.dequeueOutputBuffer(info, TIMEOUT_US);

	        if (res >= 0) {
	            int outputBufIndex = res;
	            ByteBuffer buf = codecOutputBuffers[outputBufIndex];
	
	            final byte[] chunk = new byte[info.size];
	            
	            buf.get(chunk); // Read the buffer all at once
	            buf.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN
	
	            if (chunk.length > 0) {
	            	
	            	if(mAudioRawData.size() < MAX_QUEUE_SIZE){
	            		int dataLength = chunk.length/2;
            			short[] shorts = new short[dataLength];
            			// to turn bytes to shorts as either big endian or little endian. 
            			ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            			
            			int restDataLength = dataLength;
            			int dataIndex = 0;
            			//Log.d(LOG_TAG, String.format("output Get Audio Chunk with size: %d", chunk.length));
            			while( (mCurrentIndex + restDataLength) > mAudioBufferSize ){
            				int dataToCopy = mAudioBufferSize - mCurrentIndex;
            				System.arraycopy(shorts, dataIndex, mAudioBuffer, mCurrentIndex, dataToCopy);
            				dataIndex += dataToCopy;
            				restDataLength = restDataLength - dataToCopy;
            				if(mAudioRawData.size() < MAX_QUEUE_SIZE){
            					mAudioRawData.add(mAudioBuffer.clone());
            				}
            				mCurrentIndex = 0;
            			}
            			
            			if(restDataLength > 0){
            				System.arraycopy(shorts, dataIndex, mAudioBuffer, mCurrentIndex, restDataLength);
            				mCurrentIndex += restDataLength;
            				//Log.d(LOG_TAG, "restDataLength="+restDataLength);
            			}
            			
            			if(mAudioRawData.size() > 10){
    	            		try {
    							Thread.sleep(80);
    						} catch (InterruptedException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
    	            	}
            			//Log.d(LOG_TAG, String.format("Current Queue Size=%d", mAudioRawData.size()));
            		}
	            }
	            mMediaCodec.releaseOutputBuffer(outputBufIndex, false /* render */);
	
	            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
	                sawOutputEOS = true;
	            }
	        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
	        	//Log.d(LOG_TAG, "output INFO_OUTPUT_BUFFERS_CHANGED");
	            codecOutputBuffers = mMediaCodec.getOutputBuffers();
	        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
	        	//Log.d(LOG_TAG, "output INFO_OUTPUT_FORMAT_CHANGED");
	            final MediaFormat oformat = mMediaCodec.getOutputFormat();
	            Log.d(LOG_TAG, "Output format has changed to " + oformat);
	        }
    	}
    }

    private void input()
    {
        Log.i(LOG_TAG, "inputLoop()");
        while(mIsRunning.get()){
	        int inputBufIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_US);
        	//int inputBufIndex = codec.dequeueInputBuffer(0);
	        //Log.i(LOG_TAG, "inputBufIndex : "+inputBufIndex);
	
	        if (inputBufIndex >= 0) {   
	            ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
	
	            int sampleSize = mExtractor.readSampleData(dstBuf, 0);
	            //Log.i(LOG_TAG, "sampleSize : "+sampleSize);
	            long presentationTimeUs = 0;
	            if (sampleSize < 0) {
	                Log.i(LOG_TAG, "Saw input end of stream!");
	                sawInputEOS = true;
	                sampleSize = 0;
	            } else {
	                presentationTimeUs = mExtractor.getSampleTime();
	                //Log.i(LOG_TAG, "presentationTimeUs "+presentationTimeUs);
	            }
	
	            mMediaCodec.queueInputBuffer(inputBufIndex,
	                                   0, //offset
	                                   sampleSize,
	                                   presentationTimeUs,
	                                   sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
	            if (!sawInputEOS) {
	                //Log.i(LOG_TAG, "extractor.advance()");
	                mExtractor.advance();
	            }
	            else{
	            	mIsRunning.set(false);
	            	Log.d(LOG_TAG, "EOS");
	            	break;
	            }
	        }
	        else{
	        	//Log.d(LOG_TAG, "input sleep");
	        	try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
        }
    }
    
}
