#include "eve_expanda.hpp"

#include <cctype>
#include <stdexcept>
#include <vector>

namespace eve {

static bool isWordChar(char c) {
    return std::isalnum((unsigned char)c) || c == '_';
}
static std::string upper(const std::string& s) {
    std::string o = s;
    for (char& c : o) c = (char)std::toupper((unsigned char)c);
    return o;
}
static std::string trim(const std::string& s) {
    size_t b = s.find_first_not_of(" \t\r\n");
    if (b == std::string::npos) return "";
    size_t e = s.find_last_not_of(" \t\r\n");
    return s.substr(b, e - b + 1);
}

static size_t matchParen(const std::string& s, size_t open) {
    int depth = 0;
    for (size_t i = open; i < s.size(); i++) {
        if (s[i] == '(') depth++;
        else if (s[i] == ')') { if (--depth == 0) return i; }
    }
    throw std::invalid_argument("unbalanced ( in expandaEVE body");
}

static std::vector<std::string> splitOr(const std::string& inner) {
    std::vector<std::string> out;
    int depth = 0;
    bool inq = false;
    size_t start = 0;
    size_t i = 0;
    auto flush = [&](size_t end) { out.push_back(trim(inner.substr(start, end - start))); };
    while (i < inner.size()) {
        char c = inner[i];
        if (c == '"' ) inq = !inq;
        if (!inq) {
            if (c == '(' || c == '[') depth++;
            else if (c == ')' || c == ']') depth--;
            else if (depth == 0 && (c == 'O' || c == 'o') && i + 1 < inner.size()
                     && (inner[i+1] == 'R' || inner[i+1] == 'r')) {
                bool lb = (i == 0) || std::isspace((unsigned char)inner[i-1]);
                bool rb = (i + 2 >= inner.size()) || std::isspace((unsigned char)inner[i+2]);
                if (lb && rb) { flush(i); start = i + 2; i += 2; continue; }
            }
        }
        i++;
    }
    flush(inner.size());
    return out;
}

static std::string normBody(const std::string& body);
std::string emitFunction(const std::string& inner);

static std::string skeletonArms(const std::string& inner) {
    std::string out;
    bool firstArm = true;
    for (const std::string& armRaw : splitOr(inner)) {
        std::string arm = trim(armRaw);
        size_t paren = arm.find('(');
        if (paren == std::string::npos) {
            if (!firstArm) out += '|'; firstArm = false; out += arm; continue;
        }
        std::vector<std::string> nums;
        size_t s = 0;
        std::string mapStr = arm.substr(0, paren);
        while (true) {
            size_t c = mapStr.find(':', s);
            std::string n = trim(mapStr.substr(s, c == std::string::npos ? std::string::npos : c - s));
            if (!n.empty()) nums.push_back(n);
            if (c == std::string::npos) break;
            s = c + 1;
        }
        size_t pclose = arm.rfind(')');
        std::string skel = arm.substr(paren + 1, pclose - paren - 1);
        std::string built;
        size_t ni = 0;
        for (char ch : skel) {
            if (ch == '!') { if (ni < nums.size()) built += "&" + nums[ni++]; }
            else built += ch;
        }
        if (!firstArm) out += '|'; firstArm = false; out += built;
    }
    return out;
}

static std::string orArms(const std::string& inner) {
    std::string out;
    bool first = true;
    for (const std::string& op : splitOr(inner)) {
        if (!first) out += '|';
        first = false;
        if (upper(op) == "NONE") {}
        else out += normBody(op);
    }
    return out;
}

static bool kwAt(const std::string& s, size_t i, const std::string& kw) {
    size_t n = kw.size();
    if (i + n > s.size()) return false;
    for (size_t j = 0; j < n; j++)
        if (std::toupper((unsigned char)s[i+j]) != (unsigned char)kw[j]) return false;
    bool lb = (i == 0) || std::isspace((unsigned char)s[i-1]) || s[i-1] == ')' || s[i-1] == ']';
    char after = (i + n < s.size()) ? s[i+n] : ' ';
    bool rb = std::isspace((unsigned char)after) || after == '(' || after == '\0';
    return lb && rb;
}

static std::string segmentedRoot(const std::string& body, size_t& i) {
    size_t p = i;
    std::vector<std::pair<std::string,bool>> segs;
    int bracketCount = 0;
    while (p < body.size()) {
        if (std::isspace((unsigned char)body[p])) break;
        if (body[p] == '[') {
            size_t cl = body.find(']', p);
            if (cl == std::string::npos) break;
            std::string seg = body.substr(p + 1, cl - p - 1);
            if (seg.empty() || seg.find('|') != std::string::npos) break;
            segs.push_back({seg, true});
            bracketCount++;
            p = cl + 1;
        } else if (std::isalnum((unsigned char)body[p]) || body.compare(p, 4, "(#*)") == 0) {
            size_t s = p;
            while (p < body.size() && (std::isalnum((unsigned char)body[p]) || body.compare(p, 4, "(#*)") == 0)) {
                if (body.compare(p, 4, "(#*)") == 0) p += 4; else p++;
            }
            segs.push_back({body.substr(s, p - s), false});
        } else break;
    }
    if (segs.size() < 2 || bracketCount == 0) return "";
    std::string sym = "[[";
    for (size_t k = 0; k < segs.size(); k++) {
        if (k) sym += "][";
        sym += segs[k].first;
        if (segs[k].second) sym += "*";
    }
    sym += "]]";
    i = p;
    return sym;
}

static std::string normBody(const std::string& body) {
    std::string out;
    size_t i = 0;
    {
        size_t save = i;
        std::string seg = segmentedRoot(body, i);
        if (!seg.empty()) out += seg; else i = save;
    }
    while (i < body.size()) {
        char c = body[i];

        if (c == '"') {
            size_t j = i + 1;
            out += c;
            while (j < body.size() && body[j] != '"') { out += body[j]; j++; }
            if (j < body.size()) { out += '"'; j++; }
            i = j;
            continue;
        }

        if (kwAt(body, i, "FUNCTION") && i + 8 < body.size() && body[i+8] == '(') {
            size_t close = matchParen(body, i + 8);
            std::string inner = body.substr(i + 9, close - (i + 9));
            out += emitFunction(inner);
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "SUFFIX") && i + 6 < body.size() && body[i+6] == '(') {
            size_t close = matchParen(body, i + 6);
            out += "|[" + orArms(body.substr(i + 7, close - (i + 7))) + "]";
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "NEVER") && i + 5 < body.size() && body[i+5] == '(') {
            size_t close = matchParen(body, i + 5);
            out += "[-[" + orArms(body.substr(i + 6, close - (i + 6))) + "]]";
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "BEFORE") && i + 6 < body.size() && body[i+6] == '(') {
            size_t close = matchParen(body, i + 6);
            out += "{pre:" + orArms(body.substr(i + 7, close - (i + 7))) + "}";
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "AFTER") && i + 5 < body.size() && body[i+5] == '(') {
            size_t close = matchParen(body, i + 5);
            out += "{post:" + orArms(body.substr(i + 6, close - (i + 6))) + "}";
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "SKELETON") && i + 8 < body.size() && body[i+8] == '(') {
            size_t close = matchParen(body, i + 8);
            out += "{skel:" + skeletonArms(body.substr(i + 9, close - (i + 9))) + "}";
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "NOTE") && i + 4 < body.size() && body[i+4] == '(') {
            size_t close = matchParen(body, i + 4);
            out += "{#c:" + body.substr(i + 5, close - (i + 5)) + "}";
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "ALIAS") && i + 5 < body.size() && body[i+5] == '(') {
            size_t close = matchParen(body, i + 5);
            std::string inner = trim(body.substr(i + 6, close - (i + 6)));
            std::string marker;
            if (inner.size() >= 7 && upper(inner.substr(inner.size() - 7)) == "(WHOLE)") {
                marker = "^^"; inner = trim(inner.substr(0, inner.size() - 7));
            } else if (inner.size() >= 6 && upper(inner.substr(inner.size() - 6)) == "(ROOT)") {
                marker = "*^"; inner = trim(inner.substr(0, inner.size() - 6));
            }
            out += "[" + normBody(inner) + marker + ";]";
            i = close + 1;
            while (i < body.size() && std::isspace((unsigned char)body[i])) i++;
            {
                size_t save = i;
                std::string seg = segmentedRoot(body, i);
                if (!seg.empty()) out += seg; else i = save;
            }
            continue;
        }
        if (kwAt(body, i, "ALSO") && i + 4 < body.size() && body[i+4] == '(') {
            size_t close = matchParen(body, i + 4);
            out += "[+[" + orArms(body.substr(i + 5, close - (i + 5))) + "]]";
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "SWAP") && i + 4 < body.size() && body[i+4] == '(') {
            size_t close = matchParen(body, i + 4);
            std::string inner = body.substr(i + 5, close - (i + 5));
            std::string sw; bool first = true;
            for (const std::string& op : splitOr(inner)) {
                if (!first) sw += '|';
                first = false;
                sw += op;
            }
            out += "[swap:" + sw + "]";
            i = close + 1;
            continue;
        }
        if (kwAt(body, i, "MAYBE")) {
            size_t p = i + 5;
            while (p < body.size() && std::isspace((unsigned char)body[p])) p++;
            if (kwAt(body, p, "FUNCTION") && p + 8 < body.size() && body[p+8] == '(') {
                size_t close = matchParen(body, p + 8);
                std::string nm = trim(body.substr(p + 9, close - (p + 9)));
                out += "&!" + nm;
                i = close + 1;
                continue;
            }
        }
        if (kwAt(body, i, "PLURAL")) { out += "[/*|&s]"; i += 6; continue; }
        if (kwAt(body, i, "GAP"))    { out += "(#*)";    i += 3; continue; }
        if (kwAt(body, i, "LETTERS")){ out += "($^)";    i += 7; continue; }
        if (kwAt(body, i, "LETTER")) { out += "@";       i += 6; continue; }

        if (c == '(') {
            if (upper(body.substr(i, 7)) == "(WHOLE)") { out += "[^^]"; i += 7; continue; }
            if (upper(body.substr(i, 6)) == "(ROOT)")  { out += "[*^]"; i += 6; continue; }
        }

        if ((c == 'O' || c == 'o') && kwAt(body, i, "OR")) { out += '|'; i += 2; continue; }

        out += c;
        i++;
    }
    return out;
}

