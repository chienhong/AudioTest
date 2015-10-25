package com.example.audiotest.module;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.media.AudioFormat;
import android.os.Environment;
import android.util.Log;

public class AudioWaveSaver {

	private static final int MAX_QUEUE_SIZE = 6000;
	private static final String LOG_TAG = "AudioWaveSaver";
	private BlockingQueue<short[]> mAudioRecvRawData = new ArrayBlockingQueue<short[]>(MAX_QUEUE_SIZE);
	private BlockingQueue<short[]> mAudioSendRawData = new ArrayBlockingQueue<short[]>(MAX_QUEUE_SIZE);
	
	private AudioConfig mAudioConfig = null;
	private OpusConfig mOpusConfig = null;
	
	// Recorder Save WAV FILE
	private static final int RECORDER_BPP = 16;
	private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	private static final String AUDIO_RECORDER_FOLDER = "AudioTest";
	//private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private int mSampleRate = 48000;
	private int mChannel = AudioFormat.CHANNEL_IN_STEREO;
	private int mAudioDataSize = 3840;
	private int mSilentAudioDataCount = 0;
	private int mSilentAudioDataSize = 0;
	private short[] mSilentData = null;
	private long mAudioRecvDataLen = 0;
	private long mAudioSendDataLen = 0;
	
	public AudioWaveSaver(int sampleRate, int channel, int audioDataSize){
		mSampleRate = sampleRate;
		mChannel = channel;
		mAudioDataSize = audioDataSize;
		mSilentAudioDataCount = 0;
		mSilentAudioDataSize = 0;
		mAudioConfig = AudioConfig.GetInstance();
		mOpusConfig = OpusConfig.GetInstance();
	}
	
	public AudioWaveSaver(BlockingQueue<short[]> audioData, int sampleRate, int channel, int audioDataSize){
		mAudioRecvRawData = audioData;
		mSampleRate = sampleRate;
		mChannel = channel;
		mAudioDataSize = audioDataSize;
		mAudioConfig = AudioConfig.GetInstance();
		mOpusConfig = OpusConfig.GetInstance();
	}
	
	public void addAudioSendData(short[] data){
		if(mAudioSendRawData.size() < MAX_QUEUE_SIZE){
			mAudioSendDataLen += data.length;
			mAudioSendRawData.add(data);
		}
		else{
			Log.e(LOG_TAG, "Exceed AudioSendRawData max buffer size.");
		}
	}
	
	public void addAudioData(short[] data){
		if(mAudioRecvRawData.size() < MAX_QUEUE_SIZE){
			mAudioRecvDataLen += data.length;
			mAudioRecvRawData.add(data);
		}
		else{
			Log.e(LOG_TAG, "Exceed AudioRawData max buffer size.");
		}
	}
	/*
	public boolean saveWaveFile(){
		boolean result = false;
		if(null == mAudioRawData){
			return false;
		}
		
		return result;
	}*/
	
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
	
