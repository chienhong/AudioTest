package com.example.audiotest.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect.Descriptor;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.Equalizer;
import android.media.audiofx.NoiseSuppressor;
import android.preference.PreferenceManager;
import android.util.Log;

public class AudioProcessor {

	public static final String LOG_TAG = "AudioProcessor";
	public static final String AUDIO_CODECMIME = "audio/mp4a-latm";
	
	private int mRecorderBaseSampleRate = 8000;
	private int mRecorderCurSampleRate = 8000;
	private int mAudioTrackBaseSampleRate = 8000;
	private int mAudioTrackCurSampleRate = 8000;

	// Audio Delay Mode and Settings
	public static final int AUDIO_DELAY_SMOOTH = 1; //play every data
	public static final int AUDIO_DELAY_NORMAL = 2; //every three data drop one
	public static final int AUDIO_DELAY_SERIOUS = 3; //every three data drop two
	public static final int AUDIO_DELAY_DROPPED = 4; //dropped all data

	// Adjust Audio Track playback rate: threshold queue size
	private static final int ADJUST_AUDIO_RATE_THRESHOLD = 12;

	public static final int QUEUESIZE_NONE_DELAY = 3 * 3;
	public static final int QUEUESIZE_SMOOTH_DELAY = 6 * 3;
	public static final int QUEUESIZE_NORMAL_DELAY = 10 * 3;
	public static final int QUEUESIZE_SERIOUS_DELAY = 40;
	
	// Audio Wave Saver
	private AudioWaveSaver mAudioWaveSaver = null;
	
	// Audio Data Logger: logger for encoded/decoded data size.
	private AudioDataLogger mEncDataLogger = null;
	
	// Audio Recorder Fadein Control: 
	private int mRecorderFadeInCount = 1;	//AEC first processed voice would generate annoying voice
	private int mRecorderFadeInMaxCount = 5;
	
	// Acoustic Echo Canceler
	private AecmWrapper mAecmWrapper = null;
	private short[] mAecmOutputData = null;
	
	// Noise Suppression
	private NSWrapper mNSWrapper = null;
	private short[] mNSOutputData = null;
	
	// Automatic Gain Control
	private AGCWrapper mAGCWrapper = null;
	private short[] mAGCOutputData = null;
	
	// Voice Activity Detection
	private VADWrapper mVADWrapper = null;
	private boolean mLastIsVoice = false;
	
	// Audio Effect: Equalizer
	private Equalizer mEqualizer = null;
	private boolean mIsEqualizerEnabled = false;
	
	public interface IAudioProcessorCallback {
		public void SendData(byte[] pData);
		public short[] ObtainData();	//Add for AudioTest support obtaining mp3 source data from outside.
	};
	
	private IAudioProcessorCallback mIAudioProcessorCallback = null;

	private int mDelayMode = AUDIO_DELAY_SMOOTH;

	//public static int mChannelCount = 2;
	private static final int AUDIO_PLAY_QUEUE_SIZE = 1000;
	public static BlockingQueue<AudioDataRecord> mAudioPlayBuffer = new ArrayBlockingQueue<AudioDataRecord>(AUDIO_PLAY_QUEUE_SIZE);
	private boolean mWaitVoiceBuffer = false;
	private boolean mHasGenNonVoiceData = false;
	private int mSilentCount = 0;
	private int mAmplitudeAverage = 0;
	private final int mAmplitudeSampleCount = 15;
	private int[] mAmplitudeArray = new int[mAmplitudeSampleCount];
	private int mAmplitudeIndex = 0;

	private static final int AUDIO_CHANNEL = 2;
	private AudioTrack mAudioTrack;
	private AtomicBoolean mAdjustingSampleRate = new AtomicBoolean();
	private AtomicBoolean mWaitAdjust = new AtomicBoolean();
	private Thread mAudioTrackPlay = null;
	private AudioRecord mAudioRecorder = null;
	private boolean mIsRunning = false;
	private boolean mIsRecordRunning = false;
	private Thread mAudioPacketSendThread;

	//Delay Control
	private boolean mAudioClearInitBuffer = true;	//Clear initialized audio data when first start streamer.
	//private int mInitialCount = 0;

	private boolean mIsTxFinished = false;
	//private SilenceDetection mSilenceChecker = null;
	private byte[] mDisorderFrame = null;

	// Audio Performance Counter for max amplitude and db value.
	private AudioPerformanceCounter mAudioPerformanceCounter = null;
	private int mAudioPerformanceDataCount = 0;
	private FrameAmplitudeDiffCounter mFrameAmplitudeDiffCounter = null;
	
	// Audio Default Buffer Size for encoder and decoder
	private static int mEncAudioBufferSize = 3840;
	private static int mDecAudioBufferSize = 3840;
	private short[] mSilentData = null;
	private short[] mLastPlayedData = null;
	
	// Audio No Voice Control
	private int mCurPlaybackHeadPos = 0;
	private int mRemainPlaybackFrames = 0;
	//private boolean mIsAudioTrackPause = false;
	private int mLastPitch = 0;
	private short[] mGenOpositeData = null;
	private short[] mGeneratedSinData = null;
	private short[] mGenFadeData = null;
	
	// Opus items
	private boolean bPacketLost = false;
	private OpusWrapper mOpusDecoder = null;
	private OpusWrapper mOpusEncoder = null;
	private short[] mDecodeOpusBuffer = null;
	private byte[] mEncodeOpusBuffer = null;
	private byte mVADOpusEmptyData = 99;
	private byte[] mDecodeOpusEmptyData = new byte[1];
	private short[] mDecodeOpusEmptyBuffer = null;

	// Amplitude counter for Recorder/Encoder Side
	private short[] mRecordedDataBuffer = null;
	private short[] mRecordedDataMono = null;
	private ShortBuffer mRecordDataBuffer = null;
//	private int mEncAmplitudeAverage = 0;
//	private final int mEncAmplitudeSampleCount = 20;
//	private int[] mEncAmplitudeArray = new int[mEncAmplitudeSampleCount];
//	private int mEncAmplitudeIndex = 0;
	private int mEncAmplitudeAdjustTimes = -1;
	
	// Audio Relative Configurations
	private static AudioConfig mAudioConfig = null;
	private static OpusConfig mOpusConfig = null;
	private int mAudioMode = 0;
	private AtomicInteger mAudioFrameDropCount = new AtomicInteger();	//Audio Frame Drop counter with audio drop rate.
	
	private AudioManager mAudioManager = null;
	private SharedPreferences mSharedPreferences = null;
	private Context mContext = null;
	private static final String PERF_KEY_VOLUME = "volume";
	
	public AudioProcessor(String userName, IAudioProcessorCallback audioProcessorCallback, int testmode, Context context){
		mAudioMode = testmode;
		mIAudioProcessorCallback = audioProcessorCallback;
		
		mAudioConfig = AudioConfig.GetInstance();
		mOpusConfig = OpusConfig.GetInstance();
		
		mContext = context;
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

		mSharedPreferences = (SharedPreferences) PreferenceManager.getDefaultSharedPreferences(mContext);
		//(PERF_AUDIO_STREAMER, Context.MODE_PRIVATE);
	}
	
