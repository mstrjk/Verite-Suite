#include "eve_expanda.hpp"

#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

int main(int argc, char** argv) {
    std::ifstream in(argv[1], std::ios::binary);
    std::stringstream ss; ss << in.rdbuf();
    std::string src = ss.str();

    std::vector<std::string> lines;
    std::string cur;
    for (char c : src) { cur += c; if (c == '\n') { lines.push_back(cur); cur.clear(); } }
    if (!cur.empty()) lines.push_back(cur);

    int upto = argc > 2 ? std::stoi(argv[2]) : (int)lines.size();
    std::string sub;
    for (int k = 0; k < upto && k < (int)lines.size(); k++) sub += lines[k];

    std::cerr << "trying first " << upto << " lines (" << sub.size() << " bytes)\n";
    std::string r = eve::ExpandaEVE::toCompactaEVE(sub);
    std::cerr << "OK, produced " << r.size() << " bytes\n";
    return 0;
}