	public void saveSendWaveFile(){
		String newFileName = GetCurrentTime("yyyy-MM-dd_HH-mm-ss");
		if(mAudioConfig.getAutoRename() 
				|| mAudioConfig.getOpusRename() 
				|| mAudioConfig.getAudioEffectRename()
				|| mAudioConfig.getAGCRename()
				|| mAudioConfig.getAECRename()
				|| mAudioConfig.getNSRename()
				|| mAudioConfig.getPacketLostRename()){
			
			newFileName = AudioTestFileName.getFileName();
		}
		String outFilename = String.format("Send_%s.wav",newFileName);
        FileOutputStream out = null;

        long totalAudioLen = mAudioSendDataLen * 2;
        long totalDataLen = totalAudioLen + 36; 	//convert data length from short to byte: mAudioSendDataLen*2
        long longSampleRate = mSampleRate;
        int channels = 2;
        long byteRate = RECORDER_BPP * mSampleRate * channels/8;
        
        File directory = new File(Environment.getExternalStorageDirectory()+File.separator + AUDIO_RECORDER_FOLDER);
		directory.mkdirs();
		
        try {
    		out = new FileOutputStream(new File(directory, outFilename));
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                            longSampleRate, channels, byteRate);

            DataOutputStream dos = new DataOutputStream(out);
            
            short[] shortData = null;
            int totalAudioFrameCount = mAudioSendRawData.size();
            for(int index=0; index <totalAudioFrameCount; index++){
            	shortData = mAudioSendRawData.take();
                
            	byte[] bytesData = new byte[shortData.length * 2];
            	ByteBuffer.wrap(bytesData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortData);
            	
            	dos.write(bytesData, 0, shortData.length*2);
            }

            out.close();
            Log.d(LOG_TAG, "Save Send wav file successfully.");
        } catch (FileNotFoundException e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        } catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void saveRecvWaveFile(){
		String newFileName = GetCurrentTime("yyyy-MM-dd_HH-mm-ss");
		if(mAudioConfig.getAutoRename() 
				|| mAudioConfig.getOpusRename() 
				|| mAudioConfig.getAudioEffectRename()
				|| mAudioConfig.getAGCRename()
				|| mAudioConfig.getAECRename()
				|| mAudioConfig.getNSRename()
				|| mAudioConfig.getPacketLostRename()){
			
			newFileName = AudioTestFileName.getFileName();
		}
		String outFilename = String.format("%s.wav",newFileName);
        FileOutputStream out = null;
        long totalAudioLen = mAudioRecvDataLen*2;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = mSampleRate;
        int channels = 2;
        long byteRate = RECORDER_BPP * mSampleRate * channels/8;
        
        File directory = new File(Environment.getExternalStorageDirectory()+File.separator + AUDIO_RECORDER_FOLDER);
		directory.mkdirs();
		
        try {
    		out = new FileOutputStream(new File(directory, outFilename));
            totalDataLen = totalAudioLen + 36;
            
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                            longSampleRate, channels, byteRate);

            DataOutputStream dos = new DataOutputStream(out);
            
            short[] shortData = null;
            int totalAudioFrameCount = mAudioRecvRawData.size();
            for(int index=0; index <totalAudioFrameCount; index++){
            	shortData = mAudioRecvRawData.take();
                
            	byte[] bytesData = new byte[shortData.length * 2];
            	ByteBuffer.wrap(bytesData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortData);
            	
            	dos.write(bytesData, 0, shortData.length*2);
            }

            out.close();
            Log.d(LOG_TAG, "Save wav file successfully.");
        } catch (FileNotFoundException e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        } catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {
    
	    byte[] header = new byte[44];
	    
	    header[0] = 'R';  // RIFF/WAVE header
	    header[1] = 'I';
	    header[2] = 'F';
	    header[3] = 'F';
	    header[4] = (byte) (totalDataLen & 0xff);
	    header[5] = (byte) ((totalDataLen >> 8) & 0xff);
	    header[6] = (byte) ((totalDataLen >> 16) & 0xff);
	    header[7] = (byte) ((totalDataLen >> 24) & 0xff);
	    header[8] = 'W';
	    header[9] = 'A';
	    header[10] = 'V';
	    header[11] = 'E';
	    header[12] = 'f';  // 'fmt ' chunk
	    header[13] = 'm';
	    header[14] = 't';
	    header[15] = ' ';
	    header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
	    header[17] = 0;
	    header[18] = 0;
	    header[19] = 0;
	    header[20] = 1;  // format = 1
	    header[21] = 0;
	    header[22] = (byte) channels;
	    header[23] = 0;
	    header[24] = (byte) (longSampleRate & 0xff);
	    header[25] = (byte) ((longSampleRate >> 8) & 0xff);
	    header[26] = (byte) ((longSampleRate >> 16) & 0xff);
	    header[27] = (byte) ((longSampleRate >> 24) & 0xff);
	    header[28] = (byte) (byteRate & 0xff);
	    header[29] = (byte) ((byteRate >> 8) & 0xff);
	    header[30] = (byte) ((byteRate >> 16) & 0xff);
	    header[31] = (byte) ((byteRate >> 24) & 0xff);
	    header[32] = (byte) (2 * 16 / 8);  // block align
	    header[33] = 0;
	    header[34] = RECORDER_BPP;  // bits per sample
	    header[35] = 0;
	    header[36] = 'd';
	    header[37] = 'a';
	    header[38] = 't';
	    header[39] = 'a';
	    header[40] = (byte) (totalAudioLen & 0xff);
	    header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
	    header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
	    header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
	
	    out.write(header, 0, 44);
	}
}