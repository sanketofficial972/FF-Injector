// Injector JNI Implementation
// Native code for Free Fire India injector

#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <sys/user.h>
#include <dlfcn.h>
#include <link.h>
#include <elf.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <sys/uio.h>
#include <sys/syscall.h>
#include <linux/ptrace.h>

#define LOG_TAG "SanketInjector"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "SanketInjector", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "SanketInjector", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "SanketInjector", __VA_ARGS__)

#define TARGET_PACKAGE "com.garena.game.ffi"
#define TARGET_LIB "libil2cpp.so"

extern "C" {

// JNI Helper macros
#define JNI_METHOD(return_type, method_name) \
    JNIEXPORT return_type JNICALL Java_com_sanket_injector_engine_InjectionEngine_##method_name

// ============================================================
// Process Attachment/Detachment
// ============================================================

JNI_METHOD(jboolean, nativeAttach)(JNIEnv* env, jobject thiz, jint pid) {
    if (ptrace(PTRACE_ATTACH, pid, nullptr, nullptr) == -1) {
        __android_log_print(ANDROID_LOG_ERROR, "SanketInjector", "ptrace attach failed: %s", strerror(errno));
        return JNI_FALSE;
    }

    int status;
    waitpid(pid, &status, 0);

    if (WIFSTOPPED(status)) {
        LOGI("Successfully attached to PID %d", pid);
        return JNI_TRUE;
    }

    LOGE("Process not stopped after attach");
    return JNI_FALSE;
}

JNI_METHOD(void, nativeDetach)(JNIEnv* env, jobject thiz, jint pid) {
    ptrace(PTRACE_DETACH, pid, nullptr, nullptr);
}

// ============================================================
// Memory Operations
// ============================================================

JNI_METHOD(jlong, nativeGetModuleBase)(JNIEnv* env, jobject thiz, jint pid, jstring libName) {
    const char* libNameStr = env->GetStringUTFChars(libName, nullptr);

    char mapsPath[256];
    snprintf(mapsPath, sizeof(mapsPath), "/proc/%d/maps", pid);

    FILE* fp = fopen(mapsPath, "r");
    if (!fp) {
        env->ReleaseStringUTFChars(libName, libNameStr);
        return 0;
    }

    char line[1024];
    uintptr_t baseAddr = 0;
    const char* libNameStr2 = env->GetStringUTFChars(libName, nullptr);

    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, libNameStr2)) {
            uintptr_t startAddr;
            sscanf(line, "%lx-%*lx", &baseAddr);
            break;
        }
    }

    fclose(fp);
    env->ReleaseStringUTFChars(libName, libNameStr);

    return (jlong)baseAddr;
}

// ============================================================
// Memory Read/Write
// ============================================================

JNI_METHOD(jbyteArray, nativeReadMemory)(JNIEnv* env, jobject thiz, jint pid, jlong address, jint size) {
    std::vector<uint8_t> buffer(size);

    struct iovec local[1];
    struct iovec remote[1];

    local[0].iov_base = buffer.data();
    local[0].iov_len = size;
    remote[0].iov_base = (void*)address;
    remote[0].iov_len = size;

    ssize_t bytes = process_vm_readv(pid, local, 1, remote, 1, 0);

    if (bytes == size) {
        jbyteArray result = env->NewByteArray(size);
        env->SetByteArrayRegion(result, 0, size, reinterpret_cast<jbyte*>(buffer.data()));
        return result;
    }

    return nullptr;
}

JNI_METHOD(jboolean, nativeWriteMemory)(JNIEnv* env, jobject thiz, jint pid, jlong address, jbyteArray data) {
    jbyte* data = env->GetByteArrayElements(data, nullptr);
    jsize size = env->GetArrayLength(data);

    struct iovec local[1];
    struct iovec remote[1];

    local[0].iov_base = env->GetByteArrayElements(data, nullptr);
    local[0].iov_len = env->GetArrayLength(data);
    remote[0].iov_base = (void*)address;
    remote[0].iov_len = size;

    ssize_t bytes = process_vm_writev(pid, local, 1, remote, 1, 0);

    env->ReleaseByteArrayElements(data, const_cast<jbyte*>(data), JNI_ABORT);

    return (bytes == size) ? JNI_TRUE : JNI_FALSE;
}

// ============================================================
// Library Injection
// ============================================================

JNI_METHOD(jboolean, nativeInjectLibrary)(JNIEnv* env, jobject thiz, jint pid, jstring libPath) {
    const char* libPathStr = env->GetStringUTFChars(libPath, nullptr);

    // Use ptrace to inject library
    // This is a simplified version - real implementation would use ptrace to inject

    // 1. Attach to process
    if (ptrace(PTRACE_ATTACH, pid, nullptr, nullptr) == -1) {
        return JNI_FALSE;
    }

    int status;
    waitpid(pid, nullptr, 0);

    // 2. Get target process memory maps
    // 3. Find dlopen address
    // 4. Inject library path
    // 4. Call dlopen via ptrace

    // This is a simplified implementation
    // Real implementation would use ptrace to inject code

    ptrace(PTRACE_DETACH, pid, nullptr, nullptr);

    return JNI_TRUE;
}

