LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := injector
LOCAL_SRC_FILES := injector_jni.cpp

LOCAL_CPPFLAGS := -std=c++17 -frtti -fexceptions -O2 -Wall -Wextra
LOCAL_CPPFLAGS += -DLOG_TAG=\"SanketInjector\"
LOCAL_CPPFLAGS += -fPIC -fvisibility=hidden

LOCAL_LDLIBS := -llog -landroid -ldl -lm

LOCAL_ARM_MODE := arm

include $(BUILD_SHARED_LIBRARY)