std::string emitFunction(const std::string& inner) {
    std::vector<std::string> parts;
    int depth = 0; size_t start = 0;
    for (size_t i = 0; i < inner.size(); i++) {
        char c = inner[i];
        if (c == '(' || c == '[') depth++;
        else if (c == ')' || c == ']') depth--;
        else if (c == '+' && depth == 0) { parts.push_back(trim(inner.substr(start, i - start))); start = i + 1; }
    }
    parts.push_back(trim(inner.substr(start)));
    std::string name = parts[0];
    bool additive = false, lazy = false;
    for (size_t k = 1; k < parts.size(); k++) {
        std::string m = upper(parts[k]);
        if (m == "ADDITIVE") additive = true;
        else if (m == "GAP") lazy = true;
    }
    std::string ref = "&" + name;
    if (lazy) ref += "(#*)";
    if (additive) ref = "[+[" + ref + "]]";
    return ref;
}

static std::string bareName(const std::string& raw) {
    std::string s = trim(raw);
    if (kwAt(s, 0, "FUNCTION") && s.size() > 8 && s[8] == '(' && s.back() == ')')
        return trim(s.substr(9, s.size() - 10));
    return s;
}

static bool stmtKeywordAt(const std::string& s, size_t i) {
    return kwAt(s, i, "HEAR") || kwAt(s, i, "FIND") || kwAt(s, i, "MATCH")
        || kwAt(s, i, "LET") || kwAt(s, i, "REALM") || kwAt(s, i, "DEFINE")
        || (s.compare(i, 4, "eve:") == 0);
}

