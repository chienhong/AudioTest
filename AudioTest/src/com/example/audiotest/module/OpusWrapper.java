package com.example.audiotest.module;

import android.util.Log;

public class OpusWrapper {
	private static final String LOG_TAG = "OpusWrapper";
	
	public static final int OPUS_TYPE_ENCODER = 1;
	public static final int OPUS_TYPE_DECODER = 2;
	
	//OPUS Application
	//public static final int OPUS_APPLICATION_AUTO 				= 0;
	public static final int OPUS_APPLICATION_VOIP 					= 1;
	public static final int OPUS_APPLICATION_AUDIO 					= 2;
	public static final int OPUS_APPLICATION_RESTRICTED_LOWDELAY 	= 3;
	
	//OPUS Signal
	public static final int OPUS_SIGNAL_TYPE_AUTO					= 4;
	public static final int OPUS_SIGNAL_TYPE_MUSIC 					= 5;
	public static final int OPUS_SIGNAL_TYPE_VOICE 					= 6;
	
	//OPUS Bandwidth
	public static final int OPUS_BANDWIDTH_NARROWBAND				= 7;	//4 kHz passband
	public static final int OPUS_BANDWIDTH_MEDIUMBAND				= 8;	//6 kHz passband
	public static final int OPUS_BANDWIDTH_WIDEBAND					= 9;	//8 kHz passband
	public static final int OPUS_BANDWIDTH_SUPERWIDEBAND			= 10;	//12 kHz passband
	public static final int OPUS_BANDWIDTH_FULLBAND					= 11;	//20 kHz passband
	
	private int mSampleRate = 48000;
	private int mBitrate = 48000;
	private int mChannels = 2;
	private int mComplexity = 0;
	private int mSignalType = OPUS_SIGNAL_TYPE_AUTO;
	private int mApplicationType = OPUS_APPLICATION_VOIP;
	private int mBandwidth = OPUS_BANDWIDTH_MEDIUMBAND;
	
	static{
		try{
			System.loadLibrary("opus");
			System.loadLibrary("OpusWrapper");
		}
		catch(UnsatisfiedLinkError ule){
			ule.printStackTrace();
		}
	}
	
	public native int opusInit(int sampling_rate, int channels, int bitrate, int application_type, int codec_type);
	public native int opusDestroy(int codec_type);
	public native int opusEncode( short[] in, int in_size, byte[] out, int out_size );
	public native int opusDecode( byte[] in, int in_size, short[] out, int out_size, boolean decode_fec);
	public native int opusSetComplexity( int complexity ); // complexity: 0 - 10
	public native int opusSetSignalType( int signalType );
	public native int opusSetBandwidth( int bandwidthType );
	public native int opusResetSampleRate(int new_sample_rate, int codec_type);
	public native int opusAdjustBitrate( int new_bitrate );
	public native int opusSetGainValue( int gainValue );
	public native int opusGetPitchValue();
	public native int opusSetLSBDepth(int depth);
	public native int opusSetDTX(boolean bEnable);
	public native int opusInBandFEC(boolean bEnable);
	public native int opusPacketLostPerc(int percentage);
	
	public OpusWrapper(int sampling_rate, int channels, int bitrate, int application_type, int codec_type){
		mSampleRate = sampling_rate;
		mChannels = channels;
		mApplicationType = application_type;
		mBitrate = bitrate;
		
		opusInit(sampling_rate, channels, bitrate, application_type, codec_type);
	}
	
	public OpusWrapper(int sampling_rate, int channels, int bitrate, 
			int application_type, int complexity, int signal_type, 
			int bandwidth, int codec_type){
		mSampleRate = sampling_rate;
		mChannels = channels;
		mApplicationType = application_type;
		mBitrate = bitrate;
		mComplexity = complexity;
		mSignalType = signal_type;
		mBandwidth = bandwidth;
		
		opusInit(sampling_rate, channels, bitrate, application_type, codec_type);

		opusSetComplexity(mComplexity);
		opusSetSignalType(mSignalType);
		opusSetBandwidth(mBandwidth);
	}
	
	public int getSampleRate(){
		return mSampleRate;
	}
	
	public int resetSampleRate(int new_sample_rate, int codec_type){
		if(OPUS_TYPE_ENCODER == codec_type){
			Log.i(LOG_TAG, String.format("resetSampleRate: encoder's samplerate %d", new_sample_rate));
		}
		else if(OPUS_TYPE_DECODER == codec_type){
			Log.i(LOG_TAG, String.format("resetSampleRate: decoder's samplerate %d", new_sample_rate));
		}
		mSampleRate = new_sample_rate;
		return opusResetSampleRate(new_sample_rate, codec_type);
	}
	
	public void resetBitrate(int new_bitrate){
		if(new_bitrate == mBitrate){
			return;
		}
		Log.i(LOG_TAG, String.format("resetBitrate: %d", new_bitrate));
		mBitrate = new_bitrate;
		opusAdjustBitrate(new_bitrate);
	}
}
