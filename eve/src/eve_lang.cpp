#include "eve_lang.hpp"

#include <algorithm>
#include <sstream>
#include <stdexcept>

namespace eve {

static std::string trim(const std::string& s) {
    size_t b = s.find_first_not_of(" \t\r\n");
    if (b == std::string::npos) return "";
    size_t e = s.find_last_not_of(" \t\r\n");
    return s.substr(b, e - b + 1);
}

static bool startsWord(const std::string& s, size_t i, const std::string& w) {
    if (s.compare(i, w.size(), w) != 0) return false;
    size_t after = i + w.size();
    bool beforeOk = (i == 0) || s[i - 1] == ' ' || s[i - 1] == '\t';
    bool afterOk = (after >= s.size()) || s[after] == ' ' || s[after] == '\t';
    return beforeOk && afterOk;
}

static size_t findKeyword(const std::string& s, const std::string& w, size_t from) {
    int depth = 0;
    for (size_t i = from; i < s.size(); i++) {
        char c = s[i];
        if (c == '[' || c == '{' || c == '(') depth++;
        else if (c == ']' || c == '}' || c == ')') { if (depth > 0) depth--; }
        else if (depth == 0 && startsWord(s, i, w)) return i;
    }
    return std::string::npos;
}

static int markerPos(const std::string& body) {
    int depth = 0;
    for (size_t i = 0; i + 3 < body.size(); i++) {
        char c = body[i];
        if (c == '{' || c == '(') depth++;
        else if (c == '}' || c == ')') { if (depth > 0) depth--; }
        if (depth == 0 && (body.compare(i, 4, "[*^]") == 0 || body.compare(i, 4, "[^^]") == 0))
            return (int)i;
    }
    return -1;
}

std::string Lang::toPatternString(const std::string& ruleBody) {
    std::string s = trim(ruleBody);

    struct Sec { std::string kw; std::string val; size_t at; };
    const std::vector<std::string> kws = {"only if", "miss", "bone", "then", "flag", "note"};

    std::vector<Sec> secs;
    for (const std::string& kw : kws) {
        size_t at = findKeyword(s, kw, 0);
        while (at != std::string::npos) {
            secs.push_back({kw, "", at});
            at = findKeyword(s, kw, at + kw.size());
        }
    }
    std::sort(secs.begin(), secs.end(), [](const Sec& a, const Sec& b){ return a.at < b.at; });

    size_t bodyEnd = secs.empty() ? s.size() : secs[0].at;
    std::string body = trim(s.substr(0, bodyEnd));

    for (size_t k = 0; k < secs.size(); k++) {
        size_t valStart = secs[k].at + secs[k].kw.size();
        size_t valEnd = (k + 1 < secs.size()) ? secs[k + 1].at : s.size();
        secs[k].val = trim(s.substr(valStart, valEnd - valStart));
    }

    std::string pre, post, miss, bone, note;
    std::vector<std::pair<std::string, bool>> flags;
    for (const Sec& sec : secs) {
        if (sec.kw == "only if") pre = sec.val;
        else if (sec.kw == "miss") miss = sec.val;
        else if (sec.kw == "bone") bone = sec.val;
        else if (sec.kw == "then") post = sec.val;
        else if (sec.kw == "note") note = sec.val;
        else if (sec.kw == "flag") {
            std::istringstream iss(sec.val);
            std::string fname, fval;
            iss >> fname >> fval;
            bool b = (fval == "yes" || fval == "y" || fval == "true");
            flags.push_back({fname, b});
        }
    }

    if (!post.empty()) {
        int m = markerPos(body);
        if (m >= 0) body = body.substr(0, m) + "{post:" + post + "}" + body.substr(m);
    }

    std::string out = body;
    if (!miss.empty()) out += "[-[" + miss + "]]";
    if (!bone.empty()) out += "{skel:" + bone + "}";
    if (!pre.empty()) out = "{pre:" + pre + "}" + out;
    for (auto it = flags.rbegin(); it != flags.rend(); ++it)
        out = "{" + it->first + ":" + (it->second ? "y" : "n") + "?" + out + "}";
    if (!note.empty()) out += "{#c:" + note + "}";
    return out;
}

Program Lang::parse(const std::string& source) {
    Program prog;
    prog.registry.reset();
    std::istringstream in(source);
    std::string line;
    std::string realm;

    std::vector<std::pair<std::string, std::string>> pending;

    while (std::getline(in, line)) {
        std::string t = trim(line);
        if (t.empty() || t[0] == '#') continue;

        if (t.rfind("eve:", 0) == 0) {
            std::string rest = trim(t.substr(4));
            size_t sp = rest.find(' ');
            if (sp == std::string::npos) { prog.version = rest; }
            else { prog.version = rest.substr(0, sp); prog.language = trim(rest.substr(sp + 1)); }
            continue;
        }
        if (startsWord(t, 0, "let")) {
            std::string rest = trim(t.substr(3));
            size_t be = findKeyword(rest, "be", 0);
            if (be == std::string::npos) throw std::invalid_argument("let without 'be': " + t);
            std::string name = trim(rest.substr(0, be));
            std::string members = trim(rest.substr(be + 2));
            if (!name.empty() && name[0] == '&') name = name.substr(1);
            std::vector<std::string> parts;
            size_t s0 = 0;
            while (true) {
                size_t bar = members.find('|', s0);
                parts.push_back(members.substr(s0, bar == std::string::npos ? std::string::npos : bar - s0));
                if (bar == std::string::npos) break;
                s0 = bar + 1;
            }
            prog.registry.registerClass(name, parts);
            continue;
        }
        if (startsWord(t, 0, "define")) {
            std::string rest = trim(t.substr(6));
            size_t as = findKeyword(rest, "as", 0);
            if (as == std::string::npos) throw std::invalid_argument("define without 'as': " + t);
            std::string name = trim(rest.substr(0, as));
            std::string id = trim(rest.substr(as + 2));
            if (name.empty() || id.empty()) throw std::invalid_argument("define needs a name and an id: " + t);
            prog.defines.push_back({name, id});
            continue;
        }
        if (startsWord(t, 0, "realm")) {
            realm = trim(t.substr(5));
            continue;
        }
        if (startsWord(t, 0, "hear") || startsWord(t, 0, "find")) {
            std::string rest = trim(t.substr(4));
            size_t as = findKeyword(rest, "as", 0);
            if (as == std::string::npos) throw std::invalid_argument("hear/find without 'as': " + t);
            std::string name = trim(rest.substr(0, as));
            std::string ruleBody = trim(rest.substr(as + 2));
            Rule r;
            r.kind = Rule::Kind::Fuzzy;
            r.realm = realm;
            r.name = name;
            r.pattern = toPatternString(ruleBody);
            prog.rules.push_back(std::move(r));
            continue;
        }
        if (startsWord(t, 0, "match")) {
            std::string rest = trim(t.substr(5));
            size_t as = findKeyword(rest, "as", 0);
            if (as == std::string::npos) throw std::invalid_argument("match without 'as': " + t);
            std::string name = trim(rest.substr(0, as));
            std::string ruleBody = trim(rest.substr(as + 2));
            Rule r;
            r.kind = Rule::Kind::Strict;
            r.realm = realm;
            r.name = name;
            r.pattern = ruleBody;
            prog.rules.push_back(std::move(r));
            continue;
        }
        throw std::invalid_argument("unrecognized line: " + t);
    }

    for (Rule& r : prog.rules) {
        if (r.kind == Rule::Kind::Strict)
            r.strict = Strict::compile(r.pattern, prog.registry);
        else
            r.compiled = Eve::compile(r.pattern, prog.registry);
    }

    for (const Rule& r : prog.rules) {
        if (r.kind != Rule::Kind::Fuzzy) continue;
        for (const auto& f : r.compiled.flags()) {
            bool declared = false;
            for (const auto& d : prog.defines) if (d.first == f.first) { declared = true; break; }
            if (!declared)
                throw std::invalid_argument("undefined flag '" + f.first
                        + "' -- declare it with: DEFINE " + f.first + " AS <id>");
        }
    }

    return prog;
}

}
