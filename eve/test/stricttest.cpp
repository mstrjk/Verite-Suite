#include "../src/eve_strict.hpp"
#include "../src/eve.hpp"
#include <cstdio>
#include <string>
using namespace eve;

static int fails = 0;
static void chk(const char* l, bool got, bool want) {
    std::printf("%s %s\n", got == want ? "PASS" : "FAIL", l);
    if (got != want) fails++;
}
static void cap(const std::vector<Capture>& cs, const char* name, const char* want) {
    for (auto& c : cs) if (c.name == name) {
        bool ok = c.value == want;
        std::printf("%s   %s = \"%s\" (want \"%s\")\n", ok ? "PASS" : "FAIL", name, c.value.c_str(), want);
        if (!ok) fails++;
        return;
    }
    std::printf("FAIL   capture %s missing\n", name);
    fails++;
}

int main() {

    chk("literal fuck", Strict::compile("\"fuck\"").matches("fuck"), true);
    chk("4 digits ok", Strict::compile("4 digits").matches("2024"), true);
    chk("4 digits rejects 5", Strict::compile("4 digits").matches("20245"), false);

    std::string rule =
        "4 digits as year then \"-\" then 2 digits as month then \"-\" then 2 digits as day "
        "then space then word as level "
        "then space then \"User=\" then word as user "
        "then space then \"IP=\" then ip as addr";
    std::vector<Capture> caps;
    chk("logline", Strict::compile(rule).matches("2024-06-17 INFO User=jake IP=192.168.0.1", caps), true);
    cap(caps, "year", "2024"); cap(caps, "user", "jake"); cap(caps, "addr", "192.168.0.1");

    Registry reg;
    reg.registerClass("domains", {"com", "net", "org", "co.uk"});
    std::string url = "any ( word then \".\" ) then word then \".\" then &domains as tld";
    StrictPattern urlp = Strict::compile(url, reg);
    chk("url tumblr.com (0 subdomains)", urlp.matches("tumblr.com"), true);
    chk("url friends.tumblr.com (1 subdomain)", urlp.matches("friends.tumblr.com"), true);
    chk("url a.b.tumblr.com (2 subdomains)", urlp.matches("a.b.tumblr.com"), true);
    chk("url rejects bare tumblr", urlp.matches("tumblr"), false);
    chk("url rejects bad tld", urlp.matches("tumblr.xyz"), false);

    chk("many digit on 1", Strict::compile("many digit").matches("1"), true);
    chk("many digit on 123", Strict::compile("many digit").matches("123"), true);
    chk("many digit on empty", Strict::compile("many digit").matches(""), false);

    chk("maybe sign +5", Strict::compile("maybe \"-\" then digits").matches("-5"), true);
    chk("maybe sign 5", Strict::compile("maybe \"-\" then digits").matches("5"), true);

    chk("2 to 4 digits on 3", Strict::compile("2 to 4 digits").matches("123"), true);
    chk("2 to 4 digits on 1", Strict::compile("2 to 4 digits").matches("1"), false);
    chk("2 to 4 digits on 5", Strict::compile("2 to 4 digits").matches("12345"), false);

    chk("or yes", Strict::compile("\"yes\" or \"no\"").matches("no"), true);
    chk("or rejects maybe", Strict::compile("\"yes\" or \"no\"").matches("maybe"), false);

    std::string email = "word as local then \"@\" then word then \".\" then &domains";
    std::vector<Capture> ec;
    chk("email", Strict::compile(email, reg).matches("jake@gmail.com", ec), true);
    cap(ec, "local", "jake");

    std::printf(fails ? "\n%d FAILED\n" : "\nALL PASS\n", fails);
    return fails ? 1 : 0;
}
