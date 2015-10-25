#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "ns/include/noise_suppression_x.h"
#include "com_example_audiotest_module_NSWrapper.h"
#include "ns/include/nsx_core.h"
#include <pthread.h>

#define LOG_TAG "NSWrapper"

void ns_test(int samplerate);
int ns_process(short* speechFrame, short* speechFrameHB, short* outFrame, short* outFrameHB, int sampleCount);
int ns_destroy();

static int NS_PROC_SAMPLE = 160;

NsxHandle *hNsHandle = NULL;
pthread_mutex_t ns_process_lock;
int mSamplerate = 0;

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_NSWrapper_nsInit
  (JNIEnv *env, jobject thiz, jint samplerate)
{

	if((mSamplerate == samplerate) && (NULL != hNsHandle)){
		return 0;
	}

	if(NULL != hNsHandle){
		ns_destroy();
	}

	if(pthread_mutex_init(&ns_process_lock, NULL) != 0)
	{
		//printf("\n mutex init failed\n");
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for ns_process_lock failed." );
		return -1;
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for ns_process_lock success." );
	}

	int status = WebRtcNsx_Create(&hNsHandle);

	if(0 == status){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcNs_Create success, status=%d", status);
		status = WebRtcNsx_Init(hNsHandle, samplerate);
		if(0 == status){
			mSamplerate = samplerate;
			// The input and output signals should always be 10ms (80 or 160 samples).
			if(samplerate == 8000){
				NS_PROC_SAMPLE = 80;
			}
			else{
				NS_PROC_SAMPLE = 160;
			}
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
					"WebRtcNs_Init success status=%d, samplerate=%d, NS_PROC_SAMPLE=%d",
					status, samplerate, NS_PROC_SAMPLE);
		}
		else{
			ns_destroy();
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcNs_Init failed, status=%d", status);
		}
	}
	else{
		pthread_mutex_destroy(&ns_process_lock);
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcNs_Create failed, status=%d", status);
	}

	return status;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_NSWrapper_nsDestroy
  (JNIEnv *env, jobject thiz)
{
	return ns_destroy();
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_NSWrapper_nsProcess
  (JNIEnv *env, jobject thiz, jshortArray speechFrame, jshortArray speechFrameHB, jshortArray outputFrame, jshortArray outputFrameHB, jint sampleCount)
{
	int status = -1;
	jshort* speech_frame = (*env)->GetShortArrayElements(env, speechFrame, NULL);
	jshort* speech_frame_h = NULL;
	if(NULL != speechFrameHB){
		speech_frame_h = (*env)->GetShortArrayElements(env, speechFrameHB, NULL);
	}
	jshort* output_frame = (*env)->GetShortArrayElements(env, outputFrame, NULL);
	jshort* output_frame_h = NULL;
	if(NULL != outputFrameHB){
		output_frame_h = (*env)->GetShortArrayElements(env, outputFrameHB, NULL);
	}

	pthread_mutex_lock(&ns_process_lock);
	status = ns_process(speech_frame, speech_frame_h, output_frame, output_frame_h, sampleCount);
	pthread_mutex_unlock(&ns_process_lock);

	(*env)->ReleaseShortArrayElements(env, speechFrame, speech_frame, 0);
	if(NULL != speechFrameHB){
		(*env)->ReleaseShortArrayElements(env, speechFrameHB, speech_frame_h, 0);
	}
	(*env)->ReleaseShortArrayElements(env, outputFrame, output_frame, 0);
	if(NULL != outputFrameHB){
		(*env)->ReleaseShortArrayElements(env, outputFrameHB, output_frame_h, 0);
	}
	return status;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_NSWrapper_nsSetPolicy
  (JNIEnv *env, jobject thiz, jint nsMode)
{
	int result = -1;

	if(NULL == hNsHandle){
		return result;
	}

	switch(nsMode){
		case com_example_audiotest_module_NSWrapper_NS_MODE_MILD:
			result = WebRtcNsx_set_policy(hNsHandle, com_example_audiotest_module_NSWrapper_NS_MODE_MILD);
			break;
		case com_example_audiotest_module_NSWrapper_NS_MODE_MEDIUM:
			result = WebRtcNsx_set_policy(hNsHandle, com_example_audiotest_module_NSWrapper_NS_MODE_MEDIUM);
			break;
		case com_example_audiotest_module_NSWrapper_NS_MODE_AGGRESSIVE:
			result = WebRtcNsx_set_policy(hNsHandle, com_example_audiotest_module_NSWrapper_NS_MODE_AGGRESSIVE);
			break;
	}

	if(0 == result){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcNsx_set_policy success with mode=%d", nsMode);
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcNsx_set_policy failed, result=%d", result);
	}

	return result;
}

int ns_destroy(){
	int result = -1;
	if(NULL != hNsHandle){
		pthread_mutex_lock(&ns_process_lock);
		result = WebRtcNsx_Free(hNsHandle);
		hNsHandle = NULL;
		if(0 != result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcNsx_Free failed, result=%d", result);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcNsx_Free success, result=%d", result);
		}
		pthread_mutex_unlock(&ns_process_lock);
		pthread_mutex_destroy(&ns_process_lock);
	}

	return result;
}

void ns_test(int samplerate)
{
	//NSinst_t hNsHandle2;
	//NsHandle *hNS = (NsHandle*) &hNSInstance;
	int status = WebRtcNsx_Create(&hNsHandle);

	if(0 == status){
		__android_log_print(ANDROID_LOG_VERBOSE, "NSWrapper", "WebRtcNs_Create success, status=%d", status);
		status = WebRtcNsx_Init(hNsHandle, samplerate);
		if(0 == status){
			__android_log_print(ANDROID_LOG_VERBOSE, "NSWrapper", "WebRtcNs_Init success status=%d, samplerate=%d", status, samplerate);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, "NSWrapper", "WebRtcNs_Init failed, status=%d", status);
		}
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, "NSWrapper", "WebRtcNs_Create failed, status=%d", status);
	}

	//return WebRtcNs_Init(hNS, sf);
}

int ns_process(short* speechFrame, short* speechFrameHB, short* outFrame, short* outFrameHB, int sampleCount)
{
	//WebRtcNs_Process();
	int status = -1;
	if(NULL != hNsHandle){
		int index = 0;
		int maxIndex = sampleCount - NS_PROC_SAMPLE + 1;
		for(index=0; index < maxIndex; index+=NS_PROC_SAMPLE){
			status = WebRtcNsx_Process(hNsHandle, speechFrame+index, speechFrameHB, outFrame+index, outFrameHB);
			if(0 != status){
				__android_log_print(ANDROID_LOG_VERBOSE, "NSWrapper", "WebRtcNsx_Process failed, status=%d", status);
			}
		}
	}
	return status;
}