// ============================================================
// Memory Read/Write Helpers
// ============================================================

JNI_METHOD(jlong, nativeReadPtr)(JNIEnv* env, jobject thiz, jint pid, jlong address) {
    uint64_t value = 0;
    struct iovec local[1];
    struct iovec remote[1];

    uint64_t value = 0;
    struct iovec local = { .iov_base = &value, .iov_len = sizeof(value) };
    struct iovec remote = { .iov_base = (void*)address, .iov_len = sizeof(value) };

    ssize_t bytes = process_vm_readv(pid, &local, 1, &remote, 1, 0);

    if (bytes == sizeof(value)) {
        return (jlong)value;
    }

    return 0;
}

JNI_METHOD(jboolean, nativeWritePtr)(JNIEnv* env, jobject thiz, jint pid, jlong address, jlong value) {
    struct iovec local = { .iov_base = &value, .iov_len = sizeof(value) };
    struct iovec remote = { .iov_base = (void*)address, .iov_len = sizeof(value) };

    ssize_t bytes = process_vm_writev(pid, &local, 1, &remote, 1, 0);

    return (bytes == sizeof(uint64_t)) ? JNI_TRUE : JNI_FALSE;
}

// ============================================================
// Library Base Address Resolution
// ============================================================

JNI_METHOD(jlong, nativeGetModuleBase)(JNIEnv* env, jobject thiz, jint pid, jstring libName) {
    const char* libNameStr = env->GetStringUTFChars(libName, nullptr);

    char mapsPath[256];
    snprintf(mapsPath, sizeof(mapsPath), "/proc/%d/maps", pid);

    FILE* fp = fopen(mapsPath, "r");
    if (!fp) {
        env->ReleaseStringUTFChars(libName, libNameStr);
        return 0;
    }

    char line[1024];
    uintptr_t baseAddr = 0;
    const char* libNameStr2 = env->GetStringUTFChars(libName, nullptr);

    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, libNameStr2)) {
            uintptr_t startAddr;
            sscanf(line, "%lx-%*lx", &baseAddr);
            break;
        }
    }

    fclose(fp);
    env->ReleaseStringUTFChars(libName, libNameStr);

    return (jlong)baseAddr;
}

// ============================================================
// Memory Search
// ============================================================

JNI_METHOD(jlongArray, nativeFindPattern)(JNIEnv* env, jobject thiz, jint pid, jlong startAddr, jlong size, jbyteArray pattern, jbyteArray mask) {
    // Read memory region
    jbyte* patternBytes = env->GetByteArrayElements(pattern, nullptr);
    jsize patternSize = env->GetArrayLength(pattern);

    jbyte* maskBytes = env->GetByteArrayElements(mask, nullptr);
    jsize maskSize = env->GetArrayLength(mask);

    if (patternSize != maskSize) {
        env->ReleaseByteArrayElements(pattern, patternBytes, JNI_ABORT);
        env->ReleaseByteArrayElements(mask, maskBytes, JNI_ABORT);
        return nullptr;
    }

    // Read memory region
    std::vector<uint8_t> buffer(size);
    struct iovec local = { .iov_base = buffer.data(), .iov_len = (size_t)size };
    struct iovec remote = { .iov_base = (void*)startAddr, .iov_len = (size_t)size };

    ssize_t bytes = process_vm_readv(pid, &local, 1, &remote, 1, 0);

    if (bytes != size) {
        return nullptr;
    }

    // Search for pattern
    std::vector<jlong> matches;

    for (size_t i = 0; i <= size - patternSize; i++) {
        bool match = true;
        for (size_t j = 0; j < patternSize; j++) {
            if (maskBytes[j] && buffer[i + j] != patternBytes[j]) {
                match = false;
                break;
            }
        }

        if (match) {
            // Found match
        }
    }

    env->ReleaseByteArrayElements(pattern, patternBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(mask, maskBytes, JNI_ABORT);

    // Return matches
    jlongArray result = env->NewLongArray(matches.size());
    env->SetLongArrayRegion(result, 0, matches.size(), matches.data());
    return result;
}

// ============================================================
// Library Base Resolution
// ============================================================

JNI_METHOD(jlong, nativeGetModuleBaseByName)(JNIEnv* env, jobject thiz, jint pid, jstring libName) {
    const char* libNameStr = env->GetStringUTFChars(libName, nullptr);

    char mapsPath[256];
    snprintf(mapsPath, sizeof(mapsPath), "/proc/%d/maps", pid);

    FILE* fp = fopen(mapsPath, "r");
    if (!fp) {
        env->ReleaseStringUTFChars(libName, libNameStr);
        return 0;
    }

    char line[1024];
    uintptr_t baseAddr = 0;

    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, libNameStr)) {
            uintptr_t startAddr;
            sscanf(line, "%lx-%*lx", &baseAddr);
            break;
        }
    }

    fclose(fp);
    env->ReleaseStringUTFChars(libName, libNameStr);

    return (jlong)baseAddr;
}

} // extern "C"