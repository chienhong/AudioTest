#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "agc/include/gain_control.h"
#include "com_example_audiotest_module_AGCWrapper.h"
#include <pthread.h>

#define LOG_TAG "AGCWrapper"

static int gAGCMode = kAgcModeFixedDigital;
int AGC_PROC_SAMPLE = 160;
void *mAGCVoice = NULL;
void *mAGCNonVoice = NULL;
int mSampleRate = 0;
pthread_mutex_t agc_process_lock;
pthread_mutex_t agc_addfarend_lock;

int agcDestroy();
void initAGCMode(int agcMode);
void printAGCConfig();
int setAGCConfig(int16_t targetLevelDBfs, int16_t compressionGaindB, uint8_t limiterEnable);

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AGCWrapper_agcInit
  (JNIEnv *env, jobject thiz, jint samplerate, jint agcMode, jint minVolumeLevel, jint maxVolumeLevel)
{
	int result = -1;

	if((mSampleRate == samplerate) && (NULL != mAGCVoice)){
		return 0;
	}

	if(NULL != mAGCVoice){
		agcDestroy();
	}

	if(0 != pthread_mutex_init(&agc_process_lock, NULL))
	{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for agc_process_lock failed." );
		return -1;
	}

	if(0 != pthread_mutex_init(&agc_addfarend_lock, NULL)){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for agc_addfarend_lock failed." );
		pthread_mutex_destroy(&agc_process_lock);
		return -1;
	}

	result = WebRtcAgc_Create(&mAGCVoice);
	if(0 == result){
		initAGCMode(agcMode);
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_Create success. Samplerate=%d", samplerate);
		result = WebRtcAgc_Init(mAGCVoice, minVolumeLevel, maxVolumeLevel, gAGCMode, samplerate);
		if(0 == result){

			mSampleRate = samplerate;
			if(8000 == mSampleRate)
			{
				AGC_PROC_SAMPLE = 160;
			}
			else{
				AGC_PROC_SAMPLE = 320;
			}
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_Init success. Samplerate=%d, minVolumeLevel=%d, maxVolumeLevel=%d, AGC_PROC_SAMPLE=%d.",
					mSampleRate, minVolumeLevel, maxVolumeLevel, AGC_PROC_SAMPLE);
			//setAGCConfig(3, 20, kAgcTrue);
			printAGCConfig();
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_Init failed. result=%d, samplerate=%d", result, mSampleRate);
		}
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_Create failed. result=%d, samplerate=%d", result, samplerate);
	}

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AGCWrapper_agcDestroy
  (JNIEnv *env, jobject thiz)
{
	return agcDestroy();
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AGCWrapper_agcProcess
  (JNIEnv *env, jobject thiz, jshortArray inNear, jshortArray inNearH, jshortArray output, jshortArray outputH,
		  jint samples, jint inMicLevel, jintArray outMicLevel, jint hasEcho)
{

	jshort* in_near_array = (*env)->GetShortArrayElements(env, inNear, NULL);
	jshort* in_nearH_array = NULL;
	if(NULL != inNearH){
		in_nearH_array = (*env)->GetShortArrayElements(env, inNearH, NULL);
	}
	jshort* output_array = (*env)->GetShortArrayElements(env, output, NULL);
	jshort* outputH_array = NULL;
	if(NULL != outputH){
		outputH_array  = (*env)->GetShortArrayElements(env, outputH, NULL);
	}
	jint* out_mic_level = (*env)->GetIntArrayElements(env, outMicLevel, NULL);

	int result = -1;

	int inMicLv = inMicLevel;
	int outMicLv = out_mic_level[0];
	uint8_t error = 0;
	if(NULL != mAGCVoice){
		pthread_mutex_lock(&agc_process_lock);
		int index = 0;
		int maxLength = samples - AGC_PROC_SAMPLE + 1;

		for(index = 0; index < maxLength; index+=AGC_PROC_SAMPLE){
			result = WebRtcAgc_Process(mAGCVoice, in_near_array+index, NULL, AGC_PROC_SAMPLE,
						output_array+index, NULL, inMicLevel, &outMicLv, hasEcho, &error);
			if(0 != result){
				__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_Process failed. result=%d, error=%d", result, error);
			}
			else{
				out_mic_level[0] = outMicLv;
				//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_Process success. inMicLv=%d, outMicLv=%d, hasEcho=%d, error=%d",
				//		inMicLv, outMicLv, hasEcho, error);
			}
		}
		pthread_mutex_unlock(&agc_process_lock);
	}

	(*env)->ReleaseShortArrayElements(env, inNear, in_near_array, 0);
	if(NULL != in_nearH_array){
		(*env)->ReleaseShortArrayElements(env, inNearH, in_nearH_array, 0);
	}
	(*env)->ReleaseShortArrayElements(env, output, output_array, 0);
	if(NULL != outputH_array){
		(*env)->ReleaseShortArrayElements(env, outputH, outputH_array, 0);
	}
	(*env)->ReleaseIntArrayElements(env, outMicLevel, out_mic_level, 0);

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AGCWrapper_agcAddFarend
  (JNIEnv *env, jobject thiz, jshortArray inFarEnd, jint inSize)
{
	int result = -1;
	jshort* in_farend_array = (*env)->GetShortArrayElements(env, inFarEnd, NULL);

	pthread_mutex_lock(&agc_addfarend_lock);

	int index = 0;
	int maxLength = inSize - AGC_PROC_SAMPLE + 1;
	for(index=0; index < maxLength; index += AGC_PROC_SAMPLE){
		result = WebRtcAgc_AddFarend(mAGCVoice, in_farend_array+index, AGC_PROC_SAMPLE);

		if(0 != result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_AddFarend failed. result=%d", result);
		}
		else{
			//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_AddFarend Success.");
		}
	}

	pthread_mutex_unlock(&agc_addfarend_lock);

	(*env)->ReleaseShortArrayElements(env, inFarEnd, in_farend_array, 0);
	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AGCWrapper_agcSetAGCConfig
  (JNIEnv *env, jobject thiz, jint targetLvDbfs, jint compressionGaindB, jint limiterEnable)
{
	int result = -1;

	if(NULL != mAGCVoice){
		result = setAGCConfig(targetLvDbfs, compressionGaindB, limiterEnable);
		printAGCConfig();
	}

	return result;
}

/*
 * Class:     Java_com_example_audiotest_module_AGCWrapper
 * Method:    agcSetProcessSampleTime
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AGCWrapper_agcSetProcessSampleTime
  (JNIEnv *env, jobject thiz, jint procTimeMs)
{
	int result = -1;

	if(10 == procTimeMs){
		AGC_PROC_SAMPLE = (mSampleRate/1000)*procTimeMs;
		result = 0;
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"agcSetProcessSampleTime sampleRate=%d procTimeMs=%d AGC_PROC_SAMPLE=%d",
				mSampleRate, procTimeMs, AGC_PROC_SAMPLE);
	}
	else if(20 == procTimeMs){
		AGC_PROC_SAMPLE = (mSampleRate/1000)*procTimeMs;
		result = 0;
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"agcSetProcessSampleTime sampleRate=%d procTimeMs=%d AGC_PROC_SAMPLE=%d",
				mSampleRate, procTimeMs, AGC_PROC_SAMPLE);
	}

	return result;
}

int agcDestroy(){
	int result = -1;
	pthread_mutex_lock(&agc_process_lock);
	pthread_mutex_lock(&agc_addfarend_lock);

	if(NULL != mAGCVoice){
		result = WebRtcAgc_Free(mAGCVoice);
		if(0 == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "agcDestroy WebRtcAgc_Free success. result=%d", result);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "agcDestroy WebRtcAgc_Free failed. result=%d", result);
		}
		mAGCVoice = NULL;
	}

	pthread_mutex_unlock(&agc_process_lock);
	pthread_mutex_destroy(&agc_process_lock);
	pthread_mutex_unlock(&agc_addfarend_lock);
	pthread_mutex_destroy(&agc_addfarend_lock);

	return result;
}

void initAGCMode(int agcMode)
{
	switch(agcMode){
		case com_example_audiotest_module_AGCWrapper_AGC_MODE_UNCHANGED:
			gAGCMode = kAgcModeUnchanged;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "initAGCMode kAgcModeUnchanged");
			break;
		case com_example_audiotest_module_AGCWrapper_AGC_MODE_ADAPTIVE_ANALOG:
			gAGCMode = kAgcModeAdaptiveAnalog;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "initAGCMode kAgcModeAdaptiveAnalog");
			break;
		case com_example_audiotest_module_AGCWrapper_AGC_MODE_ADAPTIVE_DIGITAL:
			gAGCMode = kAgcModeAdaptiveDigital;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "initAGCMode kAgcModeAdaptiveDigital");
			break;
		case com_example_audiotest_module_AGCWrapper_AGC_MODE_FIXED_DIGITAL:
			gAGCMode = kAgcModeFixedDigital;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "initAGCMode kAgcModeFixedDigital");
			break;
	}
}

void printAGCConfig()
{
	if(NULL != mAGCVoice){
		WebRtcAgc_config_t agcConfig;
		int result = WebRtcAgc_get_config(mAGCVoice, &agcConfig);
		if(0 == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_get_config success.");
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "compressionGaindB=%d", agcConfig.compressionGaindB);
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "limiterEnable=%d", agcConfig.limiterEnable);
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "targetLevelDbfs=%d", agcConfig.targetLevelDbfs);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAgc_get_config failed. result=%d", result);
		}
	}
}

int setAGCConfig(int16_t targetLevelDBfs, int16_t compressionGaindB, uint8_t limiterEnable)
{
	int result = -1;

	if(NULL != mAGCVoice){
		WebRtcAgc_config_t agcConfig;
		agcConfig.targetLevelDbfs = targetLevelDBfs;
		agcConfig.compressionGaindB = compressionGaindB;
		agcConfig.limiterEnable = limiterEnable;
		result = WebRtcAgc_set_config(mAGCVoice, agcConfig);
		if(0 != result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
					"WebRtcAgc_set_config failed. result=%d, targetLvDbfs=%d, compressionGaindB=%d, limiterEnable=%d",
					result, targetLevelDBfs, compressionGaindB, limiterEnable);
		}
	}

	return result;
}
