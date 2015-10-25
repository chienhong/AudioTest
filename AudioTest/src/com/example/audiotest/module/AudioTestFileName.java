package com.example.audiotest.module;

public class AudioTestFileName {

	private static AudioConfig mAudioConfig = null;
	private static OpusConfig mOpusConfig = null;
	
	public static String getFileName(){
		
		mAudioConfig = AudioConfig.GetInstance();
		mOpusConfig = OpusConfig.GetInstance();
		
		String LOG_FILE_FORMAT = "%s-%s-%s-%s";	//EX: ori_5ms_music48k_bitrate48k.wav
		if(mAudioConfig.getOpusRename()){
			LOG_FILE_FORMAT = "%s-%s-%s-%s-%s-%s-%s-%s-%s";	//opus_voip_voice_narrow_c9_dtx0_40ms_music48k_bit48k.wav
		}
		else if(mAudioConfig.getAudioEffectRename()){
			LOG_FILE_FORMAT = "%s-%s-%s-%s-%s-%s-%s-%s";	//ori_aec0_ns0mild_agc1unchanged_vad0sendifvad0_40ms_music48k_bit48k.wav
		}
		else if(mAudioConfig.getAGCRename()){
			LOG_FILE_FORMAT = "%s-%s-%s-%s-%s-%s-%s";	//agc1unchanged_max8min20compdb9_ns0mild_vad1_40ms_music16k_bit48k.wav
		}
		else if(mAudioConfig.getAECRename()){
			LOG_FILE_FORMAT = "%s-%s-%s-%s";	//aec1proc80delaybias20_40ms_music16k_bit48k.wav
		}
		else if(mAudioConfig.getNSRename()){
			LOG_FILE_FORMAT = "%s-%s-%s-%s";	//ns1mildwhite_40ms_music48k_bit48k.wav
		}
		else if(mAudioConfig.getPacketLostRename()){
			LOG_FILE_FORMAT = "%s-%s-%s-%s-%s-%s"; //tcp-10lostrate-retrans0Count0interval10-40ms-music48k-bit48k.wav
		}
		String strFileName = "";
		
		String strType = getTestType();
		String strFrameSize = getFrameSize();
		String strFileType = getFileType();
		String strBitrate = getBitrate();

		if(mAudioConfig.getOpusRename()){
			String strOpusApplication = getOpusApplication();
			String strOpusSignal = getOpusSignal();
			String strOpusBandwidth = getOpusBandwidth();
			String strOpusComplexity = getOpusComplexity();
			String strEnableDTX = getOpusEnableDTX();
			strFileName = String.format(LOG_FILE_FORMAT, strType, strOpusApplication, strOpusSignal, strOpusBandwidth,
					strOpusComplexity, strEnableDTX, strFrameSize, strFileType, strBitrate);
		}
		else if(mAudioConfig.getAudioEffectRename()){
			String strAECEffect = getAECEffect(false);
			String strNSEffect = getNSEffect(false);
			String strAGCEffect = getAGCEffect();
			String strVADEffect = getVADEffect(true);
			strFileName = String.format(LOG_FILE_FORMAT, strType, strAECEffect, strNSEffect, strAGCEffect,
					strVADEffect, strFrameSize, strFileType, strBitrate);
		}
		else if(mAudioConfig.getAGCRename()){
			String strAGCEffect = getAGCEffect();
			String strAGCParameter = getAGCParameter();
			String strVADEffect = getVADEffect(false);
			String strNSEffect = getNSEffect(false);
			strFileName = String.format(LOG_FILE_FORMAT, strAGCEffect, strAGCParameter, strNSEffect, strVADEffect,
					strFrameSize, strFileType, strBitrate);
		}
		else if(mAudioConfig.getAECRename()){
			String strAECEffect = getAECEffect(true);
			strFileName = String.format(LOG_FILE_FORMAT, strAECEffect, strFrameSize, strFileType, strBitrate);
		}
		else if(mAudioConfig.getNSRename()){
			String strNSEffect = getNSEffect(true);
			strFileName = String.format(LOG_FILE_FORMAT, strNSEffect, strFrameSize, strFileType, strBitrate);
		}
		else if(mAudioConfig.getPacketLostRename()){
			String strNetType = getNetworkType();
			String strLostRate = getNetworkLostRate();
			String strNetDuplicate = getNetDuplicate();
			strFileName = String.format(LOG_FILE_FORMAT, strNetType, strLostRate, strNetDuplicate, strFrameSize, strFileType, strBitrate);
		}
		else{
			strFileName = String.format(LOG_FILE_FORMAT, strType, strFrameSize, strFileType, strBitrate);
		}
		return strFileName;
	}
	
