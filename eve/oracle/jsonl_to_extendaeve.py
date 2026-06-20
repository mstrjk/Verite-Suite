import sys, re

def match_close(s, i, op, cl):
    depth = 0
    while i < len(s):
        if s[i] == op: depth += 1
        elif s[i] == cl:
            depth -= 1
            if depth == 0: return i
        i += 1
    return -1

def split_top(body, sep='|'):
    out, depth, q, start = [], 0, False, 0
    i = 0
    while i < len(body):
        c = body[i]
        if c in '[{(': depth += 1
        elif c in ']})': depth -= 1
        elif c == sep and depth == 0:
            out.append(body[start:i]); start = i + 1
        i += 1
    out.append(body[start:])
    return out

def peel(pattern):
    flags, pre, post, miss, bone, note = [], None, None, None, None, None
    while True:
        m = re.match(r'^\{([A-Za-z0-9_\-]+):([yn])\?', pattern)
        if m and pattern.endswith('}') and m.group(1) not in ('pre','post','skel') and not m.group(1).startswith('#'):
            flags.append((m.group(1), m.group(2) == 'y'))
            pattern = pattern[m.end():-1]
        else: break
    if pattern.startswith('{pre:'):
        e = pattern.index('}'); pre = pattern[5:e]; pattern = pattern[e+1:]
    c = pattern.find('{#c:')
    if c >= 0:
        cl = match_close(pattern, c, '{', '}'); note = pattern[c+4:cl]; pattern = pattern[:c] + pattern[cl+1:]
    sk = pattern.find('{skel:')
    if sk >= 0 and pattern.rstrip().endswith('}'):
        cl = match_close(pattern, sk, '{', '}'); bone = pattern[sk+6:cl]; pattern = pattern[:sk] + pattern[cl+1:]
    pa = pattern.find('{post:')
    if pa >= 0:
        cl = match_close(pattern, pa, '{', '}'); post = pattern[pa+6:cl]; pattern = pattern[:pa] + pattern[cl+1:]
    v = pattern.find('[-[')
    if v >= 0:
        cl = match_close(pattern, v, '[', ']'); miss = pattern[v+3:cl-1]; pattern = pattern[:v] + pattern[cl+1:]
    return pattern, flags, pre, post, miss, bone, note

def skel_to_extenda(bone):
    arms = []
    for arm in split_top(bone):
        nums = re.findall(r'&(\d+)', arm)
        body = re.sub(r'&\d+', '!', arm)
        arms.append('%s(%s)' % (':'.join(nums), body))
    return ' OR '.join(arms)

def expand_inline(s):

    s = s.replace('(#*)', ' GAP ').replace('($^)', ' LETTERS ').replace('@', ' LETTER ')
    return ' '.join(s.split())

def render_operand(p):

    if p == '': return 'NONE'
    if p.startswith('&'): return 'function(%s)' % p[1:]
    return expand_inline(p)

def or_list(s):
    return ' OR '.join(render_operand(p) for p in split_top(s))

HAS_PLURAL = True

PLURAL_APPEND = ['s','z','es','ez','ies','iez','ing','ings','ed','er','ers','y','ys']

def collapse_suffix(arms_str):
    arms = split_top(arms_str)

    if not HAS_PLURAL:
        return ['suffix(%s)' % or_list(arms_str)]

    if any(c in arms_str for c in '[]()+') :
        return ['suffix(%s)' % or_list(arms_str)]
    if '' in arms or len(arms) < 2:
        return ['suffix(%s)' % or_list(arms_str)]
    base = min(arms, key=len)
    extras = [a for a in arms if a != base]
    def is_plural_of(a, b):
        if a in (b + e for e in PLURAL_APPEND):
            return True

        if b.endswith('y') and (a == b[:-1] + 'ies' or a == b[:-1] + 'iez'):
            return True
        return False
    if extras and all(is_plural_of(a, base) for a in extras):
        return ['suffix(%s)' % base, 'PLURAL']
    return ['suffix(%s)' % or_list(arms_str)]

