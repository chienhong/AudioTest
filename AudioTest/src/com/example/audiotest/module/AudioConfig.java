package com.example.audiotest.module;

public class AudioConfig {
	
	private static final String LOG_TAG = "AudioConfig";
	
	private static AudioConfig SINGLETON_INSTANCE = null;
	//private Context mContext = null;
	
	//Audio Mode
	public static final int AUDIO_MODE_VOIP = 0;
	public static final int AUDIO_MODE_LOCAL = 1;
	public static final int AUDIO_MODE_LOCAL_NETWORK = 2;
	public static final int AUDIO_MODE_MP3 = 3;
	
	private static boolean mEnableTestMode = false;
	private static boolean mEnableAECTestMode = false;
	
	//Audio Test Mode
	private static boolean mEnableMP3 = false;
	private static boolean mEnableVoice = false;
	private static boolean mAutoRename = false;
	private static boolean mOpusRename = false;
	private static boolean mEnableWave = false;
	private static boolean mAudioEffectRename = false;
	private static boolean mAGCRename = false;
	private static boolean mAECRename = false;
	private static boolean mNSRename = false;
	private static boolean mPacketLostRename = false;
	private static boolean mDisorderPlay = false;
	
	//Packet Lost Rename Config
	private static boolean mPacketLostTCP = false;
	private static boolean mPacketLostUDPAck = false;
	private static boolean mPacketLostUDPNoAck = false;
	private static boolean mPacketLostDuplicate = false;
	private static int mRetransmitCount = 2;
	private static int mRetransmitInterval = 10;
	private static int mPacketLostRate = 0;
	
	//VoIP|Audio configuration
	public static final int AUDIO_CONFIG_TYPE_AEC 				= 0; //AcousticEchoCanceler
	public static final int AUDIO_CONFIG_TYPE_AGC 				= 1; //AutomaticGainControl
	public static final int AUDIO_CONFIG_TYPE_NS 				= 2;  //NoiseSuppressor
	public static final int AUDIO_CONFIG_TYPE_CFC			 	= 3; //ComposedFrameCount
	public static final int AUDIO_CONFIG_TYPE_PRN 				= 4; //PullRequestNumber
	public static final int AUDIO_CONFIG_TYPE_OC 				= 5; //OpusComplexity
	public static final int AUDIO_CODEC_TYPE_OPUS 				= 8;
	public static final int AUDIO_OPUS_SIGNAL					= 9;
	public static final int AUDIO_OPUS_APPLICATION 				= 10;
	public static final int AUDIO_OPUS_BANDWIDTH				= 11;
	public static final int AUDIO_DROP_MODE						= 12;
	public static final int AUDIO_DYNAMIC_PLAYBACK_RATE			= 13;
	public static final int AUDIO_AECM_CONTROL					= 14;
	
	//Android Audio Effect: Equalizer
	private static boolean mEnableEqualizer = false;
	private static short mEquBand0GainValue = 0;
	private static short mEquBand1GainValue = 0;
	private static short mEquBand2GainValue = 0;
	private static short mEquBand3GainValue = 0;
	private static short mEquBand4GainValue = 0;
	private static int mEquTargetMilliFreq = 1500000;
	
	//WebRTC VAD Config
	private static boolean mEnableVAD = true;
	private static boolean mSendIfVAD = false;
	
	//WebRTC AGC Config
	private static boolean mEnableAGC = true;	//Automatic Gain Control
	private static int mAGCMode = AGCWrapper.AGC_MODE_ADAPTIVE_DIGITAL;
	private static int mAGCMicMinLevel = 2;
	private static int mAGCMicMaxLevel = 9;
	private static boolean mAGCAddFarEnd = false;
	private static boolean mAGCHasEcho = false;
	private static int mTargetLvDbfs = 3;	//AGC Default: 3
	private static int mCompressionGaindB = 3;
	private static final int mDefaultCompressionGaindB = 9;	//AGC Default: 9
	private static int mLimiterEnable = 1;
	private static boolean mInitAGCConfig = false;
	private static int mAGCMaxCompressiondB = 24;
	private static int mAGCProcSampleTimeMs = 20;	//10ms or 20ms
	