	public static String getNetDuplicate(){
		String strRetransmit = "";
		
		if(mAudioConfig.getPacketLostDuplicate()){
			strRetransmit += "dup1";
		}
		else{
			strRetransmit += "dup0";
		}
		
		String strRetransmitCount = "count" + String.valueOf(mAudioConfig.getRetransmitCount());
		strRetransmit += strRetransmitCount;
		
		String strRetransmitInterval = "int" + String.valueOf(mAudioConfig.getRetransmitInterval());
		strRetransmit += strRetransmitInterval;
		
		return strRetransmit;
	}
	
	public static String getNetworkLostRate(){
		String percentage = String.valueOf(mAudioConfig.getPacketLostRate());
		String lostRate = percentage + "lost";
		return lostRate;
	}
	
	public static String getNetworkType(){
		String netType = "netType";
		if(mAudioConfig.getPacketLostTCP()){
			netType = "TCP";
		}
		else if(mAudioConfig.getPacketLostUDPAck()){
			netType = "UDPAck";
		}
		else if(mAudioConfig.getPacketLostUDPNoAck()){
			netType = "UDPNoAck";
		}
		
		return netType;
	}
	
	public static String getAGCParameter(){
		String strAGCParameter = "";	//max8min20gaindb9

		int maxDB = -mAudioConfig.getDBMaxThreshold();
		int minDB = -mAudioConfig.getDBMinThreshold();
		//int gaindB = mAudioConfig.getAGCCompressionGaindB();
		String caldB = "";
		if(mAudioConfig.getCalculateDB()){
			caldB = "dyndB1";
		}
		else{
			caldB = "dyndB0";
		}
		
		strAGCParameter = String.format("max%dmin%d%s", maxDB, minDB, caldB);
		
		return strAGCParameter;
	}
	
	public static String getVADEffect(boolean bDetail){
		String strVADEffect = "";
		
		if(mAudioConfig.getEnableVAD()){
			strVADEffect = "vad1";
		}
		else{
			strVADEffect = "vad0";
		}
		
		if(bDetail){
			if(mAudioConfig.getSendIfVAD()){
				strVADEffect += "sendifvad1";
			}
			else{
				strVADEffect += "sendifvad0";
			}
		}
		
		return strVADEffect;
	}
	
	public static String getAGCEffect(){
		String strAGCEffect = "";
		
		if(mAudioConfig.getEnableAGC()){
			strAGCEffect = "agc1";
		}
		else{
			strAGCEffect = "agc0";
		}
		
		int agcMode = mAudioConfig.getAGCMode();
		if(AGCWrapper.AGC_MODE_UNCHANGED == agcMode){
			strAGCEffect += "unchanged";
		}
		else if(AGCWrapper.AGC_MODE_ADAPTIVE_ANALOG == agcMode){
			strAGCEffect += "adptanalog";
		}
		else if(AGCWrapper.AGC_MODE_ADAPTIVE_DIGITAL == agcMode){
			strAGCEffect += "adptdigital";
		}
		else if(AGCWrapper.AGC_MODE_FIXED_DIGITAL == agcMode){
			strAGCEffect += "fixedigital";
		}
		
		int tarLv = mAudioConfig.getAGCTargetLvDbfs();
		int gaindB = mAudioConfig.getAGCCompressionGaindB();
		strAGCEffect = strAGCEffect + "_tarLv" + tarLv + "gaindB" + gaindB;
		
		return strAGCEffect;
	}
	
