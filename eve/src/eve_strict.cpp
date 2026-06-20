#include "eve_strict.hpp"
#include "eve.hpp"

#include <cctype>
#include <functional>
#include <stdexcept>

namespace eve {

static inline bool sDigit(char c) { return c >= '0' && c <= '9'; }
static inline bool sLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
}
static inline bool sWordChar(char c) { return sLetter(c) || sDigit(c) || c == '_'; }
static inline bool sSpace(char c) { return c == ' ' || c == '\t' || c == '\r' || c == '\n'; }
static inline bool sIpChar(char c) { return sDigit(c) || c == '.'; }

static bool classChar(StrictTerm::Kind k, char c) {
    switch (k) {
        case StrictTerm::Kind::Digits:
        case StrictTerm::Kind::Number: return sDigit(c);
        case StrictTerm::Kind::Letters: return sLetter(c);
        case StrictTerm::Kind::Word: return sWordChar(c);
        case StrictTerm::Kind::Ip: return sIpChar(c);
        case StrictTerm::Kind::Spaces: return sSpace(c);
        default: return false;
    }
}
static bool fixedClass(StrictTerm::Kind k, char c) {
    switch (k) {
        case StrictTerm::Kind::Digit: return sDigit(c);
        case StrictTerm::Kind::Letter: return sLetter(c);
        case StrictTerm::Kind::Space: return sSpace(c);
        default: return false;
    }
}

struct Matcher {
    const std::string& in;
    std::vector<Capture>* caps;
    using Cont = std::function<bool(int)>;

    void rec(const StrictTerm& t, int start, int len) {
        if (caps && !t.capture.empty()) caps->push_back({t.capture, in.substr(start, len)});
    }
    void unrec(const StrictTerm& t) {
        if (caps && !t.capture.empty() && !caps->empty()) caps->pop_back();
    }

    bool matchSeq(const std::vector<StrictTerm>& s, size_t si, int pos, const Cont& k) {
        if (si >= s.size()) return k(pos);
        const StrictTerm& t = s[si];
        Cont next = [&, si](int after) { return matchSeq(s, si + 1, after, k); };
        return matchTerm(t, pos, next);
    }

    bool matchTerm(const StrictTerm& t, int pos, const Cont& k) {
        return rep(t, pos, 0, k);
    }

    bool rep(const StrictTerm& t, int pos, int done, const Cont& k) {
        if (t.repMax < 0 || done < t.repMax) {
            Cont more = [&](int after) {
                if (after == pos) return false;
                return rep(t, after, done + 1, k);
            };
            if (once(t, pos, more)) return true;
        }
        if (done >= t.repMin) return k(pos);
        return false;
    }

    bool once(const StrictTerm& t, int pos, const Cont& k) {
        switch (t.kind) {
            case StrictTerm::Kind::Literal: {
                if (pos + (int)t.literal.size() > (int)in.size()) return false;
                if (in.compare(pos, t.literal.size(), t.literal) != 0) return false;
                rec(t, pos, (int)t.literal.size());
                if (k(pos + (int)t.literal.size())) return true;
                unrec(t);
                return false;
            }
            case StrictTerm::Kind::Digit:
            case StrictTerm::Kind::Letter:
            case StrictTerm::Kind::Space: {
                int n = t.count > 0 ? t.count : 1;
                if (pos + n > (int)in.size()) return false;
                for (int i = 0; i < n; i++) if (!fixedClass(t.kind, in[pos + i])) return false;
                rec(t, pos, n);
                if (k(pos + n)) return true;
                unrec(t);
                return false;
            }
            case StrictTerm::Kind::Digits:
            case StrictTerm::Kind::Number:
            case StrictTerm::Kind::Letters:
            case StrictTerm::Kind::Word:
            case StrictTerm::Kind::Ip:
            case StrictTerm::Kind::Spaces: {
                int end = pos;
                while (end < (int)in.size() && classChar(t.kind, in[end])) end++;
                if (end == pos) return false;
                if (t.count > 0) {
                    if (end - pos < t.count) return false;
                    rec(t, pos, t.count);
                    if (k(pos + t.count)) return true;
                    unrec(t);
                    return false;
                }
                for (int take = end; take > pos; take--) {
                    rec(t, pos, take - pos);
                    if (k(take)) return true;
                    unrec(t);
                }
                return false;
            }
            case StrictTerm::Kind::Anything: {
                for (int take = (int)in.size(); take >= pos; take--) {
                    rec(t, pos, take - pos);
                    if (k(take)) return true;
                    unrec(t);
                }
                return false;
            }
            case StrictTerm::Kind::Fuzzy: {

                if (!t.fuzzy) return false;
                int maxEnd = pos;
                while (maxEnd < (int)in.size() && !sSpace(in[maxEnd])) maxEnd++;
                for (int take = maxEnd; take > pos; take--) {
                    std::string span = in.substr(pos, take - pos);
                    if (!t.fuzzy->matches(span)) continue;
                    rec(t, pos, take - pos);
                    if (k(take)) return true;
                    unrec(t);
                }
                return false;
            }
            case StrictTerm::Kind::Group: {
                int start = pos;
                Cont after = [&, start](int end) {
                    if (caps && !t.capture.empty()) caps->push_back({t.capture, in.substr(start, end - start)});
                    if (k(end)) return true;
                    if (caps && !t.capture.empty() && !caps->empty()) caps->pop_back();
                    return false;
                };
                return matchSeq(t.group, 0, pos, after);
            }
            case StrictTerm::Kind::Alt: {
                int start = pos;
                for (const auto& branch : t.alts) {
                    Cont after = [&, start](int end) {
                        if (caps && !t.capture.empty()) caps->push_back({t.capture, in.substr(start, end - start)});
                        if (k(end)) return true;
                        if (caps && !t.capture.empty() && !caps->empty()) caps->pop_back();
                        return false;
                    };
                    if (matchSeq(branch, 0, pos, after)) return true;
                }
                return false;
            }
        }
        return false;
    }
};