	//WebRTC AEC Config
	private static boolean mEnableAECM = true;	//Acoustic Echo Canceler
	private static int mAECDelayTimeBias = 0;	//Adjust for AEC Delay Time
	private static int mAECProcessSampleCount = 160;
	
	//WebRTC NS Config
	private static boolean mEnableNS = true;	//Noise Suppression
	private static int mNSMode = NSWrapper.NS_MODE_AGGRESSIVE;
	private static boolean mNSAutoDB = false;	//Adjust AGC Default DB threshold by NS Mode
	private static String mNSSampleType = "white";	//Tested Noise Sample Type
	
	//Audio Gain Adjust
	private static boolean mAdjustGain = false;
	private static int mAdjustGainLevel = 1;
	private static int mDBMinThreshold = -28;
	private static int mDBMaxThreshold = -16;
	public static final int NS_MILD_DB_MIN_THRESHOLD = -32;
	public static final int NS_MEDIUM_DB_MIN_THRESHOLD = -34;
	public static final int NS_AGGRESSIVE_DB_MIN_THRESHOLD = -36;
	
	//Audio DB Value Counter
	private static boolean mCalculateDB = false;
	private static boolean mSaveDB = false;
	
	//Audio Wave File Saver
	private static boolean mEnableAudioSave = false;
	
	//Audio Encoded Data Size Logger
	private static boolean mEnableEncDataLog = false;
	
	//Audio Delay Control
	private static boolean mEnableDropMode = true;
	private static boolean mEnableDynPlaybackRate = false;
	private static boolean mFixedDynamicRate = true;
	private static int mAdjustPlaybackRatio = 10;	//speed up audio play back ratio with value:(1+1/mAdjustPlaybackRatio)
	private static int mAudioDropRate = 3; //Every number of mAudioDropRate, drop one audio frames.
	private static boolean mEnableAudioTrackDelay = false;
	private static int mAudioTrackDelayTime = 40;
	private static int mDynRateSpeedUpLevel = 1;
	
	//AudioRecorder FadeIn Control
	private static boolean mEnableRecorderFadeIn = true;	//Note: fadein control for initial recorded audio data.
	
	//Voice Detection Relative Control
	private static boolean mEnableQueueVoice = true;
	private static int mMinimumVoiceQueueSize = 4;
	private static boolean mDropNonVoiceData = true;
	
	//Audio Control when no voice data
	private static boolean mDuplicateAudioData = false;	//Duplicate Audio Data with Drop Control
	private static boolean mWriteSilentData = false;
	private static boolean mPauseAudioTrack = false;
	private static boolean mOpusSilentData = false;
	private static boolean mDuplicateData = false;
	//private static boolean mGenerateData = false;
	private static boolean mEnableGenData = false;
	private static boolean mGenPairData = false;
	private static boolean mGenSinData = false;
	private static int mSinToneGen = 660;
	private static boolean mGenFadeInOut = false;
	private static boolean mCountAmplitudeDiff = false;
	
	//Default Audio Frame Size in ms.
	public static int mAudioFrameSize = 40;
	
	//Audio Composed Frames Count
	public static int COUNT_COMPOSED_FRAMES = 1;
	
	//Audio channel count
	public static int AUDIO_CHANNEL_COUNT = 2;
	
	//Network simulate control
	private static boolean mSaveRecvTimeLog = false;	//Saving received audio packet's interval
	private static boolean mSaveRecvSeqNo = false;		//Saving received audio packet's seqno
	private static boolean mRecvSeqNoProfile = false;
	private static boolean mSendDelayProfile = false;
	private static boolean mRecvDelayProfile = false;
	
	//Audio Sample Rate configuration set:
//	public static int RECORDER_BASE_SAMPLERATE = 8000;
//	public static int RECORDER_CUR_SAMPLERATE = 8000;
//	public static int AUDIOTRACK_BASE_SAMPLERATE = 8000;
//	public static int AUDIOTRACK_CUR_SAMPLERATE = 8000;

//	public static int RECORDER_BASE_SAMPLERATE = 12000;
//	public static int RECORDER_CUR_SAMPLERATE = 11025;
//	public static int AUDIOTRACK_BASE_SAMPLERATE = 12000;
//	public static int AUDIOTRACK_CUR_SAMPLERATE = 12000;

