#include "../src/eve_lang.hpp"
#include <cstdio>
using namespace eve;

static int fails = 0;
static void chk(const char* l, bool got, bool want) {
    std::printf("%s %s\n", got == want ? "PASS" : "FAIL", l);
    if (got != want) fails++;
}
static void eqs(const char* l, const std::string& got, const std::string& want) {
    bool ok = got == want;
    std::printf("%s %s\n   got = %s\n  want = %s\n", ok ? "PASS" : "FAIL", l, got.c_str(), want.c_str());
    if (!ok) fails++;
}

int main() {
    eqs("translate veto+bone",
        Lang::toPatternString("ni[*^]|[g|ga]|[/*|&s] miss snigger|sniggers bone n&57gg&57r"),
        "ni[*^]|[g|ga]|[/*|&s][-[snigger|sniggers]]{skel:n&57gg&57r}");
    eqs("translate pre+then+flag+note",
        Lang::toPatternString("pedo[^^] only if &accuse then phile|file flag prof yes note fart in es"),
        "{prof:y?{pre:&accuse}pedo{post:phile|file}[^^]}{#c:fart in es}");
    eqs("translate flag no",
        Lang::toPatternString("kms[^^] flag pf no"),
        "{pf:n?kms[^^]}");
    eqs("translate bare",
        Lang::toPatternString("spic[^^]|[/*|&s]"),
        "spic[^^]|[/*|&s]");

    std::string src =
        "eve:5.4 english\n"
        "let &s be s|z|es|ez|y>ies|y>iez\n"
        "let &accuse be you are|looking for\n"
        "realm slurs\n"
        "hear nword as ni[*^]|[g|ga|gga|gger]|[/*|&s] miss snigger|sniggers\n"
        "hear fslur as [pha;]fa[*^]|[g|gg|]|[ggot|]|[gie|][/*|&s]\n"
        "hear pedo as pedo[^^] only if &accuse flag prof yes\n";
    Program p = Lang::parse(src);
    chk("version", p.version == "5.4", true);
    chk("language", p.language == "english", true);
    chk("rule count", p.rules.size() == 3, true);
    chk("realm set", p.rules[0].realm == "slurs", true);
    chk("name set", p.rules[0].name == "nword", true);

    for (auto& r : p.rules) {
        if (r.name == "nword") {
            chk("nword: nigger", r.compiled.matches("nigger"), true);
            chk("nword: niga", r.compiled.matches("niga"), true);
            chk("nword: snigger vetoed", r.compiled.matches("snigger"), false);
            chk("nword: chin clean", r.compiled.matches("chin"), false);
        }
        if (r.name == "fslur") {
            chk("fslur: fag", r.compiled.matches("fag"), true);
            chk("fslur: faggot", r.compiled.matches("faggot"), true);
            chk("fslur: frog clean", r.compiled.matches("frog"), false);
        }
        if (r.name == "pedo") {
            chk("pedo: bare needs accuser", r.compiled.matches("pedo"), false);
            chk("pedo: with accuser", r.compiled.matches("pedo", "you are a pedo"), true);
            chk("pedo: flag prof present", r.compiled.hasFlag("prof"), true);
            chk("pedo: flag prof true", r.compiled.flag("prof").value_or(false), true);
        }
    }

    std::printf(fails ? "\n%d FAILED\n" : "\nALL PASS\n", fails);
    return fails ? 1 : 0;
}
