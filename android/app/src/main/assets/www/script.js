/* =====================================================
   Đồng Hồ Việt Nam - Đồng hồ / Bấm giờ / Hẹn giờ
   ===================================================== */

/* ---------- Hằng số múi giờ Việt Nam ---------- */
const VN_TIMEZONE = 'Asia/Ho_Chi_Minh';

/* ======================= 1. CHUYỂN TAB ======================= */
const tabs = document.querySelectorAll('.tab');
const panels = document.querySelectorAll('.panel');

tabs.forEach((tab) => {
  tab.addEventListener('click', () => {
    tabs.forEach((t) => t.classList.remove('active'));
    panels.forEach((p) => p.classList.remove('active'));
    tab.classList.add('active');
    document.getElementById('panel-' + tab.dataset.tab).classList.add('active');
  });
});

/* ======================= 2. ĐỒNG HỒ VIỆT NAM ======================= */
const clockTimeEl = document.getElementById('clock-time');
const clockDateEl = document.getElementById('clock-date');
const footerTzEl = document.getElementById('footer-tz');

// Định dạng giờ theo múi giờ Việt Nam
const timeFormatter = new Intl.DateTimeFormat('vi-VN', {
  timeZone: VN_TIMEZONE,
  hour: '2-digit', minute: '2-digit', second: '2-digit',
  hour12: false,
});

// Định dạng ngày/thứ theo múi giờ Việt Nam
const dateFormatter = new Intl.DateTimeFormat('vi-VN', {
  timeZone: VN_TIMEZONE,
  weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric',
});

/** Cập nhật đồng hồ mỗi giây */
function updateClock() {
  const now = new Date();
  clockTimeEl.textContent = timeFormatter.format(now);
  clockDateEl.textContent = dateFormatter.format(now);
  footerTzEl.textContent = 'GMT+07:00 · Việt Nam';
}

updateClock();
setInterval(updateClock, 1000);

/* ======================= 3. BẤM GIỜ (STOPWATCH) ======================= */
const swDisplay = document.getElementById('sw-display');
const swStartBtn = document.getElementById('sw-start');
const swLapBtn = document.getElementById('sw-lap');
const swResetBtn = document.getElementById('sw-reset');
const swLapsEl = document.getElementById('sw-laps');

let swRunning = false;       // Đang chạy hay không
let swElapsed = 0;           // Tổng thời gian đã tích luỹ (ms)
let swStartTimestamp = 0;    // Thời điểm bắt đầu lượt chạy hiện tại
let swRafId = null;          // ID requestAnimationFrame
let swLapCount = 0;          // Số thứ tự vòng
let swLastLapTime = 0;       // Thời gian tại vòng trước

/** Định dạng ms -> { main: 'HH:MM:SS', frac: '.cc' } */
function formatStopwatch(ms) {
  const totalMs = Math.max(0, Math.floor(ms));
  const centis = Math.floor((totalMs % 1000) / 10);
  const totalSec = Math.floor(totalMs / 1000);
  const sec = totalSec % 60;
  const min = Math.floor(totalSec / 60) % 60;
  const hour = Math.floor(totalSec / 3600);
  const pad = (n) => String(n).padStart(2, '0');
  return {
    main: `${pad(hour)}:${pad(min)}:${pad(sec)}`,
    frac: `.${pad(centis)}`,
  };
}

/** Lấy thời gian hiện tại của stopwatch */
function currentStopwatchTime() {
  return swRunning ? swElapsed + (performance.now() - swStartTimestamp) : swElapsed;
}

/** Vẽ giá trị stopwatch */
function renderStopwatch() {
  const { main, frac } = formatStopwatch(currentStopwatchTime());
  swDisplay.innerHTML = `${main}<span class="frac">${frac}</span>`;
}

/** Vòng lặp cập nhật stopwatch */
function swLoop() {
  renderStopwatch();
  if (swRunning) {
    swRafId = requestAnimationFrame(swLoop);
  }
}