	public static String getNSEffect(boolean bDetail){
		String strNSEffect = "";
		if(mAudioConfig.getEnableNS()){
			strNSEffect = "ns1";
		}
		else{
			strNSEffect = "ns0";
		}
		
		int nsMode = mAudioConfig.getNSMode();
		if(NSWrapper.NS_MODE_MILD == nsMode){
			strNSEffect += "mild";
		}
		else if(NSWrapper.NS_MODE_MEDIUM == nsMode){
			strNSEffect += "medium";
		}
		else if(NSWrapper.NS_MODE_AGGRESSIVE == nsMode){
			strNSEffect += "aggressive";
		}
		
		if(bDetail){
			strNSEffect += mAudioConfig.getNSSampleType();
		}
		
		return strNSEffect;
	}
	
	public static String getAECEffect(boolean bDetail){
		String strAECEffect = "";
		if(mAudioConfig.getAECMConfig()){
			strAECEffect = "aec1";
		}
		else{
			strAECEffect = "aec0";
		}
		
		if(bDetail){
			//aec1proc80delaybias20
			String strProcSample = "proc" + mAudioConfig.getAECProcessSampleCount();
			strAECEffect += strProcSample;
			String strDelayBias = "delaybias" + mAudioConfig.getAECDelayTimeBias();
			strAECEffect += strDelayBias;
		}
		
		return strAECEffect;
	}
	
	public static String getOpusEnableDTX(){
		String strOpusDTX = "dtx";
		if(mOpusConfig.getDTXConfig()){
			strOpusDTX += "1";
		}
		else{
			strOpusDTX += "0";
		}
		
		return strOpusDTX;
	}
	
	public static String getOpusComplexity(){

		String strComplexity = String.valueOf(mOpusConfig.getOCValue());
		
		String strOpusComplexity = "c" + strComplexity;		
		
		return strOpusComplexity;
	}
	
	public static String getOpusBandwidth(){
		String strOpusBandwidth = "OB";
		
		if(OpusWrapper.OPUS_BANDWIDTH_FULLBAND == mOpusConfig.getOBValue()){
			strOpusBandwidth = "full";
		}
		else if(OpusWrapper.OPUS_BANDWIDTH_MEDIUMBAND == mOpusConfig.getOBValue()){
			strOpusBandwidth = "medium";
		}
		else if(OpusWrapper.OPUS_BANDWIDTH_NARROWBAND == mOpusConfig.getOBValue()){
			strOpusBandwidth = "narrow";
		}
		else if(OpusWrapper.OPUS_BANDWIDTH_SUPERWIDEBAND == mOpusConfig.getOBValue()){
			strOpusBandwidth = "super";
		}
		else if(OpusWrapper.OPUS_BANDWIDTH_WIDEBAND == mOpusConfig.getOBValue()){
			strOpusBandwidth = "wide";
		}
		
		return strOpusBandwidth;
	}
	
	public static String getOpusSignal(){
		String strOpusSignal = "OS";
		
		if(OpusWrapper.OPUS_SIGNAL_TYPE_AUTO == mOpusConfig.getOSTValue()){
			strOpusSignal = "auto";
		}
		else if(OpusWrapper.OPUS_SIGNAL_TYPE_MUSIC == mOpusConfig.getOSTValue()){
			strOpusSignal = "music";
		}
		else if(OpusWrapper.OPUS_SIGNAL_TYPE_VOICE == mOpusConfig.getOSTValue()){
			strOpusSignal = "voice";
		}
		
		return strOpusSignal;
	}
	
	public static String getOpusApplication(){
		String strOpusApplication = "OA";
		
		if(OpusWrapper.OPUS_APPLICATION_AUDIO == mOpusConfig.getOAValue()){
			strOpusApplication = "audio";
		}
		else if(OpusWrapper.OPUS_APPLICATION_VOIP == mOpusConfig.getOAValue()){
			strOpusApplication = "voip";
		}
		else if(OpusWrapper.OPUS_APPLICATION_RESTRICTED_LOWDELAY == mOpusConfig.getOAValue()){
			strOpusApplication = "ldelay";
		}
		
		return strOpusApplication;
	}
	
