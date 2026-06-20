#include "eve.hpp"

#include <algorithm>
#include <stdexcept>

namespace eve {

static inline bool isLetterC(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
}
static inline bool isDigitC(char c) { return c >= '0' && c <= '9'; }
static inline bool startsW(const std::string& s, const char* n, size_t off) {
    std::string needle(n);
    if (off > s.size() || needle.size() > s.size() - off) return false;
    return s.compare(off, needle.size(), needle) == 0;
}
static inline std::string toLower(const std::string& s) {
    std::string o = s;
    for (char& c : o) if (c >= 'A' && c <= 'Z') c = (char)(c - 'A' + 'a');
    return o;
}

void Registry::registerClass(std::string name, std::vector<std::string> entries) {
    std::optional<bool> hard;
    if (name.size() >= 4 && name.compare(name.size() - 4, 4, "[^^]") == 0) {
        hard = true; name = name.substr(0, name.size() - 4);
    } else if (name.size() >= 4 && name.compare(name.size() - 4, 4, "[*^]") == 0) {
        hard = false; name = name.substr(0, name.size() - 4);
    }
    classes_[name] = ClassDef{std::move(entries), hard};
}
void Registry::reset() { classes_.clear(); }
const ClassDef& Registry::get(const std::string& name) const {
    auto it = classes_.find(name);
    if (it == classes_.end()) throw std::invalid_argument("unknown class &" + name);
    return it->second;
}
std::vector<std::string> Registry::entries(const std::string& name) const { return get(name).entries; }
std::string Registry::vowelClass() const {
    std::vector<char> order;
    std::unordered_set<char> seen;
    for (const auto& [k, v] : classes_) {
        bool numeric = !k.empty();
        for (char c : k) if (!isDigitC(c)) { numeric = false; break; }
        if (!numeric) continue;
        for (const std::string& s : v.entries)
            if (s.size() == 1 && seen.insert(s[0]).second) order.push_back(s[0]);
    }
    return std::string(order.begin(), order.end());
}

static std::vector<std::string> splitTopLevel(const std::string& body) {
    std::vector<std::string> parts;
    int depth = 0; size_t start = 0;
    for (size_t k = 0; k < body.size(); k++) {
        char c = body[k];
        if (c == '[') depth++;
        else if (c == ']') depth--;
        else if (c == '|' && depth == 0) { parts.push_back(body.substr(start, k - start)); start = k + 1; }
    }
    parts.push_back(body.substr(start));
    return parts;
}

static std::vector<std::string> flattenArms(const std::string& body) {
    std::vector<std::string> out;
    for (const std::string& arm : splitTopLevel(body)) {
        if (arm.size() >= 2 && arm.front() == '[' && arm.back() == ']') {
            auto sub = flattenArms(arm.substr(1, arm.size() - 2));
            out.insert(out.end(), sub.begin(), sub.end());
        } else if (startsW(arm, "/*|", 0)) {
            auto sub = flattenArms(arm.substr(3));
            out.insert(out.end(), sub.begin(), sub.end());
        } else {
            out.push_back(arm);
        }
    }
    return out;
}

static std::vector<std::string> expandRefs(const std::vector<std::string>& operands, const Registry& reg) {
    std::vector<std::string> out;
    for (const std::string& op : operands) {
        if (!op.empty() && op[0] == '&') {
            auto e = reg.entries(op.substr(1));
            out.insert(out.end(), e.begin(), e.end());
        } else if (!op.empty()) {
            out.push_back(op);
        }
    }
    return out;
}

static int matchBracket(const std::string& s, int start) {
    int depth = 0;
    for (int k = start; k < (int)s.size(); k++) {
        char c = s[k];
        if (c == '[') depth++;
        else if (c == ']') { depth--; if (depth == 0) return k; }
    }
    throw std::invalid_argument("unbalanced bracket at " + std::to_string(start) + " in: " + s);
}

static int indexOfMarker(const std::string& s, int from) {
    size_t a = s.find("[*^]", from);
    size_t b = s.find("[^^]", from);
    if (a == std::string::npos) return b == std::string::npos ? -1 : (int)b;
    if (b == std::string::npos) return (int)a;
    return (int)std::min(a, b);
}

static Part::SwapArm parseSwapArm(const std::string& raw) {
    bool lead = !raw.empty() && raw.front() == '*';
    bool trail = !raw.empty() && raw.back() == '*';
    size_t b = 0, e = raw.size();
    while (b < e && raw[b] == '*') b++;
    while (e > b && raw[e - 1] == '*') e--;
    std::string core = raw.substr(b, e - b);
    bool doubleLead = raw.size() >= 2 && raw[0] == '*' && raw[1] == '*';
    if (doubleLead || (!lead && !trail)) return Part::SwapArm{core, true, true};
    return Part::SwapArm{core, !lead, !trail};
}

static PartList parseParts(const std::string& text) {
    PartList parts;
    size_t i = 0;
    std::string lit;
    auto flush = [&]() { parts.push_back(Part{Part::Kind::Lit, lit, false, {}}); lit.clear(); };
    while (i < text.size()) {
        if (text[i] == '@') {
            flush();
            parts.push_back(Part{Part::Kind::Any, "", false, {}});
            i += 1;
        } else if (startsW(text, "(#*)", i)) {
            flush();
            parts.push_back(Part{Part::Kind::Gap, "", false, {}});
            i += 4;
        } else if (startsW(text, "($^)", i)) {
            flush();
            parts.push_back(Part{Part::Kind::Gap, "", true, {}});
            i += 4;
        } else if (startsW(text, "[swap:", i)) {
            flush();
            size_t close = text.find(']', i);
            std::vector<Part::SwapArm> arms;
            std::string inner = text.substr(i + 6, close - (i + 6));
            size_t s = 0;
            while (true) {
                size_t bar = inner.find('|', s);
                std::string piece = inner.substr(s, bar == std::string::npos ? std::string::npos : bar - s);
                arms.push_back(parseSwapArm(piece));
                if (bar == std::string::npos) break;
                s = bar + 1;
            }
            Part p{Part::Kind::Swap, "", false, {}};
            p.swap = std::move(arms);
            parts.push_back(std::move(p));
            i = close + 1;
        } else {
            lit.push_back(text[i]);
            i++;
        }
    }
    parts.push_back(Part{Part::Kind::Lit, lit, false, {}});
    return parts;
}

static std::optional<std::string> litOnly(const PartList& parts) {
    std::string sb;
    for (const Part& p : parts) {
        if (p.kind == Part::Kind::Lit) sb += p.lit;
        else return std::nullopt;
    }
    return sb;
}
static std::string renderParts(const PartList& parts) {
    std::string sb;
    for (const Part& p : parts) {
        if (p.kind == Part::Kind::Lit) sb += p.lit;
        else if (p.kind == Part::Kind::Gap) sb += p.gapLetters ? "($^)" : "(#*)";
        else if (p.kind == Part::Kind::Any) sb += "@";
        else sb += " ";
    }
    return sb;
}
static int litLength(const PartList& parts) {
    int n = 0;
    for (const Part& p : parts) if (p.kind == Part::Kind::Lit) n += (int)p.lit.size();
    return n;
}
static PartList withFirstLitReplaced(const PartList& rootParts, const std::string& overrideText) {
    PartList out;
    bool replaced = false;
    for (const Part& p : rootParts) {
        if (!replaced && p.kind == Part::Kind::Lit) {
            PartList ov = parseParts(overrideText);
            out.insert(out.end(), ov.begin(), ov.end());
            replaced = true;
        } else {
            out.push_back(p);
        }
    }
    return out;
}

static void expandOne(const std::string& skel, size_t i, std::string& acc,
                      std::unordered_set<std::string>& out, const Registry& reg) {
    if (i >= skel.size()) { out.insert(toLower(acc)); return; }
    if (skel[i] == '&') {
        size_t j = i + 1;
        bool digits = j < skel.size() && isDigitC(skel[j]);
        while (j < skel.size() && (digits ? isDigitC(skel[j]) : isLetterC(skel[j]))) j++;
        std::string name = skel.substr(i + 1, j - (i + 1));
        for (const std::string& entry : reg.entries(name)) {
            size_t old = acc.size();
            acc += entry;
            expandOne(skel, j, acc, out, reg);
            acc.resize(old);
        }
    } else {
        acc.push_back(skel[i]);
        expandOne(skel, i + 1, acc, out, reg);
        acc.pop_back();
    }
}
static std::unordered_set<std::string> expandSkeletons(const std::string& block, const Registry& reg) {
    std::unordered_set<std::string> out;
    std::string body = startsW(block, "skel:", 0) ? block.substr(5) : block;
    for (std::string skel : splitTopLevel(body)) {
        size_t b = skel.find_first_not_of(" \t");
        size_t e = skel.find_last_not_of(" \t");
        if (b == std::string::npos) continue;
        skel = skel.substr(b, e - b + 1);
        std::string acc;
        expandOne(skel, 0, acc, out, reg);
    }
    return out;
}

std::string Eve::stripComments(const std::string& pattern) {
    if (pattern.find("{#c:") == std::string::npos) return pattern;
    std::string out;
    size_t i = 0;
    while (i < pattern.size()) {
        if (startsW(pattern, "{#c:", i)) {
            int depth = 0; size_t j = i;
            for (; j < pattern.size(); j++) {
                char c = pattern[j];
                if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) { j++; break; } }
            }
            i = j;
        } else {
            out.push_back(pattern[i]);
            i++;
        }
    }
    return out;
}

