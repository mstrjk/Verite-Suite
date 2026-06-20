#ifndef EVE_STRICT_HPP
#define EVE_STRICT_HPP

#include <memory>
#include <optional>
#include <string>
#include <vector>

namespace eve {

class Registry;
class EvePattern;

struct StrictTerm {
    enum class Kind {
        Literal,
        Digit, Digits,
        Letter, Letters,
        Word,
        Number,
        Ip,
        Space, Spaces,
        Anything,
        Group,
        Alt,
        Fuzzy,
    };
    Kind kind;
    std::string literal;
    int count = 0;
    std::string capture;

    int repMin = 1;
    int repMax = 1;

    std::vector<StrictTerm> group;
    std::vector<std::vector<StrictTerm>> alts;

    std::shared_ptr<EvePattern> fuzzy;
};

struct Capture {
    std::string name;
    std::string value;
};

class StrictPattern {
public:
    bool matches(const std::string& input) const;
    bool matches(const std::string& input, std::vector<Capture>& out) const;

    const std::vector<StrictTerm>& terms() const { return terms_; }

private:
    friend class StrictCompiler;
    std::vector<StrictTerm> terms_;
};

class Strict {
public:
    static StrictPattern compile(const std::string& body);
    static StrictPattern compile(const std::string& body, const Registry& reg);
};

}

#endif