/** Bắt đầu hoặc tạm dừng bấm giờ */
function toggleStopwatch() {
  if (swRunning) {
    // Tạm dừng
    swElapsed = currentStopwatchTime();
    swRunning = false;
    if (swRafId) cancelAnimationFrame(swRafId);
    swStartBtn.textContent = 'Tiếp tục';
  } else {
    // Bắt đầu / tiếp tục
    swStartTimestamp = performance.now();
    swRunning = true;
    swStartBtn.textContent = 'Tạm dừng';
    swLapBtn.disabled = false;
    swResetBtn.disabled = false;
    swLoop();
  }
}

/** Ghi lại một vòng */
function addLap() {
  const now = currentStopwatchTime();
  const lapDuration = now - swLastLapTime;
  swLastLapTime = now;
  swLapCount += 1;

  const empty = swLapsEl.querySelector('.empty');
  if (empty) empty.remove();

  const { main, frac } = formatStopwatch(lapDuration);
  const totalFmt = formatStopwatch(now);

  const li = document.createElement('li');
  li.innerHTML = `
    <span class="lap-index">Vòng ${swLapCount}</span>
    <span>${main}${frac}</span>
    <span style="color:#93a3c0;font-size:0.8rem">Tổng ${totalFmt.main}</span>
  `;
  swLapsEl.prepend(li);
}

/** Đặt lại bấm giờ về 0 */
function resetStopwatch() {
  swRunning = false;
  if (swRafId) cancelAnimationFrame(swRafId);
  swElapsed = 0;
  swLastLapTime = 0;
  swLapCount = 0;
  swStartBtn.textContent = 'Bắt đầu';
  swLapBtn.disabled = true;
  swResetBtn.disabled = true;
  swLapsEl.innerHTML = '<li class="empty">Chưa có vòng nào</li>';
  renderStopwatch();
}

swStartBtn.addEventListener('click', toggleStopwatch);
swLapBtn.addEventListener('click', addLap);
swResetBtn.addEventListener('click', resetStopwatch);
renderStopwatch();

/* ======================= 4. HẸN GIỜ (TIMER) ======================= */
const tmDisplay = document.getElementById('tm-display');
const tmStartBtn = document.getElementById('tm-start');
const tmResetBtn = document.getElementById('tm-reset');
const tmInputs = document.getElementById('tm-inputs');
const tmH = document.getElementById('tm-h');
const tmM = document.getElementById('tm-m');
const tmS = document.getElementById('tm-s');
const presetChips = document.querySelectorAll('.chip');

let tmRunning = false;       // Đang đếm ngược
let tmRemaining = 0;         // Thời gian còn lại (ms)
let tmEndTimestamp = 0;      // Thời điểm kết thúc dự kiến
let tmIntervalId = null;     // ID setInterval
let tmAlarm = false;         // Đang báo thức
let tmAlarmTimeout = null;   // Hẹn tự tắt chuông
let tmBeepInterval = null;   // Lặp tiếng bíp khi đang báo thức

/** Đọc giá trị từ 3 ô nhập -> tổng ms */
function readInputDuration() {
  const h = Math.min(99, Math.max(0, parseInt(tmH.value, 10) || 0));
  const m = Math.min(59, Math.max(0, parseInt(tmM.value, 10) || 0));
  const s = Math.min(59, Math.max(0, parseInt(tmS.value, 10) || 0));
  return (h * 3600 + m * 60 + s) * 1000;
}

