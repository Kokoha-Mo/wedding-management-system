/**
 * 功能：同步電腦/手機導覽列、處理 JWT 與 記住帳號、全域忘記密碼倒數
 */

const SESSION_TIMEOUT = 60 * 60 * 1000; // 1 小時過期跟jwt和cookie期限一樣

// 🌟 新增：全域變數來追蹤倒數狀態 (放在最外面，關閉彈窗才不會消失)
let g_forgotSeconds = 0;
let g_forgotTimer = null;

document.addEventListener('DOMContentLoaded', () => {
  checkAuthStatus();
  initLoginFeatures();

  document.getElementById('logoutBtn')?.addEventListener('click', handleLogout);
  document.getElementById('logoutBtnMobile')?.addEventListener('click', handleLogout);

  // 監聽其他分頁的 localStorage 變化 → 即時同步登入/登出狀態
  window.addEventListener('storage', (event) => {
    if (event.key === 'dv_username') {
      if (event.newValue === null) {
        syncNavbarUI(false);
      } else {
        syncNavbarUI(true, event.newValue);
      }
    }
  });
  setTimeout(() => window.scrollTo(0, 0), 0);
});


/* 檢查現在是誰在線 */
async function checkAuthStatus() {
  const user = localStorage.getItem('dv_username');

  // 檢查 Session 是否過期
  const loginTime = localStorage.getItem('dv_login_time');
  if (loginTime && Date.now() - parseInt(loginTime) > SESSION_TIMEOUT) {
    // Session 過期 → 強制登出
    forceLogout();
    return;
  }

  if (!user) {
    syncNavbarUI(false);
    return;
  }

  // 先用 localStorage 快速顯示（避免閃爍）
  syncNavbarUI(true, user);

  // 已登入卻在登入頁 → 直接導回首頁
  if (window.location.pathname.includes('client_login')) {
    window.location.href = './index.html';
    return;
  }

  // 問後端 Cookie 是否真的有效
  try {
    const res = await fetch('/api/customer/me', { credentials: 'include' });

    if (res.ok) {
      const data = await res.json();
      localStorage.setItem('dv_username', data.name);
      localStorage.setItem('dv_customer_id', data.customerId);
      localStorage.setItem('dv_has_project', data.hasProject); // 儲存專案狀態

      syncNavbarUI(true, data.name, data.hasProject);
    } else if (res.status === 401 || res.status === 403) {
      // 嘗試讀取後端回傳的訊息
      let errMsg = '';
      try {
        const errData = await res.json();
        errMsg = errData?.message || '';
      } catch (_) { /* 讀不到 body 就忽略 */ }

      // 清空登入狀態
      localStorage.removeItem('dv_username');
      localStorage.removeItem('dv_customer_id');
      localStorage.removeItem('dv_login_time');
      localStorage.removeItem('dv_has_project'); // 清除專案狀態
      syncNavbarUI(false);

      // 如果目前在需要登入才能進入的頁面才需要跳轉
      const protectedPages = ['customer_progress.html', 'customer_system.html'];
      const currentPage = window.location.pathname.split('/').pop();
      if (protectedPages.includes(currentPage)) {
        if (errMsg && errMsg.includes('停用')) {
          // 帳號已停用 → 先顯示通知彈窗，再跳轉
          showAccountDisabledModal(errMsg, './client_login.html');
        } else {
          // 一般 token 過期 → 直接跳轉
          window.location.href = './client_login.html';
        }
      }
    } else {
      // 其他錯誤 (如 500 後端報錯、404 找不到路徑等) 則忽略，不強制登出
      console.warn("身分驗證遇到異常，狀態碼：", res.status);
    }
  } catch (e) {
    // 網路錯誤 → 保持現狀不動
    console.error("網路連線錯誤：", e);
  }
}

