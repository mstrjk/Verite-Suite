#ifndef EVE_LANG_HPP
#define EVE_LANG_HPP

#include "eve.hpp"
#include "eve_strict.hpp"

#include <string>
#include <vector>

namespace eve {

struct Rule {
    enum class Kind { Fuzzy, Strict };
    Kind kind = Kind::Fuzzy;
    std::string realm;
    std::string name;
    std::string pattern;
    EvePattern compiled;
    StrictPattern strict;
};

struct Program {
    std::string version;
    std::string language;
    Registry registry;
    std::vector<Rule> rules;
    std::vector<std::pair<std::string, std::string>> defines;
};

class Lang {
public:
    static Program parse(const std::string& source);
    static std::string toPatternString(const std::string& ruleBody);
};

}

#endif
