
  var cm = CodeMirror.fromTextArea(document.getElementById('code'), {
    lineNumbers: true,
    mode: 'htmlmixed',
    theme: 'material-darker',
    lineWrapping: true,
    autoCloseTags: true,
    autoCloseBrackets: true,
    indentUnit: 2,
    tabSize: 2,
    extraKeys: {
      "Tab": function(cmInstance) {
        if (cmInstance.somethingSelected()) cmInstance.indentSelection('add');
        else cmInstance.replaceSelection("  ");
      }
    }
  });

  // ===== DANH SÁCH ĐẦY ĐỦ CÁC THẺ HTML =====
  var HTML_TAGS = [
    { t:'a', d:'Định nghĩa một siêu liên kết.' },
    { t:'abbr', d:'Định nghĩa dạng viết tắt của một từ hoặc cụm từ dài hơn.' },
    { t:'acronym', d:'(Lỗi thời) Định nghĩa một từ viết tắt — dùng <abbr> thay thế.' },
    { t:'address', d:'Chỉ định thông tin liên hệ của tác giả.' },
    { t:'applet', d:'(Lỗi thời) Nhúng một applet Java — dùng <object> thay thế.' },
    { t:'area', d:'Xác định một khu vực cụ thể trong bản đồ hình ảnh.' },
    { t:'article', d:'Định nghĩa một bài viết.' },
    { t:'aside', d:'Định nghĩa nội dung liên quan lỏng lẻo đến nội dung trang.' },
    { t:'audio', d:'Nhúng âm thanh hoặc luồng âm thanh vào tài liệu HTML.' },
    { t:'b', d:'Hiển thị văn bản ở dạng chữ đậm.' },
    { t:'base', d:'Xác định URL cơ sở cho tất cả URL tương đối trong tài liệu.' },
    { t:'basefont', d:'(Lỗi thời) Chỉ định phông chữ cơ bản — dùng CSS thay thế.' },
    { t:'bdi', d:'Tách biệt đoạn văn bản khỏi văn bản xung quanh cho định dạng hai chiều.' },
    { t:'bdo', d:'Ghi đè lên hướng văn bản hiện tại.' },
    { t:'big', d:'(Lỗi thời) Hiển thị văn bản kích thước lớn — dùng CSS thay thế.' },
    { t:'blockquote', d:'Đoạn văn được trích dẫn từ một nguồn khác.' },
    { t:'body', d:'Xác định nội dung chính của tài liệu.' },
    { t:'br', d:'Tạo một dấu xuống dòng.' },
    { t:'button', d:'Tạo một nút có thể nhấp chuột.' },
    { t:'canvas', d:'Vùng trong tài liệu dùng để vẽ đồ họa bằng JavaScript.' },
    { t:'caption', d:'Xác định tiêu đề hoặc chú thích của bảng.' },
    { t:'center', d:'(Lỗi thời) Căn chỉnh nội dung vào giữa — dùng CSS thay thế.' },
    { t:'cite', d:'Chỉ dẫn một trích dẫn hoặc tham khảo từ nguồn khác.' },
    { t:'code', d:'Chỉ định văn bản dưới dạng mã máy tính.' },
    { t:'col', d:'Xác định thuộc tính cho một hoặc nhiều cột trong bảng.' },
    { t:'colgroup', d:'Chỉ định thuộc tính cho nhiều cột trong một bảng.' },
    { t:'data', d:'Liên kết nội dung với bản dịch đọc được bằng máy.' },
    { t:'datalist', d:'Tập hợp các tùy chọn định trước cho phần tử <input>.' },
    { t:'dd', d:'Mô tả hoặc giá trị cho thuật ngữ <dt> trong danh sách mô tả.' },
    { t:'del', d:'Biểu thị đoạn văn bản đã bị xóa khỏi tài liệu.' },
    { t:'details', d:'Tiện ích cho phép người dùng mở thông tin bổ sung theo yêu cầu.' },
    { t:'dfn', d:'Xác định một định nghĩa.' },
    { t:'dialog', d:'Xác định một hộp thoại hoặc cửa sổ con.' },
    { t:'dir', d:'(Lỗi thời) Danh sách thư mục — dùng <ul> thay thế.' },
    { t:'div', d:'Chỉ định một phân vùng hoặc một phần trong tài liệu.' },
    { t:'dl', d:'Định nghĩa một danh sách mô tả.' },
    { t:'dt', d:'Định nghĩa một thuật ngữ trong danh sách mô tả.' },
    { t:'em', d:'Xác định văn bản được nhấn mạnh.' },
    { t:'embed', d:'Nhúng ứng dụng bên ngoài (đa phương tiện) vào tài liệu HTML.' },
    { t:'fieldset', d:'Chỉ định một tập hợp các trường biểu mẫu có liên quan.' },
    { t:'figcaption', d:'Xác định chú thích hoặc mô tả cho hình ảnh.' },
    { t:'figure', d:'Thể hiện một nhân vật được minh họa trong tài liệu.' },
    { t:'font', d:'(Lỗi thời) Định kiểu chữ, màu, kích thước — dùng CSS thay thế.' },
    { t:'footer', d:'Thể hiện phần chân trang của tài liệu hoặc một phần.' },
    { t:'form', d:'Định nghĩa một biểu mẫu HTML để người dùng nhập dữ liệu.' },
    { t:'frame', d:'(Lỗi thời) Xác định một khung hình trong một tập hợp khung.' },
    { t:'frameset', d:'(Lỗi thời) Định nghĩa một tập hợp các khung hình.' },
    { t:'h1', d:'Định nghĩa tiêu đề cấp 1 (lớn nhất).' },
    { t:'h2', d:'Định nghĩa tiêu đề cấp 2.' },
    { t:'h3', d:'Định nghĩa tiêu đề cấp 3.' },
    { t:'h4', d:'Định nghĩa tiêu đề cấp 4.' },
    { t:'h5', d:'Định nghĩa tiêu đề cấp 5.' },
    { t:'h6', d:'Định nghĩa tiêu đề cấp 6 (nhỏ nhất).' },
    { t:'head', d:'Phần đầu tài liệu chứa thông tin về tài liệu như tiêu đề.' },
    { t:'header', d:'Đại diện cho phần đầu của tài liệu hoặc một phần.' },
    { t:'hgroup', d:'Xác định một nhóm các tiêu đề.' },
    { t:'hr', d:'Vẽ một đường thẳng nằm ngang.' },
    { t:'html', d:'Xác định thư mục gốc của một tài liệu HTML (chèn khung trang đầy đủ).' },
    { t:'i', d:'Hiển thị văn bản theo kiểu chữ nghiêng.' },
    { t:'iframe', d:'Hiển thị URL trong khung nội tuyến.' },
    { t:'img', d:'Biểu thị một hình ảnh.' },
    { t:'input', d:'Định nghĩa một điều khiển đầu vào.' },
    { t:'ins', d:'Xác định một khối văn bản đã được chèn vào tài liệu.' },
    { t:'kbd', d:'Chỉ định văn bản làm đầu vào bàn phím.' },
    { t:'keygen', d:'Cơ chế điều khiển tạo cặp khóa công khai–riêng tư.' },
    { t:'label', d:'Xác định nhãn cho một điều khiển <input>.' },
    { t:'legend', d:'Xác định chú thích cho phần tử <fieldset>.' },
    { t:'li', d:'Xác định một mục trong danh sách.' },
    { t:'link', d:'Xác định mối quan hệ với một nguồn tài nguyên bên ngoài.' },
    { t:'main', d:'Thể hiện nội dung chính hoặc nổi bật của tài liệu.' },
    { t:'map', d:'Định nghĩa một image-map phía máy khách.' },
    { t:'mark', d:'Đoạn văn bản được tô sáng để tham khảo.' },
    { t:'menu', d:'Thể hiện danh sách các lệnh.' },
    { t:'menuitem', d:'Định nghĩa một mục lệnh mà người dùng có thể thực hiện.' },
    { t:'meta', d:'Cung cấp siêu dữ liệu có cấu trúc về tài liệu.' },
    { t:'meter', d:'Biểu thị một giá trị đo trong một phạm vi đã biết.' },
    { t:'nav', d:'Xác định một phần các liên kết điều hướng.' },
    { t:'noframes', d:'(Lỗi thời) Nội dung thay thế cho trình duyệt không hỗ trợ frame.' },
    { t:'noscript', d:'Nội dung thay thế hiển thị khi trình duyệt không hỗ trợ script.' },
    { t:'object', d:'Định nghĩa một đối tượng được nhúng.' },
    { t:'ol', d:'Định nghĩa một danh sách có thứ tự.' },
    { t:'optgroup', d:'Xác định một nhóm các tùy chọn trong danh sách chọn.' },
    { t:'option', d:'Xác định một tùy chọn trong danh sách chọn.' },
    { t:'output', d:'Kết quả của một phép tính hoặc hành động của người dùng.' },
    { t:'p', d:'Định nghĩa một đoạn văn.' },
    { t:'param', d:'(Lỗi thời) Xác định tham số cho phần tử <object>.' },
    { t:'picture', d:'Chứa nhiều nguồn hình ảnh cho <img> Responsive.' },
    { t:'pre', d:'Chỉ định văn bản được định dạng sẵn (giữ nguyên khoảng trắng).' },
    { t:'progress', d:'Biểu thị tiến trình hoàn thành của một công việc.' },
    { t:'q', d:'Định nghĩa một trích dẫn ngắn.' },
    { t:'rp', d:'Nội dung dự phòng cho trình duyệt không hỗ trợ ruby.' },
    { t:'rt', d:'Chú thích phát âm trong ký hiệu ruby.' },
    { t:'ruby', d:'Chú thích phát âm cho chữ ký tự Trung/Nhật.' },
    { t:'s', d:'Hiển thị văn bản không còn chính xác hoặc đã bị gạch ngang.' },
    { t:'samp', d:'Chỉ định đầu ra mẫu từ chương trình máy tính.' },
    { t:'script', d:'Nhúng hoặc tham chiếu mã script thực thi.' },
    { t:'section', d:'Định nghĩa một phần trong tài liệu.' },
    { t:'select', d:'Tạo một danh sách chọn (dropdown).' },
    { t:'small', d:'Hiển thị văn bản cỡ nhỏ hơn.' },
    { t:'source', d:'Xác định nhiều nguồn đa phương tiện cho <video>/<audio>.' },
    { t:'span', d:'Nhóm các phần tử nội dòng để định kiểu.' },
    { t:'strike', d:'(Lỗi thời) Văn bản gạch ngang — dùng <del>/<s> thay thế.' },
    { t:'strong', d:'Xác định văn bản quan trọng (in đậm).' },
    { t:'style', d:'Chứa mã CSS định kiểu cho tài liệu.' },
    { t:'sub', d:'Hiển thị văn bản dạng chỉ số dưới.' },
    { t:'summary', d:'Tiêu đề hiển thị cho phần tử <details>.' },
    { t:'sup', d:'Hiển thị văn bản dạng chỉ số trên.' },
    { t:'svg', d:'Nhúng đồ họa vector (SVG) vào tài liệu.' },
    { t:'table', d:'Định nghĩa một bảng dữ liệu.' },
    { t:'tbody', d:'Nhóm nội dung thân của bảng.' },
    { t:'td', d:'Xác định một ô dữ liệu trong bảng.' },
    { t:'template', d:'Khung nội dung ẩn dùng để sao chép bằng script.' },
    { t:'textarea', d:'Tạo vùng nhập văn bản nhiều dòng.' },
    { t:'tfoot', d:'Nhóm nội dung chân của bảng.' },
    { t:'th', d:'Xác định ô tiêu đề (header) của bảng.' },
    { t:'thead', d:'Nhóm nội dung tiêu đề của bảng.' },
    { t:'time', d:'Biểu thị ngày/giờ (hoặc giá trị thời gian).' },
    { t:'title', d:'Định nghĩa tiêu đề cho tài liệu (hiện trên tab trình duyệt).' },
    { t:'tr', d:'Xác định một hàng ô trong bảng.' },
    { t:'track', d:'Xác định rãnh phụ đề cho <audio>/<video>.' },
    { t:'tt', d:'(Lỗi thời) Văn bản kiểu máy điện báo — dùng CSS thay thế.' },
    { t:'u', d:'Hiển thị văn bản có gạch chân.' },
    { t:'ul', d:'Định nghĩa một danh sách không có thứ tự.' },
    { t:'var', d:'Định nghĩa một biến trong lập trình/toán học.' },
    { t:'video', d:'Nhúng nội dung video vào tài liệu HTML.' },
    { t:'wbr', d:'Biểu thị cơ hội xuống dòng hợp lý trong từ dài.' }
  ];

  // Các thẻ tự đóng (không có thẻ đóng)
  var VOID_TAGS = ['area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'];

  // Gợi ý thẻ khi gõ '<'
  function htmlTagHint(cmInstance) {
    var cur = cmInstance.getCursor();
    var line = cmInstance.getLine(cur.line);
    var start = cur.ch;
    while (start > 0 && /[a-zA-Z0-9-]/.test(line.charAt(start - 1))) start--;
    // Gộp cả dấu '<' vào vùng thay thế để không sinh ra '<<html>'
    if (start > 0 && line.charAt(start - 1) === '<') start--;
    var prefix = line.slice(start, cur.ch).replace('<', '').toLowerCase();
    var list = HTML_TAGS.filter(function(h) {
      return h.t.indexOf(prefix) === 0;
    });
    // Nếu không khớp thẻ nào thì hiển thị toàn bộ danh sách thẻ
    if (!list.length) list = HTML_TAGS;
    return {
      list: list.map(function(h) {
        // Tạo cặp thẻ mở - đóng (thẻ void thì chỉ có thẻ mở)
        var isVoid = VOID_TAGS.indexOf(h.t) !== -1;
        var full = isVoid ? '<' + h.t + '>' : '<' + h.t + '></' + h.t + '>';
        return {
          text: full,
          displayText: '<' + h.t + '>  —  ' + h.d,
          // Chèn cả cặp thẻ rồi đặt con trỏ vào giữa (sau tên thẻ mở)
          hint: function(cmI, data, comp) {
            cmI.replaceRange(comp.text, data.from, data.to);
            var midIndex = cmI.indexFromPos(data.from) + h.t.length + 1;
            cmI.setCursor(cmI.posFromIndex(midIndex));
          }
        };
      }),
      from: CodeMirror.Pos(cur.line, start),
      to: cur
    };
  }

  // Ghi đè hành vi hoàn thành: chèn cặp thẻ mở/đóng
  function maybeShowTagHint(cmInstance, typedText) {
    if (typedText && typedText.indexOf('<') !== -1) {
      cmInstance.showHint({
        hint: htmlTagHint,
        completeSingle: false,
        closeOnUnfocus: true
      });
    } else if (cmInstance.state.completionActive) {
      // Cập nhật danh sách khi đang gõ tiếp
      cmInstance.state.completionActive.update({ hint: htmlTagHint });
    }
  }

  // Cách 1: sự kiện inputRead (bàn phím desktop / nhập trực tiếp)
  cm.on('inputRead', function(cmInstance, change) {
    maybeShowTagHint(cmInstance, change.text && change.text.join(''));
  });

  // Cách 2 (dự phòng cho bàn phím ảo Android dùng Gboard/composition):
  // sau mỗi lần nhả phím, kiểm tra ký tự ngay trước con trỏ có phải '<' không
  cm.on('keyup', function(cmInstance, e) {
    // Bỏ qua các phím điều hướng để không mở popup liên tục
    if ([8,9,13,27,37,38,39,40,16,17,18,20,224].indexOf(e.keyCode) !== -1) return;
    var cur = cmInstance.getCursor();
    var line = cmInstance.getLine(cur.line) || '';
    var before = line.charAt(cur.ch - 1);
    var inTag = false;
    // Đang ở trong thẻ (có '<' chưa đóng '>' phía trước)?
    var lt = line.lastIndexOf('<', cur.ch - 1);
    var gt = line.lastIndexOf('>', cur.ch - 1);
    if (lt > gt) inTag = true;
    if (before === '<' || (inTag && !cmInstance.state.completionActive)) {
      if (!cmInstance.state.completionActive) {
        maybeShowTagHint(cmInstance, before === '<' ? '<' : null);
      }
    }
  });

  // ===== TỰ ĐỘNG CHÈN KHUNG HTML ĐẦY ĐỦ KHI GÕ <html> =====
  var HTML_SKELETON = '<!DOCTYPE html>\n<html>\n<head>\n<title>Tên Của Trang Web</title>\n</head>\n<body>\n<p>Nội Dung Của Trang Web</p>\n</body>\n</html>';

  // Thay toàn bộ tài liệu bằng khung mẫu và đặt con trỏ vào tên trang
  function insertHtmlSkeleton(cmInstance) {
    cmInstance.replaceRange(
      HTML_SKELETON,
      CodeMirror.Pos(0, 0),
      CodeMirror.Pos(cmInstance.lastLine(), cmInstance.getLine(cmInstance.lastLine()).length),
      '+skeleton'
    );
    // Con trỏ tới vị trí "Tên Của Trang Web"
    cmInstance.setCursor(CodeMirror.Pos(2, 7 + 'Tên Của Trang Web'.length));
    setTimeout(function() { cmInstance.focus(); }, 50);
  }

  // ===== FIX LỖI U003C: bàn phím ảo Android đôi khi gửi '<' dưới dạng chuỗi '\u003C'/'U003C' =====
  cm.on('change', function(cmInstance, change) {
    if (!change.text) return;
    var joined = change.text.join('\n');
    if (joined.indexOf('\\u003C') === -1 && joined.indexOf('U003C') === -1) return;
    var fixed = joined
      .replace(/\\u003C/gi, '<')
      .replace(/(^|[^A-Za-z0-9])U003C/gi, '$1<');
    cmInstance.replaceRange(
      fixed,
      change.from,
      change.to,
      '+fixU003C'
    );
  });

  cm.on('change', function(cmInstance, change) {
    if (change.origin === '+skeleton' || change.origin === 'setValue') return;
    // Chờ 10ms cho autoCloseTags hoàn tất, sau đó kiểm tra
    setTimeout(function() {
      var lineText = cmInstance.getLine(0) || '';
      // Dòng đầu chứa <html> và tài liệu chưa đầy đủ
      if (/^\s*<html>/i.test(lineText)) {
        var full = cmInstance.getValue();
        if (full.toLowerCase().indexOf('<!doctype') === -1) {
          insertHtmlSkeleton(cmInstance);
        }
      }
    }, 10);
  });

  // Làm sạch chuỗi: bàn phím ảo Android đôi khi gửi '<' thành '\u003C'/'U003C'
  function sanitizeU003C(s) {
    return (s || '')
      .replace(/\\u003C/gi, '<')
      .replace(/(^|[^A-Za-z0-9])U003C/g, '$1<');
  }

  // ===== POPUP GỢI Ý THẺ HTML =====
  var tagPickerEl = document.getElementById('tagPicker');
  var tpListEl = document.getElementById('tpList');
  var tpCloseEl = document.getElementById('tpClose');
  var tpSearchEl = document.getElementById('tpSearch');
  var tpOpenEl = document.getElementById('tpOpen');
  var tagFilterText = '';

  // Vẽ lại danh sách thẻ theo từ khóa lọc
  function renderTagPicker() {
    if (!tpListEl) return;
    var key = (tagFilterText || '').toLowerCase().trim();
    var list = HTML_TAGS.filter(function(h) {
      if (!key) return true;
      if (h.t.toLowerCase().indexOf(key) !== -1) return true;
      return (h.d || '').toLowerCase().indexOf(key) !== -1;
    });
    if (!list.length) {
      tpListEl.innerHTML = '<div class="tp-empty">Không tìm thấy thẻ phù hợp</div>';
      return;
    }
    var html = '';
    for (var i = 0; i < list.length; i++) {
      html += '<div class="tp-item" data-tag="' + list[i].t + '">'
        + '<span class="tp-tag">&lt;' + list[i].t + '&gt;</span>'
        + '<span class="tp-desc">' + (list[i].d || '') + '</span>'
        + '</div>';
    }
    tpListEl.innerHTML = html;
  }

  // Chèn thẻ vào vị trí con trỏ (thẻ rỗng chỉ chèn thẻ mở)
  function insertTagPair(tagName) {
    if (!tagName) return;
    var isVoid = VOID_TAGS.indexOf(tagName) !== -1;
    var text = isVoid
      ? ('<' + tagName + '>')
      : ('<' + tagName + '></' + tagName + '>');
    var cur = cm.getCursor();
    cm.replaceRange(text, cur, undefined, '+tagPicker');
    // Đặt con trỏ vào giữa cặp thẻ mở/đóng để gõ nội dung
    if (!isVoid) {
      cm.setCursor(CodeMirror.Pos(cur.line, cur.ch + tagName.length + 2));
    }
    setTimeout(function() { cm.focus(); }, 30);
  }

  // Mở popup chọn thẻ
  function openTagPicker() {
    if (!tagPickerEl) return;
    tagFilterText = '';
    if (tpSearchEl) tpSearchEl.value = '';
    renderTagPicker();
    tagPickerEl.classList.add('open');
    if (tpListEl) tpListEl.scrollTop = 0;
  }

  // Đóng popup chọn thẻ
  function closeTagPicker() {
    if (!tagPickerEl) return;
    tagPickerEl.classList.remove('open');
    setTimeout(function() { cm.focus(); }, 30);
  }

  // Cho phía Java gọi được
  window.openTagPicker = openTagPicker;
  window.closeTagPicker = closeTagPicker;

  // Gắn sự kiện cho popup
  if (tpListEl) {
    tpListEl.addEventListener('click', function(ev) {
      var node = ev.target;
      while (node && node !== tpListEl && !node.getAttribute('data-tag')) {
        node = node.parentNode;
      }
      if (!node || node === tpListEl) return;
      var tagName = node.getAttribute('data-tag');
      if (!tagName) return;
      closeTagPicker();
      insertTagPair(tagName);
    });
  }
  if (tpCloseEl) tpCloseEl.addEventListener('click', closeTagPicker);
  if (tpOpenEl) tpOpenEl.addEventListener('click', openTagPicker);
  if (tpSearchEl) {
    tpSearchEl.addEventListener('input', function() {
      tagFilterText = this.value || '';
      renderTagPicker();
    });
  }

  // Đặt nội dung từ app truyền vào
  window.setCode = function(value, mode) {
    cm.setOption('mode', mode || 'htmlmixed');
    cm.setValue(sanitizeU003C(value));
  };

  // Lấy nội dung ra để app đọc
  window.getCode = function() {
    return sanitizeU003C(cm.getValue());
  };

  // Lắng nghe thay đổi -> báo lên app để auto-save
  cm.on('change', function() {
    if (window.AndroidBridge && window.AndroidBridge.onCodeChanged) {
      window.AndroidBridge.onCodeChanged(cm.getValue());
    }
  });