/* 帳號停用通知彈窗（顯示後自動導回登入頁） */
function showAccountDisabledModal(message, redirectUrl) {
  if (document.getElementById('dvDisabledOverlay')) return;

  const overlay = createSharedModal({
    id: 'dvDisabledOverlay',
    icon: 'lock_person',
    iconColor: '#C5A059',
    eyebrow: 'Account Status',
    title: '帳號已停用',
    message: message || '此帳號已被停用，如有疑問請聯繫客服。',
    buttonText: '返回登入頁',
    buttonId: 'dvdOkBtn',
    countdownSecs: 5
  });

  // 倒數計時自動跳轉
  let sec = 5;
  const secEl = overlay.querySelector('.dv-modal-countdown strong');
  const timer = setInterval(() => {
    sec--;
    if (secEl) secEl.textContent = sec;
    if (sec <= 0) {
      clearInterval(timer);
      window.location.href = redirectUrl;
    }
  }, 1000);

  // 手動點擊立即跳轉
  overlay.querySelector('#dvdOkBtn').addEventListener('click', () => {
    clearInterval(timer);
    window.location.href = redirectUrl;
  });
}

/* UI：同步更新電腦與手機的導覽列 */
function syncNavbarUI(isLoggedIn, username = "", hasProject = null) {
  const deskLogin = document.getElementById('loginLinkDesktop');
  const deskUserWrap = document.getElementById('userDropdownWrap');
  const deskUserBtn = document.getElementById('userDropdownBtn');
  const mobLogin = document.getElementById('loginLinkMobile');
  const mobUserMenu = document.getElementById('mobileUserMenu');
  const mobWelcome = document.getElementById('mobileWelcome');
  const mobLogout = document.getElementById('logoutBtnMobile');

  const deskPlanLink = document.querySelector('#userDropdownWrap a[href="./customer_system.html"]');
  const deskProgressLink = document.querySelector('#userDropdownWrap a[href="./customer_progress.html"]');
  const mobPlanLink = document.querySelector('#mobileUserMenu a[href="./customer_system.html"]');
  const mobProgressLink = document.querySelector('#mobileUserMenu a[href="./customer_progress.html"]');

  const isProjectExists = hasProject !== null ? hasProject : (localStorage.getItem('dv_has_project') === 'true');

  if (isLoggedIn) {
    if (deskLogin) deskLogin.style.setProperty('display', 'none', 'important');
    if (mobLogin) mobLogin.style.setProperty('display', 'none', 'important');
    if (deskUserWrap) deskUserWrap.style.display = 'block';
    if (deskUserBtn) deskUserBtn.textContent = `歡迎, ${username} ▾`;
    if (mobUserMenu) mobUserMenu.style.display = 'block';
    if (mobWelcome) mobWelcome.textContent = `歡迎, ${username}`;
    if (mobLogout) mobLogout.style.display = 'block';

    if (isProjectExists) {
      if (deskPlanLink) deskPlanLink.parentElement.style.display = 'none';
      if (deskProgressLink) deskProgressLink.parentElement.style.display = 'block';
      if (mobPlanLink) mobPlanLink.style.display = 'none';
      if (mobProgressLink) mobProgressLink.style.display = 'block';
    } else {
      if (deskPlanLink) deskPlanLink.parentElement.style.display = 'block';
      if (deskProgressLink) deskProgressLink.parentElement.style.display = 'none';
      if (mobPlanLink) mobPlanLink.style.display = 'block';
      if (mobProgressLink) mobProgressLink.style.display = 'none';
    }
  } else {
    if (deskLogin) deskLogin.style.display = 'block';
    if (mobLogin) mobLogin.style.display = 'block';
    if (deskUserWrap) deskUserWrap.style.display = 'none';
    if (mobUserMenu) mobUserMenu.style.display = 'none';
    if (mobLogout) mobLogout.style.display = 'none';
  }
}

/* 登入執行 */
async function performLoginAction() {
  const email = document.getElementById('loginName')?.value.trim();
  const pass = document.getElementById('passwordInput')?.value.trim();
  const rememberMe = document.getElementById('rememberMe')?.checked;

  if (!email || !pass) return showStyledAlertModal('請完整填寫電子郵件與密碼');

  try {
    const response = await fetch('/api/customer/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email: email, password: pass })
    });

    if (!response.ok) {
      try {
        const errorData = await response.json();
        showStyledAlertModal(errorData.message || '登入失敗，請檢查帳號密碼');
      } catch {
        showStyledAlertModal('系統連線異常，請稍後再試');
      }
      return;
    }
    const data = await response.json();
    completeLoginProcess(data, email, rememberMe);

  } catch (error) {
    console.error('登入發生錯誤:', error);
    showStyledAlertModal('系統連線異常，請稍後再試');
  }
}