	public static int RECORDER_BASE_SAMPLERATE = 16000;
	public static int RECORDER_CUR_SAMPLERATE = 16000;
	public static int AUDIOTRACK_BASE_SAMPLERATE = 16000;
	public static int AUDIOTRACK_CUR_SAMPLERATE = 16000;
	
//	public static int RECORDER_BASE_SAMPLERATE = 48000;
//	public static int RECORDER_CUR_SAMPLERATE = 48000;
//	public static int AUDIOTRACK_BASE_SAMPLERATE = 48000;
//	public static int AUDIOTRACK_CUR_SAMPLERATE = 48000;
	
	private AudioConfig() {
		SINGLETON_INSTANCE = this;
	}

	public static AudioConfig GetInstance() {
		if (SINGLETON_INSTANCE == null){
			SINGLETON_INSTANCE = new AudioConfig();
		}

		return SINGLETON_INSTANCE;
	}
	
	public void setAudioFrameSize(int frameSize){
		if(frameSize > 0){
			mAudioFrameSize = frameSize;
		}
	}
	
	public int getAudioFrameSize(){
		return mAudioFrameSize;
	}
	
	public void setAudioSampleRate(int newSampleRate){
		RECORDER_BASE_SAMPLERATE = newSampleRate;
		RECORDER_CUR_SAMPLERATE = newSampleRate;
		AUDIOTRACK_BASE_SAMPLERATE = newSampleRate;
		AUDIOTRACK_CUR_SAMPLERATE = newSampleRate;
	}
	
	public int getAudioSampleRate(){
		return RECORDER_BASE_SAMPLERATE;
	}
	
	public boolean initAudioBaseSampleRate(int newSampleRate){
		boolean bValidSampleRate = true;
		if(8000 == newSampleRate){
			RECORDER_BASE_SAMPLERATE = 8000;
			RECORDER_CUR_SAMPLERATE = 8000;
			AUDIOTRACK_BASE_SAMPLERATE = 8000;
			AUDIOTRACK_CUR_SAMPLERATE = 8000;
		}
		else if(12000 == newSampleRate){
			RECORDER_BASE_SAMPLERATE = 12000;
			RECORDER_CUR_SAMPLERATE = 11025;
			AUDIOTRACK_BASE_SAMPLERATE = 12000;
			AUDIOTRACK_CUR_SAMPLERATE = 12000;
		}
		else if(16000 == newSampleRate){
			RECORDER_BASE_SAMPLERATE = 16000;
			RECORDER_CUR_SAMPLERATE = 16000;
			AUDIOTRACK_BASE_SAMPLERATE = 16000;
			AUDIOTRACK_CUR_SAMPLERATE = 16000;
		}
		else if(48000 == newSampleRate){
			RECORDER_BASE_SAMPLERATE = 48000;
			RECORDER_CUR_SAMPLERATE = 48000;
			AUDIOTRACK_BASE_SAMPLERATE = 48000;
			AUDIOTRACK_CUR_SAMPLERATE = 48000;
		}
		else{
			bValidSampleRate = false;
		}
		return bValidSampleRate;
	}
	
	public boolean getAECMConfig(){
		return mEnableAECM;
	}
	
	public void setAECMConfig(boolean bEnable){
		mEnableAECM = bEnable;
	}
	
	public boolean getDropModeConfig(){
		return mEnableDropMode;
	}
	
	public void setDropModeConfig(boolean bEnable){
		mEnableDropMode = bEnable;
	}
	
	public boolean getDynPlaybackRateConfig(){
		return mEnableDynPlaybackRate;
	}
	
	public void setDynPlaybackRateConfig(boolean bEnable){
		mEnableDynPlaybackRate = bEnable;
	}
	
	public void setEnableWaveSaver(boolean bEnable){
		mEnableAudioSave = bEnable;
	}
	
	public boolean getEnableWaveSaver(){
		return mEnableAudioSave;
	}
	
