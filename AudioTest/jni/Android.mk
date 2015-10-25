LOCAL_PATH := $(call my-dir)


include $(CLEAR_VARS)

LOCAL_MODULE    := AudioTest
LOCAL_SRC_FILES := AudioTest.cpp

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

#$(info local_path:$(LOCAL_PATH))
LOCAL_MODULE := opus
LOCAL_SRC_FILES := ./opus/lib/libopus.so

include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/opus/include

LOCAL_MODULE    := OpusWrapper
LOCAL_SRC_FILES := OpusWrapper.c

LOCAL_LDLIBS := -llog

LOCAL_LDLIBS += -L$(LOCAL_PATH)/opus/lib/ -lopus -pthread

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := webrtc_aecm
LOCAL_SRC_FILES := \
		aecm/aecm_core_c.c \
        aecm/aecm_core.c \
        aecm/complex_bit_reverse.c \
        aecm/complex_fft.c \
        audio_common/spl_init.c \
        audio_common/cross_correlation.c \
        audio_common/downsample_fast.c \
        audio_common/vector_scaling_operations.c \
        aecm/real_fft.c \
        audio_processing/delay_estimator_wrapper.c \
        audio_processing/delay_estimator.c \
        aecm/division_operations.c \
        aecm/echo_control_mobile.c \
        aecm/min_max_operations.c \
        aecm/randomization_functions.c \
        audio_processing/ring_buffer.c \
        aecm/spl_sqrt_floor.c \
        AecmWrapper.c

LOCAL_LDLIBS := -llog -pthread

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := webrtc_nsx
LOCAL_SRC_FILES := \
		audio_common/complex_bit_reverse.c \
		audio_common/complex_fft.c \
		audio_common/copy_set_operations.c \
		audio_common/cross_correlation.c \
		audio_common/division_operations.c \
		audio_common/downsample_fast.c \
		audio_common/energy.c \
		audio_common/get_scaling_square.c \
		audio_common/min_max_operations.c \
		audio_common/spl_sqrt_floor.c \
		audio_common/spl_init.c \
		audio_common/vector_scaling_operations.c \
		audio_processing/real_fft.c \
		ns/noise_suppression_x.c \
        ns/nsx_core.c \
        ns/nsx_core_c.c \
        NSWrapper.c

LOCAL_LDLIBS := -llog -pthread

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := webrtc_agc
LOCAL_SRC_FILES := \
		audio_common/copy_set_operations.c \
		audio_common/division_operations.c \
		audio_common/dot_product_with_scale.c \
		audio_common/resample_by_2.c \
		audio_common/spl_sqrt.c \
		audio_common/spl_sqrt_floor.c \
        agc/analog_agc.c \
        agc/digital_agc.c \
        AGCWrapper.c

LOCAL_LDLIBS := -llog -pthread
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := webrtc_vad
LOCAL_SRC_FILES := \
		audio_common/complex_bit_reverse.c \
		audio_common/complex_fft.c \
		audio_common/cross_correlation.c \
		audio_common/division_operations.c \
		audio_common/downsample_fast.c \
		audio_common/energy.c \
		audio_common/get_scaling_square.c \
		audio_common/min_max_operations.c \
		audio_common/spl_sqrt.c \
		audio_common/spl_init.c \
		audio_common/resample_48khz.c \
		audio_common/resample_by_2_internal.c \
		audio_common/resample_fractional.c \
		audio_common/vector_scaling_operations.c \
		audio_processing/real_fft.c \
		vad/webrtc_vad.c \
    	vad/vad_core.c \
    	vad/vad_filterbank.c \
    	vad/vad_gmm.c \
    	vad/vad_sp.c \
    	VADWrapper.c

LOCAL_LDLIBS := -llog -pthread -landroid
include $(BUILD_SHARED_LIBRARY)