bool StrictPattern::matches(const std::string& input) const {
    Matcher m{input, nullptr};
    return m.matchSeq(terms_, 0, 0, [&](int end) { return end == (int)input.size(); });
}

bool StrictPattern::matches(const std::string& input, std::vector<Capture>& out) const {
    out.clear();
    Matcher m{input, &out};
    bool ok = m.matchSeq(terms_, 0, 0, [&](int end) { return end == (int)input.size(); });
    if (!ok) out.clear();
    return ok;
}

static std::vector<std::string> tokenize(const std::string& body) {
    std::vector<std::string> toks;
    size_t i = 0;
    while (i < body.size()) {
        char c = body[i];
        if (sSpace(c)) { i++; continue; }
        if (c == '(' || c == ')') { toks.push_back(std::string(1, c)); i++; continue; }
        if (c == '"') {
            size_t j = i + 1;
            std::string lit = "\"";
            while (j < body.size() && body[j] != '"') {
                if (body[j] == '\\' && j + 1 < body.size()) { lit += body[j + 1]; j += 2; continue; }
                lit += body[j]; j++;
            }
            lit += '"';
            toks.push_back(lit);
            i = (j < body.size()) ? j + 1 : j;
            continue;
        }
        if (c == '`') {

            size_t j = i + 1;
            std::string fz = "`";
            while (j < body.size() && body[j] != '`') { fz += body[j]; j++; }
            fz += '`';
            toks.push_back(fz);
            i = (j < body.size()) ? j + 1 : j;
            continue;
        }
        size_t j = i;
        while (j < body.size() && !sSpace(body[j]) && body[j] != '"' && body[j] != '(' && body[j] != ')') j++;
        toks.push_back(body.substr(i, j - i));
        i = j;
    }
    return toks;
}

static std::string lc(const std::string& s) {
    std::string o = s;
    for (char& c : o) if (c >= 'A' && c <= 'Z') c = (char)(c - 'A' + 'a');
    return o;
}

static bool primitive(const std::string& wordRaw, StrictTerm::Kind& k) {
    std::string word = lc(wordRaw);
    if (word == "digit") { k = StrictTerm::Kind::Digit; return true; }
    if (word == "digits") { k = StrictTerm::Kind::Digits; return true; }
    if (word == "letter") { k = StrictTerm::Kind::Letter; return true; }
    if (word == "letters") { k = StrictTerm::Kind::Letters; return true; }
    if (word == "word") { k = StrictTerm::Kind::Word; return true; }
    if (word == "number") { k = StrictTerm::Kind::Number; return true; }
    if (word == "ip") { k = StrictTerm::Kind::Ip; return true; }
    if (word == "space") { k = StrictTerm::Kind::Space; return true; }
    if (word == "spaces") { k = StrictTerm::Kind::Spaces; return true; }
    if (word == "anything") { k = StrictTerm::Kind::Anything; return true; }
    return false;
}
static bool isNumberTok(const std::string& s) {
    if (s.empty()) return false;
    for (char c : s) if (!sDigit(c)) return false;
    return true;
}

