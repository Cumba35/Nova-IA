
#include <jni.h>
#include "llama.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nova_ai_MainActivity_nativeEngineVersion(
        JNIEnv *env,
        jobject /* this */) {
    const char *version = llama_version();
    return env->NewStringUTF(
        version != nullptr ? version : "llama.cpp"
    );
}
