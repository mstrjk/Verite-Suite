#ifndef EVE_HPP
#define EVE_HPP

#include <memory>
#include <optional>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <vector>

namespace eve {

struct ClassDef {
    std::vector<std::string> entries;
    std::optional<bool> hard;
};

class Registry {
public:
    void registerClass(std::string name, std::vector<std::string> entries);
    void reset();
    const ClassDef& get(const std::string& name) const;
    std::vector<std::string> entries(const std::string& name) const;

    std::string vowelClass() const;
private:
    std::unordered_map<std::string, ClassDef> classes_;
};

struct Part {
    enum class Kind { Lit, Gap, Any, Swap };
    Kind kind;
    std::string lit;
    bool gapLetters = false;
    struct SwapArm { std::string text; bool front; bool end; };
    std::vector<SwapArm> swap;
};

using PartList = std::vector<Part>;

struct Group {
    enum class Kind { Normal, Plus, Hard };
    std::vector<PartList> arms;
    bool optional = false;
    Kind kind = Kind::Normal;
};

class EvePattern {
public:
    EvePattern() = default;
    bool matches(const std::string& candidate) const { return matches(candidate, candidate); }
    bool matches(const std::string& candidate, const std::string& fullLine) const;

    bool isGated() const { return !precheck_.empty(); }

    const std::vector<std::pair<std::string, bool>>& flags() const { return flags_; }
    bool hasFlag(const std::string& name) const {
        for (const auto& f : flags_) if (f.first == name) return true;
        return false;
    }
    std::optional<bool> flag(const std::string& name) const {
        for (const auto& f : flags_) if (f.first == name) return f.second;
        return std::nullopt;
    }

    std::unordered_set<std::string> concreteWords() const;

private:
    friend class Compiler;

    bool postSatisfied(const std::string& candidate) const;
    bool matchesCore(const std::string& candidate) const;
    std::vector<int> reachableSlots(const std::string& cand, const std::vector<int>& rootEnds,
                                    const std::vector<const Group*>& positional) const;
    bool relaxedHit(const std::string& cand, const Group& g, const std::vector<int>& anchors) const;
    bool consumeChain(const std::string& cand, int pos, const std::vector<const Group*>& chain,
                      size_t gi, bool anyArmFired, bool literal) const;
    bool finish(const std::string& cand, int pos, bool anyArmFired, bool literal) const;

    std::vector<PartList> roots_;
    bool rootIsLiteral_ = false;
    std::vector<bool> rootLiterals_;
    std::vector<Group> groups_;
    std::vector<std::string> trailing_;
    std::unordered_set<std::string> skeletons_;
    std::vector<std::string> precheck_;
    std::vector<std::string> post_;
    std::string rootStem_;
    std::vector<std::string> veto_;
    std::vector<std::pair<std::string, bool>> flags_;
};

class Eve {
public:
    static std::string stripComments(const std::string& pattern);
    static EvePattern compile(const std::string& pattern, const Registry& reg);
};

}

#endif
