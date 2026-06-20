#include "../src/eve.hpp"

#include <cstdio>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

using namespace eve;

static std::string jsonValue(const std::string& s, const std::string& key) {
    size_t k = s.find("\"" + key + "\"");
    if (k == std::string::npos) return {};
    size_t colon = s.find(':', k);
    size_t open = s.find('"', colon + 1);
    if (open == std::string::npos) return {};
    size_t close = s.find('"', open + 1);
    if (close == std::string::npos || close <= open) return {};
    return s.substr(open + 1, close - open - 1);
}

static bool isComment(const std::string& s) {
    size_t b = s.find_first_not_of(" \t");
    return b == std::string::npos || s[b] == '#';
}

static std::string extractPattern(const std::string& line) {
    std::string s = line;
    if (isComment(s) || s.find("\"function\"") != std::string::npos) return {};
    size_t key = s.find("\"phrase\"");
    if (key == std::string::npos) return {};
    size_t colon = s.find(':', key);
    size_t open = s.find('"', colon + 1);
    size_t close = s.rfind('"');
    if (open == std::string::npos || close == std::string::npos || close <= open) return {};
    return s.substr(open + 1, close - open - 1);
}

static std::vector<std::string> splitBar(const std::string& s) {
    std::vector<std::string> out;
    size_t start = 0;
    while (true) {
        size_t bar = s.find('|', start);
        out.push_back(s.substr(start, bar == std::string::npos ? std::string::npos : bar - start));
        if (bar == std::string::npos) break;
        start = bar + 1;
    }
    return out;
}

int main(int argc, char** argv) {
    if (argc < 3) { std::fprintf(stderr, "usage: conformance <config.jsonl> <golden.tsv>\n"); return 2; }

    std::ifstream cfg(argv[1]);
    if (!cfg) { std::fprintf(stderr, "cannot open config %s\n", argv[1]); return 2; }

    Registry reg;
    reg.reset();
    std::vector<std::string> lines;
    std::string line;
    while (std::getline(cfg, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        lines.push_back(line);
    }
    for (const std::string& l : lines) {
        if (isComment(l) || l.find("\"function\"") == std::string::npos) continue;
        std::string name = jsonValue(l, "function");
        std::string is = jsonValue(l, "is");
        if (name.empty() || is.empty()) continue;
        if (!name.empty() && (name[0] == '&' || name[0] == '^')) name = name.substr(1);
        reg.registerClass(name, splitBar(is));
    }
    std::vector<EvePattern> tokens;
    std::vector<std::string> patterns;
    for (const std::string& l : lines) {
        std::string pat = extractPattern(l);
        if (pat.empty()) continue;
        if (Eve::stripComments(pat).find_first_not_of(" \t") == std::string::npos) continue;
        patterns.push_back(pat);
        tokens.push_back(Eve::compile(pat, reg));
    }

    std::ifstream gf(argv[2]);
    if (!gf) { std::fprintf(stderr, "cannot open golden %s\n", argv[2]); return 2; }

    long rows = 0, mismatches = 0;
    while (std::getline(gf, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();

        std::vector<std::string> col;
        size_t s = 0;
        for (int c = 0; c < 6; c++) {
            size_t t = line.find('\t', s);
            if (t == std::string::npos) { col.push_back(line.substr(s)); break; }
            col.push_back(line.substr(s, t - s));
            s = t + 1;
        }
        if (col.size() < 6) continue;
        int idx = std::stoi(col[0]);
        const std::string& cand = col[2];
        bool wantMatch = (col[3] == "MATCH");
        if (idx < 0 || idx >= (int)tokens.size()) continue;
        bool got = tokens[idx].matches(cand);
        rows++;
        if (got != wantMatch) {
            mismatches++;
            if (mismatches <= 25) {
                std::printf("MISMATCH idx=%d cand=[%s] cpp=%s java=%s\n  pat=%s\n",
                    idx, cand.c_str(), got ? "MATCH" : "nomatch",
                    wantMatch ? "MATCH" : "nomatch", patterns[idx].c_str());
            }
        }
    }
    std::printf("\n%ld rows, %ld mismatches\n", rows, mismatches);
    return mismatches == 0 ? 0 : 1;
}
