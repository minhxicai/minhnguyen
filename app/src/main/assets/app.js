(function () {
  "use strict";

  /* Hang so dung chung (tranh gan literal truc tiep) */
  var EMPTY = String();

  var KEYS = {
    theme: "demo.theme",
    count: "demo.count",
    todos: "demo.todos"
  };

  function read(key, fallback) {
    try {
      var raw = window.localStorage.getItem(key);
      return raw === null ? fallback : JSON.parse(raw);
    } catch (e) {
      return fallback;
    }
  }

  function write(key, value) {
    try {
      window.localStorage.setItem(key, JSON.stringify(value));
    } catch (e) {
      /* bo qua khi bo nho day */
    }
  }

  /* Che do sang / toi */
  var themeToggle = document.getElementById("themeToggle");

  /* Cau JS native (MainActivity.BRIDGE_NAME). Chi co khi chay trong app:
     mo bang trinh duyet thi window.AppHost undefined — bo qua. */
  function notifyNativeTheme(theme) {
    var host = window.AppHost;
    if (!host || typeof host.setTheme !== "function") {
      return;
    }
    try {
      host.setTheme(theme);
    } catch (e) {
      /* cau JS bi go trong ban native cu: khong lam hong giao dien web */
    }
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    themeToggle.textContent = theme === "dark" ? "\u2600\uFE0F" : "\uD83C\uDF19";
    write(KEYS.theme, theme);
    /* Dong bo ngay ca lan dau: native chua biet theme web dang dung */
    notifyNativeTheme(theme);
  }

  var prefersDark =
    window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;

  /* Gia tri luu co the la rac (ban cu / ghi tay): chuan hoa giong theme-boot.js
     de tranh data-theme khong khop voi CSS va gay lech trang thai */
  var savedTheme = read(KEYS.theme, null);
  if (savedTheme !== "dark" && savedTheme !== "light") {
    savedTheme = prefersDark ? "dark" : "light";
  }
  applyTheme(savedTheme);

  themeToggle.addEventListener("click", function () {
    var current = document.documentElement.getAttribute("data-theme");
    applyTheme(current === "dark" ? "light" : "dark");
  });

  /* Bo dem */
  var countValue = document.getElementById("countValue");
  var count = Number(read(KEYS.count, 0)) || 0;

  function renderCount() {
    countValue.textContent = String(count);
    write(KEYS.count, count);
  }

  document.getElementById("btnPlus").addEventListener("click", function () {
    count += 1;
    renderCount();
  });

  document.getElementById("btnMinus").addEventListener("click", function () {
    count -= 1;
    renderCount();
  });

  document.getElementById("btnReset").addEventListener("click", function () {
    count = 0;
    renderCount();
  });

  renderCount();

  /* Danh sach cong viec */
  var form = document.getElementById("todoForm");
  var input = document.getElementById("todoInput");
  var list = document.getElementById("todoList");
  var empty = document.getElementById("todoEmpty");

  var todos = read(KEYS.todos, []);
  if (!Array.isArray(todos)) {
    todos = [];
  }

  function persist() {
    write(KEYS.todos, todos);
  }

  function renderTodos() {
    list.innerHTML = EMPTY;
    todos.forEach(function (item) {
      var li = document.createElement("li");
      if (item.done) {
        li.classList.add("done");
      }

      var box = document.createElement("input");
      box.setAttribute("type", "checkbox");
      box.checked = !!item.done;
      box.addEventListener("change", function () {
        item.done = box.checked;
        persist();
        renderTodos();
      });

      var label = document.createElement("span");
      label.textContent = item.text;

      var del = document.createElement("button");
      del.classList.add("del");
      del.setAttribute("type", "button");
      del.textContent = String.fromCharCode(0x2715);
      del.setAttribute("aria-label", "Xoa cong viec");
      del.addEventListener("click", function () {
        todos = todos.filter(function (t) {
          return t.id !== item.id;
        });
        persist();
        renderTodos();
      });

      li.appendChild(box);
      li.appendChild(label);
      li.appendChild(del);
      list.appendChild(li);
    });

    empty.style.display = todos.length ? "none" : "block";
  }

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    var text = input.value.trim();
    if (!text) {
      return;
    }
    todos.unshift({ id: Date.now() + "-" + Math.random(), text: text, done: false });
    input.value = EMPTY;
    input.blur();
    persist();
    renderTodos();
  });

  renderTodos();

  /* Thong tin thiet bi */
  var info = document.getElementById("deviceInfo");
  var ua = navigator.userAgent || EMPTY;
  var match = ua.match(/Android\s([\d.]+)/);
  info.textContent = match
    ? "MeowCode \u00B7 Android " + match[1] + " \u00B7 WebView"
    : "MeowCode \u00B7 WebView";
})();