	public void setPlaybackSpeedupRatio(int value){
		mAdjustPlaybackRatio = value;
	}

	public int getPlaybackSpeedupRatio(){
		return mAdjustPlaybackRatio;
	}
	
	public void setComposedFrameCount(int composedFrameCount){
		if((composedFrameCount > 0) && (composedFrameCount < 20)){
			COUNT_COMPOSED_FRAMES = composedFrameCount;
		}
	}
	
	public int getComposedFrameCount(){
		return COUNT_COMPOSED_FRAMES;
	}
	
	public int getAudioDropRate(){
		return mAudioDropRate;
	}
	
	public void setAudioDropRate(int droprate){
		if(droprate > 1){
			mAudioDropRate = droprate;
		}
	}
	
	public boolean getEnableLogEncData()
	{
		return mEnableEncDataLog;
	}
	
	public void setEnableLogEncData(boolean bEnable){
		mEnableEncDataLog = bEnable;
	}
	
	public boolean getEnableQueueVoice(){
		return mEnableQueueVoice;
	}
	
	public void setEnableQueueVoice(boolean bEnable){
		mEnableQueueVoice = bEnable;
	}
	
	public int getMinimumVoicePlayQueueSize(){
		return mMinimumVoiceQueueSize;
	}
	
	public void setMinimumVoicePlayQueueSize(int value){
		mMinimumVoiceQueueSize = value;
	}
	
	public boolean getIsFixedDynamicRate(){
		return mFixedDynamicRate;
	}
	
	public void setIsFixedDynamicRate(boolean bEnable){
		mFixedDynamicRate = bEnable;
	}
	
	public boolean getEnableAudioTrackDelay(){
		return mEnableAudioTrackDelay;
	}
	
	public void setEnableAudioTrackDelay(boolean bEnable){
		mEnableAudioTrackDelay = bEnable;
	}
	
	public int getAudioTrackDelayTime(){
		return mAudioTrackDelayTime;
	}
	
	public void setAudioTrackDelayTime(int delayTime){
		mAudioTrackDelayTime = delayTime;
	}
	
	public boolean getDropNonVoiceData(){
		return mDropNonVoiceData;
	}
	
	public void setDropNonVoiceData(boolean bEnable){
		mDropNonVoiceData = bEnable;
	}
	
	public boolean getEnableTestMode(){
		return mEnableTestMode;
	}
	
	public void setEnableTestMode(boolean bEnable){
		mEnableTestMode = bEnable;
	}
	
	public void setEnableAECTestMode(boolean bEnable){
		mEnableAECTestMode = bEnable;
	}
	
	public boolean getEnableAECTestMode(){
		return mEnableAECTestMode;
	}
	
	public boolean getWriteSilentData(){
		return mWriteSilentData;
	}
	
	public void setWriteSilentData(boolean bEnable){
		mWriteSilentData = bEnable;
	}
	
	public boolean getPauseAudioTrack(){
		return mPauseAudioTrack;
	}
	
	public void setPauseAudioTrack(boolean bEnable){
		mPauseAudioTrack = bEnable;
	}
	
	public boolean getOpusSilentData(){
		return mOpusSilentData;
	}
	
	public void setOpusSilentData(boolean bEnable){
		mOpusSilentData = bEnable;
	}
	
	public boolean getDuplicateData(){
		return mDuplicateData;
	}
	
	public void setDuplicateData(boolean bEnable){
		mDuplicateData = bEnable;
	}
	
	public boolean getEnableGenData(){
		return mEnableGenData;
	}
	
	public void setEnableGenData(boolean bEnable){
		mEnableGenData = bEnable;
	}
	
	public boolean getGenPairData(){
		return mGenPairData;
	}
	
	public void setGenPairData(boolean bEnable){
		mGenPairData = bEnable;
	}
	
	public boolean getGenSinData(){
		return mGenSinData;
	}
	
	public void setGenSinData(boolean bEnable){
		mGenSinData = bEnable;
	}
	
	public boolean getGenFadeInOut(){
		return mGenFadeInOut;
	}
	
