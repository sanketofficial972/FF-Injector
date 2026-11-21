APP_ABI := arm64-v8a
APP_PLATFORM := android-21
APP_STL := c++_shared
APP_CPPFLAGS := -frtti -fexceptions -std=c++17 -O2
APP_CPPFLAGS += -DLOG_TAG=\"SanketInjector\"
APP_OPTIM := release
APP_CPPFLAGS += -fPIC -fvisibility=hidden