std::string ExpandaEVE::toCompactaEVE(const std::string& src) {
    std::vector<std::string> stmts;
    size_t i = 0, last = 0;
    bool started = false;
    bool inq = false;
    bool lineStart = true;
    while (i < src.size()) {
        char ch = src[i];
        if (ch == '"') inq = !inq;
        if (!inq && lineStart && !std::isspace((unsigned char)ch) && stmtKeywordAt(src, i)) {
            if (started) stmts.push_back(src.substr(last, i - last));
            last = i;
            started = true;
        }
        if (ch == '\n') lineStart = true;
        else if (!std::isspace((unsigned char)ch)) lineStart = false;
        i++;
    }
    if (started) stmts.push_back(src.substr(last));

    std::string out;
    for (std::string raw : stmts) {
        if (raw.compare(0, 4, "eve:") == 0) {
            std::string h = trim(raw);
            size_t nl = h.find('\n');
            out += (nl == std::string::npos ? h : h.substr(0, nl));
            out += '\n';
            continue;
        }
        std::string stmt;
        bool q = false;
        for (size_t k = 0; k < raw.size(); k++) {
            char ch = raw[k];
            if (ch == '"') q = !q;
            if (!q && std::isspace((unsigned char)ch)) {
                if (!stmt.empty() && stmt.back() != ' ') stmt += ' ';
            } else {
                stmt += ch;
            }
        }
        stmt = trim(stmt);
        if (stmt.empty()) continue;

        std::string U = upper(stmt);
        if (U.rfind("REALM", 0) == 0) {
            out += "realm " + trim(stmt.substr(5)) + "\n";
            continue;
        }
        if (U.rfind("LET", 0) == 0) {
            size_t be = U.find(" BE ");
            std::string nameRaw = trim(stmt.substr(3, be - 3));
            std::string nameMark;
            if (nameRaw.size() >= 7 && upper(nameRaw.substr(nameRaw.size() - 7)) == "(WHOLE)") {
                nameMark = "[^^]"; nameRaw = trim(nameRaw.substr(0, nameRaw.size() - 7));
            } else if (nameRaw.size() >= 6 && upper(nameRaw.substr(nameRaw.size() - 6)) == "(ROOT)") {
                nameMark = "[*^]"; nameRaw = trim(nameRaw.substr(0, nameRaw.size() - 6));
            }
            std::string name = bareName(nameRaw) + nameMark;
            if (!name.empty() && name[0] == '&') name = name.substr(1);
            std::string members = normBody(trim(stmt.substr(be + 4)));
            std::string m; bool q2 = false;
            for (size_t z = 0; z < members.size(); z++) {
                char ch = members[z];
                if (ch == '"') q2 = !q2;
                if (!q2 && ch == ' ' &&
                    ((!m.empty() && m.back() == '|') ||
                     (z + 1 < members.size() && members[z + 1] == '|'))) continue;
                m += ch;
            }
            out += "let &" + name + " be " + m + "\n";
            continue;
        }
        if (U.rfind("DEFINE", 0) == 0) {
            std::string rest = trim(stmt.substr(6));
            std::string ru2 = upper(rest);
            size_t as = ru2.find(" AS ");
            if (as == std::string::npos) throw std::invalid_argument("DEFINE without AS: " + stmt);
            out += "define " + trim(rest.substr(0, as)) + " as " + trim(rest.substr(as + 4)) + "\n";
            continue;
        }
        bool strict = (U.rfind("MATCH", 0) == 0);
        bool fuzzy = (U.rfind("HEAR", 0) == 0) || (U.rfind("FIND", 0) == 0);
        if (strict || fuzzy) {
            size_t verbLen = strict ? 5 : 4;
            std::string rest = trim(stmt.substr(verbLen));
            size_t as = std::string::npos;
            int depth = 0; bool q2 = false;
            std::string ru = upper(rest);
            for (size_t k = 0; k + 1 < rest.size(); k++) {
                char ch = rest[k];
                if (ch == '"') q2 = !q2;
                if (q2) continue;
                if (ch == '(' || ch == '[') depth++;
                else if (ch == ')' || ch == ']') depth--;
                else if (depth == 0 && ru.compare(k, 4, " AS ") == 0) { as = k; break; }
            }
            if (as == std::string::npos) throw std::invalid_argument("statement without AS: " + stmt);
            std::string name = bareName(rest.substr(0, as));
            std::string body = trim(rest.substr(as + 4));

            std::string flagsPrefix, flagsSuffix;
            std::string scanned;
            {
                size_t p = 0;
                while (p < body.size()) {
                    if (kwAt(body, p, "FLAG") && p + 4 < body.size() && body[p+4] == '(') {
                        size_t close = matchParen(body, p + 4);
                        std::string inner = trim(body.substr(p + 5, close - (p + 5)));
                        size_t sp = inner.find_last_of(" \t");
                        std::string fn = trim(inner.substr(0, sp));
                        std::string yn = upper(trim(inner.substr(sp + 1)));
                        std::string v = (yn == "YES" || yn == "Y" || yn == "TRUE") ? "y" : "n";
                        flagsPrefix += "{" + fn + ":" + v + "?";
                        flagsSuffix += "}";
                        p = close + 1;
                        while (p < body.size() && std::isspace((unsigned char)body[p])) p++;
                    } else {
                        scanned += body[p];
                        p++;
                    }
                }
            }
            std::string bu = upper(scanned);
            bool hasExpandaSyntax =
                bu.find("FUNCTION(") != std::string::npos || bu.find("(WHOLE)") != std::string::npos ||
                bu.find("(ROOT)") != std::string::npos || bu.find("SUFFIX(") != std::string::npos ||
                bu.find("NEVER(") != std::string::npos || bu.find("AFTER(") != std::string::npos ||
                bu.find("BEFORE(") != std::string::npos || bu.find("ALSO(") != std::string::npos ||
                bu.find("SWAP(") != std::string::npos || bu.find("SKELETON(") != std::string::npos ||
                bu.find("NOTE(") != std::string::npos || bu.find("ALIAS(") != std::string::npos ||
                bu.find("MAYBE") != std::string::npos || bu.find(" OR ") != std::string::npos ||
                bu.find(" PLURAL") != std::string::npos || bu.find(" ADDITIVE") != std::string::npos ||
                bu.find(" GAP") != std::string::npos;
            if (!hasExpandaSyntax) {
                std::string verb0 = strict ? "match " : "hear ";
                out += verb0 + name + " as " + flagsPrefix + scanned + flagsSuffix + "\n";
                continue;
            }
            std::string body2 = normBody(scanned);
            if (!strict) {
                std::string s; bool q3 = false;
                for (char ch : body2) {
                    if (ch == '"') q3 = !q3;
                    if (!q3 && ch == ' ') continue;
                    s += ch;
                }
                body2 = s;
            }
            std::string verb = strict ? "match " : "hear ";
            out += verb + name + " as " + flagsPrefix + body2 + flagsSuffix + "\n";
            continue;
        }
        out += stmt + "\n";
    }
    return out;
}

Program ExpandaEVE::parse(const std::string& expandaSource) {
    return Lang::parse(toCompactaEVE(expandaSource));
}

}