std::unordered_set<std::string> EvePattern::concreteWords() const {
    std::unordered_set<std::string> out(skeletons_.begin(), skeletons_.end());
    if (!precheck_.empty() || !veto_.empty()) return out;
    for (const PartList& root : roots_) {
        auto base = litOnly(root);
        if (!base) continue;
        std::vector<std::string> forms{*base};
        bool relaxed = false;
        for (const Group& g : groups_) {
            if (g.kind != Group::Kind::Normal) { relaxed = true; break; }
            std::vector<std::string> next;
            for (const std::string& f : forms) {
                for (const PartList& arm : g.arms) {
                    auto a = litOnly(arm);
                    if (a) next.push_back(f + *a);
                }
            }
            forms = std::move(next);
        }
        if (relaxed) continue;
        for (const std::string& f : forms) {
            for (const std::string& suf : trailing_) {
                size_t gt = suf.find('>');
                if (gt == std::string::npos) {
                    out.insert(f + suf);
                } else {
                    std::string from = suf.substr(0, gt), to = suf.substr(gt + 1);
                    if (f.size() >= from.size() && f.compare(f.size() - from.size(), from.size(), from) == 0)
                        out.insert(f.substr(0, f.size() - from.size()) + to);
                }
            }
        }
    }
    for (const Group& g : groups_) {
        if (g.kind != Group::Kind::Hard) continue;
        for (const PartList& arm : g.arms) {
            auto a = litOnly(arm);
            if (!a || a->empty()) continue;
            for (const std::string& suf : trailing_)
                if (suf.find('>') == std::string::npos) out.insert(*a + suf);
        }
    }
    return out;
}

