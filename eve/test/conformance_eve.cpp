#include "../src/eve_lang.hpp"

#include <cstdio>
#include <fstream>
#include <string>
#include <vector>

using namespace eve;

int main(int argc, char** argv) {
    if (argc < 3) { std::fprintf(stderr, "usage: conformance_eve <file.eve> <golden.tsv>\n"); return 2; }
    std::ifstream f(argv[1]);
    if (!f) { std::fprintf(stderr, "cannot open %s\n", argv[1]); return 2; }
    std::string src((std::istreambuf_iterator<char>(f)), std::istreambuf_iterator<char>());

    Program prog = Lang::parse(src);

    std::ifstream gf(argv[2]);
    if (!gf) { std::fprintf(stderr, "cannot open %s\n", argv[2]); return 2; }

    long rows = 0, mism = 0;
    std::string line;
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
        bool want = (col[3] == "MATCH");
        if (idx < 0 || idx >= (int)prog.rules.size()) continue;
        bool got = prog.rules[idx].compiled.matches(cand);
        rows++;
        if (got != want) {
            mism++;
            if (mism <= 25)
                std::printf("MISMATCH idx=%d cand=[%s] eve=%s java=%s\n  pat=%s\n",
                    idx, cand.c_str(), got ? "MATCH" : "nomatch",
                    want ? "MATCH" : "nomatch", prog.rules[idx].pattern.c_str());
        }
    }
    std::printf("\n%ld rows, %ld mismatches\n", rows, mism);
    return mism == 0 ? 0 : 1;
}
