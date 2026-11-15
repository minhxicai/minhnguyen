const fs = require('fs');
const path = require('path');

function walk(dir, out) {
  const acc = out || [];
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, acc);
    else if (e.name.slice(-4) === '.xml') acc.push(p);
  }
  return acc;
}

const ENTITIES = ['amp', 'lt', 'gt', 'quot', 'apos'];

function isDigit(c) { return c >= 48 && c <= 57; }
function isHexLetter(c) {
  return (c >= 97 && c <= 102) || (c >= 65 && c <= 70);
}

function checkAmp(src) {
  let count = 0;
  for (let i = 0; i < src.length; i++) {
    if (src[i] !== '&') continue;
    let j = i + 1;
    let word = String();
    while (j < src.length && word.length < 10 && src[j] !== ';') {
      word += src[j];
      j = j + 1;
    }
    if (src[j] !== ';') { count = count + 1; continue; }
    let ok = false;
    if (word.length > 1 && word.charCodeAt(0) === 35) {
      const body = word.slice(1);
      const head = body.charCodeAt(0);
      const hex = head === 120 || head === 88;
      const digits = hex ? body.slice(1) : body;
      ok = digits.length > 0;
      for (let k = 0; k < digits.length; k++) {
        const c = digits.charCodeAt(k);
        if (!(isDigit(c) || (hex && isHexLetter(c)))) ok = false;
      }
    } else {
      ok = ENTITIES.indexOf(word) >= 0;
    }
    if (!ok) count = count + 1;
  }
  return count;
}

function stripNoise(src) {
  let out = String();
  let i = 0;
  while (i < src.length) {
    if (src.startsWith('<?', i)) {
      const e1 = src.indexOf('?>', i);
      i = e1 < 0 ? src.length : e1 + 2;
    } else if (src.startsWith('<!--', i)) {
      const e2 = src.indexOf('-->', i);
      i = e2 < 0 ? src.length : e2 + 3;
    } else if (src.startsWith('<![CDATA[', i)) {
      const e3 = src.indexOf(']]>', i);
      i = e3 < 0 ? src.length : e3 + 3;
    } else if (src.startsWith('<!', i)) {
      const e4 = src.indexOf('>', i);
      i = e4 < 0 ? src.length : e4 + 1;
    } else {
      out = out + src[i];
      i = i + 1;
    }
  }
  return out;
}

function checkTags(src) {
  const stack = [];
  let i = 0;
  while (i < src.length) {
    if (src[i] !== '<') { i = i + 1; continue; }
    const end = src.indexOf('>', i);
    if (end < 0) return 'the < khong duoc dong tai vi tri ' + i;
    let inner = src.slice(i + 1, end);
    i = end + 1;

    let closing = false;
    if (inner[0] === '/') { closing = true; inner = inner.slice(1); }
    let selfClose = false;
    if (inner.length > 0 && inner[inner.length - 1] === '/') {
      selfClose = true;
      inner = inner.slice(0, inner.length - 1);
    }
    let name = String();
    for (let k = 0; k < inner.length; k++) {
      const c = inner[k];
      const code = c.charCodeAt(0);
      const isSpace = code === 32 || code === 9 || code === 10 || code === 13;
      if (isSpace) break;
      name = name + c;
    }
    if (name.length === 0) return 'ten the rong tai vi tri ' + i;

    if (closing) {
      const top = stack.pop();
      if (top !== name) return 'dong the sai: </' + name + '> nhung dang mo <' + top + '>';
    } else if (!selfClose) {
      stack.push(name);
    }
  }
  if (stack.length > 0) return 'chua dong the: ' + stack.join(', ');
  return null;
}

const files = walk('app/src/main');
let bad = 0;
for (const f of files) {
  const src = fs.readFileSync(f, 'utf8');
  const ampCount = checkAmp(src);
  if (ampCount > 0) {
    console.log('AMP_ERR ' + f + ' - so ky tu & chua escape: ' + ampCount);
    bad = bad + 1;
  }
  const tagErr = checkTags(stripNoise(src));
  if (tagErr) {
    console.log('TAG_ERR ' + f + ' - ' + tagErr);
    bad = bad + 1;
  }
}
if (bad === 0) console.log('XML_ALL_OK (' + files.length + ' tep)');
else console.log('XML_BAD=' + bad);
