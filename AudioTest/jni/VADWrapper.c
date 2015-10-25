#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "vad/include/webrtc_vad.h"
#include "com_example_audiotest_module_VADWrapper.h"
#include <pthread.h>

#define LOG_TAG "VADWrapper"

void vadDestroy();
int vadSendSideProcess(jint sampleRate, jshort *audio_frame_array, jint frameLength);
int vadRecvSideProcess(jint sampleRate, jshort *audio_frame_array, jint frameLength);

VadInst *mVadInstSend = NULL;	//VAD for send side
VadInst *mVadInstRecv = NULL;	//VAD for recv side
int VAD_PROC_SAMPLE = 160;
pthread_mutex_t vad_send_process_lock;
pthread_mutex_t vad_recv_process_lock;

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_VADWrapper_vadInit
  (JNIEnv *env, jobject thiz)
{

	if(NULL != mVadInstSend){
		vadDestroy();
	}

	if(0 != pthread_mutex_init(&vad_send_process_lock, NULL))
	{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for vad_process_lock failed." );
		return -1;
	}
	if(0 != pthread_mutex_init(&vad_recv_process_lock, NULL))
	{
		pthread_mutex_destroy(&vad_send_process_lock);
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for vad_recv_process_lock failed." );
		return -1;
	}

	int result = -1;
	result = WebRtcVad_Create(&mVadInstSend);
	if(0 == result){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Create success, result=%d", result);
		result = WebRtcVad_Init(mVadInstSend);
		if(0 == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Init success, result=%d", result);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Init failed, result=%d", result);
			vadDestroy();
			pthread_mutex_destroy(&vad_recv_process_lock);
			return result;
		}
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Create failed, result=%d", result);
		mVadInstSend = NULL;
		pthread_mutex_destroy(&vad_send_process_lock);
		pthread_mutex_destroy(&vad_recv_process_lock);
		return result;
	}

	result = WebRtcVad_Create(&mVadInstRecv);
	if(0 == result){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Create recv side success, result=%d", result);
		result = WebRtcVad_Init(mVadInstRecv);
		if(0 == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Init recv side success, result=%d", result);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Init recv side failed, result=%d", result);
			vadDestroy();
			return result;
		}
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Create recv failed, result=%d", result);
		vadDestroy();
		return result;
	}

	return result;
}

/*
 * Class:     Java_com_example_audiotest_module_OpusWrapper
 * Method:    vadDestroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_example_audiotest_module_VADWrapper_vadDestroy
  (JNIEnv *env, jobject thiz)
{
	vadDestroy();
}

/*
 * Class:     Java_com_example_audiotest_module_OpusWrapper
 * Method:    vadProcess
 * Signature: (I[SI)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_VADWrapper_vadProcess
  (JNIEnv *env, jobject thiz, jint sampleRate, jshortArray audioFrame, jint frameLength, jint side)
{
	jshort* audio_frame_array = (*env)->GetShortArrayElements(env, audioFrame, NULL);

	int result = -1;
	if(com_example_audiotest_module_VADWrapper_VAD_SEND_SIDE == side){
		result = vadSendSideProcess(sampleRate, audio_frame_array, frameLength);
	}
	else if(com_example_audiotest_module_VADWrapper_VAD_RECV_SIZE == side){
		result = vadRecvSideProcess(sampleRate, audio_frame_array, frameLength);
	}

	(*env)->ReleaseShortArrayElements(env, audioFrame, audio_frame_array, 0);

	return result;
}

int vadRecvSideProcess(jint sampleRate, jshort *audio_frame_array, jint frameLength){

	int result = -1;
	if(NULL != mVadInstRecv){
		pthread_mutex_lock(&vad_recv_process_lock);
		int index = 0;
		int isVoice = -1;
		int maxLength = frameLength - VAD_PROC_SAMPLE + 1;
		for(index = 0; index < maxLength; index += VAD_PROC_SAMPLE){
			int isValid = WebRtcVad_ValidRateAndFrameLength(sampleRate, VAD_PROC_SAMPLE);
			if(0 != isValid){
				__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
						"WebRtcVad_ValidRateAndFrameLength failed, result=%d. SampleRate=%d, frameLength=%d",
						result, sampleRate, VAD_PROC_SAMPLE);
				break;
			}

			int voiceResult = WebRtcVad_Process(mVadInstRecv, sampleRate, audio_frame_array+VAD_PROC_SAMPLE, VAD_PROC_SAMPLE);
			if(1 == voiceResult){
//				__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Process recv side success, Vad value=%d", voiceResult);
				isVoice = 1;
			}
			else if(0 == voiceResult){
				if(1 != voiceResult){
					isVoice = 0;
				}
//				__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
//						"WebRtcVad_Process failed, result=%d. SampleRate=%d, frameLength=%d",
//						voiceResult, sampleRate, frameLength);
			}
		}

		if(1 == isVoice){
			result = 1;
		}
		else if(0 == isVoice){
			result = 0;
		}
		pthread_mutex_unlock(&vad_recv_process_lock);
	}

	return result;
}

int vadSendSideProcess(jint sampleRate, jshort *audio_frame_array, jint frameLength){

	int result = -1;
	if(NULL != mVadInstSend){
		pthread_mutex_lock(&vad_send_process_lock);
		int index = 0;
		int isVoice = -1;
		int maxLength = frameLength - VAD_PROC_SAMPLE + 1;
		for(index = 0; index < maxLength; index += VAD_PROC_SAMPLE){
			int isValid = WebRtcVad_ValidRateAndFrameLength(sampleRate, VAD_PROC_SAMPLE);
			if(0 != isValid){
				__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
						"WebRtcVad_ValidRateAndFrameLength failed, result=%d. SampleRate=%d, frameLength=%d",
						result, sampleRate, VAD_PROC_SAMPLE);
				break;
			}

			int voiceResult = WebRtcVad_Process(mVadInstSend, sampleRate, audio_frame_array+VAD_PROC_SAMPLE, VAD_PROC_SAMPLE);
			if(1 == voiceResult){
//				__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "WebRtcVad_Process success, Vad value=%d", voiceResult);
				isVoice = 1;
			}
			else if(0 == voiceResult){
				if(1 != voiceResult){
					isVoice = 0;
				}
//				__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
//						"WebRtcVad_Process failed, result=%d. SampleRate=%d, frameLength=%d",
//						voiceResult, sampleRate, frameLength);
			}
		}

		if(1 == isVoice){
			result = 1;
		}
		else if(0 == isVoice){
			result = 0;
		}
		pthread_mutex_unlock(&vad_send_process_lock);
	}

	return result;
}

void vadDestroy()
{
	if(NULL != mVadInstSend){
		pthread_mutex_lock(&vad_send_process_lock);
		WebRtcVad_Free(mVadInstSend);
		mVadInstSend = NULL;
		pthread_mutex_unlock(&vad_send_process_lock);
		pthread_mutex_destroy(&vad_send_process_lock);
	}

	if(NULL != mVadInstRecv){
		pthread_mutex_lock(&vad_recv_process_lock);
		WebRtcVad_Free(mVadInstRecv);
		mVadInstRecv = NULL;
		pthread_mutex_unlock(&vad_recv_process_lock);
		pthread_mutex_destroy(&vad_recv_process_lock);
	}
}