	public void setGenFadeInOut(boolean bEnable){
		mGenFadeInOut = bEnable;
	}
	
	public int getSinTone(){
		return mSinToneGen;
	}
	
	public void setSinTone(int value){
		mSinToneGen = value;
	}
	
	public boolean getCountAmplitudeDiff(){
		return mCountAmplitudeDiff;
	}
	
	public void setCountAmplitudeDiff(boolean bEnable){
		mCountAmplitudeDiff = bEnable;
	}
	
	public int getDynSpeedUpLevel(){
		return mDynRateSpeedUpLevel;
	}
	
	public void addDynSpeedUpLevel(int value){
		mDynRateSpeedUpLevel += value;
	}
	
	public void setDynSpeedUpLevel(int value){
		mDynRateSpeedUpLevel = value;
	}
	
	public boolean getAdjustGain(){
		return mAdjustGain;
	}
	
	public void setAdjustGain(boolean bEnable){
		mAdjustGain = bEnable;
	}
	
	public int getAdjustGainLevel(){
		return mAdjustGainLevel;
	}
	
	public void addAdjustGainLevel(int value){
		mAdjustGainLevel += value;
	}
	
	public void setAdjustGainLevel(int value){
		mAdjustGainLevel = value;
	}
	
	public int getDBMinThreshold(){
		return mDBMinThreshold;
	}
	
	public void setDBMinThreshold(int dbMinThreshold){
		mDBMinThreshold = dbMinThreshold;
	}
	
	public int getAGCMaxCompressiondB(){
		return mAGCMaxCompressiondB;
	}
	
	public void setAGCMaxCompressiondB(int value){
		mAGCMaxCompressiondB = value;
	}
	
	public int getAGCProcSampleTime(){
		return mAGCProcSampleTimeMs;
	}
	
	public void setAGCProcSampleTime(int time){
		mAGCProcSampleTimeMs = time;
	}
	
	public int getDBMaxThreshold(){
		return mDBMaxThreshold;
	}
	
	public void setDBMaxThreshold(int dbMaxThreshold){
		mDBMaxThreshold = dbMaxThreshold;
	}
	
	public boolean getEnableNS(){
		return mEnableNS;
	}
	
	public void setEnableNS(boolean bEnable){
		mEnableNS = bEnable;
	}
	
	public boolean getEnableVAD(){
		return mEnableVAD;
	}
	
	public void setEnableVAD(boolean bEnable){
		mEnableVAD = bEnable;
	}
	
	public boolean getSendIfVAD(){
		return mSendIfVAD;
	}
	
	public void setSendIfVAD(boolean bEnable){
		mSendIfVAD = bEnable;
	}
	
	public boolean getEnableEqualizer(){
		return mEnableEqualizer;
	}
	
	public void setEnableEqualizer(boolean bEnable){
		mEnableEqualizer = bEnable;
	}
	
	public boolean getEnableAGC(){
		return mEnableAGC;
	}
	
	public void setEnableAGC(boolean bEnable){
		mEnableAGC = bEnable;
	}
	
	public int getAGCMode(){
		return mAGCMode;
	}
	
	public void setAGCMode(int mode){
		mAGCMode = mode;
	}
	
	public int getAGCMicMinLevel(){
		return mAGCMicMinLevel;
	}
	
	public void setAGCMicMinLevel(int value){
		mAGCMicMinLevel = value;
	}
	
	public int getAGCMicMaxLevel(){
		return mAGCMicMaxLevel;
	}
	
	public void setAGCMicMaxLevel(int value){
		mAGCMicMaxLevel = value;
	}
	
	public boolean getCalculateDB(){
		return mCalculateDB;
	}
	
	public void setCalculateDB(boolean bEnable){
		mCalculateDB = bEnable;
	}
	
	public boolean getSaveDB(){
		return mSaveDB;
	}
	
	public void setSaveDB(boolean bEnable){
		mSaveDB = bEnable;
	}
	
	public boolean getDuplicateAudioData(){
		return mDuplicateAudioData;
	}
	
	public void setDuplicateAudioData(boolean bEnable){
		mDuplicateAudioData = bEnable;
	}
	
