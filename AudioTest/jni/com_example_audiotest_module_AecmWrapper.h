/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_example_audiotest_module_AecmWrapper */

#ifndef _Included_com_example_audiotest_module_AecmWrapper
#define _Included_com_example_audiotest_module_AecmWrapper

#undef com_example_audiotest_module_AecmWrapper_AEC_PROCESS_SAMPLE_COUNT_80
#define com_example_audiotest_module_AecmWrapper_AEC_PROCESS_SAMPLE_COUNT_80 80L
#undef com_example_audiotest_module_AecmWrapper_AEC_PROCESS_SAMPLE_COUNT_160
#define com_example_audiotest_module_AecmWrapper_AEC_PROCESS_SAMPLE_COUNT_160 160L
/*
 * Class:     com_example_audiotest_module_AecmWrapper
 * Method:    aecmInit
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmInit
  (JNIEnv *, jobject, jint);

/*
 * Class:     com_example_audiotest_module_AecmWrapper
 * Method:    aecmDestroy
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmDestroy
  (JNIEnv *, jobject);

/*
 * Class:     com_example_audiotest_module_AecmWrapper
 * Method:    aecmBufferFarend
 * Signature: ([SI)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmBufferFarend
  (JNIEnv *, jobject, jshortArray, jint);

/*
 * Class:     com_example_audiotest_module_AecmWrapper
 * Method:    aecmAECProcess
 * Signature: ([S[SII)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmAECProcess
  (JNIEnv *, jobject, jshortArray, jshortArray, jint, jint);

/*
 * Class:     com_example_audiotest_module_AecmWrapper
 * Method:    aecmAECProcessWithNS
 * Signature: ([S[S[SII)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmAECProcessWithNS
  (JNIEnv *, jobject, jshortArray, jshortArray, jshortArray, jint, jint);

/*
 * Class:     com_example_audiotest_module_AecmWrapper
 * Method:    aecmSetProcessSampleCount
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_example_audiotest_module_AecmWrapper_aecmSetProcessSampleCount
  (JNIEnv *, jobject, jint);


#endif
