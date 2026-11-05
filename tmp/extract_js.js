// Trích đoạn script inline trong index.html để kiểm tra cú pháp
const fs = require('fs');
const file = 'app/src/main/assets/editor/index.html';
const s = fs.readFileSync(file, 'utf8');
const bodyIdx = s.indexOf('<body');
const i = s.indexOf('<script>', bodyIdx);
const j = s.indexOf('</script>', i);
const code = s.slice(i + 8, j);
fs.writeFileSync('tmp/main_inline.js', code, 'utf8');
console.log('extracted chars:', code.length);
