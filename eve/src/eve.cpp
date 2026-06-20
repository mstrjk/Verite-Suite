#include "eve.hpp"

#include <algorithm>
#include <stdexcept>

namespace eve {

static inline bool isLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
}

static inline bool startsWith(const std::string& s, const std::string& needle, size_t off) {
    if (off > s.size() || needle.size() > s.size() - off) return false;
    return s.compare(off, needle.size(), needle) == 0;
}
static inline bool startsWith(const std::string& s, const std::string& needle) {
    return startsWith(s, needle, 0);
}
static inline bool startsWith(const std::string& s, const char* needle, size_t off) {
    return startsWith(s, std::string(needle), off);
}

static inline bool isEmptyArm(const PartList& arm) {
    return arm.size() == 1 && arm[0].kind == Part::Kind::Lit && arm[0].lit.empty();
}

static inline bool isBoundaryBefore(const std::string& s, int pos) {
    return pos == 0 || !isLetter(s[pos - 1]);
}
static inline bool isBoundaryAfter(const std::string& s, int pos) {
    return pos >= (int)s.size() || !isLetter(s[pos]);
}
static int matchSwapArm(const std::string& cand, int pos, const Part::SwapArm& arm) {
    if (!startsWith(cand, arm.text, pos)) return -1;
    int after = pos + (int)arm.text.size();
    if (arm.front && !isBoundaryBefore(cand, pos)) return -1;
    if (arm.end && !isBoundaryAfter(cand, after)) return -1;
    return after;
}

static std::vector<int> consumeParts(const std::string& cand, int pos, const PartList& parts) {
    std::vector<int> frontier{pos};
    for (const Part& part : parts) {
        std::vector<int> next;
        for (int at : frontier) {
            switch (part.kind) {
                case Part::Kind::Lit:
                    if (startsWith(cand, part.lit, at)) next.push_back(at + (int)part.lit.size());
                    break;
                case Part::Kind::Gap: {
                    int p = at;
                    next.push_back(p);
                    while (p < (int)cand.size() && isLetter(cand[p]) == part.gapLetters) {
                        p++;
                        next.push_back(p);
                    }
                    break;
                }
                case Part::Kind::Any:
                    if (at < (int)cand.size() && isLetter(cand[at])) next.push_back(at + 1);
                    break;
                case Part::Kind::Swap:
                    for (const auto& arm : part.swap) {
                        int end = matchSwapArm(cand, at, arm);
                        if (end >= 0) next.push_back(end);
                    }
                    break;
            }
        }
        frontier = std::move(next);
        if (frontier.empty()) return frontier;
    }
    return frontier;
}

bool EvePattern::finish(const std::string& cand, int pos, bool anyArmFired, bool literal) const {
    if (!literal && !anyArmFired) return false;
    for (const std::string& suf : trailing_) {
        if (suf.find('>') != std::string::npos) continue;
        int end = pos + (int)suf.size();
        if (end == (int)cand.size() && startsWith(cand, suf, pos)) return true;
    }
    return false;
}

bool EvePattern::consumeChain(const std::string& cand, int pos,
                              const std::vector<const Group*>& chain, size_t gi,
                              bool anyArmFired, bool literal) const {
    if (gi >= chain.size()) return finish(cand, pos, anyArmFired, literal);
    for (const PartList& arm : chain[gi]->arms) {
        bool empty = isEmptyArm(arm);
        for (int after : consumeParts(cand, pos, arm)) {
            if (consumeChain(cand, after, chain, gi + 1, anyArmFired || !empty, literal)) return true;
        }
    }
    return false;
}

std::vector<int> EvePattern::reachableSlots(const std::string& cand, const std::vector<int>& rootEnds,
                                            const std::vector<const Group*>& positional) const {
    std::vector<int> slots;
    std::unordered_set<int> seen;
    auto add = [&](int v) { if (seen.insert(v).second) slots.push_back(v); };
    for (int e : rootEnds) add(e);
    std::vector<int> frontier = rootEnds;
    for (const Group* g : positional) {
        std::vector<int> next;
        for (int at : frontier) {
            next.push_back(at);
            for (const PartList& arm : g->arms) {
                if (isEmptyArm(arm)) continue;
                for (int e : consumeParts(cand, at, arm)) next.push_back(e);
            }
        }
        for (int v : next) add(v);
        frontier = std::move(next);
    }
    return slots;
}

bool EvePattern::relaxedHit(const std::string& cand, const Group& g,
                            const std::vector<int>& anchors) const {
    for (const PartList& arm : g.arms) {
        if (isEmptyArm(arm)) continue;
        for (int anchor : anchors) {
            for (int after : consumeParts(cand, anchor, arm)) {
                if (after == (int)cand.size()) return true;
            }
        }
    }
    return false;
}

bool EvePattern::matchesCore(const std::string& cand) const {
    std::vector<const Group*> positional, relaxed;
    for (const Group& g : groups_) {
        if (g.kind == Group::Kind::Hard) continue;
        (g.kind == Group::Kind::Normal ? positional : relaxed).push_back(&g);
    }
    for (const Group& g : groups_) {
        if (g.kind != Group::Kind::Hard) continue;
        for (const PartList& arm : g.arms) {
            if (isEmptyArm(arm)) continue;
            for (int after : consumeParts(cand, 0, arm)) {
                if (finish(cand, after, true, true)) return true;
            }
        }
    }
    for (size_t ri = 0; ri < roots_.size(); ri++) {
        const PartList& root = roots_[ri];
        bool thisLiteral = rootLiterals_[ri];
        std::vector<int> rootEnds = consumeParts(cand, 0, root);
        if (rootEnds.empty()) continue;
        if (thisLiteral) {
            for (int after : rootEnds) {
                if (finish(cand, after, true, true)) return true;
            }
        }
        for (int after : rootEnds) {
            if (consumeChain(cand, after, positional, 0, false, thisLiteral)) return true;
        }
        std::vector<int> plusAnchors = reachableSlots(cand, rootEnds, positional);
        for (const Group* g : relaxed) {
            if (relaxedHit(cand, *g, plusAnchors)) return true;
        }
    }
    return false;
}

bool EvePattern::postSatisfied(const std::string& candidate) const {
    if (post_.empty() || !startsWith(candidate, rootStem_)) return false;
    std::string rest = candidate.substr(rootStem_.size());
    for (const std::string& suf : post_) {
        if (rest == suf || rest == suf + "s") return true;
    }
    return false;
}

bool EvePattern::matches(const std::string& candidate, const std::string& fullLine) const {
    if (!precheck_.empty()) {
        bool activated = false;
        for (const std::string& act : precheck_) {
            if (fullLine.find(act) != std::string::npos) { activated = true; break; }
        }
        if (!activated && !postSatisfied(candidate)) return false;
    }
    for (const std::string& v : veto_) {
        if (fullLine.find(v) != std::string::npos) return false;
    }
    if (skeletons_.count(candidate)) return true;
    if (matchesCore(candidate)) return true;
    for (const std::string& suf : trailing_) {
        size_t gt = suf.find('>');
        if (gt == std::string::npos) continue;
        std::string from = suf.substr(0, gt), to = suf.substr(gt + 1);
        if (candidate.size() >= to.size() &&
            candidate.compare(candidate.size() - to.size(), to.size(), to) == 0) {
            std::string restored = candidate.substr(0, candidate.size() - to.size()) + from;
            if (matchesCore(restored)) return true;
        }
    }
    return false;
}

}
