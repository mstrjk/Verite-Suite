import sys

def strip(src):
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            out.append(c); i += 1
            while i < n:
                out.append(src[i])
                if src[i] == '\\' and i + 1 < n:
                    out.append(src[i + 1]); i += 2; continue
                if src[i] == '"':
                    i += 1; break
                i += 1
            continue
        if c == "'":
            out.append(c); i += 1
            while i < n:
                out.append(src[i])
                if src[i] == '\\' and i + 1 < n:
                    out.append(src[i + 1]); i += 2; continue
                if src[i] == "'":
                    i += 1; break
                i += 1
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            while i < n and src[i] != '\n':
                i += 1
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '*':
            i += 2
            while i + 1 < n and not (src[i] == '*' and src[i + 1] == '/'):
                i += 1
            i += 2
            continue
        out.append(c); i += 1
    text = ''.join(out)
    lines = [ln.rstrip() for ln in text.split('\n')]
    res, blank = [], False
    for ln in lines:
        if ln.strip() == '':
            if blank:
                continue
            blank = True
        else:
            blank = False
        res.append(ln)
    while res and res[0].strip() == '':
        res.pop(0)
    return '\n'.join(res).rstrip() + '\n'

for path in sys.argv[1:]:
    with open(path, encoding='utf-8') as f:
        s = f.read()
    open(path, 'w', encoding='utf-8', newline='\n').write(strip(s))
    print('stripped', path)
