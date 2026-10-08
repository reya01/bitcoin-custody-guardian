// Minimal JNI bridge: load a GGUF, run one greedy completion.
// Android-only build (target arm64-v8a). Not part of upstream llama.cpp.
#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>

#include "llama.h"

namespace {

struct Session {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    const llama_vocab * vocab = nullptr;
    llama_sampler * sampler = nullptr;
};

Session g_sess;
std::mutex g_mutex;

std::string to_std(JNIEnv * env, jstring js) {
    if (!js) return "";
    const char * p = env->GetStringUTFChars(js, nullptr);
    std::string s = p ? p : "";
    env->ReleaseStringUTFChars(js, p);
    return s;
}

std::string token_to_str(const llama_vocab * vocab, llama_token t) {
    char buf[256];
    int n = llama_token_to_piece(vocab, t, buf, sizeof(buf), 0, true);
    if (n < 0) return "";
    return std::string(buf, buf + n);
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_custodyguardian_LlamaBridge_loadModel(JNIEnv * env, jobject, jstring jpath, jint n_ctx) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_sess.model) return JNI_TRUE; // already loaded

    llama_log_set([](ggml_log_level level, const char * text, void *) {
        if (level >= GGML_LOG_LEVEL_ERROR) __android_log_print(ANDROID_LOG_ERROR, "btcg-llama", "%s", text);
    }, nullptr);

    auto mp = llama_model_default_params();
    mp.n_gpu_layers = 0; // CPU only
    g_sess.model = llama_model_load_from_file(to_std(env, jpath).c_str(), mp);
    if (!g_sess.model) return JNI_FALSE;

    auto cp = llama_context_default_params();
    cp.n_ctx = (uint32_t) n_ctx;
    cp.n_batch = 256;
    cp.n_threads = 4;
    cp.n_threads_batch = 4;
    g_sess.ctx = llama_init_from_model(g_sess.model, cp);
    if (!g_sess.ctx) {
        llama_model_free(g_sess.model);
        g_sess.model = nullptr;
        return JNI_FALSE;
    }
    g_sess.vocab = llama_model_get_vocab(g_sess.model);
    g_sess.sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(g_sess.sampler, llama_sampler_init_greedy());
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_custodyguardian_LlamaBridge_complete(JNIEnv * env, jobject, jstring jprompt, jint max_tokens) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_sess.ctx || !g_sess.vocab) {
        return env->NewStringUTF("");
    }
    std::string prompt = to_std(env, jprompt);

    llama_memory_clear(llama_get_memory(g_sess.ctx), false);

    int n_prompt = llama_tokenize(g_sess.vocab, prompt.c_str(), (int32_t) prompt.size(), nullptr, 0, false, true);
    std::vector<llama_token> toks((size_t) std::max(n_prompt, 0));
    llama_tokenize(g_sess.vocab, prompt.c_str(), (int32_t) prompt.size(), toks.data(), (int32_t) toks.size(), false, true);
    if (toks.empty()) return env->NewStringUTF("");

    llama_batch batch = llama_batch_get_one(toks.data(), (int32_t) toks.size());
    std::string out;
    for (int i = 0; i < max_tokens; i++) {
        if (llama_decode(g_sess.ctx, batch)) break;
        llama_token t = llama_sampler_sample(g_sess.sampler, g_sess.ctx, -1);
        if (llama_vocab_is_eog(g_sess.vocab, t)) break;
        out += token_to_str(g_sess.vocab, t);
        batch = llama_batch_get_one(&t, 1);
    }
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_custodyguardian_LlamaBridge_unload(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_sess.sampler) llama_sampler_free(g_sess.sampler);
    if (g_sess.ctx) llama_free(g_sess.ctx);
    if (g_sess.model) llama_model_free(g_sess.model);
    g_sess = Session{};
}
