#include <jni.h>
#include "eve_lang.hpp"
#include "eve_expanda.hpp"

#include <new>
#include <string>

using namespace eve;

static std::string jstr(JNIEnv* env, jstring s) {
    if (s == nullptr) return {};
    const char* c = env->GetStringUTFChars(s, nullptr);
    if (c == nullptr) return {};
    std::string out(c);
    env->ReleaseStringUTFChars(s, c);
    return out;
}

static Program* asProgram(jlong h) {
    return reinterpret_cast<Program*>(h);
}

extern "C" {

JNIEXPORT jlong JNICALL Java_teacommontea_eve_Eve_nParse(JNIEnv* env, jclass, jstring source) {
    try {

        Program* p = new Program(ExpandaEVE::parse(jstr(env, source)));
        return reinterpret_cast<jlong>(p);
    } catch (...) {
        return 0;
    }
}

JNIEXPORT void JNICALL Java_teacommontea_eve_Eve_nFree(JNIEnv*, jclass, jlong handle) {
    delete asProgram(handle);
}

JNIEXPORT jint JNICALL Java_teacommontea_eve_Eve_nRuleCount(JNIEnv*, jclass, jlong handle) {
    Program* p = asProgram(handle);
    if (!p) return 0;
    return static_cast<jint>(p->rules.size());
}

JNIEXPORT jstring JNICALL Java_teacommontea_eve_Eve_nRuleName(JNIEnv* env, jclass, jlong handle, jint i) {
    Program* p = asProgram(handle);
    if (!p || i < 0 || i >= (jint)p->rules.size()) return nullptr;
    return env->NewStringUTF(p->rules[i].name.c_str());
}

JNIEXPORT jstring JNICALL Java_teacommontea_eve_Eve_nRuleRealm(JNIEnv* env, jclass, jlong handle, jint i) {
    Program* p = asProgram(handle);
    if (!p || i < 0 || i >= (jint)p->rules.size()) return nullptr;
    return env->NewStringUTF(p->rules[i].realm.c_str());
}

JNIEXPORT jboolean JNICALL Java_teacommontea_eve_Eve_nMatches(
        JNIEnv* env, jclass, jlong handle, jint rule, jstring candidate, jstring fullLine) {
    try {
        Program* p = asProgram(handle);
        if (!p || rule < 0 || rule >= (jint)p->rules.size()) return JNI_FALSE;
        std::string cand = jstr(env, candidate);
        std::string full = jstr(env, fullLine);
        return p->rules[rule].compiled.matches(cand, full) ? JNI_TRUE : JNI_FALSE;
    } catch (...) {
        return JNI_FALSE;
    }
}

JNIEXPORT jint JNICALL Java_teacommontea_eve_Eve_nFirstMatch(
        JNIEnv* env, jclass, jlong handle, jstring candidate, jstring fullLine) {
    try {
        Program* p = asProgram(handle);
        if (!p) return -1;
        std::string cand = jstr(env, candidate);
        std::string full = jstr(env, fullLine);
        for (size_t i = 0; i < p->rules.size(); i++) {
            if (p->rules[i].compiled.matches(cand, full)) return static_cast<jint>(i);
        }
        return -1;
    } catch (...) {
        return -1;
    }
}

JNIEXPORT jstring JNICALL Java_teacommontea_eve_Eve_nScan(
        JNIEnv* env, jclass, jlong handle, jstring candidate, jstring fullLine) {
    try {
        Program* p = asProgram(handle);
        if (!p) return env->NewStringUTF("");
        std::string cand = jstr(env, candidate);
        std::string full = jstr(env, fullLine);

        static const size_t MAX_CAND = 64;
        if (cand.size() > MAX_CAND) {
            return env->NewStringUTF("");
        }

        std::string out;
        for (size_t i = 0; i < p->rules.size(); i++) {
            const Rule& r = p->rules[i];
            std::string flagsField, capField;
            bool hit = false;
            if (r.kind == Rule::Kind::Strict) {
                std::vector<Capture> caps;
                if (!r.strict.matches(cand, caps)) continue;
                hit = true;
                bool firstc = true;
                for (const auto& c : caps) {
                    if (!firstc) capField += '\x1f';
                    firstc = false;
                    capField += c.name;
                    capField += '\x1e';
                    capField += c.value;
                }
            } else {
                if (!r.compiled.matches(cand, full)) continue;
                hit = true;
                bool first = true;
                for (const auto& f : r.compiled.flags()) {
                    if (!first) flagsField += ' ';
                    first = false;
                    flagsField += f.first;
                    flagsField += '=';
                    flagsField += (f.second ? '1' : '0');
                }
            }
            if (!hit) continue;
            if (!out.empty()) out += '\n';
            out += std::to_string(i);
            out += '\t'; out += r.name;
            out += '\t'; out += r.realm;
            out += '\t'; out += flagsField;
            out += '\t'; out += capField;
        }
        return env->NewStringUTF(out.c_str());
    } catch (...) {
        return env->NewStringUTF("");
    }
}

JNIEXPORT jstring JNICALL Java_teacommontea_eve_Eve_nFlags(JNIEnv* env, jclass, jlong handle, jint rule) {
    try {
        Program* p = asProgram(handle);
        if (!p || rule < 0 || rule >= (jint)p->rules.size()) return env->NewStringUTF("");
        std::string out;
        for (const auto& f : p->rules[rule].compiled.flags()) {
            if (!out.empty()) out += ' ';
            out += f.first;
            out += '=';
            out += (f.second ? '1' : '0');
        }
        return env->NewStringUTF(out.c_str());
    } catch (...) {
        return env->NewStringUTF("");
    }
}

JNIEXPORT jstring JNICALL Java_teacommontea_eve_Eve_nConcreteWords(JNIEnv* env, jclass, jlong handle) {
    try {
        Program* p = asProgram(handle);
        if (!p) return env->NewStringUTF("");
        std::string out;
        for (const Rule& r : p->rules) {
            if (r.kind != Rule::Kind::Fuzzy) continue;
            std::string flagstr;
            bool first = true;
            for (const auto& f : r.compiled.flags()) {
                if (!first) flagstr += ' ';
                first = false;
                flagstr += f.first; flagstr += '='; flagstr += (f.second ? '1' : '0');
            }
            for (const std::string& w : r.compiled.concreteWords()) {
                out += w;
                out += '\t';
                out += flagstr;
                out += '\n';
            }
        }
        return env->NewStringUTF(out.c_str());
    } catch (...) {
        return env->NewStringUTF("");
    }
}

}