class StrictCompiler {
public:
    static StrictPattern compile(const std::string& body, const Registry* reg) {
        StrictPattern p;
        std::vector<std::string> toks = tokenize(body);
        size_t i = 0;
        p.terms_ = parseSeq(toks, i, reg, false);
        return p;
    }

private:
    static std::vector<StrictTerm> parseSeq(const std::vector<std::string>& toks, size_t& i,
                                            const Registry* reg, bool inGroup) {
        std::vector<StrictTerm> seq;
        while (i < toks.size()) {
            std::string tk = lc(toks[i]);
            if (tk == ")") { if (inGroup) return seq; i++; continue; }
            if (tk == "then") { i++; continue; }
            if (tk == "or") {
                std::vector<std::vector<StrictTerm>> branches;
                branches.push_back(seq);
                seq.clear();
                while (i < toks.size() && lc(toks[i]) == "or") {
                    i++;
                    std::vector<StrictTerm> b;
                    while (i < toks.size() && lc(toks[i]) != "or" && toks[i] != ")") {
                        if (lc(toks[i]) == "then") { i++; continue; }
                        b.push_back(parseTerm(toks, i, reg));
                    }
                    branches.push_back(b);
                }
                StrictTerm alt;
                alt.kind = StrictTerm::Kind::Alt;
                alt.alts = branches;
                return { alt };
            }
            seq.push_back(parseTerm(toks, i, reg));
        }
        return seq;
    }

    static StrictTerm parseTerm(const std::vector<std::string>& toks, size_t& i, const Registry* reg) {
        int repMin = 1, repMax = 1;
        bool hasRep = false;
        if (lc(toks[i]) == "any") { repMin = 0; repMax = -1; hasRep = true; i++; }
        else if (lc(toks[i]) == "many") { repMin = 1; repMax = -1; hasRep = true; i++; }
        else if (lc(toks[i]) == "maybe") { repMin = 0; repMax = 1; hasRep = true; i++; }
        else if (isNumberTok(toks[i]) && i + 2 < toks.size() && lc(toks[i + 1]) == "to") {
            repMin = std::stoi(toks[i]); repMax = std::stoi(toks[i + 2]); hasRep = true; i += 3;
        }

        StrictTerm t;
        const std::string& tk = toks[i];
        if (lc(tk) == "find" && i + 1 < toks.size() && !toks[i + 1].empty() && toks[i + 1].front() == '`') {

            std::string fzraw = toks[i + 1];
            std::string fz = fzraw.substr(1, fzraw.size() - 2);
            t.kind = StrictTerm::Kind::Fuzzy;
            if (!reg) throw std::invalid_argument("find in strict but no registry");
            t.fuzzy = std::make_shared<EvePattern>(Eve::compile(fz, *reg));
            i += 2;
        } else if (tk == "(") {
            i++;
            t.kind = StrictTerm::Kind::Group;
            t.group = parseSeq(toks, i, reg, true);
            if (i < toks.size() && toks[i] == ")") i++;
        } else if (tk.size() >= 2 && tk.front() == '"' && tk.back() == '"') {
            t.kind = StrictTerm::Kind::Literal;
            t.literal = tk.substr(1, tk.size() - 2);
            i++;
        } else if (!tk.empty() && tk[0] == '&') {
            if (!reg) throw std::invalid_argument("&ref in strict but no registry: " + tk);
            t.kind = StrictTerm::Kind::Alt;
            for (const std::string& m : reg->entries(tk.substr(1))) {
                StrictTerm lit; lit.kind = StrictTerm::Kind::Literal; lit.literal = m;
                t.alts.push_back({ lit });
            }
            i++;
        } else if (isNumberTok(tk) && i + 1 < toks.size() && isPrimWord(toks[i + 1])) {
            int n = std::stoi(tk);
            StrictTerm::Kind k; primitive(toks[i + 1], k);
            t.kind = k; t.count = n; i += 2;
        } else {
            StrictTerm::Kind k;
            if (!primitive(tk, k)) throw std::invalid_argument("unknown strict term: " + tk);
            t.kind = k; i++;
        }
        if (hasRep) {

            switch (t.kind) {
                case StrictTerm::Kind::Digits:
                case StrictTerm::Kind::Number: t.kind = StrictTerm::Kind::Digit; break;
                case StrictTerm::Kind::Letters:
                case StrictTerm::Kind::Word:   t.kind = StrictTerm::Kind::Letter; break;
                case StrictTerm::Kind::Spaces: t.kind = StrictTerm::Kind::Space; break;
                default: break;
            }
            t.repMin = repMin; t.repMax = repMax;
        }
        if (i + 1 < toks.size() && lc(toks[i]) == "as") { t.capture = toks[i + 1]; i += 2; }
        return t;
    }

    static bool isPrimWord(const std::string& s) { StrictTerm::Kind k; return primitive(s, k); }
};

StrictPattern Strict::compile(const std::string& body) {
    return StrictCompiler::compile(body, nullptr);
}
StrictPattern Strict::compile(const std::string& body, const Registry& reg) {
    return StrictCompiler::compile(body, &reg);
}

}
