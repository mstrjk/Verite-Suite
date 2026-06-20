#ifndef EVE_EXPANDA_HPP
#define EVE_EXPANDA_HPP

#include "eve_lang.hpp"

#include <string>

namespace eve {

class ExpandaEVE {
public:
    static std::string toCompactaEVE(const std::string& expandaSource);
    static Program parse(const std::string& expandaSource);
};

}

#endif