def core_clauses(core, post=None):
    lines = []
    i = 0

    while i < len(core) and core[i] == '[':
        cl = match_close(core, i, '[', ']')
        inner = core[i+1:cl]
        if inner.endswith(';'):
            body = inner[:-1]
            marker = ''
            if body.endswith('^^'): marker = ' (WHOLE)'; body = body[:-2]
            elif body.endswith('*^'): marker = ' (ROOT)'; body = body[:-2]

            alias = 'function(%s)' % body[1:] if body.startswith('&') else body
            lines.append('ALIAS(%s%s)' % (alias, marker))
            i = cl + 1
        elif inner.startswith('+['):

            sub = inner[2:-1]
            lines.append(func_ref(sub, additive=True))
            i = cl + 1
        else:
            break

    mk = re.search(r'\[\*\^\]|\[\^\^\]', core[i:])
    if not mk:

        if core[i:]: lines.append(core[i:])
        return lines
    root_text = core[i:i+mk.start()]
    marker = '(ROOT)' if mk.group(0) == '[*^]' else '(WHOLE)'
    i += mk.end()

    if root_text.startswith('[['):
        segs = []
        kk = 1
        while kk < len(root_text) and root_text[kk] == '[':
            c2 = match_close(root_text, kk, '[', ']')
            seg = root_text[kk+1:c2]
            star = seg.endswith('*')
            segs.append((seg[:-1] if star else seg, star))
            kk = c2 + 1

        seg_str = ''.join(('[%s]' % s if star else s) for s, star in segs)
        lines.append(seg_str + (' (ROOT)' if marker == '(ROOT)' else ' (WHOLE)'))
    elif root_text.startswith('&'):

        rt = root_text[1:]
        lazy = rt.endswith('(#*)')
        if lazy: rt = rt[:-4]
        lines.append('function(%s)' % rt)
        if lazy: lines.append('GAP')
        lines.append('(ROOT)' if marker == '(ROOT)' else '(WHOLE)')
    else:

        pieces = emit_pieces(root_text)
        mk = '(WHOLE)' if marker == '(WHOLE)' else '(ROOT)'
        gapwords = ('GAP', 'LETTERS', 'LETTER')

        last_lit = -1
        for idx in range(len(pieces)):
            if pieces[idx] not in gapwords:
                last_lit = idx
        if last_lit >= 0 and not post:
            for idx, piece in enumerate(pieces):
                if idx == last_lit:
                    lines.append('%s%s' % (piece, mk))
                else:
                    lines.append(piece)
        else:
            for piece in pieces:
                lines.append(piece)
            if post:
                lines.append('AFTER(%s)' % or_list(post))
            lines.append(mk)

    if i < len(core) and core[i] == '|': i += 1
    rest = core[i:]

    j = 0
    while j < len(rest):
        if rest[j] == '|': j += 1; continue
        if rest.startswith('[/*|', j):
            cl = match_close(rest, j, '[', ']')
            arms = rest[j+1:cl]
            if arms.startswith('/*|'): arms = arms[3:]
            if HAS_PLURAL and (arms == '&s' or arms.startswith('s|z|es|ez')):
                lines.append('PLURAL')
            else:
                lines.append('suffix(%s)' % or_list(arms))
            j = cl + 1
        elif rest.startswith('[+[', j):

            cl = match_close(rest, j, '[', ']')
            inner = rest[j+3:cl-1]
            lines.append('ALSO(%s)' % or_list(inner))
            j = cl + 1
        elif rest.startswith('[swap:', j):
            cl = match_close(rest, j, '[', ']')
            inner = rest[j+6:cl]
            lines.append('SWAP(%s)' % ' OR '.join(split_top(inner)))
            j = cl + 1
        elif rest[j] == '[':
            cl = match_close(rest, j, '[', ']')
            arms = rest[j+1:cl]
            lines.extend(collapse_suffix(arms))
            j = cl + 1
        elif rest.startswith('(#*)', j):
            lines.append('GAP'); j += 4
        elif rest.startswith('($^)', j):
            lines.append('LETTERS'); j += 4
        elif rest.startswith('&!', j):

            k = j + 2
            while k < len(rest) and (rest[k].isalnum() or rest[k] == '_'):
                k += 1
            lines.append('MAYBE function(%s)' % rest[j+2:k])
            j = k
        elif rest.startswith('&', j):

            k = j + 1
            while k < len(rest) and (rest[k].isalnum() or rest[k] == '_'):
                k += 1
            lines.append(func_ref(rest[j+1:k]))
            j = k
        else:

            lines[-1] += rest[j] if lines else rest[j]
            j += 1
    return lines

