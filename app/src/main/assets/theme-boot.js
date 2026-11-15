/* Nạp trước khi stylesheet áp dụng để tránh nhấp nháy nền sáng khi app ở chế độ tối.
   Dùng chung khóa localStorage và định dạng JSON với app.js (cùng KEYS.theme). */
(function () {
  "use strict";

  /* Hang so: tranh gan literal truc tiep o cap lenh (dung quy uoc cua app.js) */
  var KEYS = { theme: "demo.theme" };
  var THEMES = { dark: "dark", light: "light" };

  function isKnown(theme) {
    return theme === THEMES.dark || theme === THEMES.light;
  }

  function storedTheme() {
    try {
      var raw = window.localStorage.getItem(KEYS.theme);
      return raw === null ? null : JSON.parse(raw);
    } catch (e) {
      return null;
    }
  }

  function systemTheme() {
    var query = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)");
    return query && query.matches ? THEMES.dark : THEMES.light;
  }

  var theme = storedTheme();
  if (!isKnown(theme)) {
    theme = systemTheme();
  }

  document.documentElement.setAttribute("data-theme", theme);
})();
