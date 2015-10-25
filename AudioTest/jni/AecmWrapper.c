
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <stddef.h>
#include "aecm/include/aecm_defines.h"
#include "aecm/include/echo_control_mobile.h"
#include "com_example_audiotest_module_AecmWrapper.h"
#include <stdbool.h>
#include <pthread.h>

#define LOG_TAG "AECM"

static int AEC_PROC_SAMPLE = 160;

bool aecmInit(int samplerate);
int aecmDestroy();
int aecmBufferFarend(void *aecm, int16_t *farEnd, int16_t sampleCount);
void printAECMConfig();

void *mAecm = 0;
int mSamplerate = 0;
pthread_mutex_t aecm_process_lock;
pthread_mutex_t aecm_buffer_lock;

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmInit
  (JNIEnv *env, jobject thiz, jint samplerate)
{
	if((mSamplerate == samplerate) && (NULL != mAecm)){
		return 0;
	}

	if(NULL != mAecm){
		aecmDestroy();
	}

	if(pthread_mutex_init(&aecm_process_lock, NULL) != 0)
	{
		//printf("\n mutex init failed\n");
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for aecm_process_lock failed." );
		return -1;
	}
	if(pthread_mutex_init(&aecm_buffer_lock, NULL) != 0){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for aecm_buffer_lock failed." );
		return -1;
	}

	if( aecmInit(samplerate) ){
		mSamplerate = samplerate;
		if(samplerate == 8000){
			AEC_PROC_SAMPLE = 80;
		}
		else{
			AEC_PROC_SAMPLE = 160;
		}
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "aecmInit success. MAX_DELAY=%d", MAX_DELAY);
		return 0;
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "aecmInit failed.");
		return -1;
	}
}

JNIEXPORT int JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmDestroy
  (JNIEnv *env, jobject thiz)
{
	int result = 0;

	result = aecmDestroy();

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmBufferFarend
  (JNIEnv *env, jobject thiz, jshortArray farEnd, jint sampleCount)
{
	jshort* farend_array = (*env)->GetShortArrayElements(env, farEnd, NULL);

	jint result = 0;

	if(NULL != mAecm){
		pthread_mutex_lock(&aecm_buffer_lock);
		int index = 0;
		int maxIndex = sampleCount - AEC_PROC_SAMPLE + 1;
		for(index=0; index < maxIndex; index+=AEC_PROC_SAMPLE){
			result = aecmBufferFarend(mAecm, farend_array+index, AEC_PROC_SAMPLE);
		}
		pthread_mutex_unlock(&aecm_buffer_lock);
	}

	(*env)->ReleaseShortArrayElements(env, farEnd, farend_array, 0);

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmAECProcess
  (JNIEnv *env, jobject thiz, jshortArray nearEnd, jshortArray output, jint sampleCount, jint delayTimeMs)
{
	jint result = 0;

	jshort* nearend_array = (*env)->GetShortArrayElements(env, nearEnd, NULL);
	jshort* output_array = (*env)->GetShortArrayElements(env, output, NULL);

	if(NULL != mAecm){
		// process Acoustic Echo Canceling
		pthread_mutex_lock(&aecm_process_lock);
		int index = 0;
		int maxIndex = sampleCount - AEC_PROC_SAMPLE + 1;
		for(index = 0; index < maxIndex; index+=AEC_PROC_SAMPLE){
			result = WebRtcAecm_Process( mAecm, nearend_array+index, NULL, output_array+index, AEC_PROC_SAMPLE, delayTimeMs);
			//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_Process status=%d at index=%d", result, index);
		}
		//result = WebRtcAecm_Process( mAecm, nearend_array, NULL, output_array, 160, delayTimeMs);
		pthread_mutex_unlock(&aecm_process_lock);
	}
	else{
		result = -1;
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "aecmAECProcess failed, mAecm is NULL.");
	}

	(*env)->ReleaseShortArrayElements(env, output, output_array, 0);
	(*env)->ReleaseShortArrayElements(env, nearEnd, nearend_array, 0);

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmAECProcessWithNS
  (JNIEnv *env, jobject thiz, jshortArray nearEndNoise, jshortArray nearEndClean, jshortArray output, jint sampleCount, jint delayTimeMs)
{
	int result = 0;

	jshort* nearend_noise_array = (*env)->GetShortArrayElements(env, nearEndNoise, NULL);
	jshort* nearend_clean_array = (*env)->GetShortArrayElements(env, nearEndClean, NULL);
	jshort* output_array = (*env)->GetShortArrayElements(env, output, NULL);

	if(NULL != mAecm){
		// process Acoustic Echo Canceling
		pthread_mutex_lock(&aecm_process_lock);
		int index = 0;
		int maxIndex = sampleCount - AEC_PROC_SAMPLE + 1;
		for(index = 0; index < maxIndex; index+=AEC_PROC_SAMPLE){
			result = WebRtcAecm_Process( mAecm, nearend_noise_array+index, nearend_clean_array+index, output_array+index, AEC_PROC_SAMPLE, delayTimeMs);
			//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_Process status=%d at index=%d", result, index);
		}
		pthread_mutex_unlock(&aecm_process_lock);
	}
	else{
		result = -1;
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "aecmAECProcess failed, mAecm is NULL.");
	}

	(*env)->ReleaseShortArrayElements(env, output, output_array, 0);
	(*env)->ReleaseShortArrayElements(env, nearEndNoise, nearend_noise_array, 0);
	(*env)->ReleaseShortArrayElements(env, nearEndClean, nearend_clean_array, 0);

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmSetProcessSampleCount
  (JNIEnv *env, jobject thiz, jint sampleCount)
{
	int result = -1;

	if((80 == sampleCount) || (160 == sampleCount)){
		AEC_PROC_SAMPLE = sampleCount;
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "AEC set process sample count=%d.", AEC_PROC_SAMPLE);
	}

	return result;
}

int aecmBufferFarend(void *aecm, int16_t *farEnd, int16_t sampleCount)
{
	int status = 0;
	if(NULL != aecm){
		//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Before bufferFarEnd data=%3d, %3d, %3d, %3d, %3d", farEnd[0], farEnd[1], farEnd[2], farEnd[3], farEnd[4]);
		int status = WebRtcAecm_BufferFarend( aecm, farEnd, sampleCount );
		//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_BufferFarend status=%d", status);
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "bufferFarend failed, mAecm is NULL.");
		status = -1;
	}
	return status;
}


