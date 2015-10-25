#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <stdlib.h>
#include <stdbool.h>
#include <sys/time.h>
#include <pthread.h>
#include <unistd.h>
#include "opus/opus.h"
#include "opus/opus_types.h"
#include "com_example_audiotest_module_OpusWrapper.h"
#include "CodecErrorCode.h"

#define LOG_TAG "OPUS"

int init_opus_encoder(int sampling_rate, int channels, int application, bool force);
int init_opus_decoder(int sampling_rate, int channels, bool force);
int opus_decode_data(unsigned char *in, int in_size, opus_int16 *out, int out_size, int inbandFEC);
int opus_encode_data(opus_int16 *in, int in_size, unsigned char *out, int out_size);
int opus_adjust_bitrate(int new_bitrate);
void destroy_decoder();
void destroy_encoder();
void init_opus_application(int application_type);
void print_enc_default_value();
void print_dec_default_value();
long long current_timestamp();
int opus_set_lsb_depth(int depth);

static int gSampleRate = 48000;
static int gBitrate = 48000;
static int gChannels = 2;
static int gComplexity = 0;
static int gSignalType = OPUS_AUTO;
static int gApplication = OPUS_APPLICATION_VOIP;
static int gBandwidth = OPUS_BANDWIDTH_FULLBAND;

