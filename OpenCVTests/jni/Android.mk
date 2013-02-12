LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include ../../sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := opencv_tests
LOCAL_SRC_FILES := motion_averager.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
