package com.example.audiotest.config;

public class AudioTestConfig {

	private static final String LOG_TAG = "AudioTestConfig";
	
	private static AudioTestConfig SINGLETON_INSTANCE = null;
	
	public static final int AUDIO_TEST_LOCAL_LOOP = 0;
	public static final int AUDIO_TEST_LOCAL_UDP = 1;
	
	private static int mTestMode = AUDIO_TEST_LOCAL_LOOP;
	
	private AudioTestConfig() {
		SINGLETON_INSTANCE = this;
	}

	public static AudioTestConfig GetInstance() {
		if (SINGLETON_INSTANCE == null){
			SINGLETON_INSTANCE = new AudioTestConfig();
		}

		return SINGLETON_INSTANCE;
	}
	
	public void setAudioTestMode(int testmode){
		mTestMode = testmode;
	}
	
	public int getAudioTestMode(){
		return mTestMode;
	}
	
}
