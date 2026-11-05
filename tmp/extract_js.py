import re, io, sys, os

base = 'app/src/main/assets/editor/index.html'
s = io.open(base, encoding='utf-8').read()
# Tìm script inline (không có src) sau thẻ body
i = s.find('<script>', s.find('<body'))
j = s.find('</script>', i)
code = s[i + 8:j]
io.open('tmp/main_inline.js', 'w', encoding='utf-8').write(code)
print('extracted chars:', len(code))