function completeLoginProcess(data, email, rememberMe) {
  localStorage.setItem('dv_username', data.name || data.email);
  localStorage.setItem('dv_login_time', Date.now());
  localStorage.setItem('dv_has_project', data.hasProject === true);

  if (data.customerId) localStorage.setItem('dv_customer_id', data.customerId);

  if (rememberMe) {
    localStorage.setItem('dv_remember_email', email);
  } else {
    localStorage.removeItem('dv_remember_email');
  }

  showSuccessModal(data.name || data.email);
  setTimeout(() => { window.location.href = './index.html'; }, 1800);
}

function showSuccessModal(username) {
  const overlay = createSharedModal({
    id: 'dvSuccessTransOverlay',
    icon: 'check',
    iconColor: '#C5A059',
    eyebrow: 'LOGIN SUCCESSFUL',
    title: '歡迎回來',
    message: `${username}，您已成功登入<br>正在為您跳轉至專屬頁面⋯`,
    buttonText: '前往首頁探索',
    buttonId: 'dvsGoBtn'
  });
  const btn = overlay.querySelector('#dvsGoBtn');
  btn.style.background = 'transparent';
  btn.style.color = '#7A6F66';
  btn.style.borderColor = 'rgba(197,160,89,0.3)';
  btn.addEventListener('click', () => { window.location.href = './index.html'; });
}

/* 登出處理 */
function handleLogout(e) {
  e.preventDefault();
  forceLogout();
}

async function forceLogout() {
  try {
    await fetch('/api/customer/logout', { method: 'POST', credentials: 'include' });
  } catch (e) { }

  localStorage.removeItem('dv_username');
  localStorage.removeItem('dv_login_time');
  localStorage.removeItem('dv_customer_id');
  localStorage.removeItem('dv_has_project');
  sessionStorage.clear();
  showStyledAlertModal('您已成功登出');
  setTimeout(() => { window.location.href = './index.html'; }, 1500);
}

/* 共用彈窗產生器 */
function createSharedModal(options) {
  const { id, icon = 'info', iconColor = '#C5A059', eyebrow = 'System Message', title = '系統提示', message = '', buttonText = '確定', buttonId = 'dvModalOkBtn', countdownSecs = null } = options;
  const overlay = document.createElement('div');
  overlay.id = id;
  overlay.className = 'dv-modal-overlay';
  let countdownHtml = countdownSecs !== null ? `<p class="dv-modal-countdown">將於 <strong>${countdownSecs}</strong> 秒後自動跳轉</p>` : '';

  overlay.innerHTML = `
    <div class="dv-modal-box">
      <div class="dvd-corner-tr"></div><div class="dvd-corner-bl"></div>
      <div class="dv-modal-icon-wrap">
        <span class="material-symbols-outlined" style="font-size:28px; color:${iconColor};">${icon}</span>
      </div>
      <span class="dv-modal-eyebrow">${eyebrow}</span>
      <h2 class="dv-modal-title">${title}</h2>
      <div class="dv-modal-divider"><div class="dv-modal-dot"></div></div>
      <p class="dv-modal-msg">${message}</p>
      <button class="dv-modal-btn" id="${buttonId}"><span>${buttonText}</span></button>
      ${countdownHtml}
    </div>
  `;
  document.body.appendChild(overlay);
  return overlay;
}

function showStyledAlertModal(message) {
  if (document.getElementById('dvAlertOverlay')) document.getElementById('dvAlertOverlay').remove();
  const overlay = createSharedModal({
    id: 'dvAlertOverlay', icon: 'info', iconColor: '#C5A059', eyebrow: 'System Message', title: '系統提示', message: message, buttonText: '確定', buttonId: 'dvAlertOkBtn'
  });
  overlay.querySelector('#dvAlertOkBtn').addEventListener('click', () => { overlay.remove(); });
}

