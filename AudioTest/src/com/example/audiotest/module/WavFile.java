package com.example.audiotest.module;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavFile {

	private static final String LOG_TAG = "WavFile";
	
	private static final String RIFF_HEADER = "RIFF";
	private static final String WAVE_HEADER = "WAVE";
	private static final String FMT_HEADER = "fmt ";
	private static final String DATA_HEADER = "data";

	private static final int HEADER_SIZE = 44;

	private static final String CHARSET = "ASCII";

	public WavFile(){
		
	}
	
	public static WavInfo readHeader(InputStream wavStream) 
			  throws IOException {
	 

	    ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
	    buffer.order(ByteOrder.LITTLE_ENDIAN);
	 
	    wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());
	 
	    buffer.rewind();
	    buffer.position(buffer.position() + 20);
	    int format = buffer.getShort();
	    //checkFormat(format == 1, "Unsupported encoding: " + format); // 1 means Linear PCM
	    int channels = buffer.getShort();
	    //checkFormat(channels == 1 || channels == 2, "Unsupported channels: " + channels);
	    int rate = buffer.getInt();
	    //checkFormat(rate <= 48000 && rate >= 11025, "Unsupported rate: " + rate);
	    buffer.position(buffer.position() + 6);
	    int bits = buffer.getShort();
	    //checkFormat(bits == 16, "Unsupported bits: " + bits);
	    int dataSize = 0;
	    while (buffer.getInt() != 0x61746164) { // "data" marker
	      //android.util.Log.d(LOG_TAG, "Skipping non-data chunk");
	      int size = buffer.getInt();
	      wavStream.skip(size);
	       
	      buffer.rewind();
	      wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
	      buffer.rewind();
	    } 
	    dataSize = buffer.getInt();
	    //checkFormat(dataSize > 0, "wrong datasize: " + dataSize);
	     
	    return new WavInfo(rate, channels, dataSize);
	}
	
	public static class WavInfo{
		
		int mSampleRate = 0;
		int mChannels = 0;
		int mDataSize = 0;
		
		public WavInfo(int samplerate, int channels, int dataSize){
			mSampleRate = samplerate;
			mChannels = channels;
			mDataSize = dataSize;
		}
		
	}
}