OpusEncoder *mOpusEncoder = NULL;
OpusDecoder *mOpusDecoder = NULL;
int mEncAvgTime = 0;
int mDecAvgTime = 0;
pthread_mutex_t opus_dec_lock;
pthread_mutex_t opus_enc_lock;

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusInit(
		JNIEnv *env, jobject thiz, jint sampling_rate, jint channels,
		jint bitrate, jint application_type, jint codec_type){
	int result = 0;
	gSampleRate = sampling_rate;
	gChannels = channels;
	gBitrate = bitrate;

	mEncAvgTime = 0;
	mDecAvgTime = 0;

	init_opus_application(application_type);

	if(com_example_audiotest_module_OpusWrapper_OPUS_TYPE_ENCODER == codec_type){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"opusInit Encoder with sampling_rate=%d, channels=%d, application=%d", sampling_rate, channels, gApplication);

		if (pthread_mutex_init(&opus_enc_lock, NULL) != 0)
		{
			//printf("\n mutex init failed\n");
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for encoder failed." );
			return -1;
		}

		pthread_mutex_lock(&opus_enc_lock);
		result = init_opus_encoder(sampling_rate, channels, gApplication, false);
		pthread_mutex_unlock(&opus_enc_lock);
	}
	if(com_example_audiotest_module_OpusWrapper_OPUS_TYPE_DECODER == codec_type){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"opusInit Decoder with sampling_rate=%d, channels=%d, application=%d", sampling_rate, channels, gApplication);

		if (pthread_mutex_init(&opus_dec_lock, NULL) != 0)
		{
			//printf("\n mutex init failed\n");
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "create mutex for decoder failed." );
			return -1;
		}
		pthread_mutex_lock(&opus_dec_lock);
		result = init_opus_decoder(sampling_rate, channels, false);
		pthread_mutex_unlock(&opus_dec_lock);
	}

	if(result < 0){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
			"opusInit failed:%d with sampling_rate=%d, channels=%d, application=%d, codec_type=%d",
			result, sampling_rate, channels, gApplication, codec_type);
	}

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusDestroy(
		JNIEnv *env, jobject thiz, jint codec_type){
	int result = 0;

	if(com_example_audiotest_module_OpusWrapper_OPUS_TYPE_ENCODER == codec_type){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "destroyOpus encoder");
		pthread_mutex_lock(&opus_enc_lock);
		destroy_encoder();
		pthread_mutex_unlock(&opus_enc_lock);
		pthread_mutex_destroy(&opus_enc_lock);
	}
	else if(com_example_audiotest_module_OpusWrapper_OPUS_TYPE_DECODER == codec_type){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "destroyOpus decoder");
		pthread_mutex_lock(&opus_dec_lock);
		destroy_decoder();
		pthread_mutex_unlock(&opus_dec_lock);
		pthread_mutex_destroy(&opus_dec_lock);
	}

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusEncode(
		JNIEnv *env, jobject thiz, jshortArray input, jint input_buf_size,
		jbyteArray output, jint output_buf_size){

	int result = 0;

	jshort* input_array = (*env)->GetShortArrayElements(env, input, NULL);
	jbyte* output_array = (*env)->GetByteArrayElements(env, output, NULL);

	result = opus_encode_data( input_array, input_buf_size, output_array, output_buf_size );
	//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OpusEncode: encoded data size=%d", result);

	(*env)->ReleaseByteArrayElements(env, output, output_array, 0);
	(*env)->ReleaseShortArrayElements(env, input, input_array, 0);

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusDecode(
		JNIEnv *env, jobject thiz, jbyteArray input, jint input_buf_size,
		jshortArray output, jint output_buf_size, jboolean inband_FEC){

	int result = 0;

	jshort* output_array = (*env)->GetShortArrayElements(env, output, NULL);
	jbyte* input_array = NULL;
	if(NULL != input){
		input_array = (*env)->GetByteArrayElements(env, input, NULL);
	}

	int inbandFEC = 0;
	if(inband_FEC){
		inbandFEC = 1;
	}

	result = opus_decode_data( input_array, input_buf_size, output_array, output_buf_size, inbandFEC);
	//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
	//		"OpusDecode: decoded data size=%d, input size=%d, output size=%d",
	//		result, input_buf_size, output_buf_size);

	(*env)->ReleaseShortArrayElements(env, output, output_array, 0);
	if(NULL != input){
		(*env)->ReleaseByteArrayElements(env, input, input_array, 0);
	}

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusSetComplexity(
		JNIEnv *env, jobject thiz, jint complexity ){
	int err = 0;

	gComplexity = complexity;
	if(NULL != mOpusEncoder){
		err = opus_encoder_ctl(mOpusEncoder, OPUS_SET_COMPLEXITY(gComplexity));
		//if(err!=OPUS_OK) test_failed();
		int value = 0;
		opus_encoder_ctl(mOpusEncoder, OPUS_GET_COMPLEXITY(&value));
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Opus set complexity with new complexity: %d", value);
	}
	else{
		err = ERROR_ENCODER_NOT_EXIST;
	}

	return err;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusSetSignalType(
		JNIEnv *env, jobject thiz, jint signal_type){
	int err = 0;
	switch(signal_type){
		case com_example_audiotest_module_OpusWrapper_OPUS_SIGNAL_TYPE_AUTO:
			gSignalType = OPUS_AUTO;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Set OPUS Signal as OPUS_AUTO");
			break;
		case com_example_audiotest_module_OpusWrapper_OPUS_SIGNAL_TYPE_MUSIC:
			gSignalType = OPUS_SIGNAL_MUSIC;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Set OPUS Signal as OPUS_SIGNAL_MUSIC");
			break;
		case com_example_audiotest_module_OpusWrapper_OPUS_SIGNAL_TYPE_VOICE:
			gSignalType = OPUS_SIGNAL_VOICE;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Set OPUS Signal as OPUS_SIGNAL_VOICE");
			break;
		default:
			break;
	}

	if(NULL != mOpusEncoder){
		err = opus_encoder_ctl(mOpusEncoder, OPUS_SET_SIGNAL(gSignalType));
		//if(err!=OPUS_OK) test_failed();
		int value = 0;
		opus_encoder_ctl(mOpusEncoder, OPUS_GET_SIGNAL(&value));
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Opus set signal type with new signal type: %d", value);
	}
	else{
		err = ERROR_ENCODER_NOT_EXIST;
	}

	return err;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusSetBandwidth(
		JNIEnv *env, jobject thiz, jint bandwidth_type){

	int err = 0;

	switch(bandwidth_type){
		case com_example_audiotest_module_OpusWrapper_OPUS_BANDWIDTH_NARROWBAND:
			gBandwidth = OPUS_BANDWIDTH_NARROWBAND;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Set OPUS Bandwidth as OPUS_BANDWIDTH_NARROWBAND");
			break;
		case com_example_audiotest_module_OpusWrapper_OPUS_BANDWIDTH_MEDIUMBAND:
			gBandwidth = OPUS_BANDWIDTH_MEDIUMBAND;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Set OPUS Bandwidth as OPUS_BANDWIDTH_MEDIUMBAND");
			break;
		case com_example_audiotest_module_OpusWrapper_OPUS_BANDWIDTH_WIDEBAND:
			gBandwidth = OPUS_BANDWIDTH_WIDEBAND;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Set OPUS Bandwidth as OPUS_BANDWIDTH_WIDEBAND");
			break;
		case com_example_audiotest_module_OpusWrapper_OPUS_BANDWIDTH_SUPERWIDEBAND:
			gBandwidth = OPUS_BANDWIDTH_SUPERWIDEBAND;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Set OPUS Bandwidth as OPUS_BANDWIDTH_SUPERWIDEBAND");
			break;
		case com_example_audiotest_module_OpusWrapper_OPUS_BANDWIDTH_FULLBAND:
			gBandwidth = OPUS_BANDWIDTH_FULLBAND;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Set OPUS Bandwidth as OPUS_BANDWIDTH_FULLBAND");
			break;
		default:
			break;
	}

	if(NULL != mOpusEncoder){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "gBandwidth=%d", gBandwidth);
		//err = opus_encoder_ctl(mOpusEncoder, OPUS_SET_BANDWIDTH(OPUS_BANDWIDTH_NARROWBAND));
		err = opus_encoder_ctl(mOpusEncoder, OPUS_SET_MAX_BANDWIDTH(gBandwidth));
		//if(err!=OPUS_OK) test_failed();
		int value = 0;
		int result = opus_encoder_ctl(mOpusEncoder, OPUS_GET_MAX_BANDWIDTH(&value));
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_MAX_BANDWIDTH: %d, result=%d, err=%d", value, result, err);
	}
	else{
		err = ERROR_ENCODER_NOT_EXIST;
	}

	return err;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusResetSampleRate(
		JNIEnv *env, jobject thiz, jint new_sample_rate, jint codec_type){
	int result = 0;
	if(com_example_audiotest_module_OpusWrapper_OPUS_TYPE_ENCODER == codec_type){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"opusResetSampleRate encoder with new_sample_rate:%d", new_sample_rate);
		pthread_mutex_lock(&opus_enc_lock);
		destroy_encoder();
		result = init_opus_encoder(new_sample_rate, gChannels, gApplication, true);
		pthread_mutex_unlock(&opus_enc_lock);
	}
	if(com_example_audiotest_module_OpusWrapper_OPUS_TYPE_DECODER == codec_type){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"opusResetSampleRate decoder with new_sample_rate:%d", new_sample_rate);
		pthread_mutex_lock(&opus_dec_lock);
		destroy_decoder();
		result = init_opus_decoder(new_sample_rate, gChannels, true);
		pthread_mutex_unlock(&opus_dec_lock);
	}

	if(result < 0){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"opusResetSampleRate failed result:%d", result);
	}
	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusAdjustBitrate(
		JNIEnv *env, jobject thiz, jint new_bitrate){

	return opus_adjust_bitrate(new_bitrate);
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusSetGainValue
  (JNIEnv *env, jobject thiz, jint gainValue)
{
	int result = -1;

	if(NULL != mOpusDecoder){
		int value = gainValue;
		opus_decoder_ctl(mOpusDecoder, OPUS_SET_GAIN(value));

		result = opus_decoder_ctl(mOpusDecoder, OPUS_GET_GAIN(&value));
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "opusSetGainValue new value=%d", value);
	}

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusGetPitchValue
  (JNIEnv *env, jobject thiz)
{
	int result = 0;
	if(NULL != mOpusDecoder){
		int value = 0;
		result = opus_decoder_ctl(mOpusDecoder, OPUS_GET_PITCH(&value));
		if(OPUS_OK == result){
			//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Opus success to GetPitch value=%d", value);
			return value;
		}
		else{
			//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Opus failed to GetPitch value=%d, result=%d", value, result);
		}
	}
	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusSetLSBDepth
  (JNIEnv *env, jobject thiz, jint value)
{
	int result = 0;

	result = opus_set_lsb_depth(value);

	return result;
}

JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusSetDTX
  (JNIEnv *env, jobject thiz, jboolean bEnable)
{
	int result = -1;

	if(NULL != mOpusEncoder){
		int value = 0;
		if(bEnable){
			value = 1;
		}
		result = opus_encoder_ctl(mOpusEncoder, OPUS_SET_DTX(value));
		if(OPUS_OK == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_SET_DTX success with value: %d", value);
			int dtxValue = -1;
			opus_encoder_ctl(mOpusEncoder, OPUS_GET_DTX(&dtxValue));
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_DTX: dtx value: %d", dtxValue);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_SET_DTX failed with value: %d", value);
		}
	}

	return result;
}

/*
 * Class:     Java_com_example_audiotest_module_OpusWrapper
 * Method:    opusInBandFEC
 * Signature: (Z)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusInBandFEC
  (JNIEnv *env, jobject thiz, jboolean bEnable)
{
	int result = -1;

	if(NULL != mOpusEncoder){
		int value = 0;
		if(bEnable){
			value = 1;
		}
		result = opus_encoder_ctl(mOpusEncoder, OPUS_SET_INBAND_FEC(value));
		if(OPUS_OK == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_SET_INBAND_FEC success with value: %d", value);
			int inbandFEC = -1;
			opus_encoder_ctl(mOpusEncoder, OPUS_GET_INBAND_FEC(&inbandFEC));
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_SET_INBAND_FEC: inbandFEC value: %d", inbandFEC);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_SET_INBAND_FEC failed with value: %d", value);
		}
	}

	return result;
}

/*
 * Class:     Java_com_example_audiotest_module_OpusWrapper
 * Method:    opusPacketLostPerc
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_OpusWrapper_opusPacketLostPerc
  (JNIEnv *env, jobject thiz, jint percentage)
{
	int result = -1;
	if(NULL != mOpusEncoder){
		result = opus_encoder_ctl(mOpusEncoder, OPUS_SET_PACKET_LOSS_PERC(percentage));
		if(OPUS_OK == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_SET_PACKET_LOSS_PERC success with value: %d", percentage);
			int newPercentage = -1;
			opus_encoder_ctl(mOpusEncoder, OPUS_GET_PACKET_LOSS_PERC(&newPercentage));
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_PACKET_LOSS_PERC: percentage: %d", newPercentage);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_SET_PACKET_LOSS_PERC failed with value: %d", percentage);
		}
	}
	return result;
}

void init_opus_application(int application_type){
	switch(application_type){
		case com_example_audiotest_module_OpusWrapper_OPUS_APPLICATION_VOIP:
			gApplication = OPUS_APPLICATION_VOIP;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Start OPUS with OPUS_APPLICATION_VOIP");
			break;
		case com_example_audiotest_module_OpusWrapper_OPUS_APPLICATION_AUDIO:
			gApplication = OPUS_APPLICATION_AUDIO;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Start OPUS with OPUS_APPLICATION_AUDIO");
			break;
		case com_example_audiotest_module_OpusWrapper_OPUS_APPLICATION_RESTRICTED_LOWDELAY:
			gApplication = OPUS_APPLICATION_RESTRICTED_LOWDELAY;
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Start OPUS with OPUS_APPLICATION_RESTRICTED_LOWDELAY");
			break;
		default:
			break;
	}
}

int init_opus_encoder(int sampling_rate, int channels, int application, bool force){
	int err = 0;
	if(mOpusEncoder != NULL){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "mOpusEncoder already exist, skip creating new encoder.");
		return err;
	}

	mOpusEncoder = opus_encoder_create(sampling_rate, channels, application, &err);

    if (err != OPUS_OK)
    {
    	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Cannot create encoder: %s\n", opus_strerror(err));
    	return ERROR_CREATE_ENCODER_FAILED;
    }
    else{
    	// setup for various bitrate
    	int use_vbr = 1;
    	opus_encoder_ctl(mOpusEncoder, OPUS_SET_VBR(use_vbr));
    	opus_adjust_bitrate(gBitrate);
    	opus_set_lsb_depth(16);

    	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
    			"Create OPUS encoder success, setup OPUS encoder with sample rate=%d.", sampling_rate);
		print_enc_default_value();


    }
    return err;
}

int init_opus_decoder(int sampling_rate, int channels, bool force){
	int err = 0;

	if(mOpusDecoder != NULL){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"mOpusDecoder already exist, skip creating new decoder.");
		return err;
	}

	mOpusDecoder = opus_decoder_create(sampling_rate, channels, &err);

	if (err != OPUS_OK)
	{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "Cannot create decoder: %s\n", opus_strerror(err));
		return ERROR_CREATE_DECODER_FAILED;
	}
	else{
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"Create OPUS decoder success, setup OPUS decoder with sample rate=%d.", sampling_rate);

		//int value = 1;
		//opus_decoder_ctl(mOpusDecoder, OPUS_SET_GAIN(value));

		print_dec_default_value();
	}

	return err;
}

void print_dec_default_value(){
	int value = 0;
	opus_decoder_ctl(mOpusDecoder, OPUS_GET_GAIN(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_GAIN: %d", value);

}

void print_enc_default_value(){
	//print opus encoder default values

	int value = 0;
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_APPLICATION(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_APPLICATION: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_VBR(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_VBR: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_COMPLEXITY(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_COMPLEXITY: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_BANDWIDTH(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_BANDWIDTH: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_VBR_CONSTRAINT(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_VBR_CONSTRAINT: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_INBAND_FEC(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_INBAND_FEC: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_FORCE_CHANNELS(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_FORCE_CHANNELS: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_DTX(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_DTX: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_PACKET_LOSS_PERC(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_PACKET_LOSS_PERC: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_PREDICTION_DISABLED(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_PREDICTION_DISABLED: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_EXPERT_FRAME_DURATION(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_EXPERT_FRAME_DURATION: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_LSB_DEPTH(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_LSB_DEPTH: %d", value);
	opus_encoder_ctl(mOpusEncoder, OPUS_GET_SIGNAL(&value));
	__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "OPUS_GET_SIGNAL: %d", value);

}

long long current_timestamp() {
    struct timeval te;
    gettimeofday(&te, NULL); // get current time
    long long milliseconds = te.tv_sec*1000LL + te.tv_usec/1000; // caculate milliseconds

    return milliseconds;
}

int opus_encode_data(opus_int16 *in, int in_size, unsigned char *out, int out_size){
	int enc_result = 0;

	if( NULL == mOpusEncoder ){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "mOpusEncoder is null");
		return ERROR_ENCODE_DATA;
	}

	pthread_mutex_lock(&opus_enc_lock);

	//long long enc_start_time = current_timestamp();
	enc_result = opus_encode(mOpusEncoder, in, in_size/gChannels, out, out_size);
	//long long enc_end_time = current_timestamp();
	//mEncAvgTime = (mEncAvgTime + enc_end_time - enc_start_time)/2;
	//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "mEncAvgTime: %d", mEncAvgTime);

	pthread_mutex_unlock(&opus_enc_lock);

	if(enc_result < 0){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"opus_encode_data failed: %d, in_size=%d, out_size=%d.",
				enc_result, in_size, out_size);
		return ERROR_ENCODE_DATA;
	}

	return enc_result;
}

int opus_decode_data(unsigned char *in, int in_size, opus_int16 *out, int out_size, int inbandFEC){
	int dec_result = 0;

	if(NULL == mOpusDecoder){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "mOpusDecoder is null");
		return ERROR_DECODE_DATA;
	}

	pthread_mutex_lock(&opus_dec_lock);

	//long long dec_start_time = current_timestamp();
	dec_result = opus_decode(mOpusDecoder, in, in_size, out, out_size/gChannels, inbandFEC);
	//long long dec_end_time = current_timestamp();
	//mDecAvgTime = (mEncAvgTime + dec_start_time - dec_end_time)/2;
	//__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "mDecAvgTime: %d", mDecAvgTime);

	pthread_mutex_unlock(&opus_dec_lock);

	if(dec_result < 0){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"opus_decode_data failed: %d, in_size=%d, out_size=%d",
				dec_result, in_size, out_size);
		return ERROR_DECODE_DATA;
	}

	return dec_result;
}

int opus_adjust_bitrate(int new_bitrate){
	int result = 0;

	if(NULL != mOpusEncoder){
		gBitrate = new_bitrate;
		opus_encoder_ctl(mOpusEncoder, OPUS_SET_BITRATE(gBitrate));

		opus_encoder_ctl(mOpusEncoder, OPUS_GET_BITRATE(&result));
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
				"opusAdjustBitrate new bitrate: %d", result);
	}

	return result;
}

int opus_set_lsb_depth(int depth){
	int result = 0;
	if((depth < 8) || (depth > 24)){
		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
						"opus_set_lsb_depth failed with invalid value=%d", depth);
		return -1;
	}

	if(NULL != mOpusEncoder){
		result = opus_encoder_ctl(mOpusEncoder, OPUS_SET_LSB_DEPTH(depth));
		if(OPUS_OK == result){
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
							"opus_set_lsb_depth with depth: %d success", depth);
		}
		else{
			__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG,
							"opus_set_lsb_depth with depth: %d failed", depth);
		}
	}
	return result;
}

void destroy_decoder(){
	if(NULL != mOpusDecoder){
		OpusDecoder* tmpDecoder = mOpusDecoder;
		mOpusDecoder = NULL;

		// 50 * 1000 = 100000 us = 50 ms
		usleep(50000);

		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "destroy_decoder");
		opus_decoder_destroy(tmpDecoder);
	}
}

void destroy_encoder(){
	if(NULL != mOpusEncoder){
		OpusEncoder* tmpEncoder = mOpusEncoder;
		mOpusEncoder = NULL;

		// 50 * 1000 = 100000 us = 50 ms
		usleep(50000);

		__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, "destroy_encoder");
		opus_encoder_destroy(tmpEncoder);
	}
}