function initLoginFeatures() {
  const savedEmail = localStorage.getItem('dv_remember_email');
  const emailField = document.getElementById('loginName');
  if (savedEmail && emailField) emailField.value = savedEmail;
  document.getElementById('loginSubmitBtn')?.addEventListener('click', performLoginAction);
  document.getElementById('passwordInput')?.addEventListener('keydown', (e) => { if (e.key === 'Enter') performLoginAction(); });
  document.getElementById('forgotBtn')?.addEventListener('click', (e) => { e.preventDefault(); showForgotModal(); });

  // 🌟 頁面載入時恢復倒數
  const sentAt = localStorage.getItem('dv_forgot_sent_at');
  if (sentAt) {
    const elapsed = Math.floor((Date.now() - parseInt(sentAt)) / 1000);
    const remaining = 60 - elapsed;
    if (remaining > 0) {
      startGlobalForgotCountdown(remaining, true); // ← isResume = true，在 if 裡面
    } else {
      localStorage.removeItem('dv_forgot_sent_at');
    }
  }

}

function togglePassword(btn) {
  const input = document.getElementById('passwordInput');
  const icon = btn.querySelector('.material-symbols-outlined');
  if (!input) return;
  const isPass = input.type === 'password';
  input.type = isPass ? 'text' : 'password';
  icon.textContent = isPass ? 'visibility' : 'visibility_off';
}

/* 忘記密碼邏輯 - 整合全域倒數與一致彈窗樣式 */

function showForgotModal() {
  if (document.getElementById('dvForgotOverlay')) {
    document.getElementById('dvForgotOverlay').style.display = 'flex';
    updateForgotBtnUI(); // 打開時立即更新按鈕倒數狀態
    return;
  }

  const overlay = document.createElement('div');
  overlay.id = 'dvForgotOverlay';
  overlay.className = 'dv-modal-overlay'; // 改用 global.css 的彈窗樣式

  overlay.innerHTML = `
    <div class="dv-modal-box">
      <div class="dvd-corner-tr"></div><div class="dvd-corner-bl"></div>
      <div class="dv-modal-icon-wrap">
        <span class="material-symbols-outlined" style="font-size:28px; color:#C5A059;">lock_reset</span>
      </div>
      <span class="dv-modal-eyebrow">Forgot Password</span>
      <h2 class="dv-modal-title">忘記密碼？</h2>
      <div class="dv-modal-divider"><div class="dv-modal-dot"></div></div>
      <p class="dv-modal-msg">請輸入您預約時使用的電子郵件<br>我們將寄送重設連結給您</p>
      
      <div id="dvfAlert" style="display:none; color:#c45040; font-size:12px; margin-bottom:15px; font-family:var(--font-sans);">
          <span id="dvfAlertMsg"></span>
      </div>

      <div style="margin-bottom: 20px; text-align: left;">
          <input type="email" id="dvfEmail" placeholder="example@email.com" 
              style="width:100%; padding:14px; border:1px solid rgba(197,160,89,0.22); background:#faf7f2; outline:none; border-radius:3px; font-family:var(--font-sans);">
      </div>

<button id="dvfSendBtn" 
    onmouseover="this.style.background='linear-gradient(135deg,#C5A059 0%,#A68648 100%)'; this.style.color='#fff'; this.style.borderColor='#C5A059';"
    onmouseout="this.style.background='transparent'; this.style.color='#2D2520'; this.style.borderColor='rgba(197,160,89,0.6)';"
    style="display:inline-block; width:100%; background:transparent; border:1px solid rgba(197,160,89,0.6); color:#2D2520; padding:0.9rem; font-size:12px; font-weight:500; letter-spacing:0.44em; text-transform:uppercase; font-family:var(--font-sans); cursor:pointer; outline:none; transition: color 0.3s, border-color 0.3s, background 0.3s;">
    <span>傳送重設連結</span>
</button>   
      <button id="dvfCancelBtn" style="display:inline-block; width:100%; border:none; margin-top:10px; background:transparent; cursor:pointer; outline:none;">
    <span style="letter-spacing:0.2em; color:#9c8e7f; font-size:11px; transition: color 0.3s, font-size 0.3s;">← 返回登入頁</span>
</button>
    </div>
  `;

  const cancelBtn = overlay.querySelector('#dvfCancelBtn');
  cancelBtn.addEventListener('mouseover', () => {
    const span = cancelBtn.querySelector('span');
    span.style.color = '#C5A059';
    span.style.fontSize = '12px';
  });
  cancelBtn.addEventListener('mouseout', () => {
    const span = cancelBtn.querySelector('span');
    span.style.color = '#9c8e7f';
    span.style.fontSize = '11px';
  });

  document.body.appendChild(overlay);

  overlay.addEventListener('click', (e) => { if (e.target === overlay) closeForgotModal(); });
  document.getElementById('dvfCancelBtn').addEventListener('click', closeForgotModal);
  document.getElementById('dvfSendBtn').addEventListener('click', submitForgotPassword);

  updateForgotBtnUI();
  setTimeout(() => document.getElementById('dvfEmail')?.focus(), 100);
}