bool aecmInit(int samplerate)
{
	int status = WebRtcAecm_Create(&mAecm);
	if(0 != status){
		mAecm = NULL;
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_Create false status=%d", status);
		return false;
	}

	status = WebRtcAecm_Init(mAecm, samplerate);
	if(0 != status){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_Init with samplerate:%d failed status=%d", samplerate, status);
		WebRtcAecm_Free(mAecm);
		mAecm = NULL;
		return false;
	}

	//setAECMConfig();
	printAECMConfig();

	return true;
}

int aecmDestroy()
{
	int result = -1;
	if(NULL != mAecm){
		pthread_mutex_lock(&aecm_buffer_lock);
		pthread_mutex_lock(&aecm_process_lock);

		result = WebRtcAecm_Free(mAecm);
		if(0 != result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_Free failed status=%d", result);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_Free success status=%d", result);
		}
		mAecm = NULL;
		pthread_mutex_unlock(&aecm_buffer_lock);
		pthread_mutex_destroy(&aecm_buffer_lock);
		pthread_mutex_unlock(&aecm_process_lock);
		pthread_mutex_destroy(&aecm_process_lock);
	}
	return result;
}

//Test for AECM Config
int setAECMConfig(){

	int result = -1;
	if(NULL != mAecm){
		AecmConfig aecmConfig;
		aecmConfig.cngMode = AecmTrue;
		aecmConfig.echoMode = 4;

		int result = WebRtcAecm_set_config(mAecm, aecmConfig);
		if(0 == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_set_config success result=%d", result);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_set_config failed result=%d", result);
		}

	}

	return result;
}

void printAECMConfig()
{
	if(NULL != mAecm){
		AecmConfig aecmConfig;
		int result = WebRtcAecm_get_config(mAecm, &aecmConfig);
		if(0 == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_get_config success result=%d", result);
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_get_config CNG Mode=%d, Echo Mode=%d",
					aecmConfig.cngMode, aecmConfig.echoMode);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcAecm_get_config failed result=%d", result);
		}
	}
}
