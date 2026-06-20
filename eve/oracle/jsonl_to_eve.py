import sys, re, json

def find_top(s, sub, start=0):
    depth = 0
    i = start
    while i < len(s):
        c = s[i]
        if c in '[{(':
            depth += 1
        elif c in ']})':
            if depth > 0: depth -= 1
        elif depth == 0 and s.startswith(sub, i):
            return i
        i += 1
    return -1

def match_close(s, i):
    depth = 0
    while i < len(s):
        c = s[i]
        if c == '{': depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0: return i
        i += 1
    return -1

def match_close_sq(s, i):
    depth = 0
    while i < len(s):
        c = s[i]
        if c == '[': depth += 1
        elif c == ']':
            depth -= 1
            if depth == 0: return i
        i += 1
    return -1

def to_eve(pattern):
    flags = []
    pre = post = miss = bone = note = None

    while True:
        m = re.match(r'^\{([A-Za-z0-9_\-]+):([yn])\?', pattern)
        if m and pattern.endswith('}'):
            name = m.group(1)
            val = m.group(2) == 'y'

            if name in ('pre', 'post', 'skel') or name.startswith('#'):
                break
            inner = pattern[m.end():-1]
            flags.append((name, val))
            pattern = inner
        else:
            break

    if pattern.startswith('{pre:'):
        end = pattern.index('}')
        pre = pattern[5:end]
        pattern = pattern[end+1:]

    c = pattern.find('{#c:')
    if c >= 0:
        close = match_close(pattern, c)
        note = pattern[c+4:close]
        pattern = pattern[:c] + pattern[close+1:]

    sk = pattern.find('{skel:')
    if sk >= 0 and pattern.rstrip().endswith('}'):
        close = match_close(pattern, sk)
        bone = pattern[sk+6:close]
        pattern = pattern[:sk] + pattern[close+1:]

    pa = pattern.find('{post:')
    if pa >= 0:
        close = match_close(pattern, pa)
        post = pattern[pa+6:close]
        pattern = pattern[:pa] + pattern[close+1:]

    v = pattern.find('[-[')
    if v >= 0:
        close = match_close_sq(pattern, v)
        inner = pattern[v+3:close-1]
        miss = inner
        pattern = pattern[:v] + pattern[close+1:]

    body = pattern
    parts = [body]
    if miss is not None: parts.append('miss ' + miss)
    if bone is not None: parts.append('bone ' + bone)
    if pre is not None: parts.append('only if ' + pre)
    if post is not None: parts.append('then ' + post)
    for name, val in flags:
        parts.append('flag %s %s' % (name, 'yes' if val else 'no'))
    if note is not None: parts.append('note ' + note)
    return ' '.join(parts)

def jval(line, key):
    m = re.search(r'"%s"\s*:\s*"((?:[^"\\]|\\.)*)"' % re.escape(key), line)
    return m.group(1) if m else None

def main():
    src, out = sys.argv[1], sys.argv[2]
    lines = open(src, encoding='utf-8').read().splitlines()
    eve = ['eve:5.4 english', '']
    realm = None
    idx = 0
    for line in lines:
        t = line.strip()
        if not t: continue
        if t.startswith('#'):

            header = t.strip('# ').strip('-').strip()
            if header and 'function' not in header.lower():
                slug = re.sub(r'[^a-z0-9]+', '_', header.lower()).strip('_')
                if slug:
                    realm = slug
                    eve.append('')
                    eve.append('realm ' + slug)
            continue
        if '"function"' in t:
            name = jval(t, 'function')
            members = jval(t, 'is')
            if name and members is not None:
                eve.append('let %s be %s' % (name if name.startswith('&') else '&'+name, members))
            continue
        if '"phrase"' in t:

            k = t.index('"phrase"')
            o = t.index('"', t.index(':', k)+1)
            cl = t.rindex('"')
            pat = t[o+1:cl]
            idx += 1
            eve.append('hear rule%d as %s' % (idx, to_eve(pat)))
            continue
    open(out, 'w', encoding='utf-8', newline='\n').write('\n'.join(eve) + '\n')
    print('wrote %s (%d rules)' % (out, idx))

main()