/** Định dạng ms -> HH:MM:SS */
function formatDuration(ms) {
  const totalSec = Math.ceil(Math.max(0, ms) / 1000);
  const sec = totalSec % 60;
  const min = Math.floor(totalSec / 60) % 60;
  const hour = Math.floor(totalSec / 3600);
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(hour)}:${pad(min)}:${pad(sec)}`;
}

/** Vẽ thời gian còn lại */
function renderTimer() {
  tmDisplay.textContent = formatDuration(tmRemaining);
}

/** AudioContext dùng chung - Android chỉ cho phép khởi tạo sau thao tác chạm */
let sharedAudioCtx = null;

/** Lấy (và mở khoá) AudioContext */
function getAudioCtx() {
  const AudioCtx = window.AudioContext || window.webkitAudioContext;
  if (!AudioCtx) return null;
  try {
    if (!sharedAudioCtx) sharedAudioCtx = new AudioCtx();
    if (sharedAudioCtx.state === 'suspended') sharedAudioCtx.resume();
    return sharedAudioCtx;
  } catch (err) {
    console.warn('Không tạo được AudioContext:', err);
    return null;
  }
}

/** Phát âm thanh báo thức bằng Web Audio API */
function playBeep() {
  try {
    const ctx = getAudioCtx();
    if (!ctx) return;
    const now = ctx.currentTime;

    // Ba tiếng bíp liên tiếp
    for (let i = 0; i < 3; i++) {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.value = 880;
      osc.connect(gain);
      gain.connect(ctx.destination);

      const start = now + i * 0.35;
      gain.gain.setValueAtTime(0.0001, start);
      gain.gain.exponentialRampToValueAtTime(0.35, start + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.25);
      osc.start(start);
      osc.stop(start + 0.3);
    }
    // Không đóng ctx vì dùng chung cho các lần báo sau
  } catch (err) {
    console.warn('Không phát được âm thanh:', err);
  }
}

/** Rung thiết bị (nếu hỗ trợ) */
function vibrateDevice() {
  if (navigator.vibrate) {
    navigator.vibrate([400, 200, 400, 200, 600]);
  }
}

/** Kích hoạt báo thức khi hết giờ */
function triggerAlarm() {
  tmAlarm = true;
  tmRunning = false;
  tmRemaining = 0;
  if (tmIntervalId) clearInterval(tmIntervalId);
  renderTimer();

  tmDisplay.classList.add('alarm');
  tmStartBtn.textContent = 'Tắt chuông';

  // Lặp chuông trong suốt thời gian báo thức (giống đồng hồ báo thức thật)
  playBeep();
  vibrateDevice();
  notifyTimerDone();   // Thông báo hệ thống Android

  // Cứ 1.6s bíp lại một lần cho tới khi tắt chuông
  if (tmBeepInterval) clearInterval(tmBeepInterval);
  tmBeepInterval = setInterval(() => {
    if (!tmAlarm) return;
    playBeep();
    vibrateDevice();
  }, 1600);

  // Tự tắt chuông sau 8 giây
  tmAlarmTimeout = setTimeout(() => stopAlarm(), 8000);
}

/** Tắt trạng thái báo thức */
function stopAlarm() {
  tmAlarm = false;
  // Dừng lặp chuông trên Android
  if (tmBeepInterval) {
    clearInterval(tmBeepInterval);
    tmBeepInterval = null;
  }
  tmDisplay.classList.remove('alarm');
  if (tmAlarmTimeout) {
    clearTimeout(tmAlarmTimeout);
    tmAlarmTimeout = null;
  }
  tmStartBtn.textContent = 'Bắt đầu';
  tmRemaining = 0;
  tmInputs.style.opacity = '1';
  renderTimer();
}

/** Bắt đầu hoặc tạm dừng hẹn giờ */
function toggleTimer() {
  // Nếu đang báo thức thì nhấn nút = tắt chuông
  if (tmAlarm) {
    stopAlarm();
    return;
  }

  if (tmRunning) {
    // Tạm dừng
    tmRemaining = Math.max(0, tmEndTimestamp - Date.now());
    tmRunning = false;
    if (tmIntervalId) clearInterval(tmIntervalId);
    tmStartBtn.textContent = 'Tiếp tục';
    tmDisplay.classList.remove('running');
    return;
  }

  // Nếu chưa có thời gian còn lại thì đọc từ ô nhập
  if (tmRemaining <= 0) {
    tmRemaining = readInputDuration();
    if (tmRemaining <= 0) {
      tmDisplay.textContent = '00:00:00';
      showToast('Vui lòng nhập thời gian lớn hơn 0!');
      return;
    }
  }

  // Bắt đầu đếm ngược
  tmRunning = true;
  tmEndTimestamp = Date.now() + tmRemaining;
  tmStartBtn.textContent = 'Tạm dừng';
  tmDisplay.classList.add('running');
  tmInputs.style.opacity = '0.45';

  renderTimer();
  tmIntervalId = setInterval(() => {
    tmRemaining = tmEndTimestamp - Date.now();
    if (tmRemaining <= 0) {
      triggerAlarm();
    } else {
      renderTimer();
    }
  }, 200);
}

/** Đặt lại hẹn giờ */
function resetTimer() {
  tmRunning = false;
  if (tmIntervalId) clearInterval(tmIntervalId);
  stopAlarm();
  tmRemaining = 0;
  tmDisplay.classList.remove('running');
  tmStartBtn.textContent = 'Bắt đầu';
  tmInputs.style.opacity = '1';
  renderTimer();
}

/** Áp dụng preset nhanh */
function applyPreset(totalSeconds) {
  if (tmRunning) return; // Không cho đổi khi đang chạy
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  tmH.value = h;
  tmM.value = m;
  tmS.value = s;
  tmRemaining = totalSeconds * 1000;
  renderTimer();
}

tmStartBtn.addEventListener('click', toggleTimer);
tmResetBtn.addEventListener('click', resetTimer);
presetChips.forEach((chip) => {
  chip.addEventListener('click', () => applyPreset(parseInt(chip.dataset.sec, 10)));
});

// Khởi tạo hiển thị hẹn giờ từ giá trị mặc định trong ô nhập
tmRemaining = readInputDuration();
renderTimer();
/* =========================================================
   TỐI ƯU RIÊNG CHO ANDROID
   - Toast thay cho alert() (alert bị chặn trong PWA standalone)
   - Thông báo hệ thống khi hết giờ
   - Service Worker cho chế độ offline
   - Wake Lock giữ sáng màn hình khi đang đếm
   - Mở khoá âm thanh sau thao tác chạm đầu tiên
   ========================================================= */

/* ---------------- Toast thông báo nổi ---------------- */
const toastEl = document.getElementById('toast');
let toastTimeoutId = null;

/** Hiện thông báo nổi thay cho alert() */
function showToast(message, duration) {
  if (!toastEl) return;
  toastEl.textContent = message;
  toastEl.classList.add('show');
  if (toastTimeoutId) clearTimeout(toastTimeoutId);
  toastTimeoutId = setTimeout(() => toastEl.classList.remove('show'), duration || 2600);
}

/* ---------------- Thông báo hệ thống ---------------- */
/**
 * Xin quyền thông báo. Luôn trả về Promise để nơi gọi dùng await an toàn.
 * Lưu ý: WebView Android đời cũ trả về undefined thay vì Promise,
 * nên phải kiểm tra kiểu dữ liệu trước khi gọi .catch()/.then().
 */
function requestNotifyPermission() {
  if (!('Notification' in window)) return Promise.resolve('unsupported');
  if (Notification.permission !== 'default') {
    return Promise.resolve(Notification.permission);
  }
  try {
    // Android yêu cầu gọi trong ngữ cảnh thao tác người dùng
    const result = Notification.requestPermission();
    if (result && typeof result.then === 'function') {
      return result.catch(() => 'denied');
    }
  } catch (err) {
    console.warn('Không xin được quyền thông báo:', err);
  }
  // Bản cũ: quyền được cập nhật đồng bộ, đọc lại sau khi hàm kết thúc
  return Promise.resolve(Notification.permission);
}

/** Bắn thông báo khi hết giờ hẹn */
function notifyTimerDone() {
  if (!('Notification' in window)) return;
  if (Notification.permission !== 'granted') {
    // Không có quyền thì dựa vào rung + chuông + toast
    showToast('⏰ Hết giờ hẹn!', 4000);
    return;
  }
  try {
    const opts = {
      body: 'Thời gian hẹn giờ đã kết thúc.',
      icon: 'icon.svg',
      badge: 'icon.svg',
      tag: 'timer-done',
      renotify: true,
      vibrate: [400, 200, 400, 200, 600],
      requireInteraction: true
    };
    // Ưu tiên dùng Service Worker để thông báo hiện cả khi app ở nền
    if (navigator.serviceWorker && navigator.serviceWorker.ready) {
      navigator.serviceWorker.ready
        .then((reg) => reg.showNotification('Đồng Hồ Việt Nam', opts))
        .catch(() => new Notification('Đồng Hồ Việt Nam', opts));
    } else {
      new Notification('Đồng Hồ Việt Nam', opts);
    }
  } catch (err) {
    console.warn('Không gửi được thông báo:', err);
  }

/**
 * Gửi một thông báo hệ thống bất kỳ (dùng chung cho nút "Bật thông báo").
 * Ưu tiên Service Worker để thông báo vẫn hiện khi app ở nền.
 */
function sendNotification(title, body) {
  if (!('Notification' in window)) return;
  if (Notification.permission !== 'granted') return;

  const opts = {
    body: body || '',
    icon: 'icon.svg',
    badge: 'icon.svg',
    tag: 'dong-ho-vn',
    vibrate: [200, 100, 200]
  };

  try {
    if (navigator.serviceWorker && navigator.serviceWorker.ready) {
      navigator.serviceWorker.ready
        .then((reg) => reg.showNotification(title, opts))
        .catch(() => new Notification(title, opts));
    } else {
      new Notification(title, opts);
    }
  } catch (err) {
    console.warn('Không gửi được thông báo:', err);
  }
}

}

/* ---------------- Wake Lock: giữ sáng màn hình ---------------- */
let wakeLock = null;

/** Bật giữ sáng màn hình (chỉ khi đang đếm) */
async function acquireWakeLock() {
  if (!('wakeLock' in navigator) || wakeLock) return;
  try {
    wakeLock = await navigator.wakeLock.request('screen');
    wakeLock.addEventListener('release', () => { wakeLock = null; });
  } catch (err) {
    // Một số máy Android tiết kiệm pin sẽ từ chối - không sao
    console.warn('Không giữ được sáng màn hình:', err);
  }
}

/** Tắt giữ sáng màn hình */
function releaseWakeLock() {
  if (wakeLock) {
    wakeLock.release().catch(() => {});
    wakeLock = null;
  }
}

/* ---------------- Đăng ký Service Worker ---------------- */
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('sw.js').catch((err) => {
      console.warn('Service Worker không đăng ký được:', err);
    });
  });
}

/* ---------------- Mở khoá âm thanh trên Android ---------------- */
/* Android chặn phát âm thanh nếu chưa có thao tác chạm của người dùng.
   Ta khởi tạo AudioContext ngay lần chạm đầu tiên để các lần sau phát được. */
function unlockAudio() {
  const ctx = getAudioCtx();
  if (ctx && ctx.state === 'suspended') ctx.resume();
  // Chỉ xin quyền thông báo khi người dùng bấm nút, không tự bật hộp thoại
  document.removeEventListener('pointerdown', unlockAudio);
  document.removeEventListener('touchstart', unlockAudio);
}
document.addEventListener('pointerdown', unlockAudio, { once: true });
document.addEventListener('touchstart', unlockAudio, { once: true });

/* ---------------- Xử lý khi app bị ẩn / hiện lại ---------------- */
/* Android có thể tạm dừng JS khi app ở nền. Vì bộ đếm dựa trên Date.now()
   nên khi quay lại chỉ cần tính lại thời gian còn lại là chính xác. */
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') {
    // Quay lại app: cập nhật ngay bộ đếm hẹn giờ
    if (tmRunning && tmEndTimestamp) {
      tmRemaining = tmEndTimestamp - Date.now();
      if (tmRemaining <= 0) {
        triggerAlarm();
      } else {
        renderTimer();
      }
    }
    // Đang đếm thì bật lại wake lock (bị nhả khi ẩn app)
    if (tmRunning || swRunning) acquireWakeLock();
  } else {
    // Ẩn app: nhả wake lock để tiết kiệm pin
    releaseWakeLock();
  }
});

/* ---------------- Quản lý wake lock theo trạng thái ---------------- */
/** Đồng bộ wake lock với trạng thái chạy hiện tại */
function syncWakeLock() {
  if (swRunning || (tmRunning && !tmAlarm)) {
    acquireWakeLock();
  } else {
    releaseWakeLock();
  }
}

/* Đồng bộ wake lock định kỳ.
   Lưu ý: KHÔNG bọc lại toggleStopwatch/toggleTimer bằng cách gán đè,
   vì các nút đã gắn listener tới hàm gốc từ trước nên sẽ không có tác dụng. */
setInterval(syncWakeLock, 1000);

/* ---------------- Chặn thoát nhầm khi đang đếm ---------------- */
window.addEventListener('beforeunload', (event) => {
  if (swRunning || tmRunning) {
    event.preventDefault();
    event.returnValue = '';
    return '';
  }
});

/* ---------------- Chặn zoom bằng 2 ngón (giữ cảm giác app native) ---------------- */
document.addEventListener('gesturestart', (event) => event.preventDefault());

/* ---------------- Chào mừng ---------------- */
window.addEventListener('load', () => {
  showToast('Chào mừng! Giờ Việt Nam GMT+7', 2200);
});
/* ---------------- Gắn sự kiện cho thanh công cụ Android ---------------- */
const btnNotify = document.getElementById('btn-notify');
const btnTestSound = document.getElementById('btn-test-sound');

if (btnNotify) {
  btnNotify.addEventListener('click', async () => {
    if (!('Notification' in window)) {
      showToast('Thiết bị không hỗ trợ thông báo');
      return;
    }
    if (Notification.permission === 'granted') {
      showToast('Thông báo đã được bật sẵn');
      return;
    }
    if (Notification.permission === 'denied') {
      showToast('Bạn đã chặn thông báo. Hãy mở Cài đặt trình duyệt để bật lại.');
      return;
    }
    try {
      const result = await Notification.requestPermission();
      if (result === 'granted') {
        showToast('Đã bật thông báo ✓');
        sendNotification('Đồng Hồ Việt Nam', 'Thông báo đã hoạt động!');
      } else {
        showToast('Bạn chưa cho phép thông báo');
      }
    } catch (err) {
      // Một số WebView Android không hỗ trợ requestPermission dạng Promise
      showToast('Không bật được thông báo trên trình duyệt này');
    }
  });
}

if (btnTestSound) {
  btnTestSound.addEventListener('click', () => {
    // Chạm là thao tác người dùng -> AudioContext được mở khoá hợp lệ
    playBeep();
    vibrateDevice();
    showToast('Đang phát thử chuông');
  });
}
/* ---------------- Mở đúng tab khi vào từ shortcut (?tab=...) ---------------- */
(function openTabFromUrl() {
  const params = new URLSearchParams(window.location.search);
  const target = params.get('tab');
  if (!target) return;

  const targetTab = document.querySelector(`.tab[data-tab="${target}"]`);
  if (targetTab) {
    // Nhấn "ảo" để dùng lại đúng logic chuyển tab sẵn có
    targetTab.click();
  }
})();