function closeForgotModal() {
  const overlay = document.getElementById('dvForgotOverlay');
  if (overlay) overlay.style.display = 'none';
}

// 更新按鈕狀態
function updateForgotBtnUI() {
  const btn = document.getElementById('dvfSendBtn');
  if (!btn) return;

  if (g_forgotSeconds > 0) {
    btn.disabled = true;
    btn.querySelector('span').textContent = `請等待 ${g_forgotSeconds} 秒...`;
    btn.style.opacity = "0.6";
    btn.style.cursor = "not-allowed";
  } else {
    btn.disabled = false;
    btn.querySelector('span').textContent = "傳送重設連結";
    btn.style.opacity = "1";
    btn.style.cursor = "pointer";
  }
}

// 執行忘記密碼
async function submitForgotPassword() {
  const email = document.getElementById('dvfEmail')?.value.trim();
  const alertEl = document.getElementById('dvfAlert');
  const alertMsg = document.getElementById('dvfAlertMsg');

  if (!email || !email.includes('@')) {
    alertEl.style.display = 'block';
    alertMsg.textContent = '請輸入有效的電子郵件格式';
    return;
  }

  // 按下後立即鎖住按鈕＋顯示載入中
  const sendBtn = document.getElementById('dvfSendBtn');
  sendBtn.disabled = true;
  sendBtn.querySelector('span').textContent = '寄送中 . . .';
  sendBtn.style.opacity = '0.7';
  sendBtn.style.cursor = 'not-allowed';

  try {
    const response = await fetch('/api/customer/forgot-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });

    closeForgotModal();

    const successModal = createSharedModal({
      id: 'dvfSuccessOverlay',
      icon: 'mark_email_read',
      title: '郵件已寄出',
      message: '重設連結將在幾分鐘內寄達。<br>請檢查您的收件匣（包含垃圾郵件）。',
      buttonText: '我知道了',
      buttonId: 'dvfSuccessOkBtn'
    });
    successModal.querySelector('#dvfSuccessOkBtn').addEventListener('click', () => successModal.remove());

    startGlobalForgotCountdown(60);

  } catch (err) {
    // 失敗時恢復按鈕
    sendBtn.disabled = false;
    sendBtn.querySelector('span').textContent = '傳送重設連結';
    sendBtn.style.opacity = '1';
    sendBtn.style.cursor = 'pointer';
    showStyledAlertModal('伺服器連線失敗，請稍後再試');
  }
}

// 啟動倒數計時器
function startGlobalForgotCountdown(seconds, isResume = false) {
  if (g_forgotTimer) clearInterval(g_forgotTimer);

  // 🌟 只有「新發送」才寫入時間戳，恢復時不蓋掉
  if (!isResume) {
    localStorage.setItem('dv_forgot_sent_at', Date.now());
  }

  g_forgotSeconds = seconds;
  updateForgotBtnUI();

  g_forgotTimer = setInterval(() => {
    g_forgotSeconds--;
    updateForgotBtnUI();
    if (g_forgotSeconds <= 0) {
      clearInterval(g_forgotTimer);
      g_forgotTimer = null;
      localStorage.removeItem('dv_forgot_sent_at'); // 🌟 倒數結束清掉
    }
  }, 1000);
}