# Script kiểm tra ngoặc chưa đóng trong mã nguồn Java (bỏ qua chuỗi và chú thích)
import sys

path = sys.argv[1]
s = open(path, encoding='utf-8').read()
stack = []
line = 1
i = 0
n = len(s)
while i < n:
    c = s[i]
    if c == '\n':
        line += 1
    if s[i:i+2] == '//':
        while i < n and s[i] != '\n':
            i += 1
        continue
    if s[i:i+2] == '/*':
        i += 2
        while i < n and s[i:i+2] != '*/':
            if s[i] == '\n':
                line += 1
            i += 1
        i += 2
        continue
    if c == '"':
        i += 1
        while i < n:
            if s[i] == '\\':
                i += 2
                continue
            if s[i] == '"':
                break
            if s[i] == '\n':
                line += 1
            i += 1
        i += 1
        continue
    if c == "'":
        i += 1
        while i < n:
            if s[i] == '\\':
                i += 2
                continue
            if s[i] == "'":
                break
            i += 1
        i += 1
        continue
    if c in '{([':
        stack.append((c, line))
    elif c in '})]':
        if stack:
            stack.pop()
    i += 1

print('unclosed:', stack[:8])
