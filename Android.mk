# Copyright (c) 2018 HUMAX Co., Ltd. All rights reserved.
# Use of this source code is governed by HUMAX license that can be found in the LICENSE file.

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := HmxSystemUI

LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-Iaidl-files-under, src)

LOCAL_AIDL_INCLUDES := $(call all-Iaidl-files-under, src)

LOCAL_USE_AAPT2 := true

LOCAL_AAPT_FLAGS += -c mdpi,hdpi,xhdpi

LOCAL_JAVA_LIBRARIES += \
	android.car \
	SystemUIPluginLib \

LOCAL_STATIC_ANDROID_LIBRARIES := \
	android-support-v4 \
	android-support-v7-preference \
	android-support-v7-appcompat \
	android-support-v14-preference \
	android-arch-lifecycle-extensions \
	setup-wizard-lib-gingerbread-compat \
	SettingsLib \

LOCAL_STATIC_JAVA_LIBRARIES += jsr305 \
	android.support.car \
	androidx.legacy_legacy-support-v4 \
	android.extension.car
	  
LOCAL_RESOURCE_DIR := \
	$(LOCAL_PATH)/res-statusbar \
	$(LOCAL_PATH)/res-volumedialog \
	$(LOCAL_PATH)/res-notificationui \
	$(LOCAL_PATH)/res-wallpaper

ifneq ($(filter dydl_%, $(TARGET_PRODUCT)),)
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res-droplist-dl3c 
else
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res-droplist
endif 

LOCAL_CERTIFICATE := platform

LOCAL_MODULE_TAGS := optional

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_PRIVILEGED_MODULE := true

LOCAL_DEX_PREOPT := false


include $(BUILD_PACKAGE)