	public int getAECDelayTimeBias(){
		return mAECDelayTimeBias;
	}
	
	public void setAECDelayTimeBias(int delayBias){
		mAECDelayTimeBias = delayBias;
	}
	
	public int getAECProcessSampleCount(){
		return mAECProcessSampleCount;
	}
	
	public void setAECProcessSampleCount(int sampleCount){
		mAECProcessSampleCount = sampleCount;
	}
	
	public boolean getAGCHasEcho(){
		return mAGCHasEcho;
	}
	
	public void setAGCHasEcho(boolean bEnable){
		mAGCHasEcho = bEnable;
	}
	
	public boolean getAGCAddFarEnd(){
		return mAGCAddFarEnd;
	}
	
	public void setAGCAddFarEnd(boolean bEnable){
		mAGCAddFarEnd = bEnable;
	}
	
	public int getAGCTargetLvDbfs(){
		return mTargetLvDbfs;
	}
	
	public void setAGCTargetLvDbfs(int targetDbfs){
		mTargetLvDbfs = targetDbfs;
	}
	
	public int getAGCDefaultCompressionGaindB(){
		return mDefaultCompressionGaindB;
	}
	
//	public void setAGCDefaultCompressionGaindB(int value){
//		mDefaultCompressionGaindB = value;
//	}
	
	public int getAGCCompressionGaindB(){
		return mCompressionGaindB;
	}
	
	public void setAGCCompressionGaindB(int compressionGaindB){
		mCompressionGaindB = compressionGaindB;
	}
	
	public int getAGCLimiterEnable(){
		return mLimiterEnable;
	}
	
	public void setAGCLimiterEnable(int limiterEnable){
		mLimiterEnable = limiterEnable;
	}
	
	public boolean getInitAGCConfig(){
		return mInitAGCConfig;
	}
	
	public void setInitAGCConfig(boolean bEnable){
		mInitAGCConfig = bEnable;
	}
	
	public int getNSMode(){
		return mNSMode;
	}
	
	public void setNSMode(int mode){
		mNSMode = mode;
	}
	
	public boolean getNSAutoDB(){
		return mNSAutoDB;
	}
	
	public void setNSAutoDB(boolean bEnable){
		mNSAutoDB = bEnable;
	}
	
	public boolean getEnableMP3(){
		return mEnableMP3;
	}
	
	public void setEnableMP3(boolean bEnable){
		mEnableMP3 = bEnable;
	}
	
	public boolean getEnableVoice(){
		return mEnableVoice;
	}
	
	public void setEnableVoice(boolean bEnable){
		mEnableVoice = bEnable;
	}
	
	public boolean getAutoRename(){
		return mAutoRename;
	}
	
	public void setAutoRename(boolean bEnable){
		mAutoRename = bEnable;
	}
	
	public boolean getOpusRename(){
		return mOpusRename;
	}
	
	public void setOpusRename(boolean bEnable){
		mOpusRename = bEnable;
	}
	
	public boolean getAudioEffectRename(){
		return mAudioEffectRename;
	}
	
	public void setAudioEffectRename(boolean bEnable){
		mAudioEffectRename = bEnable;
	}
	
	public boolean getAGCRename(){
		return mAGCRename;
	}
	
	public void setAGCRename(boolean bEnable){
		mAGCRename = bEnable;
	}
	
	public boolean getAECRename(){
		return mAECRename;
	}
	
	public void setAECRename(boolean bEnable){
		mAECRename = bEnable;
	}
	
	public boolean getNSRename(){
		return mNSRename;
	}
	
	public void setNSRename(boolean bEnable){
		mNSRename = bEnable;
	}
	
	public boolean getEnableWave(){
		return mEnableWave;
	}
	
	public void setEnableWave(boolean bEnable){
		mEnableWave = bEnable;
	}
	
	public boolean getSendDelayProfile(){
		return mSendDelayProfile;
	}
	
	public void setSendDelayProfile(boolean bEnable){
		mSendDelayProfile = bEnable;
	}
	
	public boolean getRecvSeqNoProfile(){
		return mRecvSeqNoProfile;
	}
	