struct FlagWrap { std::string first; bool second; std::string third; };

static bool isFlagIdentChar(char c) {
    return isLetterC(c) || isDigitC(c) || c == '_' || c == '-';
}

static std::optional<FlagWrap> peelFlagWrapper(const std::string& pattern) {
    if (pattern.size() < 5 || pattern.front() != '{' || pattern.back() != '}') return std::nullopt;
    size_t i = 1;
    while (i < pattern.size() && isFlagIdentChar(pattern[i])) i++;
    if (i == 1) return std::nullopt;
    std::string name = pattern.substr(1, i - 1);
    if (i + 2 >= pattern.size() || pattern[i] != ':') return std::nullopt;
    char yn = pattern[i + 1];
    if ((yn != 'y' && yn != 'n') || pattern[i + 2] != '?') return std::nullopt;
    std::string inner = pattern.substr(i + 3, pattern.size() - (i + 3) - 1);
    return FlagWrap{name, yn == 'y', inner};
}

class Compiler {
public:
    static EvePattern compile(const std::string& patternIn, const Registry& reg) {
        EvePattern tok;
        std::string pattern = Eve::stripComments(patternIn);

        std::vector<std::pair<std::string, bool>> flags;
        while (true) {
            auto fl = peelFlagWrapper(pattern);
            if (!fl) break;
            flags.push_back({fl->first, fl->second});
            pattern = fl->third;
        }

        std::vector<std::string> precheck;
        if (startsW(pattern, "{pre:", 0)) {
            size_t end = pattern.find('}');
            for (std::string act : expandRefs(splitTopLevel(pattern.substr(5, end - 5)), reg))
                precheck.push_back(toLower(act));
            pattern = pattern.substr(end + 1);
        }

        std::unordered_set<std::string> skeletons;
        size_t brace = pattern.find('{');
        if (brace != std::string::npos && !pattern.empty() && pattern.back() == '}') {
            skeletons = expandSkeletons(pattern.substr(brace + 1, pattern.size() - brace - 2), reg);
            pattern = pattern.substr(0, brace);
        }

        std::vector<std::string> overrideTexts;
        std::vector<std::optional<bool>> overrideLiterals;
        int i = 0;

        while (i < (int)pattern.size() && pattern[i] == '[') {
            size_t close = pattern.find(']', i);
            if (close == std::string::npos) break;
            std::string inner = pattern.substr(i + 1, close - i - 1);
            if (!inner.empty() && inner.back() == ';') {
                std::string body = inner.substr(0, inner.size() - 1);
                std::optional<bool> lit;
                if (body.size() >= 2 && body.compare(body.size() - 2, 2, "^^") == 0) { lit = true; body = body.substr(0, body.size() - 2); }
                else if (body.size() >= 2 && body.compare(body.size() - 2, 2, "*^") == 0) { lit = false; body = body.substr(0, body.size() - 2); }
                if (!body.empty() && body[0] == '&') {
                    for (const std::string& member : reg.entries(body.substr(1))) {
                        overrideTexts.push_back(member); overrideLiterals.push_back(lit);
                    }
                } else {
                    overrideTexts.push_back(body); overrideLiterals.push_back(lit);
                }
                i = (int)close + 1;
            } else break;
        }

        std::vector<std::string> rootPrefix{""};
        while (startsW(pattern, "[+[", i)) {
            int close = matchBracket(pattern, i);
            std::string inner = pattern.substr(i + 3, close - 1 - (i + 3));
            std::vector<std::string> group{""};
            for (const std::string& arm : splitTopLevel(inner)) {
                if (!arm.empty() && arm[0] == '&') {
                    size_t end = 1;
                    while (end < arm.size() && arm[end] != '(') end++;
                    std::string name = arm.substr(1, end - 1);
                    std::string tail = arm.substr(end);
                    for (const std::string& member : reg.entries(name)) group.push_back(member + tail);
                } else {
                    group.push_back(arm);
                }
            }
            std::vector<std::string> composed;
            composed.reserve(rootPrefix.size() * group.size());
            for (const std::string& pre : rootPrefix)
                for (const std::string& g : group) composed.push_back(pre + g);
            rootPrefix = std::move(composed);
            i = close + 1;
        }
        if (rootPrefix.size() == 1) rootPrefix.clear();

        int marker = indexOfMarker(pattern, i);
        if (marker < 0) throw std::invalid_argument("no root marker in: " + pattern);
        std::string rootText = pattern.substr(i, marker - i);
        bool literal = startsW(pattern, "[^^]", marker);
        i = marker + 4;

        std::vector<std::string> post;
        size_t postAt = rootText.find("{post:");
        if (postAt != std::string::npos) {
            size_t postEnd = rootText.find('}', postAt);
            for (std::string s : expandRefs(splitTopLevel(rootText.substr(postAt + 6, postEnd - postAt - 6)), reg))
                post.push_back(toLower(s));
            rootText = rootText.substr(0, postAt) + rootText.substr(postEnd + 1);
        }
        std::string rootStem = rootText;

        std::vector<PartList> roots;
        std::vector<bool> rootLiterals;
        PartList rootParts;
        if (startsW(rootText, "[[", 0)) {
            std::vector<std::string> segs;
            std::vector<bool> starred;
            int k = 1;
            while (k < (int)rootText.size() && rootText[k] == '[') {
                int close = matchBracket(rootText, k);
                std::string seg = rootText.substr(k + 1, close - k - 1);
                bool star = !seg.empty() && seg.back() == '*';
                segs.push_back(star ? seg.substr(0, seg.size() - 1) : seg);
                starred.push_back(star);
                k = close + 1;
            }
            std::string tail = rootText.substr(rootText.rfind(']') + 1);
            std::vector<std::string> firstSeg{segs[0]};
            firstSeg.insert(firstSeg.end(), overrideTexts.begin(), overrideTexts.end());
            overrideTexts.clear(); overrideLiterals.clear();
            std::vector<std::string> alts; std::unordered_set<std::string> altSeen;
            auto addAlt = [&](const std::string& a) { if (altSeen.insert(a).second) alts.push_back(a); };
            for (size_t s = 0; s < segs.size(); s++) {
                if (!starred[s]) continue;
                std::string rest;
                for (size_t z = s; z < segs.size(); z++) rest += segs[z];
                addAlt(rest);
                std::string mid;
                for (size_t z = 1; z < s; z++) mid += segs[z];
                if (s == 0) addAlt(std::string("") + mid + rest);
                else for (const std::string& pfx : firstSeg) addAlt(pfx + mid + rest);
            }
            if (alts.empty()) throw std::invalid_argument("segmented root has no * entry point: " + rootText);
            std::vector<PartList> built;
            for (const std::string& a : alts) built.push_back(parseParts(a + tail));
            rootParts = built[0];
            for (auto& b : built) { roots.push_back(b); rootLiterals.push_back(literal); }
        } else if (!rootText.empty() && rootText[0] == '&') {
            int j = 1;
            while (j < (int)rootText.size() && rootText[j] != '|' && rootText[j] != '(' && rootText[j] != '[') j++;
            std::string name = rootText.substr(1, j - 1);
            std::string tail = rootText.substr(j);
            auto members = reg.entries(name);
            rootParts = parseParts(members[0] + tail);
            roots.push_back(rootParts); rootLiterals.push_back(literal);
            for (size_t m = 1; m < members.size(); m++) { roots.push_back(parseParts(members[m] + tail)); rootLiterals.push_back(literal); }
        } else {
            rootParts = parseParts(rootText);
            roots.push_back(rootParts); rootLiterals.push_back(literal);
        }
        for (size_t oi = 0; oi < overrideTexts.size(); oi++) {
            roots.push_back(withFirstLitReplaced(rootParts, overrideTexts[oi]));
            auto ovLit = overrideLiterals[oi];
            rootLiterals.push_back(ovLit.has_value() ? *ovLit : literal);
        }
        if (!rootPrefix.empty()) {
            std::unordered_set<std::string> seen;
            std::vector<PartList> prefixed; std::vector<bool> prefixedLit;
            for (size_t r = 0; r < roots.size(); r++) {
                for (const std::string& pre : rootPrefix) {
                    PartList alt;
                    if (!pre.empty()) { PartList pp = parseParts(pre); alt.insert(alt.end(), pp.begin(), pp.end()); }
                    alt.insert(alt.end(), roots[r].begin(), roots[r].end());
                    if (seen.insert(renderParts(alt)).second) { prefixed.push_back(alt); prefixedLit.push_back(rootLiterals[r]); }
                }
            }
            roots = std::move(prefixed); rootLiterals = std::move(prefixedLit);
            rootParts = roots[0];
        }
        std::vector<int> order(roots.size());
        for (size_t x = 0; x < roots.size(); x++) order[x] = (int)x;
        std::stable_sort(order.begin(), order.end(), [&](int a, int b) {
            return litLength(roots[b]) < litLength(roots[a]);
        });
        {
            std::vector<PartList> sr; std::vector<bool> sl;
            for (int idx : order) { sr.push_back(roots[idx]); sl.push_back(rootLiterals[idx]); }
            roots = std::move(sr); rootLiterals = std::move(sl);
        }

        std::vector<Group> groups;
        std::vector<std::string> trailing{""};
        std::vector<std::string> veto;

        if (i < (int)pattern.size() && pattern[i] == '|') i++;
        while (i < (int)pattern.size()) {
            char c = pattern[i];
            if (c == '|') { i++; continue; }
            if (startsW(pattern, "(#*)", i)) {
                Group g; g.arms = {PartList{Part{Part::Kind::Gap, "", false, {}}}}; g.optional = true; g.kind = Group::Kind::Normal;
                groups.push_back(std::move(g)); i += 4; continue;
            }
            if (startsW(pattern, "($^)", i)) {
                Group g; g.arms = {PartList{Part{Part::Kind::Gap, "", true, {}}}}; g.optional = true; g.kind = Group::Kind::Normal;
                groups.push_back(std::move(g)); i += 4; continue;
            }
            if (c == '&') {
                bool opt = i + 1 < (int)pattern.size() && pattern[i + 1] == '!';
                int start = i + (opt ? 2 : 1);
                int j = start;

                while (j < (int)pattern.size() && pattern[j] != '|' && pattern[j] != '[' && pattern[j] != '(') j++;
                std::string name = pattern.substr(start, j - start);
                std::optional<bool> refHard;
                if (startsW(pattern, "[^^]", j)) { refHard = true; j += 4; }
                else if (startsW(pattern, "[*^]", j)) { refHard = false; j += 4; }
                const ClassDef& cd = reg.get(name);
                bool hard = refHard.has_value() ? *refHard : (cd.hard.has_value() && *cd.hard);
                Group g; g.optional = opt; g.kind = hard ? Group::Kind::Hard : Group::Kind::Normal;
                for (const std::string& entry : cd.entries) g.arms.push_back(parseParts(entry));
                if (opt) g.arms.push_back(PartList{Part{Part::Kind::Lit, "", false, {}}});
                groups.push_back(std::move(g));
                i = j; continue;
            }
            if (c != '[') throw std::invalid_argument("expected group at " + std::to_string(i) + " in: " + pattern);
            int close = matchBracket(pattern, i);
            std::string body = pattern.substr(i + 1, close - i - 1);
            i = close + 1;

            if (startsW(body, "/*|", 0)) {
                for (const std::string& arm : expandRefs(flattenArms(body.substr(3)), reg))
                    if (!arm.empty()) trailing.push_back(arm);
                continue;
            }
            if (startsW(body, "-[", 0) && !body.empty() && body.back() == ']') {
                for (std::string v : expandRefs(flattenArms(body.substr(2, body.size() - 3)), reg))
                    veto.push_back(toLower(v));
                continue;
            }
            if (startsW(body, "swap:", 0)) {
                std::vector<Part::SwapArm> sw;
                std::string inner = body.substr(5);
                size_t s = 0;
                while (true) {
                    size_t bar = inner.find('|', s);
                    std::string piece = inner.substr(s, bar == std::string::npos ? std::string::npos : bar - s);
                    sw.push_back(parseSwapArm(piece));
                    if (bar == std::string::npos) break;
                    s = bar + 1;
                }
                Part p{Part::Kind::Swap, "", false, {}}; p.swap = std::move(sw);
                Group g; g.arms = {PartList{std::move(p)}}; g.optional = false; g.kind = Group::Kind::Normal;
                groups.push_back(std::move(g));
                continue;
            }
            Group::Kind kind = Group::Kind::Normal;
            if (startsW(body, "+[", 0) && !body.empty() && body.back() == ']') {
                kind = Group::Kind::Plus; body = body.substr(1);
            }
            if (kind != Group::Kind::Normal) body = body.substr(1, body.size() - 2);

            std::vector<std::string> armTexts;
            for (const std::string& arm : flattenArms(body)) {
                if (!arm.empty() && arm[0] == '&') {
                    auto e = reg.entries(arm.substr(1));
                    armTexts.insert(armTexts.end(), e.begin(), e.end());
                } else {
                    armTexts.push_back(arm);
                }
            }
            Group g; g.kind = kind; g.optional = false;
            for (const std::string& arm : armTexts) {
                g.arms.push_back(parseParts(arm));
                if (arm.empty()) g.optional = true;
            }
            groups.push_back(std::move(g));
        }

        if (!post.empty()) {
            Group g; g.optional = true; g.kind = Group::Kind::Normal;
            for (const std::string& suf : post) g.arms.push_back(parseParts(suf));
            g.arms.push_back(parseParts(""));
            groups.insert(groups.begin(), std::move(g));
        }

        tok.roots_ = std::move(roots);
        tok.rootIsLiteral_ = literal;
        tok.rootLiterals_ = std::move(rootLiterals);
        tok.groups_ = std::move(groups);
        tok.trailing_ = std::move(trailing);
        tok.skeletons_ = std::move(skeletons);
        tok.precheck_ = std::move(precheck);
        tok.post_ = std::move(post);
        tok.rootStem_ = std::move(rootStem);
        tok.veto_ = std::move(veto);
        tok.flags_ = std::move(flags);
        return tok;
    }
};

EvePattern Eve::compile(const std::string& pattern, const Registry& reg) {
    return Compiler::compile(pattern, reg);
}

}
