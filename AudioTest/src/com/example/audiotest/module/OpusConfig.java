package com.example.audiotest.module;

public class OpusConfig {
	
	private static final String LOG_TAG = "OpusConfig";
	
	private static OpusConfig SINGLETON_INSTANCE = null;
	
	private boolean mAutoComplexity = true; //Automatically decide opus complexity by CPU Speed
	private int mOpusComplexity = 9;		//default value of OpusComplexity	
	private int mOpusSignalType = OpusWrapper.OPUS_SIGNAL_TYPE_VOICE;
	//private int mOpusApplication = OpusWrapper.OPUS_APPLICATION_RESTRICTED_LOWDELAY;
	private int mOpusApplication = OpusWrapper.OPUS_APPLICATION_VOIP;
	private int mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_WIDEBAND;
	//private int mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_NARROWBAND;
	//private int mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_MEDIUMBAND;
	private int mOpusBitrate = 48000;
	private int mOpusGainValue = 0;
	private boolean mEnableDTX = false;
	private boolean mInbandFEC = false;
	private boolean mEnablePLC = true;
	private boolean mAddSilent = false;	//Add Silent Data when packet lost.
	private int mOpusPacketLostPerc = 2;
	
	private OpusConfig() {
		SINGLETON_INSTANCE = this;
	}

	public static OpusConfig GetInstance() {
		if (SINGLETON_INSTANCE == null){
			SINGLETON_INSTANCE = new OpusConfig();
		}

		return SINGLETON_INSTANCE;
	}
	
	public int getOCValue(){
		return mOpusComplexity;
	}
	
	public void setOCValue(int value){
		mOpusComplexity = value;
	}
	
	public int getOSTValue(){
		return mOpusSignalType;
	}
	
	public void setOSTValue(int value){
		mOpusSignalType = value;
	}
	
	public int getOAValue(){
		return mOpusApplication;
	}
	
	public void setOAValue(int value){
		mOpusApplication = value;
	}
	
	public int getOBValue(){
		return mOpusBandwidth;
	}
	
	public void setOBValue(int value){
		mOpusBandwidth = value;
	}
	
	public int getBitrate(){
		return mOpusBitrate;
	}
	
	public void setBitrate(int bitrate)
	{
		mOpusBitrate = bitrate;
	}

	public int getGainValue(){
		return mOpusGainValue;
	}
	
	public void setGainValue(int value){
		if((value>=0) && (value<32768)){
			mOpusGainValue = value;
		}
	}
	
	public boolean getAutoComplexity(){
		return mAutoComplexity;
	}
	
	public void setAutoComplexity(boolean bEnable){
		mAutoComplexity = bEnable;
	}
	
	public boolean getDTXConfig(){
		return mEnableDTX;
	}
	
	public void setDTXConfig(boolean bEnable){
		mEnableDTX = bEnable;
	}
	
	public boolean getInbandFEC(){
		return mInbandFEC;
	}
	
	public void setInbandFEC(boolean bEnable){
		mInbandFEC = bEnable;
	}
	
	public boolean getEnablePLC(){
		return mEnablePLC;
	}
	
	public void setEnablePLC(boolean bEnable){
		mEnablePLC = bEnable;
	}
	
	public boolean getAddSilent(){
		return mAddSilent;
	}
	
	public void setAddSilent(boolean bEnable){
		mAddSilent = bEnable;
	}
	
	public int getPacketLostPerc(){
		return mOpusPacketLostPerc;
	}
	
	public void setPacketLostPerc(int percentage){
		mOpusPacketLostPerc = percentage;
	}
	
}