	public void setRecvSeqNoProfile(boolean bEnable){
		mRecvSeqNoProfile = bEnable;
	}
	
	public boolean getRecvDelayProfile(){
		return mRecvDelayProfile;
	}
	
	public void setRecvDelayProfile(boolean bEnable){
		mRecvDelayProfile = bEnable;
	}
	
	public String getNSSampleType(){
		return mNSSampleType;
	}
	
	public void setNSSampleType(String sampleType){
		mNSSampleType = sampleType;
	}
	
	public boolean getEnableRecvPacketTimeLog(){
		return mSaveRecvTimeLog;
	}
	
	public void setEnableRecvPacketTimeLog(boolean bEnable){
		mSaveRecvTimeLog = bEnable;
	}
	
	public boolean getSaveRecvSeqNo(){
		return mSaveRecvSeqNo;
	}
	
	public void setSaveRecvSeqNo(boolean bEnable){
		mSaveRecvSeqNo = bEnable;
	}
	
	public void setEquBand0GainValue(short value){
		mEquBand0GainValue = value;
	}
	
	public short getEquBand0GainValue(){
		return mEquBand0GainValue;
	}
	
	public void setEquBand1GainValue(short value){
		mEquBand1GainValue = value;
	}
	
	public short getEquBand1GainValue(){
		return mEquBand1GainValue;
	}
	
	public void setEquBand2GainValue(short value){
		mEquBand2GainValue = value;
	}
	
	public short getEquBand2GainValue(){
		return mEquBand2GainValue;
	}
	
	public void setEquBand3GainValue(short value){
		mEquBand3GainValue = value;
	}
	
	public short getEquBand3GainValue(){
		return mEquBand3GainValue;
	}
	
	public void setEquBand4GainValue(short value){
		mEquBand4GainValue = value;
	}
	
	public short getEquBand4GainValue(){
		return mEquBand4GainValue;
	}
	
	public int getEquTargetMilliFreq(){
		return mEquTargetMilliFreq;
	}
	
	public void setEquTargetMilliFreq(int freqMili){
		mEquTargetMilliFreq = freqMili;
	}
	
	public boolean getEnableRecorderFadeIn(){
		return mEnableRecorderFadeIn;
	}
	
	public void setEnableRecorderFadeIn(boolean bEnable){
		mEnableRecorderFadeIn = bEnable;
	}
	
	public boolean getDisorderPlay(){
		return mDisorderPlay;
	}
	
	public void setDisorderPlay(boolean bEnable){
		mDisorderPlay = bEnable;
	}
	
	public boolean getPacketLostRename(){
		return mPacketLostRename;
	}
	
	public void setPacketLostRename(boolean bEnable){
		mPacketLostRename = bEnable;
	}
	
	public void setPacketLostTCP(boolean bEnable){
		mPacketLostTCP = bEnable;
	}
	
	public boolean getPacketLostTCP(){
		return mPacketLostTCP;
	}
	
	public void setPacketLostUDPAck(boolean bEnable){
		mPacketLostUDPAck = bEnable;
	}
	
	public boolean getPacketLostUDPAck(){
		return mPacketLostUDPAck;
	}
	
	public boolean getPacketLostUDPNoAck(){
		return mPacketLostUDPNoAck;
	}
	
	public void setPacketLostUDPNoAck(boolean bEnable){
		mPacketLostUDPNoAck = bEnable;
	}
	
	public boolean getPacketLostDuplicate(){
		return mPacketLostDuplicate;
	}
	
	public void setPacketLostDuplicate(boolean bEnable){
		mPacketLostDuplicate = bEnable;
	}
	
	public int getRetransmitCount(){
		return mRetransmitCount;
	}
	
	public void setRetransmitCount(int count){
		mRetransmitCount = count;
	}
	
	public int getPacketLostRate(){
		return mPacketLostRate;
	}
	
	public void setPacketLostRate(int lostRate){
		mPacketLostRate = lostRate;
	}
	
	public int getRetransmitInterval(){
		return mRetransmitInterval;
	}
	
	public void setRetransmitInterval(int interval){
		mRetransmitInterval = interval;
	}
	
}