	public static String getTestType(){
		String testType = "none";
		
		if(mAudioConfig.getDropModeConfig() && mAudioConfig.getDuplicateAudioData()){
			if(4 == mAudioConfig.getAudioDropRate()){
				testType = "1134";
			}
			else if(3 == mAudioConfig.getAudioDropRate()){
				testType = "113";
			}
		}
		else if(mAudioConfig.getDropModeConfig()){
			if(4 == mAudioConfig.getAudioDropRate()){
				testType = "4drop1";
			}
			else if(3 == mAudioConfig.getAudioDropRate()){
				testType = "3drop1";
			}
			else if(2 == mAudioConfig.getAudioDropRate()){
				testType = "2drop1";
			}
		}
		else if(mAudioConfig.getDuplicateAudioData()){
			if(4 == mAudioConfig.getAudioDropRate()){
				testType = "11234";
			}
			else if(3 == mAudioConfig.getAudioDropRate()){
				testType = "1123";
			}
		}
		else if(mAudioConfig.getGenPairData()){
			if(4 == mAudioConfig.getAudioDropRate()){
				testType = "1-11234";
			}
			else if(3 == mAudioConfig.getAudioDropRate()){
				testType = "1-1123";
			}
			else if(2 == mAudioConfig.getAudioDropRate()){
				testType = "1-11";
			}
		}
		else if(mAudioConfig.getOpusRename()){
			testType = "opus";
		}
		else{
			testType = "ori";
		}
		
		return testType;
	}
	
	public static String getFrameSize(){
		String strFrameSize = "ms";
		
		if(60 == mAudioConfig.getAudioFrameSize()){
			strFrameSize = "60ms";
		}
		else if(40 == mAudioConfig.getAudioFrameSize()){
			strFrameSize = "40ms";
		}
		else if(20 == mAudioConfig.getAudioFrameSize()){
			strFrameSize = "20ms";
		}
		else if(10 == mAudioConfig.getAudioFrameSize()){
			strFrameSize = "10ms";
		}
		else if(5 == mAudioConfig.getAudioFrameSize()){
			strFrameSize = "5ms";
		}
		
		return strFrameSize;
	}
	
	public static String getFileType(){
		String strFileType = "file";
		
		if(mAudioConfig.getEnableMP3()){
			strFileType = "music";
		}
		else if(mAudioConfig.getEnableVoice()){
			strFileType = "voice";
		}
		else if(mAudioConfig.getEnableWave()){
			strFileType = "wavfile";
		}
		
		if(48000 == mAudioConfig.getAudioSampleRate()){
			strFileType += "48k";
		}
		else if(24000 == mAudioConfig.getAudioSampleRate()){
			strFileType += "24k";
		}
		else if(16000 == mAudioConfig.getAudioSampleRate()){
			strFileType += "16k";
		}
		else if(12000 == mAudioConfig.getAudioSampleRate()){
			strFileType += "12k";
		}
		else if(8000 == mAudioConfig.getAudioSampleRate()){
			strFileType += "8k";
		}
		
		return strFileType;
	}
	
	public static String getBitrate(){
		String strBitrate = "";
		
		if(8000 == mOpusConfig.getBitrate()){
			strBitrate = "bit8k";
		}
		else if(12000 == mOpusConfig.getBitrate()){
			strBitrate = "bit12k";
		}
		else if(16000 == mOpusConfig.getBitrate()){
			strBitrate = "bit16k";
		}
		else if(24000 == mOpusConfig.getBitrate()){
			strBitrate = "bit24k";
		}
		else if(48000 == mOpusConfig.getBitrate()){
			strBitrate = "bit48k";
		}
		else if(64000 == mOpusConfig.getBitrate()){
			strBitrate = "bit64k";
		}
		else if(96000 == mOpusConfig.getBitrate()){
			strBitrate = "bit96k";
		}
		else if(128000 == mOpusConfig.getBitrate()){
			strBitrate = "bit128k";
		}
		else if(256000 == mOpusConfig.getBitrate()){
			strBitrate = "bit256k";
		}
		else if(510000 == mOpusConfig.getBitrate()){
			strBitrate = "bit510k";
		}
		
		return strBitrate;
	}
	
}