	public AudioProcessor(String userName, IAudioProcessorCallback audioProcessorCallback, Context context) {
		mIAudioProcessorCallback = audioProcessorCallback;
		
		mAudioConfig = AudioConfig.GetInstance();
		mOpusConfig = OpusConfig.GetInstance();
		
		mContext = context;
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		//mSharedPreferences = (SharedPreferences) mContext.getSharedPreferences(PERF_AUDIO_STREAMER, Context.MODE_PRIVATE);
		mSharedPreferences = (SharedPreferences) PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	public boolean isTxDone() {
		return mIsTxFinished;
	}
	
	public boolean isPlayRunning() {
		return mIsRunning;
	}

	public boolean isRecordRunning() {
		return mIsRecordRunning;
	}

	private void initSampleRate()
	{
		mRecorderBaseSampleRate = AudioConfig.RECORDER_BASE_SAMPLERATE;
		mRecorderCurSampleRate = AudioConfig.RECORDER_CUR_SAMPLERATE;
		mAudioTrackBaseSampleRate = AudioConfig.AUDIOTRACK_BASE_SAMPLERATE;
		mAudioTrackCurSampleRate = AudioConfig.AUDIOTRACK_CUR_SAMPLERATE;
	}
	
	private void initAudioBufferSize(){
		int sampleRate = getValidSampleRates(mRecorderCurSampleRate);
		Log.d(LOG_TAG, "getValidSampleRates: " + sampleRate);
		if (sampleRate > 0) {
			mRecorderCurSampleRate = sampleRate;
			if (sampleRate <= 8000) {
				mRecorderBaseSampleRate = 8000;
				mAudioTrackBaseSampleRate = 8000;
				mAudioTrackCurSampleRate = 8000;
			}
			else if (sampleRate < 16000) {
				mRecorderBaseSampleRate = 12000;
				mAudioTrackBaseSampleRate = 12000;
				mAudioTrackCurSampleRate = 12000;
			}
			else if(sampleRate == 16000){
				mRecorderBaseSampleRate = 16000;
				mAudioTrackBaseSampleRate = 16000;
				mAudioTrackCurSampleRate = 16000;
			}
			else if (sampleRate <= 26000) {
				mRecorderBaseSampleRate = 24000;
				mAudioTrackBaseSampleRate = 24000;
				mAudioTrackCurSampleRate = 24000;
			}
			else {
				mRecorderBaseSampleRate = 48000;
				mAudioTrackBaseSampleRate = 48000;
				mAudioTrackCurSampleRate = 48000;
			}
			mEncAudioBufferSize = (mRecorderBaseSampleRate*mAudioConfig.getAudioFrameSize()*AUDIO_CHANNEL)/1000;
			mDecAudioBufferSize = (mAudioTrackBaseSampleRate*mAudioConfig.getAudioFrameSize()*AUDIO_CHANNEL)/1000;
			Log.d(LOG_TAG, String.format("mEncAudioBufferSize=%d, mDecAudioBufferSize=%d", 
					mEncAudioBufferSize, mDecAudioBufferSize));
		}
	}
	
	/**
	 * initialize Audio Control relative settings
	 */
	private void initAudioControlConfig(){
		mAudioConfig.setDynSpeedUpLevel(1);
		mWaitVoiceBuffer = true;

		mAdjustingSampleRate.set(false);	//might need to adjust sample rate
		mWaitAdjust.set(false);
		
		mAudioPlayBuffer.clear();	//clear audio play buffer

		mAudioFrameDropCount.set(1);
		
		for (int index = 0; index < mAmplitudeSampleCount; index++) {
			mAmplitudeArray[index] = 0;
		}

		//Read CPU Speed and init opus complexity
		if(mOpusConfig.getAutoComplexity()){
			Log.d(LOG_TAG, String.format("Auto adjust opus complexity with CPU speed."));
			initOpusComplexity();
		}
		
		if(mAudioConfig.getEnableWaveSaver() || mAudioConfig.getEnableAECTestMode()){
			mAudioWaveSaver = new AudioWaveSaver(mRecorderBaseSampleRate, 2, mDecAudioBufferSize);
		}
		
		if(mAudioConfig.getEnableLogEncData()){
			mEncDataLogger = new AudioDataLogger(AudioDataLogger.MODE_ENC_DATA, mEncAudioBufferSize);
		}
	}
	
	OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                // Pause playback
            	Log.i(LOG_TAG, String.format("onAudioFocusChange AUDIOFOCUS_LOSS_TRANSIENT."));
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback 
            	Log.i(LOG_TAG, String.format("onAudioFocusChange AUDIOFOCUS_GAIN."));
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            	//mAudioManager.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
            	Log.i(LOG_TAG, String.format("onAudioFocusChange AUDIOFOCUS_LOSS."));
            	mAudioManager.abandonAudioFocus(afChangeListener);
                // Stop playback
            }
        }
    };
    
	//Start AudioProcessor with both player and recorder
	public void startAudioProcessor() {

		if (mIsRunning) {
			Log.e(LOG_TAG, "AudioProcessor is already running.");
			return;
		}
		
		mIsRunning = true;
		
		if((null != mAudioManager) && (null != mSharedPreferences)){
			// Request audio focus for playback
			int result = mAudioManager.requestAudioFocus(afChangeListener,
			                                 // Use the music stream.
			                                 AudioManager.STREAM_VOICE_CALL,
			                                 // Request permanent focus.
			                                 AudioManager.AUDIOFOCUS_GAIN);
			   
			if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				//mAudioManager.registerMediaButtonEventReceiver(RemoteControlReceiver);
			    // Start playback.
				Log.i(LOG_TAG, "AUDIOFOCUS_REQUEST_GRANTED success");
			}
			else{
				Log.e(LOG_TAG, "AUDIOFOCUS_REQUEST failed");
			}
			
			int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
			int prefVolume = maxVolume;
			if(mSharedPreferences.contains(PERF_KEY_VOLUME)){
				prefVolume = mSharedPreferences.getInt(PERF_KEY_VOLUME, maxVolume);
				Log.i(LOG_TAG, "shared preference contains key: PERF_KEY_VOLUME");
			}
			else{
				Log.i(LOG_TAG, "shared preference does not contains key: PERF_KEY_VOLUME");
			}
			mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, prefVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
			int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
			Log.i(LOG_TAG, String.format("maxVolume=%d, prefVolume=%d, curVolume=%d", 
					maxVolume, prefVolume, curVolume));
		}
		
		//Set performance counter network status check threshold
		int threshold = ((1000/mAudioConfig.getAudioFrameSize())*3) - 5;
		//mAudioRecvPerformance.setPacketCountThreshold(threshold);
		//mAudioSendPerformance.setPacketCountThreshold(threshold);
		mAudioPerformanceCounter = new AudioPerformanceCounter();
		if(mAudioConfig.getCountAmplitudeDiff()){
			mFrameAmplitudeDiffCounter = new FrameAmplitudeDiffCounter();
		}
		mRecorderFadeInCount = 1;
		
		initSampleRate();
		initAudioBufferSize();
		
		mVADWrapper = new VADWrapper();
		if(!mVADWrapper.getIsVADInit()){
			mVADWrapper = null;
		}
		
		mAecmWrapper = new AecmWrapper(mRecorderBaseSampleRate, mAudioConfig.getAECProcessSampleCount());
		if(!mAecmWrapper.getIsAecmInitSuccess()){
			mAecmWrapper = null;
		}
		
		int nsMode = mAudioConfig.getNSMode();
		mNSWrapper = new NSWrapper(mRecorderBaseSampleRate, nsMode);
		if(!mNSWrapper.getIsNSInit()){
			mNSWrapper = null;
		}
		else{
			if(mAudioConfig.getNSAutoDB()){
				if(NSWrapper.NS_MODE_MILD == nsMode){
					mAudioConfig.setDBMinThreshold(AudioConfig.NS_MILD_DB_MIN_THRESHOLD);
				}
				else if(NSWrapper.NS_MODE_MEDIUM == nsMode){
					mAudioConfig.setDBMinThreshold(AudioConfig.NS_MEDIUM_DB_MIN_THRESHOLD);
				}
				else if(NSWrapper.NS_MODE_AGGRESSIVE == nsMode){
					mAudioConfig.setDBMinThreshold(AudioConfig.NS_AGGRESSIVE_DB_MIN_THRESHOLD);
				}
			}
		}
		
		int minPossibleMicLevel = mAudioConfig.getAGCMicMinLevel();
		int maxPossibleMicLevel = mAudioConfig.getAGCMicMaxLevel();
		mAGCWrapper = new AGCWrapper(mRecorderBaseSampleRate, mAudioConfig.getAGCMode(), minPossibleMicLevel, maxPossibleMicLevel);
		if(!mAGCWrapper.getIsAGCInit()){
			mAGCWrapper = null;
		}
		else{
			if(mAudioConfig.getCalculateDB()){
				//Adjust AGC by DB Value
				mAudioConfig.setAGCCompressionGaindB(mAudioConfig.getAGCDefaultCompressionGaindB());
				mAGCWrapper.agcSetAGCConfig(mAudioConfig.getAGCTargetLvDbfs(), mAudioConfig.getAGCCompressionGaindB(), mAudioConfig.getAGCLimiterEnable());
				mAGCWrapper.agcSetProcessSampleTime(mAudioConfig.getAGCProcSampleTime());
				Log.i("AGCWrapper", String.format("AGC default config: targetLvDbfs=%d, compressionGaindB=%d.", 
					mAudioConfig.getAGCTargetLvDbfs(), mAudioConfig.getAGCCompressionGaindB()));
			}
		}
		
		initAudioControlConfig();
		
		startAudioRecorder();
		startAudioPlay();

		//Start receiver and wait for incoming audio data to play
		//mTransport.startReceiverRunning();

		/*
		(new Thread(){
			@Override
			public void run(){
				mTransport.sendAdjustSampleRate(mRecorderCurSampleRate);
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					Log.e(e);
				}
				mTransport.sendAdjustSampleRate(mRecorderCurSampleRate);
			}
		}).start();*/
	}

	//Stop AudioProcessor with both player and recorder
	public void stopAudioProcessor() {
		if (!mIsRunning) {
			Log.e(LOG_TAG, "AudioProcessor is already stopped.");
			return;
		}
		mIsRunning = false;
		
		try {
			Thread.sleep(200);
		}
		catch (InterruptedException e) {
			//Log.e(LOG_TAG, e.printStackTrace());
			e.printStackTrace();
		}
		
		if(null != mEqualizer){
			mEqualizer.setEnabled(false);
			mEqualizer.release();
			mEqualizer = null;
		}

		if((null != mAudioWaveSaver) && mAudioConfig.getEnableWaveSaver()){
			Thread audioSaveThread = new Thread()
			{
			    @Override
			    public void run() {
			    	mAudioWaveSaver.saveRecvWaveFile();
			    	mAudioWaveSaver = null;
			    }
			};
			audioSaveThread.start();
		}
		
		if((null != mAudioWaveSaver) && mAudioConfig.getEnableAECTestMode()){
			Thread audioSendSaveThread = new Thread()
			{
			    @Override
			    public void run() {
			    	mAudioWaveSaver.saveSendWaveFile();
			    	mAudioWaveSaver = null;
			    }
			};
			audioSendSaveThread.start();
		}
		
		if(mAudioConfig.getEnableLogEncData() && (null != mEncDataLogger)){
			Thread encLoggerThread = new Thread(){
				@Override
				public void run(){
					mEncDataLogger.saveEncDataSize();
					mEncDataLogger = null;
				}
			};
			encLoggerThread.start();
		}
		
//		mTransport.stopRunning();
		stopAudioRecorder();
		stopAudioPlayer();
		
		if(null != mNSWrapper){
			mNSWrapper.nsDestroy();
			mNSWrapper = null;
		}
		
		if(null != mAecmWrapper){
			mAecmWrapper.destroyAEC();
			mAecmWrapper = null;
		}
		
		if(null != mAGCWrapper){
			mAGCWrapper.agcDestroy();
			mAGCWrapper = null;
		}
		
		if(null != mVADWrapper){
			mVADWrapper.vadDestroy();
			mVADWrapper = null;
		}
		
		if((null != mAudioPerformanceCounter) 
				&& mAudioConfig.getCalculateDB()
				&& mAudioConfig.getSaveDB()){
			mAudioPerformanceCounter.writeLogFile();
		}
		
		if(mAudioConfig.getCountAmplitudeDiff() 
			&& (null != mFrameAmplitudeDiffCounter)){
			mFrameAmplitudeDiffCounter.saveAmplitudeDiffResult();
		}
		
		mAudioPlayBuffer.clear();

		if(null != mSharedPreferences){
			int volume = 4;
			if(null != mAudioManager){
				volume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
			}
			
			SharedPreferences.Editor editor = mSharedPreferences.edit();
			editor.putInt(PERF_KEY_VOLUME, volume);
			boolean bCommit = editor.commit();
			mSharedPreferences = null;
			
			Log.d(LOG_TAG, String.format("stopAudioProcessor set pref volume=%d, bCommit=%b",
					volume, bCommit));
		}
		
		if(null != mAudioManager){
			mAudioManager.abandonAudioFocus(afChangeListener);
			mAudioManager = null;
		}

	}

	static int seqno = 0;
	//processing incoming avpacket data
	public boolean processData(byte[] pData) {
		//char packetType, int seqno, long presentationTimeMs, byte[] data, boolean bPacketLost, int lostCount
		if(null != pData){
			DataReceived('A', seqno++, System.currentTimeMillis(), pData, false, 0);
			//mAudioRecvPerformance.setPushQueueSize(1 + AudioProcessor.mAudioPlayBuffer.size());
			//mAudioRecvPerformance.dataCount(pData.length);
			//return mTransport.processData(pData);
			return true;
		}
		else{
			return false;
		}
	}

	private int getOpusSupportedSampleRate(int sampleRate) {
		if (sampleRate >= 36000)
			return 48000;
		else if (sampleRate >= 22050)
			return 24000;
		else if (sampleRate >= 16000)
			return 16000;
		else if (sampleRate >= 11025)
			return 12000;
		return 8000;
	}

	private void initSilentData() {
		mSilentData = new short[mDecAudioBufferSize];
		for (int i = 0; i < mDecAudioBufferSize - 1; i += 2) {
			mSilentData[i] = 0;
			mSilentData[i + 1] = 0;
		}
	}

	private void initDecodeOpusEmptyData(){
		//Configure Opus Silent Data
		int audioFrameSize = mAudioConfig.getAudioFrameSize();
		if(40 == audioFrameSize){
			mDecodeOpusEmptyData[0] = 20;
		}
		else if(20 == audioFrameSize){
			mDecodeOpusEmptyData[0] = 12;
		}
		else if(60 == audioFrameSize){
			mDecodeOpusEmptyData[0] = 28;
		}
		else if(10 == audioFrameSize){
			mDecodeOpusEmptyData[0] = 4;
		}
	}
	
	// Initialize Play environment
	private boolean initializePlayer() {
		Log.d(LOG_TAG, String.format("initializePlay() mAudioTrackBaseSampleRate=%d",
				mAudioTrackBaseSampleRate));

		mDecodeOpusBuffer = new short[mDecAudioBufferSize];
		mDecodeOpusEmptyBuffer = new short[mDecAudioBufferSize];
		mGenOpositeData = new short[mDecAudioBufferSize];
		int opusComplexity = mOpusConfig.getOCValue();
		int opusSignalType = mOpusConfig.getOSTValue();
		int opusApplication = mOpusConfig.getOAValue();
		int opusBandwidth = mOpusConfig.getOBValue();
		int sampleRate = getOpusSupportedSampleRate(mAudioTrackBaseSampleRate);
		mOpusDecoder = new OpusWrapper(sampleRate, AUDIO_CHANNEL, mOpusConfig.getBitrate(),
			opusApplication, opusComplexity, opusSignalType,
			opusBandwidth, OpusWrapper.OPUS_TYPE_DECODER);
		
		int opusGainValue = mOpusConfig.getGainValue();
		mOpusDecoder.opusSetGainValue(opusGainValue);
		
		initDecodeOpusEmptyData();
		initSilentData();

		// Initialize AudioTrack
		if (mAudioTrack == null) {
			createAudioTrack(mAudioTrackBaseSampleRate, AUDIO_CHANNEL);
		}

		Log.d(LOG_TAG, String.format("initializePlay() AudioProcessor Bitrate=%d, BufferSize=%d, SampleRate=%d",
			mOpusConfig.getBitrate(), mDecAudioBufferSize, mAudioTrackBaseSampleRate));

		return true;
	}

	private void countAvgAmplitude(int amplitude) {
		if (mAmplitudeIndex > 10) {
			mAmplitudeIndex = 0;
		}

		if (mAudioClearInitBuffer) {
			mAudioClearInitBuffer = false;
			//Log.d(LOG_TAG, String.format("Initial AudioBufferSize=%d, RecvQueueSize=%d", mAudioPlayBuffer.size(), mTransport.getRecvQueueSize()));
			//mTransport.resetRecvQueue();
		}
		
		mAmplitudeArray[mAmplitudeIndex] = amplitude;
		mAmplitudeIndex++;

		for (int index = 0; index < mAmplitudeSampleCount; index++) {
			mAmplitudeAverage += mAmplitudeArray[index];
		}
		mAmplitudeAverage = mAmplitudeAverage / mAmplitudeSampleCount;
	}

	private void dropAudioByPitchVAD() throws InterruptedException {
		//Log.i(LOG_TAG, String.format("dropAudioByPitch()"));
		AudioDataRecord audioData = mAudioPlayBuffer.take();
		AudioDataRecord audioData2 = mAudioPlayBuffer.take();

		short[] playData = audioData.getData();
		short[] playData2 = audioData2.getData();
		
		int playDataVad = 0;
		int playDataVad2 = 0;
		if(mAudioConfig.getEnableVAD()){
			//mRecorderBaseSampleRate, audioRawData, audioRawData.length, VADWrapper.VAD_SEND_SIDE
			//long timeBeforeProc = System.currentTimeMillis();
			playDataVad = mVADWrapper.vadProcess(mAudioTrackBaseSampleRate, playData, playData.length, VADWrapper.VAD_RECV_SIZE);
			playDataVad2 = mVADWrapper.vadProcess(mAudioTrackBaseSampleRate, playData2, playData2.length, VADWrapper.VAD_RECV_SIZE);
			
			//long timeAfterProc = System.currentTimeMillis();
			//int procTime = (int) (timeAfterProc - timeBeforeProc);
			//Log.d("VADWrapper", 
			//		String.format("playDataVad=%d, playDataVad2=%d, procTime=%d", playDataVad, playDataVad2, procTime));	
		}
		
		if((audioData.mPitch == 0)
				&& (0 == playDataVad)){
			Log.d(LOG_TAG, String.format("drop audio frame1"));
			writeAudioTrackData(playData2, 0, playData2.length, true, true);
		}
		else if((audioData2.mPitch == 0)
				&& (0 == playDataVad2)){
			Log.e(LOG_TAG, String.format("drop audio frame2"));
			writeAudioTrackData(playData, 0, playData.length, true, true);			
		}
		else{
			writeAudioTrackData(playData, 0, playData.length, true, true);
			writeAudioTrackData(playData2, 0, playData2.length, true, true);
		}
	}
	
	private void writeAudioTrackData(short[] data, int start, int length, boolean bAECBuf, boolean bKeep)
	{
		if(null != mAudioTrack){			
			if(bAECBuf){
				if((null != mAecmWrapper) && mAudioConfig.getAECMConfig()){
					mAecmWrapper.aecmBufferFarend(data, length);
				}
				if((null != mAGCWrapper) && mAudioConfig.getEnableAGC() && mAudioConfig.getAGCAddFarEnd()){
					mAGCWrapper.agcAddFarend(data, length);
				}
			}
			
			mRemainPlaybackFrames = mRemainPlaybackFrames + (length/2);
			//Log.i(LOG_TAG, 
			//		String.format("CurPlaybackHeadPos:%d, RemainPlaybackFrames:%d", mCurPlaybackHeadPos, mRemainPlaybackFrames/2));
			if(mAudioConfig.getCountAmplitudeDiff() && (null != mFrameAmplitudeDiffCounter)){
				short ch1LastValue = data[length-2];
				short ch2LastValue = data[length-1];
				mFrameAmplitudeDiffCounter.addFrameLastAmplitude( ch1LastValue, ch2LastValue );
			}
			
			if(mHasGenNonVoiceData && bKeep){
				short lastValueCh1 = data[0];
				short lastValueCh2 = data[1];
				mHasGenNonVoiceData = false;
				genSinTone(lastValueCh1, lastValueCh2, 10, false, true, true);
			}
			
			mAudioTrack.write(data, start, length);
			if(bKeep){
				mLastPlayedData = data;
			}
			
			if((null != mAudioWaveSaver) && mAudioConfig.getEnableWaveSaver()){
				mAudioWaveSaver.addAudioData(data);
			}
			
			if(mAudioConfig.getEnableAudioTrackDelay()){
				int sleepTime = mAudioConfig.getAudioTrackDelayTime();
				if(sleepTime > 0){
					try {
						Log.i(LOG_TAG, String.format("AudioTrack Sleep Time=%d", sleepTime));
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void genSilentData(int timeMs){
		int dataLength = mDecAudioBufferSize * timeMs / mAudioConfig.getAudioFrameSize();
		short[] silentData = new short[dataLength];
		for (int i = 0; i < dataLength - 1; i += 2) {
			silentData[i] = 0;
			silentData[i + 1] = 0;
		}
		writeAudioTrackData(silentData, 0, silentData.length, false, false);
	}
	
	private void genSilentData(){
		AudioDataRecord audioData = new AudioDataRecord(mSilentData.clone(), 0);
		audioData.setPitch(0);
		if ((mAudioPlayBuffer.size() < 1000) && (audioData != null)) {
			mAudioPlayBuffer.add(audioData);
		}
	}
	
	private void genOpusSilentData(boolean bPacketLost){
		int result = mOpusDecoder.opusDecode(mDecodeOpusEmptyData, mDecodeOpusEmptyData.length, mDecodeOpusEmptyBuffer, mDecodeOpusEmptyBuffer.length, bPacketLost);
		if (result <= 0) {
			Log.e(LOG_TAG, 
					String.format("mOpusDecoder decode generate data. error code: %d, bufferSize=%d, mDecAudioBufferSize=%d, bPacketLost=%b", 
							result, mDecodeOpusEmptyBuffer.length, mDecAudioBufferSize, bPacketLost));
		}
		else{
			mLastPitch = mOpusDecoder.opusGetPitchValue();
			AudioDataRecord audioData = new AudioDataRecord(mDecodeOpusEmptyBuffer.clone(), 0);
			audioData.setPitch(mLastPitch);
			if ((mAudioPlayBuffer.size() < 1000) && (audioData != null)) {
				mAudioPlayBuffer.add(audioData);
			}
			Log.i(LOG_TAG, 
					String.format("mOpusDecoder decode generate data. success: %d, mDecAudioBufferSize=%d, bPacketLost=%b", 
							result, mDecAudioBufferSize, bPacketLost));
		}
	}
	
	private void genFadeInOut(short baseValueCh1, short baseValueCh2, int genTimeMs, boolean bFadeOut, boolean bFadeIn){
		
		int numSamples = genTimeMs * mRecorderBaseSampleRate * 2 / 1000;
		int numSamplesFade = 3 * mRecorderBaseSampleRate * 2/ 1000; //FadeIn or FadeOut in 5 ms
		
		int bias1 = baseValueCh1 / numSamplesFade;
		int bias2 = baseValueCh2 / numSamplesFade;
		
		if(null == mGenFadeData){
			mGenFadeData = new short[numSamples];
		}
		
		if(bFadeOut){
			int icount = 1;
			for(int index=0; index < (numSamples/2); index++){
				if(icount < numSamplesFade){
					mGenFadeData[index*2] = (short) (baseValueCh1 - (bias1*icount));
					mGenFadeData[index*2+1] = (short) (baseValueCh2 - (bias2*icount));
					icount++;
				}
				else{
					mGenFadeData[index*2] = 0;
					mGenFadeData[index*2+1] = 0;
				}
			}
		}
		else if(bFadeIn){
			int icount = 1;
			for(int index = ((numSamples/2)-1); index >= 0; index--){
				if(icount < numSamplesFade){
					mGenFadeData[index*2] = (short) (baseValueCh1 - (bias1*icount));
					mGenFadeData[index*2+1] = (short) (baseValueCh2 - (bias2*icount));
					icount++;
				}
				else{
					mGenFadeData[index*2] = 0;
					mGenFadeData[index*2+1] = 0;
				}
			}	
		}
		
		AudioDataRecord audioData = new AudioDataRecord(mGenFadeData.clone(), 0);
		if ((mAudioPlayBuffer.size() < 1000) && (audioData != null)) {
			mAudioPlayBuffer.add(audioData);
		}
	}
	
	private void genSinTone(short baseValueCh1, short baseValueCh2, int genTimeMs, 
			boolean bFadeOut, boolean bFadeIn, boolean bWriteAudioTrack){
		int interval = mRecorderBaseSampleRate/mAudioConfig.getSinTone();
		//int startInterval = (lastValue*interval)/32767 + interval/4;
		int startInterval1 = interval/4 + 1;
		if(baseValueCh1 < 0){
			startInterval1 = (interval*5)/4 + 1;
		}
		
		int startInterval2 = interval/4 + 1;
		if(baseValueCh2 < 0){
			startInterval2 = (interval*5)/4 + 1;
		}
		
		int numSamples = genTimeMs * mRecorderBaseSampleRate * 2 / 1000;
		double sampleCh1[] = new double[numSamples/2];
		double sampleCh2[] = new double[numSamples/2];
		if(bFadeOut){
			//Generate sin tone with fade out
			double fadeout = 1;
			for (int index = 0; index < (numSamples/2); index++) {
	            sampleCh1[index] = Math.sin(2 * Math.PI * (index+startInterval1) / interval) * fadeout;
	            sampleCh2[index] = Math.sin(2 * Math.PI * (index+startInterval2) / interval) * fadeout;
	            if(0 == ((index+1) % 3)){
	            	if(fadeout >= 0.05){
	            		fadeout -= 0.05;
	            	}
	            	else{
	            		fadeout = 0.0;
	            	}
	            }
	        }
		}
		else if(bFadeIn){
			//Generate sin tone with fade in
			double fadein = 1;
			for (int index = ((numSamples/2)-1); index >= 0 ; index--) {
	            sampleCh1[index] = Math.sin(2 * Math.PI * (index+startInterval1) / interval) * fadein;
	            sampleCh2[index] = Math.sin(2 * Math.PI * (index+startInterval2) / interval) * fadein;
	            if(0 == ((index+1) % 3)){
	            	if(fadein >= 0.05){
	            		fadein -= 0.05;
	            	}
	            	else{
	            		fadein = 0.0;
	            	}
	            }
	        }
		}
		else{
			//Generate normal sin tone
			for (int i = 0; i < (numSamples/2); ++i) {
	            sampleCh1[i] = Math.sin(2 * Math.PI * (i+startInterval1) / interval);
	            sampleCh2[i] = Math.sin(2 * Math.PI * (i+startInterval2) / interval);
	        }
		}
		
		mGeneratedSinData = new short[numSamples];
		
		double dValCh1 = 0.0;
		double dValCh2 = 0.0;
		short valCh1 = 0;
		short valCh2 = 0;
		for(int index=0; index < numSamples; index+=2){
        	dValCh1 = sampleCh1[index/2];
        	dValCh2 = sampleCh2[index/2];
            // scale to maximum amplitude
            valCh1 = (short) ((dValCh1 * baseValueCh1));
            valCh2 = (short) ((dValCh2 * baseValueCh2));

            mGeneratedSinData[index] = valCh1;
            mGeneratedSinData[index+1] = valCh2;
        }

		if(!bWriteAudioTrack){
			AudioDataRecord audioData = new AudioDataRecord(mGeneratedSinData.clone(), 0);
			if ((mAudioPlayBuffer.size() < 1000) && (audioData != null)) {
				mAudioPlayBuffer.add(audioData);
			}
		}
		else{
			writeAudioTrackData(mGeneratedSinData.clone(), 0, mGeneratedSinData.length, false, false);
//			Log.i(LOG_TAG, 
//				String.format("Write Generated Sin Data into AudioTrack. FadeOut=%b, FadeIn=%b", bFadeOut, bFadeIn));
		}
	}
	
	private void audioNoVoiceControl(boolean bStart){
		if(bStart){
			mCurPlaybackHeadPos = mAudioTrack.getPlaybackHeadPosition();
			int remainFrames = mRemainPlaybackFrames - mCurPlaybackHeadPos;
			//Log.d(LOG_TAG, 
			//		String.format("mCurPlaybackHeadPos=%d, RemainPlaybackFrames=%d, remainFrames=%d", 
			//				mCurPlaybackHeadPos, mRemainPlaybackFrames, remainFrames));
			
			int expectBufferSize = mDecAudioBufferSize;
			if((remainFrames < expectBufferSize) 
				&& (null != mLastPlayedData)){
				short lastValueCh1 = mLastPlayedData[mLastPlayedData.length-2];
				short lastValueCh2 = mLastPlayedData[mLastPlayedData.length-1];
				genSinTone(lastValueCh1, lastValueCh2, 10, true, false, true);
				mHasGenNonVoiceData = true;
			}
		}
	}
	
	private void startAudioPlay() {
		Log.d(LOG_TAG, "startAudioPlay");

		if (initializePlayer()) {
			Log.d(LOG_TAG, "init audiotrack and decoder, ready to play");
			mAudioTrack.play();
		}

		mAudioTrackPlay = new Thread() {
			@Override
			public void run() {
				while (mIsRunning) {
					if (null != mAudioTrack) {
						try {
							if (mAdjustingSampleRate.get()) {
								mWaitAdjust.set(true);
								while (mAdjustingSampleRate.get()) {
									//Log.d(LOG_TAG, "AudioProcessor wait for mAdjustingSampleRate");
									Thread.sleep(5);
								}
							}
							AudioDataRecord checkData = mAudioPlayBuffer.peek();
							if (null == checkData) {
								if(mAudioConfig.getEnableGenData() && !mHasGenNonVoiceData){
									audioNoVoiceControl(true);
									mSilentCount = 0;
								}
								else if(mHasGenNonVoiceData){
//									mSilentCount++;
//									if(0 == (mSilentCount % 8)){
//										genSilentData();
//									}
									mSilentCount++;
									if(0 == (mSilentCount % 4)){
										genSilentData(20);
									}
								}
								Thread.sleep(5);
								continue;
							}

							boolean bIsVoice = (checkData.mPitch > 0 ) ? true : false;
//							int curQueueSize = mAudioPlayBuffer.size() + mTransport.getRecvQueueSize();
							int curQueueSize = mAudioPlayBuffer.size();
							//Log.i(LOG_TAG, String.format("IsVoice Data=%b, curQueueSize=%d", bIsVoice, curQueueSize));

							if (curQueueSize > mAudioConfig.getMinimumVoicePlayQueueSize()) {
								mWaitVoiceBuffer = false;
								if (mAudioConfig.getDropNonVoiceData()) {
									dropAudioByPitchVAD();
								}
								else {
									AudioDataRecord audioData = mAudioPlayBuffer.take();
									short[] playData = audioData.getData();
									if ((null != playData) && (null != mAudioTrack)) {
										writeAudioTrackData(playData, 0, playData.length, true, true);
									}
								}
							}
							else if ((!bIsVoice) || (!mAudioConfig.getEnableQueueVoice())) {
								AudioDataRecord audioData = mAudioPlayBuffer.take();
								short[] playData = audioData.getData();
								countAvgAmplitude(audioData.getAmplitude());
								if ((null != playData) && (null != mAudioTrack)) {
									writeAudioTrackData(playData, 0, playData.length, true, true);
								}

								//Log.i(LOG_TAG, String.format("Play Audio Data Directly."));
								//Log.d(LOG_TAG, String.format("CurrentPlayBackRate:%d, AUDIOTRACK_CUR_SAMPLERATE=%d, AUDIOTRACK_BASE_SAMPLERATE=%d",
								//		AUDIOTRACK_CUR_SAMPLERATE, AUDIOTRACK_BASE_SAMPLERATE, mAudioTrack.getPlaybackRate()));
								if(curQueueSize <= 1){
									mWaitVoiceBuffer = true;
								}
							}
							else if(!mWaitVoiceBuffer){
								AudioDataRecord audioData = mAudioPlayBuffer.take();
								short[] playData = audioData.getData();
								if ((null != playData) && (null != mAudioTrack)) {
									//mAudioTrack.write(playData, 0, playData.length);
									writeAudioTrackData(playData, 0, playData.length, true, true);
								}
								if(curQueueSize <= 1){
									mWaitVoiceBuffer = true;
								}
							}
							else {
								//Log.d(LOG_TAG, String.format("Wait for audio data in. RecvQueueSize=%d, AudioPlayBuffer=%d", 
								//		mTransport.getRecvQueueSize(), mAudioPlayBuffer.size()));
								if(mAudioConfig.getEnableGenData() && !mHasGenNonVoiceData){
									audioNoVoiceControl(true);
								}
							}
							adjustDynamicPlayRate();
							//adjustEqualizer();	//Note: Default disable Equalizer
						}
						catch (InterruptedException e) {
							//Log.e(LOG_TAG, e.printStackTrace());
							e.printStackTrace();
							break;
						}
					}
				}
			}
		};
		mAudioTrackPlay.start();
	}
	
	public void adjustEqualizer(){
		if(null == mEqualizer){
			return;
		}
		
		if(mAudioConfig.getEnableEqualizer() && !mIsEqualizerEnabled){
			Log.i(LOG_TAG, String.format("Enable Equalizer"));
			mEqualizer.setEnabled(true);
			mIsEqualizerEnabled = true;
		}
		else if(!mAudioConfig.getEnableEqualizer() && mIsEqualizerEnabled){
			Log.i(LOG_TAG, String.format("Disable Equalizer"));
			mEqualizer.setEnabled(false);
			mIsEqualizerEnabled = false;
		}
	}

	private void adjustDynamicPlayRate(){
		//android.util.Log.d(LOG_TAG, String.format("current play rate: %d", mAudioTrack.getPlaybackRate()));
		if (mAudioConfig.getDynPlaybackRateConfig()) {
			//adjustPlaybackRateDynamic();
			if(mAudioConfig.getIsFixedDynamicRate()){
				adjustPlaybackRateFix();
			}
			else{
				adjustPlaybackRateDynamic();
			}
		}
		else {
			if (mAudioTrackCurSampleRate != mAudioTrackBaseSampleRate) {
				mAudioTrackCurSampleRate = mAudioTrackBaseSampleRate;
				if (null != mAudioTrack)
					mAudioTrack.setPlaybackRate(mAudioTrackCurSampleRate);
			}
		}
	}
	
	// In pull-push mode, call stopPullPlay to end play process
	public void stopAudioPlayer() {
		Log.d(LOG_TAG, "stopAudioPlay");

		if (null != mAudioTrack) {
			mAudioTrack.stop();
			mAudioTrack.release();
			mAudioTrack = null;
		}
		stopAudioDecoder();
		mAudioTrackPlay.interrupt();
		mAudioTrackPlay = null;
	}

	@SuppressLint("NewApi")
	private void stopAudioDecoder() {
		Log.d(LOG_TAG, "Audio decoder stopped!");

		if (null != mOpusDecoder) {
			mOpusDecoder.opusDestroy(OpusWrapper.OPUS_TYPE_DECODER);
			mOpusDecoder = null;
		}
	}

	private void decodeSingleFrameByDrop(int delayMode, byte[] singleData) {
		//Every number of mAudioConfig.getAudioDropRate(), drop one audio frame.
		if(1 == mAudioFrameDropCount.get()){
			//Duplicate audio data if needed
			decodeSingleFrame(singleData, false, 0);
		}
		else if(2 == mAudioFrameDropCount.get()){
			Log.i(LOG_TAG, String.format("Drop AudioFrame. Every %d AudioFrames drop 1.", mAudioConfig.getAudioDropRate()));
		}
		else{
			decodeSingleFrame(singleData, false, 0);
		}
	}

	// decodePlay is used to decode income encoded voice data. It can be MediaCodec AAC or CELT
	private void decodePlay(byte[] data, int delayMode, boolean packetLost, int lostCount) {
		if(null != data){
			if (data.length > 0) {
				switch (delayMode)
				{
					case AUDIO_DELAY_DROPPED:
						if (mAudioConfig.getDropModeConfig()) {
							decodeSingleFrameByDrop(delayMode, data);
						}
						else if(mAudioConfig.getEnableTestMode() 
							&& mAudioConfig.getDisorderPlay()
							&& (mAudioFrameDropCount.get() == mAudioConfig.getAudioDropRate())){
							mDisorderFrame = data;
						}
						else {
							decodeSingleFrame(data, bPacketLost, lostCount);
						}
						break;
					default:
						if(mAudioConfig.getEnableTestMode() && mAudioConfig.getDropModeConfig()){
							decodeSingleFrameByDrop(AUDIO_DELAY_DROPPED, data);
						}
						else if(mAudioConfig.getEnableTestMode() 
							&& mAudioConfig.getDisorderPlay()
							&& (mAudioFrameDropCount.get() == mAudioConfig.getAudioDropRate())){
							mDisorderFrame = data;
						}
						else{
							decodeSingleFrame(data, bPacketLost, lostCount);
						}
						bPacketLost = false;
						break;
				}
				if(mAudioConfig.getEnableTestMode()){
					audioTestDropControl();
				}

				if(mAudioFrameDropCount.get() >= mAudioConfig.getAudioDropRate()){
					mAudioFrameDropCount.set(1);
				}
				else{
					mAudioFrameDropCount.addAndGet(1);
				}
			}
		}
	}
	
	//Audio Test Control for adding audio data by drop rate control.
	private void audioTestDropControl(){
		if((1 == mAudioFrameDropCount.get()) && 
			(mAudioConfig.getDuplicateAudioData() 
					|| mAudioConfig.getGenPairData()
					|| mAudioConfig.getOpusSilentData()
					|| mAudioConfig.getGenSinData()
					|| mAudioConfig.getGenFadeInOut()
					|| mAudioConfig.getWriteSilentData()
					|| mAudioConfig.getDisorderPlay())){
			if(mAudioConfig.getDuplicateAudioData()){
				Log.i(LOG_TAG, String.format("Add duplicate audio frame data. EX: 11234."));
				addAudioDataRecord(mLastPitch);
			}
			else if(mAudioConfig.getGenPairData()){
				Log.i(LOG_TAG, String.format("Add duplicate audio frame data. EX: 1-11234."));
				int length = mDecodeOpusBuffer.length;
				for(int index=0; index < length; index++){
					mGenOpositeData[index] = mDecodeOpusBuffer[length - index - 1];
				}
				
				AudioDataRecord audioData = null;
				audioData = new AudioDataRecord(mGenOpositeData.clone(), 0);
				audioData.setPitch(mLastPitch);
				if ((mAudioPlayBuffer.size() < AUDIO_PLAY_QUEUE_SIZE) && (audioData != null)) {
					mAudioPlayBuffer.add(audioData);
				}
				addAudioDataRecord(mLastPitch);
			}
			else if(mAudioConfig.getOpusSilentData()){
				genOpusSilentData(false);
			}
			else if(mAudioConfig.getGenSinData()){
				short lastValueCh1 = mDecodeOpusBuffer[mDecAudioBufferSize-2];
				short lastValueCh2 = mDecodeOpusBuffer[mDecAudioBufferSize-1];
				genSinTone(lastValueCh1, lastValueCh2, 20, true, false, false);
			}
			else if(mAudioConfig.getGenFadeInOut()){
				short lastValueCh1 = mDecodeOpusBuffer[mDecAudioBufferSize-2];
				short lastValueCh2 = mDecodeOpusBuffer[mDecAudioBufferSize-1];
				genFadeInOut(lastValueCh1, lastValueCh2, 20, true, false);
			}
			else if(mAudioConfig.getWriteSilentData()){
				genSilentData();
			}
			else if(mAudioConfig.getDisorderPlay() && (null != mDisorderFrame)){
				decodeSingleFrame(mDisorderFrame, bPacketLost, 0);
				//Log.i(LOG_TAG, String.format("Decode mDisorderFrame"));
			}
		}
		
	}

	private void addAudioDataRecord(int pitch) {
		//boolean bWait = false;
		int positive = 0;
		int value = 0;

		for (int index = 0; index < mDecAudioBufferSize; index += 4) {
			value = mDecodeOpusBuffer[index];
			if (value > 0) {
				positive += value;
			}
			else {
				positive = positive - value;
			}
			value = mDecodeOpusBuffer[index + 2];
			if (value > 0) {
				positive += value;
			}
			else {
				positive = positive - value;
			}
		}
		int sample_count = mDecAudioBufferSize / 2;
		int amplitude = positive / sample_count;

		AudioDataRecord audioData = null;
		audioData = new AudioDataRecord(mDecodeOpusBuffer.clone(), amplitude);
		audioData.setPitch(pitch);

		if ((mAudioPlayBuffer.size() < AUDIO_PLAY_QUEUE_SIZE) && (audioData != null)) {
			if((3 == mAudioFrameDropCount.get()) && mAudioConfig.getGenFadeInOut()){
				short lastValueCh1 = mDecodeOpusBuffer[0];
				short lastValueCh2 = mDecodeOpusBuffer[1];
				genFadeInOut(lastValueCh1, lastValueCh2, 20, false, true);
			}
			else if((3 == mAudioFrameDropCount.get()) && mAudioConfig.getGenSinData()){
				short lastValueCh1 = mDecodeOpusBuffer[0];
				short lastValueCh2 = mDecodeOpusBuffer[1];
				genSinTone(lastValueCh1, lastValueCh2, 20, false, true, false);
			}
			mAudioPlayBuffer.add(audioData);
		}
	}
	
	private void adjustPlaybackRateFix(){
		int adjustRate = mAudioTrackBaseSampleRate * mAudioConfig.getPlaybackSpeedupRatio()/100;
		int speedLevel = mAudioConfig.getDynSpeedUpLevel();

		int adjustSampleRate = speedLevel * adjustRate;
		int newSampleRate = mAudioTrackBaseSampleRate + adjustSampleRate;
		if (mAudioTrackCurSampleRate != newSampleRate) {
			mAudioTrackCurSampleRate = newSampleRate;
			Log.i(LOG_TAG, "Reset audio track sample rate:" + mAudioTrackCurSampleRate);
			if (null != mAudioTrack) {
				mAudioTrack.setPlaybackRate(mAudioTrackCurSampleRate);
			}
		}
	}

	private void adjustPlaybackRateDynamic() {
		int queueSize = mAudioPlayBuffer.size();
		if (queueSize < ADJUST_AUDIO_RATE_THRESHOLD) {
			if (mAudioConfig.getDynPlaybackRateConfig()) {
				int adjustRate = mAudioTrackBaseSampleRate * mAudioConfig.getPlaybackSpeedupRatio() / 100;
				if (mAudioTrackCurSampleRate != (mAudioTrackBaseSampleRate + adjustRate)) {
					if (queueSize < 5) {
						mAudioTrackCurSampleRate = (mAudioTrackBaseSampleRate + adjustRate);
						Log.i(LOG_TAG, "Reset audio track sample rate:" + mAudioTrackCurSampleRate);
						if (null != mAudioTrack)
							mAudioTrack.setPlaybackRate(mAudioTrackCurSampleRate);
					}
				}
			}
			else {
				if (mAudioTrackCurSampleRate != mAudioTrackBaseSampleRate) {
					if (queueSize < 5) {
						mAudioTrackCurSampleRate = mAudioTrackBaseSampleRate;
						Log.i(LOG_TAG, "Reset audio track sample rate:" + mAudioTrackCurSampleRate);
						if (null != mAudioTrack)
							mAudioTrack.setPlaybackRate(mAudioTrackCurSampleRate);
					}
				}
			}
		}
		else {
			//Adjust sample rate to higher
			int adjustRate = mAudioTrackBaseSampleRate * mAudioConfig.getPlaybackSpeedupRatio() / 100;
			int speedLevel = (queueSize - ADJUST_AUDIO_RATE_THRESHOLD) / 10 + 2;
			if (speedLevel > 10)
				speedLevel = 10;
			//if(speedLevel > 10) speedLevel = 10;
			//if(speedLevel > 5) adjustRate = adjustRate*2;

			int adjustSampleRate = speedLevel * adjustRate;
			int nextLevelSampleRate = (speedLevel + 1) * adjustRate;
			int newSampleRate = mAudioTrackBaseSampleRate + adjustSampleRate;
			int nextSampleRate = mAudioTrackBaseSampleRate + nextLevelSampleRate;
			if (mAudioTrackCurSampleRate != newSampleRate) {
				if (mAudioTrackCurSampleRate != nextSampleRate) {
					mAudioTrackCurSampleRate = newSampleRate;
					Log.i(LOG_TAG, "Reset audio track sample rate:" + mAudioTrackCurSampleRate);
					if (null != mAudioTrack) {
						mAudioTrack.setPlaybackRate(mAudioTrackCurSampleRate);
					}
				}
			}
		}
	}

	private void packetLostControl(byte[] data, int lostCount){
//		int curPacektLostRate = mTransport.getPacketLostRate();
		int curPacketLostRate = 0;
		int opusLostPerc = mOpusConfig.getPacketLostPerc();
		if(opusLostPerc < curPacketLostRate){
			mOpusConfig.setPacketLostPerc(curPacketLostRate);
			mOpusEncoder.opusPacketLostPerc(curPacketLostRate);
		}
		
		if(mOpusConfig.getInbandFEC()){
			if(lostCount > 1){
				addOpusPLCData();
			}
			addOpusFECData(data);
		}
		else if(mOpusConfig.getEnablePLC()){
			addOpusPLCData();
		}
		else if(mOpusConfig.getAddSilent()){
			genSilentData();
		}
	}
	
	private void addOpusFECData(byte[] data){
		int result = mOpusDecoder.opusDecode(data, data.length, mDecodeOpusBuffer, mDecAudioBufferSize, true);
		mLastPitch = mOpusDecoder.opusGetPitchValue();
		if(result > 0){
			Log.i(LOG_TAG, 
				String.format("mOpusDecoder generate opus data (inband FEC) with packetLost."));
			addAudioDataRecord(mLastPitch);
		}
	}
	
	private void addOpusPLCData(){
		int result = mOpusDecoder.opusDecode(null, 0, mDecodeOpusBuffer, mDecAudioBufferSize, false);
		mLastPitch = mOpusDecoder.opusGetPitchValue();
		if(result > 0){
			Log.i(LOG_TAG, 
				String.format("mOpusDecoder generate opus data (PLC) with packetLost."));
			addAudioDataRecord(mLastPitch);
		}
	}
	
	
	@SuppressLint({ "InlinedApi", "NewApi" })
	private byte[] decodeSingleFrame(byte[] data, boolean bPacketLost, int lostCount) {

		if (null != mOpusDecoder) {
//			if(bPacketLost && mOpusConfig.getInbandFEC()){
//				genOpusSilentData(bPacketLost);
//			}
			
			int result = -1;
			if((1 == data.length) && (mVADOpusEmptyData == data[0])){
				result = 1;
				short lastValue = mDecodeOpusBuffer[mDecAudioBufferSize-1];
				for(int index=0; index < mDecAudioBufferSize-1; index+=2){
					mDecodeOpusBuffer[index] = lastValue;
					mDecodeOpusBuffer[index+1] = lastValue;
				}
				Log.i(LOG_TAG, String.format("mOpusDecoder generate silent data from VAD value: %d", data[0]));
				mLastPitch = 0;
			}
			else{
				if(bPacketLost){
					packetLostControl(data, lostCount);
				}
				
				result = mOpusDecoder.opusDecode(data, data.length, mDecodeOpusBuffer, mDecAudioBufferSize, false);
				mLastPitch = mOpusDecoder.opusGetPitchValue();
				if(0 < mLastPitch){
					Log.i("OpusWrapper", String.format("OPUS_GET_PITCH: Voice (%d)", mLastPitch));
				}
				else{
					Log.i("OpusWrapper", String.format("OPUS_GET_PITCH: %d", mLastPitch));
				}
			}
			if (result <= 0) {
				Log.e(LOG_TAG, "mOpusDecoder decode error, error code:" + result);
				return null;
			}
//			Log.i("OpusDecode", 
//					String.format("decoded data %d is ready. mDecAudioBufferSize=%d, original data lenght=%d, pitch=%d", result, 
//							mDecAudioBufferSize, data.length, mLastPitch));
//			if(data.length == 1){
//				android.util.Log.d("OpusDecode", String.format("decoded data: %3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d, %3d",
//						mDecodeOpusBuffer[0], mDecodeOpusBuffer[1], mDecodeOpusBuffer[2], mDecodeOpusBuffer[3], mDecodeOpusBuffer[4], 
//						mDecodeOpusBuffer[5], mDecodeOpusBuffer[6], mDecodeOpusBuffer[7], mDecodeOpusBuffer[8], mDecodeOpusBuffer[9]));
//			}
			
			addAudioDataRecord(mLastPitch);
		}
		else {
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			Log.d(LOG_TAG, "mInitAudioTrack.set(true)");

			//initial audio track, it's may take 100~200 ms
			if (initializePlayer()) {
				Log.d(LOG_TAG, "init audiotrack and Audio decode opus data and play");
				if (null != mAudioTrack) {
					mAudioTrack.play();
					//mAudioTrack.write(mDecodeOpusBuffer, 0, mDecAudioBufferSize);
				}
			}
			Log.d(LOG_TAG, "mInitAudioTrack.set(false)");
		}

		return null;
	}

	private int mRecordBufferSize = 0;
	private int mPlayBufferSize = 0;

	// Initialize record environment. It is used when environment is created
	private boolean initializeRecord() {

		int opusComplexity = mOpusConfig.getOCValue();
		int opusSignalType = mOpusConfig.getOSTValue();
		int opusApplication = mOpusConfig.getOAValue();
		int opusBandwidth = mOpusConfig.getOBValue();
		int sampleRate = getOpusSupportedSampleRate(mRecorderBaseSampleRate);
		mOpusEncoder = new OpusWrapper(sampleRate, AUDIO_CHANNEL, mOpusConfig.getBitrate(),
			opusApplication, opusComplexity, opusSignalType,
			opusBandwidth, OpusWrapper.OPUS_TYPE_ENCODER);
		mOpusEncoder.opusSetDTX(mOpusConfig.getDTXConfig());
		mOpusEncoder.opusInBandFEC(mOpusConfig.getInbandFEC());
		mOpusConfig.setPacketLostPerc(2);	//init opus pacekt lost perc as 2.
		mOpusEncoder.opusPacketLostPerc(mOpusConfig.getPacketLostPerc());
		mEncodeOpusBuffer = new byte[mEncAudioBufferSize / 2];

		if (mAudioRecorder == null) {
			//int sampleRate = 0;
			createAudioRecord(mRecorderCurSampleRate, AUDIO_CHANNEL);
		}

		Log.d(LOG_TAG, String.format("initializeRecord() AudioProcessor Bitrate=%d, BufferSize=%d.",
				mOpusConfig.getBitrate(), mEncAudioBufferSize));

		return true;
	}

	// Before start recording, this method is called to start recording, which mean AudioRecord will open and smaple comes in
	private void prepareRecord() {
		mAudioRecorder.startRecording();

		if(null != mAudioPacketSendThread){
			mAudioPacketSendThread.interrupt();
			mAudioPacketSendThread = null;
		}
		
		mAudioPacketSendThread = new Thread(){
			@Override
			public void run(){
				while(mIsRunning){
					GetAudioData('A');
				}
			}
		};
		mAudioPacketSendThread.start();
		
	}

	// To start pull-push mode for Tx side, call this method to start recording. Then use AVPullTxInterface to pull data
	public void startAudioRecorder() {

		Log.d(LOG_TAG,
			String.format("startPullRecord COUNT_COMPOSED_FRAMES=%d, mAudioBufferSize=%d",
				mAudioConfig.getComposedFrameCount(), mEncAudioBufferSize));

		mEncAmplitudeAdjustTimes = -1;
		//mEncAmplitudeIndex = 0;
		//mEncAmplitudeAverage = 0;
		if (!initializeRecord())
			return;
		//Log.d(LOG_TAG, "init RecordedData with size: " + mAudioBufferSize);
		mRecordedDataBuffer = new short[mEncAudioBufferSize];
		mRecordedDataMono = new short[mEncAudioBufferSize/2];
		mAecmOutputData = new short[mEncAudioBufferSize];
		mNSOutputData = new short[mEncAudioBufferSize];
		mAGCOutputData = new short[mEncAudioBufferSize];
		prepareRecord();

	}

	public void stopAudioRecorder() {
		synchronized (this) {
			mIsRunning = false;
			if (mAudioRecorder != null) {
				mAudioRecorder.stop();
				mAudioRecorder.release();
				mAudioRecorder = null;
			}
			stopAudioEncoder();
		}
	}

	@SuppressLint("NewApi")
	private void stopAudioEncoder() {

		if (null != mOpusEncoder) {
			mOpusEncoder.opusDestroy(OpusWrapper.OPUS_TYPE_ENCODER);
			mOpusEncoder = null;
		}
	}

	public boolean createAudioTrack(int audioSampleRate, int audioChannels) {
		int chcfg = (audioChannels >= 2) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
		int audiofmt = AudioFormat.ENCODING_PCM_16BIT;//(audioChannels >= 2) ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
		mPlayBufferSize = AudioTrack.getMinBufferSize(audioSampleRate, chcfg, audiofmt);
		if (mPlayBufferSize <= 0) {
			Log.e(LOG_TAG, String.format("createAudioTrack device does not support sample rate 48000 reset to 44100"));
			audioSampleRate = 44100;
			mPlayBufferSize = AudioTrack.getMinBufferSize(audioSampleRate, chcfg, audiofmt);
		}
		if ((mPlayBufferSize == AudioTrack.ERROR_BAD_VALUE) || (mPlayBufferSize == AudioTrack.ERROR))
			return false;
		
		//mAecMaxSoundBufTimeMs = (mPlayBufferSize*1000)/audioSampleRate + 120;	//Add 120 for system sound buffer
		//Log.i(LOG_TAG, String.format("iMinBufSize=%d, mAecMaxSoundBufTimeMs=%d", mPlayBufferSize, mAecMaxSoundBufTimeMs));
		Log.i(LOG_TAG, String.format("iMinBufSize=%d", mPlayBufferSize));
		try {
			String osBrand = android.os.Build.BRAND;
			//String osModel = android.os.Build.MODEL;
			if (osBrand.equalsIgnoreCase("Xiaomi")) {
				mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, chcfg,
					audiofmt, mPlayBufferSize, AudioTrack.MODE_STREAM);
			}
			else {
				mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, audioSampleRate, chcfg,
					audiofmt, mPlayBufferSize, AudioTrack.MODE_STREAM);
			}

			float volumeMax = AudioTrack.getMaxVolume();
			float volumeMin = AudioTrack.getMinVolume();
			Log.d("AudioTrack", String.format("volumeMax=%f, volumeMin=%f", volumeMax, volumeMin));

		}
		catch (IllegalArgumentException e) {
			Log.i(LOG_TAG, String.format("AudioTrack initial parameters error:", e.getMessage()));
			return false;
		}
		
		if ((android.os.Build.VERSION.SDK_INT >= 9)
			&& mAudioConfig.getEnableEqualizer()) {
			enableEqualizer();
		}
		
		return true;
	}
	
	@SuppressLint("NewApi")
	public void enableNoiseSuppressor() {
		if (!NoiseSuppressor.isAvailable()) {
			Log.e("AudioProcessor", "NoiseSuppressor is not supported in this device!");
		}
		else {
			NoiseSuppressor noiseSuppressor = NoiseSuppressor.create(mAudioRecorder.getAudioSessionId());
			if (!noiseSuppressor.getEnabled()) {
				noiseSuppressor.setEnabled(true);
				Log.i(LOG_TAG, "NoiseSuppressor is enabled!");
			}
			else {
				Log.i(LOG_TAG, "NoiseSupporessor is already enabled!");
			}
		}
	}

	@SuppressLint("NewApi")
	public void enableAutomaticGainControl() {
		if (!AutomaticGainControl.isAvailable()) {
			Log.e(LOG_TAG, "AutomaticGainControl is not supported in this device!");
		}
		else {
			AutomaticGainControl autoGainControl = AutomaticGainControl.create(mAudioRecorder.getAudioSessionId());
			if (!autoGainControl.getEnabled()) {
				autoGainControl.setEnabled(true);
				Log.i(LOG_TAG, "AutomaticGainControl is enabled!");
			}
			else {
				Log.i(LOG_TAG, "AutomaticGainControl is already enabled!");
			}
		}
	}

	@SuppressLint("NewApi")
	public void enableAcousticEchoCanceler() {
		if (!AcousticEchoCanceler.isAvailable()) {
			Log.e("AudioProcessor", "Acoustic Echo Canceler is not supported in this device!");
		}
		else {
			AcousticEchoCanceler echoCanceler = AcousticEchoCanceler.create(mAudioRecorder.getAudioSessionId());
			echoCanceler.setEnabled(true);
			if (echoCanceler.getEnabled()) {
				Log.i("AudioProcessor", "Acoustic Echo Canceler is enabled!");
			}
		}
	}
	
	public void enableEqualizer(){
		try{
			mEqualizer = new Equalizer(0, mAudioTrack.getAudioSessionId());
			
			short numBands = mEqualizer.getNumberOfBands();
			short[] levelRange = mEqualizer.getBandLevelRange();
			//short maxGainLevel = levelRange[1];
			//short minGainLevel = levelRange[0];
			Log.i("Equalizer", String.format("numBands=%d, levelRange=%d", numBands, levelRange.length));
			Log.i("Equalizer", String.format("lower limit=%d, upper limit=%d", levelRange[0], levelRange[1]));
			//int targetFrequency = mAudioConfig.getEquTargetMilliFreq();
			short bandLevel = 0;
			for(int index=0; index<numBands; index++){
				int[] freqRange = mEqualizer.getBandFreqRange((short) index);
				bandLevel = mEqualizer.getBandLevel((short) index);
				Log.i("Equalizer", String.format("Band:%d, lower frequency=%d, upper frequency=%d, bandLevel=%d",
					index, freqRange[0], freqRange[1], bandLevel));
//				if(freqRange[0] < targetFrequency){
//					mEqualizer.setBandLevel((short) index, maxGainLevel);
//					Log.i("Equalizer", String.format("Adjust band:%d gainLevel to=%d", 
//						index, maxGainLevel));
//				}
			}
			
			Descriptor equalDesp = mEqualizer.getDescriptor();
			Log.i("Equalizer", String.format("equalDesp name=%s, connectMode=%s, implementor=%s", 
				equalDesp.name, equalDesp.connectMode, equalDesp.implementor));

			if(mAudioConfig.getEnableEqualizer()){
				mEqualizer.setEnabled(true);
				mIsEqualizerEnabled = true;
			}
		}
		catch(Exception e){
			Log.e(LOG_TAG, String.format("Create Equalizer failed: %s", e.getMessage()));
			e.printStackTrace();
			mEqualizer = null;
		}
	}

	/**
	 * Reference: http://stackoverflow.com/questions/4807428/audiorecord-could-not-get-audio-input-for-record-source-1
	 * Scan for the best configuration parameter for AudioRecord object on this device.
	 * Constants value are the audio source, the encoding and the number of channels.
	 * That means were are actually looking for the fitting sample rate and the minimum
	 * buffer size. Once both values have been determined, the corresponding program
	 * variable are initialized (audioSource, sampleRate, channelConfig, audioFormat)
	 * For each tested sample rate we request the minimum allowed buffer size. Testing the
	 * return value let us know if the configuration parameter are good to go on this
	 * device or not.
	 * This should be called in at start of the application in onCreate().
	 * */
	public int initRecorderParameters(int[] sampleRates, int minSampleRate) {

		for (int i = 0; i < sampleRates.length; ++i) {
			try {
				if (sampleRates[i] < minSampleRate) {
					continue;
				}
				Log.i(LOG_TAG, "Indexing " + sampleRates[i] + "Hz Sample Rate");
				int tmpBufferSize = AudioRecord.getMinBufferSize(sampleRates[i],
					AudioFormat.CHANNEL_IN_STEREO,
					AudioFormat.ENCODING_PCM_16BIT);

				// Test the minimum allowed buffer size with this configuration on this device.
				if (tmpBufferSize != AudioRecord.ERROR_BAD_VALUE) {
					// Seems like we have ourself the optimum AudioRecord parameter for this device.
					AudioRecord tmpRecoder = new AudioRecord(MediaRecorder.AudioSource.MIC,
						sampleRates[i],
						AudioFormat.CHANNEL_IN_STEREO,
						AudioFormat.ENCODING_PCM_16BIT,
						tmpBufferSize);
					// Test if an AudioRecord instance can be initialized with the given parameters.
					if (tmpRecoder.getState() == AudioRecord.STATE_INITIALIZED) {
						String configResume = "initRecorderParameters(sRates) has found recorder settings supported by the device:"
							+ "\nSource   = MICROPHONE"
							+ "\nsRate    = " + sampleRates[i] + "Hz"
							+ "\nChannel  = STEREO"
							+ "\nEncoding = 16BIT";
						Log.i(LOG_TAG, configResume);

						//+++Release temporary recorder resources and leave.
						tmpRecoder.release();
						tmpRecoder = null;

						return sampleRates[i];
					}
				}
				else {
					Log.d(LOG_TAG, "Incorrect buffer size. Continue sweeping Sampling Rate...");
				}
			}
			catch (IllegalArgumentException e) {
				Log.e(LOG_TAG, "The " + sampleRates[i] + "Hz Sampling Rate is not supported on this device");
			}
		}
		return -1;
	}

	public int getValidSampleRates(int minSampleRate) {
		//OPUS: Fs is the sampling rate and must be 8000, 12000, 16000, 24000, or 48000
		int[] sampleRates = new int[] { 8000, 12000, 11025, 16000, 24000, 22050, 26000, 44100, 48000 };

		return initRecorderParameters(sampleRates, minSampleRate);
	}

	//initOpusComplexity: Read current CPU Speed and config opus complexity.
	private void initOpusComplexity()
	{
		//int sampleRate = 0;
		ProcessBuilder cmd;
		String result = "";
		try {
			String[] args = { "/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
			cmd = new ProcessBuilder(args);

			Process process = cmd.start();
			InputStream in = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			String line = "";
			while ((line = reader.readLine()) != null) {
				result += line;
			}
			reader.close();
			in.close();
		}
		catch (IOException e) {
			//Log.e(e);
			e.printStackTrace();
		}
		try {
			long lCPUSpeed = Long.parseLong(result);
			Log.d("OPUS", String.format("CPUSpeed=%d", lCPUSpeed));
			//Set OPUS Complexity, according CPU speed
//			if(lCPUSpeed < 1280000){
//				mOpusConfig.setOCValue(0);
//			}
//			else if (lCPUSpeed < 1536000) {
			if(lCPUSpeed < 1536000){
				mOpusConfig.setOCValue(0);
			}
			else if (lCPUSpeed < 1792000) {
				mOpusConfig.setOCValue(1);
			}
			else if (lCPUSpeed < 2048000) {
				mOpusConfig.setOCValue(2);
			}
			else{
				mOpusConfig.setOCValue(4);
			}
		}
		catch (NumberFormatException e) {
			//Log.e(e);
			e.printStackTrace();
		}

		Log.d(LOG_TAG, "ReadCPUInfo:" + result);
	}
	
	public boolean createAudioRecord(int sampleRate, int audioChannels) {
		int chcfg = (audioChannels >= 2) ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
		int audiofmt = AudioFormat.ENCODING_PCM_16BIT;
		try {
			//Note: make the recorder's buffer size as twice as min buffer size.
			//mRecordBufferSize = AudioRecord.getMinBufferSize(sampleRate, chcfg, audiofmt) * 2;
			mRecordBufferSize = AudioRecord.getMinBufferSize(sampleRate, chcfg, audiofmt);
			mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, chcfg,
				audiofmt, mRecordBufferSize);

			Log.i(LOG_TAG, String.format("Create AudioRecorder with mRecordBufferSize=%d, mAudioBufferSize=%d, sampleRate=%d",
				mRecordBufferSize, mEncAudioBufferSize, sampleRate));
			if (null == mAudioRecorder) {
				Log.e(LOG_TAG, String.format("Create AudioRecorder failed"));
			}
			
			mAudioTrackBaseSampleRate = sampleRate;

			if (android.os.Build.VERSION.SDK_INT >= 16) {
//					enableNoiseSuppressor();
//					enableAutomaticGainControl();
//					enableAcousticEchoCanceler();
			}

			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public int getAudioDataDelayMode(int seqno, long presentationTimeMs) {
		mDelayMode = checkAudioDelayByQueueSize();
		return mDelayMode;
	}

	public int checkAudioDelayByQueueSize() {
//		int audioQueueSize = mAudioRecvPerformance.getPushQueueSize();
//		if (QUEUESIZE_SERIOUS_DELAY < audioQueueSize) {
//			return AUDIO_DELAY_DROPPED;
//		}
//		else {
//			return AUDIO_DELAY_SMOOTH;
//		}
		return AUDIO_DELAY_SMOOTH;
	}

	/***
	 *  AVReceiverInterface implementation start
	 */
	//@Override
	public void DataReceived(char packetType, int seqno, long presentationTimeMs, byte[] data, boolean bPacketLost, int lostCount) {
		switch (packetType)
		{
			case 'A':
				AudioDataReceived(seqno, presentationTimeMs, data, bPacketLost, lostCount);
				break;
			case 'V':
				break;
			case 'M':
				// Meta is set by default
				Log.i(LOG_TAG, "Abnormal source. Meta packet is received, but is not expected.");
				break;
			default:
				break;
		}
	}

	//@Override
	public void ConnectionStop(int stateCode) {
		Log.i(LOG_TAG, String.format("Received internal connecton stopped signal(%d). Stop connection.", stateCode));
		//stop(true);
	}

	//@Override
	public void SampleRateAdjust(int samplerate) {
		
		if(samplerate < 8000){
			Log.e(LOG_TAG, "SampleRateAdjust not supported sample rate:" + samplerate);
			return;
		}
		
		// Adjust based sample rate adjust from peer's recorder's sample rate.
		Log.i(LOG_TAG, "SampleRateAdjust new sample rate:" + samplerate);
		adjustBaseSampleRate(samplerate);

	}

	//@Override
	public void BitrateAdjust(int bitrate) {
		if (null != mOpusEncoder) {
			mOpusEncoder.resetBitrate(bitrate);
		}
	}

	//@Override
	public void EndOfPlay() {
		Log.i(LOG_TAG, "Audio EOF is received.");
		mIsTxFinished = true;
	}
	/***
	 *  AVReceiverInterface implementation end
	 */
	
	private void AudioDataReceived(int seqno, long presentationTimeMs, byte[] data, boolean bPacketLost, int lostCount) {
		// Run in Pull-Push mode
//		long packetRecvTime = mTransport.getPacketReceiveTime(seqno);
//		if (-1 != packetRecvTime) {
//			long playDelayTime = System.currentTimeMillis() - packetRecvTime;
//			mTransport.mAudioRecvPerformance.mPlayDelayTimeMs = playDelayTime;
//		}

//		int audioQueueSize = mAudioRecvPerformance.getPushQueueSize();
//		if (AUDIO_DELAY_DROPPED == mDelayMode) {
//			if ((QUEUESIZE_SERIOUS_DELAY - 15) > audioQueueSize) {
//				//getAudioDataDelayMode(seqno, presentationTimeMs);
//				mDelayMode = AUDIO_DELAY_SMOOTH;
//			}
//		}
//		else {
//			getAudioDataDelayMode(seqno, presentationTimeMs);
//		}
		mDelayMode = AUDIO_DELAY_SMOOTH;
		//Log.d(LOG_TAG, String.format("Audio DelayMode: %d, audioQueueSize=%d", mDelayMode, audioQueueSize));
		decodePlay(data, mDelayMode, bPacketLost, lostCount);
	}

	public void resetDecBufferSize(int sampleRate) {
		
		mDecAudioBufferSize = (mAudioTrackBaseSampleRate*mAudioConfig.getAudioFrameSize()*2)/1000;
		mDecodeOpusBuffer = new short[mDecAudioBufferSize];
		mDecodeOpusEmptyBuffer = new short[mDecAudioBufferSize];

	}

	public void adjustBaseSampleRate(int sampleRate) {
		if (mAudioTrackBaseSampleRate == sampleRate)
			return;

		Log.i(LOG_TAG, "adjust audiotrack base sample rate:" + sampleRate);
		mAdjustingSampleRate.set(true);
		while (mAdjustingSampleRate.get()) {
			try {
				Thread.sleep(10);
			}
			catch (InterruptedException e) {
				//Log.e(e);
				e.printStackTrace();
			}
			if (mWaitAdjust.get()) {
				break;
			}
		}
		mAudioTrackBaseSampleRate = sampleRate;
		resetDecBufferSize(sampleRate);

		if (null != mOpusDecoder) {
			if (sampleRate >= 44100) {
				if (mOpusDecoder.getSampleRate() != 48000) {
					mOpusDecoder.resetSampleRate(48000, OpusWrapper.OPUS_TYPE_DECODER);
				}
			}
			else if (sampleRate >= 22050) {
				if (mOpusDecoder.getSampleRate() != 24000) {
					mOpusDecoder.resetSampleRate(24000, OpusWrapper.OPUS_TYPE_DECODER);
				}
			}
			else if (sampleRate >= 11025) {
				if (mOpusDecoder.getSampleRate() != 12000) {
					mOpusDecoder.resetSampleRate(12000, OpusWrapper.OPUS_TYPE_DECODER);
				}
			}
			else if (sampleRate >= 8000) {
				if (mOpusDecoder.getSampleRate() != 8000) {
					mOpusDecoder.resetSampleRate(8000, OpusWrapper.OPUS_TYPE_DECODER);
				}
			}
		}
		if (null != mAudioTrack) {
			mAudioTrack.stop();
			mAudioTrack.release();
			mAudioTrack = null;

			createAudioTrack(mAudioTrackBaseSampleRate, 2);
			mAudioTrack.play();

			Log.d(LOG_TAG, String.format("adjustBaseSampleRate() create AudioTrack Bitrate=%d, BufferSize=%d, SampleRate=%d",
					mOpusConfig.getBitrate(), mDecAudioBufferSize, mAudioTrackBaseSampleRate));
		}
		mAdjustingSampleRate.set(false);
		mWaitAdjust.set(false);
	}

	
	
	public void adjustMicAmplitude(AudioDataRecord audioData){
		mEncAmplitudeAdjustTimes = mAudioConfig.getAdjustGainLevel();
   		if(mEncAmplitudeAdjustTimes > 1){
   			if(mEncAmplitudeAdjustTimes > 10){
   				mEncAmplitudeAdjustTimes = 10;
   			}
   			Log.d(LOG_TAG, String.format("AudioStramer adjust gain with adjustTimes=%d", mEncAmplitudeAdjustTimes));
   			audioData.adjustAmplitude(mEncAmplitudeAdjustTimes);
   		}
	}
	
	protected short[] processByNS(short[] audioRawData){
		int nsResult = mNSWrapper.nsProcess(audioRawData, null, mNSOutputData, null, mEncAudioBufferSize);
		if(nsResult == 0){
			//Log.i(LOG_TAG, String.format("NS process success."));
			audioRawData = mNSOutputData;
		}
		else{
			Log.i(LOG_TAG, String.format("NS process failed."));
		}
		
		return audioRawData;
	}
	
	protected short[] processByAGC(short[] audioRawData, boolean bVoice){
		int inMicLevel = 1;
		int[] outMicLevel = new int[1];
		outMicLevel[0] = 3;
		int hasEcho = 0;
		if(mAudioConfig.getAGCHasEcho()){
			hasEcho = 1;
		}
		int agcResult = -1;
		if((null != mVADWrapper) && mAudioConfig.getEnableVAD() && bVoice){
			//mAGCWrapper.agcSetAGCConfig(mAudioConfig.getAGCTargetLvDbfs(), mAudioConfig.getAGCCompressionGaindB(), mAudioConfig.getAGCLimiterEnable());
			agcResult = mAGCWrapper.agcProcess(audioRawData, null, mAGCOutputData, null, mEncAudioBufferSize, inMicLevel, outMicLevel, hasEcho);
		}
		else if((null != mVADWrapper) && (mAudioConfig.getEnableVAD())){
			//Note: skip AGC when non voice frame.
			//mAGCWrapper.agcSetAGCConfig(mAudioConfig.getAGCTargetLvDbfs(), mAudioConfig.getAGCCompressionGaindB()/2, mAudioConfig.getAGCLimiterEnable());
			//agcResult = mAGCWrapper.agcProcess(audioRawData, null, mAGCOutputData, null, mEncAudioBufferSize, inMicLevel, outMicLevel, hasEcho);
			//Log.i(LOG_TAG, String.format("Skip AGC for non-voice data."));
		}
		else{
			agcResult = mAGCWrapper.agcProcess(audioRawData, null, mAGCOutputData, null, mEncAudioBufferSize, inMicLevel, outMicLevel, hasEcho);
		}
		
		if(0 == agcResult){
			audioRawData = mAGCOutputData;
		}
		else{
			if(!((null != mVADWrapper) && mAudioConfig.getEnableVAD())){
				Log.e(LOG_TAG, String.format("AGC process failed."));
			}
		}
		return audioRawData;
	}
	
	protected short[] processByAEC(short[] audioRawData){
		int delayTimeMs = 80;
		if(null != mAudioTrack){
			mCurPlaybackHeadPos = mAudioTrack.getPlaybackHeadPosition();
			int remainFrames = mRemainPlaybackFrames - mCurPlaybackHeadPos;
			delayTimeMs = (remainFrames*2*mAudioConfig.getAudioFrameSize())/mEncAudioBufferSize 
					+ (mAudioConfig.getAudioFrameSize()) 
					+ mAudioConfig.getAECDelayTimeBias();
			//Log.i(LOG_TAG, String.format("AEC Current delayTimeMs=%d", delayTimeMs));
			if(delayTimeMs < 0){
				delayTimeMs = 0;
			}
		}
		
		int aecmResult = -1;
		//if((null != mNSWrapper) && mAudioConfig.getEnableNS()){
		//	aecmResult = mAecmWrapper.aecmAECProcessWithNS(audioRawData, mNSOutputData, mAecmOutputData, mEncAudioBufferSize, delayTimeMs);
			//Log.i("NSWrapper", String.format("Output audio raw data with AEC and NS"));
			//Log.i(LOG_TAG, String.format("AECM process with NS result=%d.", aecmResult));
		//}
		//else{
			aecmResult = mAecmWrapper.aecmAECProcess(audioRawData, mAecmOutputData, mEncAudioBufferSize, delayTimeMs);
			//Log.i(LOG_TAG, String.format("AECM process result=%d.", aecmResult));
		//}
		
		if(0 == aecmResult){
			audioRawData = mAecmOutputData;
		}

		return audioRawData;
	}
	
	protected byte[] processAudioRecordData(short[] data) {
		short[] audioRawData = data;

		if(AudioConfig.AUDIO_MODE_MP3 == mAudioMode){
			audioRawData = mIAudioProcessorCallback.ObtainData();
			if(null == audioRawData){
				return null;
			}
		}
		
		if((null != mAecmWrapper) && mAudioConfig.getAECMConfig()){
			audioRawData = processByAEC(audioRawData);
		}
		
		if(null == audioRawData){
			Log.e(LOG_TAG, 
					String.format("audioRawData is null. mAecmWrapper=%d, enableAEC=%b", mAecmWrapper, mAudioConfig.getAECMConfig()));
			return null;
		}
		
		boolean bVoice = false;
		if((null != mVADWrapper) && mAudioConfig.getEnableVAD()){
			int isVoice = mVADWrapper.vadProcess(mRecorderBaseSampleRate, audioRawData, audioRawData.length, VADWrapper.VAD_SEND_SIDE);
			Log.i("VADProcess", String.format("VAD Process result=%d", isVoice));
			if(1 == isVoice){
				 bVoice = true;
			}
		}
		if((null != mAGCWrapper) && mAudioConfig.getEnableAGC()){
			audioRawData = processByAGC(audioRawData, bVoice);
		}
		if((null != mNSWrapper) && mAudioConfig.getEnableNS()){
			audioRawData = processByNS(audioRawData);
		}
		
		AudioDataRecord audioData = new AudioDataRecord(audioRawData, 0);
		
		if(mAudioConfig.getCalculateDB()){
			countDBandAdjustAGC(audioData, bVoice);
		}
		if(mAudioConfig.getAdjustGain()){
			adjustMicAmplitude(audioData);
		}
		if(mAudioConfig.getEnableRecorderFadeIn()){
			if(mRecorderFadeInCount < mRecorderFadeInMaxCount){
				audioData.fadeIn(mRecorderFadeInCount, mRecorderFadeInMaxCount);
			}
			mRecorderFadeInCount++;
		}
		
//		if((null != mAudioWaveSaver) && mAudioConfig.getEnableAECTestMode()){
//			mAudioWaveSaver.addAudioSendData(audioRawData.clone());
//			audioRawData = audioRawData2;	//Switch back audio raw data.
//		}
		
		int enc_out_size = -1;
		if(null != mOpusEncoder){
			if(mAudioConfig.getSendIfVAD()){
				if(bVoice || mLastIsVoice){
					enc_out_size = mOpusEncoder.opusEncode(audioRawData, mEncAudioBufferSize, mEncodeOpusBuffer, mEncAudioBufferSize / 2);
					if(bVoice){
						mLastIsVoice = true;
					}
					else{
						mLastIsVoice = false;
					}
				}
				else{
					enc_out_size = 1;
					mEncodeOpusBuffer[0] = mVADOpusEmptyData;
					mLastIsVoice = false;
				}
			}
			else{
				enc_out_size = mOpusEncoder.opusEncode(audioRawData, mEncAudioBufferSize, mEncodeOpusBuffer, mEncAudioBufferSize / 2);
			}
		}
		else{
			Log.e(LOG_TAG, String.format("mOpusEncoder is null."));
		}
		
		if (enc_out_size < 0) {
			Log.e(LOG_TAG, "mEncodeOpus encode error, error code:" + enc_out_size);
			return null;
		}
		else if(1 == enc_out_size){
			Log.i("OpusEncode", String.format("Send silent data, mEncodeOpusBuffer[0]=%d", mEncodeOpusBuffer[0]));
		}
		
		if(mAudioConfig.getEnableLogEncData() && (null != mEncDataLogger)){
			mEncDataLogger.recordEncDataSize(enc_out_size);
		}

		ByteBuffer outBuf = ByteBuffer.wrap(mEncodeOpusBuffer);
		outBuf.rewind();
		byte[] encOutData = new byte[enc_out_size];
		outBuf.get(encOutData, 0, enc_out_size);

		if (encOutData != null) {
			//Log.i("OpusEncode", String.format("encoded data %d is ready. mEncAudioBufferSize=%d", encOutData.length, mEncAudioBufferSize));
			//mTransport.putEncodedAudioData(encOutData);
			if(null != mIAudioProcessorCallback){
				mIAudioProcessorCallback.SendData(encOutData);
			}
		}
		return encOutData;

	}
	
	private void countDBandAdjustAGC(AudioDataRecord audioData, boolean bVoice){
		audioData.countMaxAmplitude();
		int maxAmplitude = audioData.getMaxAmplitude();
		int dbValue = 0;
		if(mAudioConfig.getEnableVAD()){
			if(bVoice){
				mAudioPerformanceCounter.addAmplitude(maxAmplitude);
				dbValue = mAudioPerformanceCounter.getLastDBValue();
			}
		}
		else{
			mAudioPerformanceCounter.addAmplitude(maxAmplitude);
			dbValue = mAudioPerformanceCounter.getLastDBValue();
		}
		//Log.i("CalculateDB", String.format("Max Amplitude=%d, dbValue=%d", maxAmplitude, dbValue));
		mAudioPerformanceDataCount++;
		
		if(0 == (mAudioPerformanceDataCount % 15)){
			mAudioPerformanceDataCount = 0;
			int avgDBValue = mAudioPerformanceCounter.getAvgDBValue();
			Log.i("CalculateDB", String.format("avgDBValue=%d, dbMinThreshold=%d, dbMaxThreshold=%d", 
					avgDBValue, mAudioConfig.getDBMinThreshold(), mAudioConfig.getDBMaxThreshold()));
			if(0 == avgDBValue){
				//skip avgDBValue == 0
			}
			else if(avgDBValue < mAudioConfig.getDBMinThreshold()){
				//mAudioConfig.setEnableAGC(true);
				if((null != mAGCWrapper) && (mAudioConfig.getEnableAGC())){
					int newCompressionGaindB = mAudioConfig.getAGCCompressionGaindB() + 2;
					if(newCompressionGaindB > mAudioConfig.getAGCMaxCompressiondB()){
						newCompressionGaindB = mAudioConfig.getAGCMaxCompressiondB();
					}
					Log.i("AGCWrapper", String.format("AGCWrapper adjust up compressionGaindB=%d", newCompressionGaindB));
					mAudioConfig.setAGCCompressionGaindB(newCompressionGaindB);
					mAGCWrapper.agcSetAGCConfig(mAudioConfig.getAGCTargetLvDbfs(), newCompressionGaindB, mAudioConfig.getAGCLimiterEnable());
				}
			}
			else if(avgDBValue > mAudioConfig.getDBMaxThreshold()){
				if(0 == mAudioConfig.getAGCCompressionGaindB()){
					//Do nothing
				}
				else if((null != mAGCWrapper) && (mAudioConfig.getEnableAGC())){
					int newCompressionGaindB = mAudioConfig.getAGCCompressionGaindB() - 2;
					if(newCompressionGaindB < 0){
						newCompressionGaindB = 0;
					}
					mAudioConfig.setAGCCompressionGaindB(newCompressionGaindB);
					mAGCWrapper.agcSetAGCConfig(mAudioConfig.getAGCTargetLvDbfs(), newCompressionGaindB, mAudioConfig.getAGCLimiterEnable());
					Log.i("AGCWrapper", String.format("AGCWrapper adjust down compressionGaindB=%d", newCompressionGaindB));
				}
			}
		}
	}

	public int getAudioQueueSize() {
//		int queueSize = mTransport.getRecvQueueSize() + mAudioPlayBuffer.size();
		int queueSize = mAudioPlayBuffer.size();
		//Log.d(LOG_TAG, String.format("mTransport recv queue:%d, playbuffer:%d", mTransport.getRecvQueueSize(), mAudioPlayBuffer.size()));
		return queueSize;
	}

	/***
	 * AVSenderInterface Start
	 */
//	@Override
	public void SendData(byte[] pData) {
		if(mIAudioProcessorCallback != null){
			mIAudioProcessorCallback.SendData(pData);
		}
	}
	
	public byte[] GetAudioData(char dataType) {

		byte[] data = null;
		while (data == null) {
			while (mIsRunning && (mAudioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)) {
				int waitCount = 0;
				try {
					Thread.sleep(20);
				}
				catch (InterruptedException e) {
					Log.e(LOG_TAG, "Sleep is interrupted in PullData waiting loop.");
					return null;
				}
				if (waitCount++ >= 5) {
					Log.e(LOG_TAG, "Failed to get recorded data in PullData.");
					return null;
				}
			}
				
			if (!mIsRunning)
				return null;

			int readSize = 0;
			if (null != mAudioRecorder) {
				readSize = mAudioRecorder.read(mRecordedDataBuffer, 0, mEncAudioBufferSize);
			}
			
			//Log.i("GetAudioData", String.format("mAudioBufferSize=%d, readSize=%d", mEncAudioBufferSize, readSize));
			
			if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
				if ((readSize != mEncAudioBufferSize) && mIsRunning) {
					// sometimes readSize return 0, which means the operation is normal but just no data
					Log.e(LOG_TAG, 
							String.format("RECORDED:%d: Read size: %d, buffer size: %d", 
							System.currentTimeMillis(), readSize, mEncAudioBufferSize));
						if (readSize > 0) {
						return wrapRecordData(readSize);
					}
				}
				else {
					data = processAudioRecordData(mRecordedDataBuffer);
					
					if (data != null){
						return data;
					}
				}
			}
			else{
				Log.e("GetAudioData", 
					String.format("AudioRecord.ERROR_INVALID_OPERATION != readSize"));
			}
		}

		return null;
	}
	/***
	 * AVSenderInterface End
	 */
	
	private byte[] wrapRecordData(int readSize){
		byte[] data = null;
		if (null == mRecordDataBuffer) {
			mRecordDataBuffer = ShortBuffer.allocate(mEncAudioBufferSize);
		}

		int restSize = 0;
		int remainSize = mEncAudioBufferSize - mRecordDataBuffer.position();
		int index = 0;
		if (remainSize < readSize) {
			mRecordDataBuffer.put(mRecordedDataBuffer, 0, remainSize);
			restSize = readSize - remainSize;
			index += remainSize;
		}
		else {
			mRecordDataBuffer.put(mRecordedDataBuffer, 0, readSize);
			remainSize = 0;
			index += readSize;
		}

		if (mRecordDataBuffer.position() == mRecordDataBuffer.limit()) {
			// Directly fill in rawDataIn for encoding
			short[] tempData = mRecordDataBuffer.duplicate().array();
			data = processAudioRecordData(tempData);
			mRecordDataBuffer.clear();
			
			while(restSize >= mEncAudioBufferSize){
				mRecordDataBuffer.put(mRecordedDataBuffer, index, mEncAudioBufferSize);
				index += mEncAudioBufferSize;
				short[] tempData2 = mRecordDataBuffer.duplicate().array();
				data = processAudioRecordData(tempData2);
				remainSize = 0;
				mRecordDataBuffer.clear();
				
				restSize = restSize - mEncAudioBufferSize;
			}
			
			if (restSize > 0) {
				mRecordDataBuffer.put(mRecordedDataBuffer, index, restSize);
			}

			if (data != null){
				return data;
			}
		}
		return null;
	}
	
	public void adjustEquBandLevel(short bandIndex, short gainValue){
		if(null == mEqualizer){
			return;
		}
		Log.i("Equalizer", String.format("adjustEquBandLevel band=%d, gainValue=%d", bandIndex, gainValue));
		mEqualizer.setBandLevel(bandIndex, gainValue);
	}
	
	public void configEnableAGC(boolean bEnable){
		mAudioConfig.setEnableAGC(bEnable);
	}
	
	public void configEnableNS(boolean bEnable){
		mAudioConfig.setEnableNS(bEnable);
	}
	
	public void configEnableAECM(boolean bEnable){
		mAudioConfig.setAECMConfig(bEnable);
	}
	
	public void configDropMode(boolean bEnable) {
		//mEnableDropMode = bEnable;
		mAudioConfig.setDropModeConfig(bEnable);
	}

	public void configDynPlaybackRate(boolean bEnable) {
		//mDynPlaybackRate = bEnable;
		mAudioConfig.setDynPlaybackRateConfig(bEnable);
	}
	
	public int getLastDBValue(){
		if(null != mAudioPerformanceCounter){
			return mAudioPerformanceCounter.getLastDBValue();
		}
		return 0;
	}

	public class AudioDataRecord {
		private short[] mData;
		private int mAmplitude = 0;
		private static final short maxThreshold = 30000;
		private static final short minThreshold = -30000;
		private int mPitch = 0;
		private int mMaxAmplitude = 0;
		
		public AudioDataRecord(short[] data, int amplitude){
			mData = data;
			mAmplitude = amplitude;
		}
		
		public short[] getData(){
			return mData;
		}
		
		public int getAmplitude(){
			return mAmplitude;
		}
		
		public int getMaxAmplitude(){
			return mMaxAmplitude;
		}
		
		public void fadeIn(int targetValue, int baseValue){
			//Log.d(LOG_TAG, 
			//	String.format("AudioDataRecord fadeIn: targetValue=%d, baseValue=%d", targetValue, baseValue));
			for(int index=0; index < (mData.length-1); index+=2){
				mData[index] = (short) (mData[index] * targetValue / baseValue);
				mData[index+1] = (short) (mData[index+1] * targetValue / baseValue);
			}
		}
		
		public void adjustAmplitude(int adjustTimes){
			int times = adjustTimes;
			int maxValue = maxThreshold/adjustTimes;
			int minValue = -maxValue;
			//Log.d(LOG_TAG, String.format("amplitude=%d, times=%d", mAmplitude, times));
			for(int index=0; index<(mData.length-3); index+=4){
				if((mData[index] > maxValue) || (mData[index] < minValue)){
					if(mData[index] > 0)
						mData[index] = maxThreshold;
					else
						mData[index] = minThreshold;
				}
				else{
					mData[index] = (short) (mData[index]*times);					
				}
				
				if((mData[index+1] > maxValue) || (mData[index+1] < minValue)){
					if(mData[index] > 0)
						mData[index+1] = maxThreshold;
					else
						mData[index+1] = minThreshold;
				}
				else{
					mData[index+1] = (short) (mData[index+1]*times);
				}
				
				if((mData[index+2] > maxValue) || (mData[index+2] < minValue)){
					if(mData[index+2] > 0)
						mData[index+2] = maxThreshold;
					else
						mData[index+2] = minThreshold;
				}
				else{
					mData[index+2] = (short) (mData[index+2]*times);
				}
				
				if((mData[index+3] > maxValue) || (mData[index+3] < minValue)){
					if(mData[index+3] > 0)
						mData[index+3] = maxThreshold;
					else
						mData[index+3] = minThreshold;
				}
				else{
					mData[index+3] = (short) (mData[index+3]*times);				
				}
			}
		}
		
		public void countMaxAmplitude(){
			for(int index=0; index<(mData.length); index++){
				int value = Math.abs(mData[index]);
				if(value > mMaxAmplitude){
					mMaxAmplitude = value;
				}
			}
		}
		
		public void countAmplitude(){
			int length = mData.length;
			if(length <= 0){
				return;
			}
			
			int positive = 0;
	    	int negitive = 0;
	    	int value = 0;
			for(int index=0; index<(mData.length-3); index+=4){
	    		value = mData[index];
	    		if(value > 0){
	    			positive += value;
	    		}
	    		else{
	    			negitive += value;
	    		}
	    		value = mData[index+2];
	    		if(value >0){
	    			positive += value;
	    		}
	    		else{
	    			negitive += value;
	    		}
			}
			int sample_count = length/2;
	    	positive = positive/sample_count;
	    	negitive = negitive/sample_count;
	    	
	    	mAmplitude = positive - negitive;
		}
		
		public void setPitch(int value){
			if(mPitch > 0){
				//Note: if pitch value > 0, it would be recognized as voice data.
				Log.i(LOG_TAG, String.format("AudioDataRecord setPitch=%d, voice detected.", value));
			}
			mPitch = value;
		}
		
		public int getPitch(){
			return mPitch;
		}
	}
}