def func_ref(sub, additive=False):
    name = sub
    lazy = False
    if name.startswith('&'): name = name[1:]
    if name.endswith('(#*)'): name = name[:-4]; lazy = True
    mods = []
    if additive: mods.append('ADDITIVE')
    if lazy: mods.append('GAP')
    inner = name + (' + ' + ' + '.join(mods) if mods else '')
    return 'function(%s)' % inner

def emit_pieces(text, first_marker=None, quote=False):

    out = []
    parts = re.split(r'(\(#\*\)|\(\$\^\)|@)', text)
    first_lit_done = False
    for p in parts:
        if p == '': continue
        if p == '(#*)': out.append('GAP')
        elif p == '($^)': out.append('LETTERS')
        elif p == '@': out.append('LETTER')
        else:
            if first_marker and not first_lit_done:
                out.append('%s%s' % (p, first_marker)); first_lit_done = True
            elif quote:
                out.append('"%s"' % p)
            else:
                out.append(p)
    if first_marker and not first_lit_done:
        out.insert(0, first_marker)
    return out

def needs_symbol(pattern):
    core, *_ = peel(pattern)

    return False

def to_extenda(name, pattern):
    if needs_symbol(pattern):
        return 'hear %s as %s' % (name, pattern)
    core, flags, pre, post, miss, bone, note = peel(pattern)
    clauses = core_clauses(core, post)
    if miss is not None: clauses.append('NEVER(%s)' % or_list(miss))
    if bone is not None: clauses.append('SKELETON(%s)' % skel_to_extenda(bone))

    if pre is not None: clauses.insert(0, 'BEFORE(%s)' % or_list(pre))
    for fn, val in flags: clauses.append('FLAG(%s %s)' % (fn, 'YES' if val else 'NO'))
    if note is not None: clauses.append('NOTE(%s)' % note)

    out = ['HEAR rule', 'AS']
    for cl in clauses:
        out.append('  ' + cl)
    return '\n'.join(out)

def jval(line, key):
    m = re.search(r'"%s"\s*:\s*"((?:[^"\\]|\\.)*)"' % re.escape(key), line)
    return m.group(1) if m else None

def main():
    global HAS_PLURAL
    src, out = sys.argv[1], sys.argv[2]
    lines = open(src, encoding='utf-8').read().splitlines()
    HAS_PLURAL = any('"&s"' in l and '"function"' in l for l in lines)
    eve = ['eve:5.4 english', '']
    idx = 0
    for line in lines:
        t = line.strip()
        if not t: continue
        if t.startswith('#'):
            header = t.strip('# ').strip('-').strip()
            if header and 'function' not in header.lower():
                slug = re.sub(r'[^a-z0-9]+', '_', header.lower()).strip('_')
                if slug:
                    eve.append(''); eve.append('REALM ' + slug)
            continue
        if '"function"' in t:
            nm = jval(t, 'function'); members = jval(t, 'is')
            if nm and members is not None:
                nm2 = nm[1:] if nm.startswith('&') else nm

                name_marker = ''
                if nm2.endswith('[^^]'): nm2 = nm2[:-4]; name_marker = ' (WHOLE)'
                elif nm2.endswith('[*^]'): nm2 = nm2[:-4]; name_marker = ' (ROOT)'
                mem = ' OR '.join(render_operand(p) for p in split_top(members))
                eve.append('LET function(%s)%s BE %s' % (nm2, name_marker, mem))
            continue
        if '"phrase"' in t:
            k = t.index('"phrase"'); o = t.index('"', t.index(':', k)+1); cl = t.rindex('"')
            pat = t[o+1:cl]
            idx += 1
            eve.append('')
            eve.append(to_extenda('rule%d' % idx, pat))
            continue
    open(out, 'w', encoding='utf-8', newline='\n').write('\n'.join(eve) + '\n')
    print('wrote %s (%d rules)' % (out, idx))

main()
