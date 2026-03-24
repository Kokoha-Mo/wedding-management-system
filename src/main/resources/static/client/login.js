/**
 * 功能：同步電腦/手機導覽列、處理 JWT 與 記住帳號
 */

const SESSION_TIMEOUT = 60 * 60 * 1000; // 1 小時過期跟jwt和cookie期限一樣

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
      localStorage.setItem('dv_has_project', data.hasProject); // 🌟 儲存專案狀態
      
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
      localStorage.removeItem('dv_has_project'); // 🌟 清除專案狀態
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
  // 防止重複建立
  if (document.getElementById('dvDisabledOverlay')) return;

  const overlay = document.createElement('div');
  overlay.id = 'dvDisabledOverlay';
  overlay.innerHTML = /*HTML*/`
    <style>
      #dvDisabledOverlay {
        position: fixed; inset: 0; z-index: 99999;
        background: rgba(20,15,10,0.60);
        backdrop-filter: blur(6px);
        -webkit-backdrop-filter: blur(6px);
        display: flex; align-items: center; justify-content: center;
        padding: 24px; box-sizing: border-box;
        animation: dvdFadeIn 0.3s ease;
      }
      @keyframes dvdFadeIn { from{opacity:0} to{opacity:1} }

      #dvDisabledBox {
        background: #faf7f2;
        border-radius: 2px;
        box-shadow: 0 8px 48px rgba(45,37,32,0.18), 0 2px 12px rgba(45,37,32,0.08);
        width: 100%; max-width: 400px;
        padding: 52px 44px 44px;
        box-sizing: border-box;
        text-align: center;
        position: relative;
        animation: dvdSlideUp 0.45s cubic-bezier(0.16,1,0.3,1) both;
      }
      @keyframes dvdSlideUp {
        from { opacity:0; transform:translateY(28px) scale(0.97); }
        to   { opacity:1; transform:translateY(0) scale(1); }
      }

      /* 四角框線 */
      #dvDisabledBox::before, #dvDisabledBox::after {
        content:''; position:absolute; width:18px; height:18px;
        border-color:rgba(197,160,89,0.45); border-style:solid;
      }
      #dvDisabledBox::before { top:-1px; left:-1px; border-width:1px 0 0 1px; }
      #dvDisabledBox::after  { bottom:-1px; right:-1px; border-width:0 1px 1px 0; }
      #dvDisabledBox .dvd-corner-tr { position:absolute; top:-1px; right:-1px; width:18px; height:18px; border-color:rgba(197,160,89,0.45); border-style:solid; border-width:1px 1px 0 0; }
      #dvDisabledBox .dvd-corner-bl { position:absolute; bottom:-1px; left:-1px;  width:18px; height:18px; border-color:rgba(197,160,89,0.45); border-style:solid; border-width:0 0 1px 1px; }

      .dvd-icon-wrap {
        width: 64px; height: 64px; border-radius: 50%;
        border: 1px solid rgba(197,160,89,0.3);
        display: flex; align-items: center; justify-content: center;
        margin: 0 auto 24px;
        background: radial-gradient(circle, rgba(197,160,89,0.08) 0%, transparent 80%);
      }
      .dvd-eyebrow {
        display: block; font-size: 9px; letter-spacing: 0.5em;
        text-transform: uppercase; color: rgba(197,160,89,0.85); margin-bottom: 10px;
        font-family: 'Plus Jakarta Sans','Noto Sans TC',sans-serif;
      }
      .dvd-title {
        font-family: 'Bodoni Moda','Noto Serif TC',serif;
        font-size: 22px; font-weight: 400; color: #2D2520;
        letter-spacing: 0.06em; margin-bottom: 16px; line-height: 1.3;
      }
      .dvd-divider {
        display: flex; align-items: center; gap: 12px;
        margin: 0 auto 18px; width: 80%;
      }
      .dvd-divider::before,.dvd-divider::after {
        content:''; flex:1; height:1px; background:rgba(197,160,89,0.25);
      }
      .dvd-dot { width:4px; height:4px; background:rgba(197,160,89,0.5); transform:rotate(45deg); }
      .dvd-msg {
        font-size: 13px; color: #7A6F66; letter-spacing: 0.06em;
        line-height: 1.9; margin-bottom: 28px;
        font-family: 'Plus Jakarta Sans','Noto Sans TC',sans-serif;
      }
      .dvd-btn {
        display: inline-block; width: 100%;
        background: transparent; border: 1px solid rgba(197,160,89,0.6);
        color: #2D2520; padding: 0.9rem;
        font-size: 10px; font-weight: 500; letter-spacing: 0.44em;
        text-transform: uppercase;
        font-family: 'Plus Jakarta Sans','Noto Sans TC',sans-serif;
        cursor: pointer; position: relative; overflow: hidden;
        transition: color 0.35s, border-color 0.35s;
      }
      .dvd-btn::before {
        content:''; position:absolute; inset:0;
        background: linear-gradient(135deg,#C5A059 0%,#A68648 100%);
        opacity:0; transition:opacity 0.35s;
      }
      .dvd-btn:hover { color:#fff; border-color:#C5A059; }
      .dvd-btn:hover::before { opacity:1; }
      .dvd-btn span { position:relative; z-index:1; }
      .dvd-countdown {
        font-size: 11px; color: #B0A49A; letter-spacing: 0.08em;
        margin-top: 12px; font-family: 'Plus Jakarta Sans','Noto Sans TC',sans-serif;
      }
    </style>

    <div id="dvDisabledBox">
      <div class="dvd-corner-tr"></div>
      <div class="dvd-corner-bl"></div>

      <div class="dvd-icon-wrap">
        <span class="material-symbols-outlined"
          style="font-size:28px; color:#C5A059;
                 font-variation-settings:'FILL' 0,'wght' 200,'GRAD' 0,'opsz' 24;">
          lock_person
        </span>
      </div>

      <span class="dvd-eyebrow">Account Status</span>
      <h2 class="dvd-title">帳號已停用</h2>

      <div class="dvd-divider"><div class="dvd-dot"></div></div>

      <p class="dvd-msg" id="dvdMessage">${message || '此帳號已被停用，如有疑問請聯繫客服。'}</p>

      <button class="dvd-btn" id="dvdOkBtn">
        <span>返回登入頁</span>
      </button>
      <p class="dvd-countdown" id="dvdCountdown">將於 <strong id="dvdSec">5</strong> 秒後自動跳轉</p>
    </div>
  `;

  document.body.appendChild(overlay);

  // 倒數計時自動跳轉
  let sec = 5;
  const secEl = document.getElementById('dvdSec');
  const timer = setInterval(() => {
    sec--;
    if (secEl) secEl.textContent = sec;
    if (sec <= 0) {
      clearInterval(timer);
      window.location.href = redirectUrl;
    }
  }, 1000);

  // 手動點擊立即跳轉
  document.getElementById('dvdOkBtn').addEventListener('click', () => {
    clearInterval(timer);
    window.location.href = redirectUrl;
  });
}

/* UI：同步更新電腦與手機的導覽列 */
// 🌟 新增 hasProject 參數，預設為 null
function syncNavbarUI(isLoggedIn, username = "", hasProject = null) {
  const deskLogin = document.getElementById('loginLinkDesktop');
  const deskUserWrap = document.getElementById('userDropdownWrap');
  const deskUserBtn = document.getElementById('userDropdownBtn');
  const mobLogin = document.getElementById('loginLinkMobile');
  const mobUserMenu = document.getElementById('mobileUserMenu');
  const mobWelcome = document.getElementById('mobileWelcome');
  const mobLogout = document.getElementById('logoutBtnMobile');

  // 🌟 1. 抓取桌機版與手機版的兩個選單按鈕
  const deskPlanLink = document.querySelector('#userDropdownWrap a[href="./customer_system.html"]');
  const deskProgressLink = document.querySelector('#userDropdownWrap a[href="./customer_progress.html"]');
  const mobPlanLink = document.querySelector('#mobileUserMenu a[href="./customer_system.html"]');
  const mobProgressLink = document.querySelector('#mobileUserMenu a[href="./customer_progress.html"]');

  // 🌟 2. 決定專案狀態：如果有傳入就用傳入的，沒有的話去 localStorage 抓 (確保重新整理不會錯亂)
  const isProjectExists = hasProject !== null ? hasProject : (localStorage.getItem('dv_has_project') === 'true');

  if (isLoggedIn) {
    if (deskLogin) deskLogin.style.setProperty('display', 'none', 'important');
    if (mobLogin) mobLogin.style.setProperty('display', 'none', 'important');
    if (deskUserWrap) deskUserWrap.style.display = 'block';
    if (deskUserBtn) deskUserBtn.textContent = `歡迎, ${username} ▾`;
    if (mobUserMenu) mobUserMenu.style.display = 'block';
    if (mobWelcome) mobWelcome.textContent = `歡迎, ${username}`;
    if (mobLogout) mobLogout.style.display = 'block';
    
    // 🌟 3. 核心邏輯：根據 isProjectExists 來控制顯示哪一個選單
    if (isProjectExists) {
        // 【已有專案】：顯示「我的籌備進度」，隱藏「我的婚禮規劃」
        // 桌機版因為是 <li> 裡面包 <a>，所以我們隱藏外層的 <li> 避免留空隙 (.parentElement)
        if (deskPlanLink) deskPlanLink.parentElement.style.display = 'none';
        if (deskProgressLink) deskProgressLink.parentElement.style.display = 'block';
        if (mobPlanLink) mobPlanLink.style.display = 'none';
        if (mobProgressLink) mobProgressLink.style.display = 'block';
    } else {
        // 【尚無專案】：顯示「我的婚禮規劃」，隱藏「我的籌備進度」
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

/* 登入執行 (包含首次強制修改密碼邏輯) */
async function performLoginAction() {
  const email = document.getElementById('loginName')?.value.trim();
  const pass = document.getElementById('passwordInput')?.value.trim();
  const rememberMe = document.getElementById('rememberMe')?.checked;

  if (!email || !pass) return alert('請完整填寫電子郵件與密碼');

  try {
    // 1. POST 請求給後端
    const response = await fetch('/api/customer/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      credentials: 'include',
      body: JSON.stringify({
        email: email,
        password: pass
      })
    });

    if (!response.ok) {
      const errorData = await response.json();
      alert(errorData.message || '登入失敗，請檢查帳號密碼');
      return;
    }

    const data = await response.json();

    // 2. 判斷是否被後端標記為「首次登入強制修改密碼」
    if (data.forcePasswordChange) {
      sessionStorage.setItem('temp_force_name', data.name || data.email);
      if (data.customerId) {
        sessionStorage.setItem('temp_force_id', data.customerId);
      }
      window.location.href = './reset_password.html?mode=force';
      return;
    }

    // 3. 正常老客戶登入
    completeLoginProcess(data, email, rememberMe);

  } catch (error) {
    console.error('登入發生錯誤:', error);
    alert('伺服器連線失敗，請稍後再試');
  }
}

/* 登入成功的共用流程 (寫入 Storage、顯示成功畫面、跳轉) */
function completeLoginProcess(data, email, rememberMe) {
  localStorage.setItem('dv_username', data.name || data.email);
  localStorage.setItem('dv_login_time', Date.now());

  // 🌟 登入成功時，立刻把專案狀態存起來
  localStorage.setItem('dv_has_project', data.hasProject === true);
  
  if (data.customerId) {
    localStorage.setItem('dv_customer_id', data.customerId);
  }

  if (rememberMe) {
    localStorage.setItem('dv_remember_email', email);
  } else {
    localStorage.removeItem('dv_remember_email');
  }

  showSuccessModal(data.name || data.email);
  setTimeout(() => {
    window.location.href = './index.html';
  }, 1800);
}

/* 登出處理 */
function handleLogout(e) {
  e.preventDefault();
  forceLogout();
}

async function forceLogout() {
  try {
    await fetch('/api/customer/logout', {
      method: 'POST',
      credentials: 'include'
    });
  } catch (e) {
    // 網路失敗，前端仍繼續清除本地
  }

  localStorage.removeItem('dv_token');
  localStorage.removeItem('dv_username');
  localStorage.removeItem('dv_login_time');
  localStorage.removeItem('dv_customer_id');
  sessionStorage.clear();
  alert('您已成功登出');
  window.location.href = './index.html';
}

/* 初始化：記住帳號、綁定按鈕與 Enter */
function initLoginFeatures() {
  const savedEmail = localStorage.getItem('dv_remember_email');
  const emailField = document.getElementById('loginName');
  if (savedEmail && emailField) emailField.value = savedEmail;

  // 登入綁定
  document.getElementById('loginSubmitBtn')?.addEventListener('click', performLoginAction);
  document.getElementById('passwordInput')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') performLoginAction();
  });

  // 忘記密碼按鈕綁定
  document.getElementById('forgotBtn')?.addEventListener('click', (e) => {
    e.preventDefault();
    showForgotModal();
  });
}

/* 密碼眼睛切換 */
function togglePassword(btn) {
  const input = document.getElementById('passwordInput');
  const icon = btn.querySelector('.material-symbols-outlined');
  if (!input) return;
  const isPass = input.type === 'password';
  input.type = isPass ? 'text' : 'password';
  icon.textContent = isPass ? 'visibility' : 'visibility_off';
}


/* 忘記密碼 Modal */

function showForgotModal() {
  if (document.getElementById('dvForgotOverlay')) {
    document.getElementById('dvForgotOverlay').style.display = 'flex';
    document.getElementById('dvfViewForm').style.display = 'block';
    document.getElementById('dvfViewSuccess').classList.remove('show');
    document.getElementById('dvfAlert').classList.remove('show');
    const emailInput = document.getElementById('dvfEmail');
    if (emailInput) { emailInput.value = ''; emailInput.classList.remove('is-error'); }
    setTimeout(() => document.getElementById('dvfEmail')?.focus(), 100);
    return;
  }

  const overlay = document.createElement('div');
  overlay.id = 'dvForgotOverlay';
  overlay.innerHTML = /*html*/`
    <style>
      #dvForgotOverlay {
        position: fixed; inset: 0; z-index: 9999;
        background: rgba(0,0,0,0.52);
        display: flex; align-items: center; justify-content: center;
        padding: 24px; box-sizing: border-box;
        animation: dvfFadeIn 0.2s ease;
      }
      @keyframes dvfFadeIn { from{opacity:0} to{opacity:1} }

      #dvForgotBox {
        background: #ffffff;
        border-radius: 4px;
        box-shadow:
          0 2px 4px rgba(0,0,0,0.03),
          0 12px 40px rgba(0,0,0,0.06),
          0 0 0 1px rgba(197,160,89,0.08);
        width: 100%; max-width: 420px;
        padding: 48px 44px 40px;
        box-sizing: border-box;
        animation: dvfSlideUp 0.5s cubic-bezier(0.22,1,0.36,1) both;
      }
      @keyframes dvfSlideUp { from{opacity:0;transform:translateY(20px)} to{opacity:1;transform:translateY(0)} }

      @media(max-width: 480px) {
        #dvForgotBox { padding: 36px 28px 32px; }
      }

      .dvf-eyebrow {
        display: block; text-align: center;
        font-size: 9px; letter-spacing: 0.45em; text-transform: uppercase;
        color: rgba(197,160,89,0.8); margin-bottom: 10px;
        font-family: 'Plus Jakarta Sans', 'Noto Sans TC', sans-serif;
      }
      .dvf-title {
        font-family: 'Noto Serif TC', serif;
        font-size: 24px; font-weight: 300; color: #2b2520;
        text-align: center; letter-spacing: 0.06em; line-height: 1.4;
        margin: 0 0 8px;
      }
      .dvf-sub {
        font-size: 12px; color: #9c8e7f; letter-spacing: 0.12em;
        text-align: center; line-height: 1.9; margin: 0 0 28px;
        font-family: 'Noto Sans TC', sans-serif;
      }
      .dvf-divider { display: flex; align-items: center; gap: 16px; margin-bottom: 28px; }
      .dvf-divider::before, .dvf-divider::after {
        content: ''; flex: 1; height: 1px; background: rgba(197,160,89,0.25);
      }
      .dvf-dot { width: 5px; height: 5px; border-radius: 50%; background: rgba(197,160,89,0.4); flex-shrink: 0; }

      .dvf-alert {
        display: none; align-items: flex-start; gap: 10px;
        padding: 11px 14px; border-radius: 3px; margin-bottom: 16px;
        font-size: 12px; letter-spacing: 0.1em; line-height: 1.8;
        background: rgba(210,100,80,0.06); border: 1px solid rgba(210,100,80,0.2); color: #c45040;
        font-family: 'Noto Sans TC', sans-serif;
      }
      .dvf-alert.show { display: flex; }
      .dvf-alert .material-symbols-outlined { font-size: 16px; flex-shrink: 0; margin-top: 1px; }

      .dvf-label {
        font-size: 10px; letter-spacing: 0.3em; text-transform: uppercase;
        color: #8c7e6f; display: block; margin-bottom: 10px; font-weight: 500;
        font-family: 'Noto Sans TC', sans-serif;
      }

      /* input wrap：背景色放在 wrap，不放在 input，避免蓋住 icon */
      .dvf-input-wrap {
        position: relative; margin-bottom: 20px;
        background: #faf7f2;
        border: 1px solid rgba(197,160,89,0.22); border-radius: 3px;
        transition: border-color 0.3s, box-shadow 0.3s;
      }
      .dvf-input-wrap:focus-within {
        border-color: rgba(197,160,89,0.6);
        box-shadow: 0 0 0 3px rgba(197,160,89,0.08);
        background: #ffffff;
      }
      .dvf-input-wrap.is-error {
        border-color: rgba(210,100,80,0.5);
        box-shadow: 0 0 0 3px rgba(210,100,80,0.06);
      }
      .dvf-input-wrap svg {
        position: absolute; left: 14px; top: 50%; transform: translateY(-50%);
        pointer-events: none; z-index: 0;
      }
      .dvf-input {
        width: 100%; box-sizing: border-box;
        background: transparent;
        border: none; border-radius: 3px;
        padding: 13px 16px 13px 42px;
        font-size: 14px; color: #2b2520; letter-spacing: 0.06em;
        font-family: 'Noto Sans TC', sans-serif;
        outline: none;
        position: relative; z-index: 1;
      }
      .dvf-input::placeholder { color: #c4b8ac; letter-spacing: 0.08em; }

      .dvf-submit {
        width: 100%; background: #2b2520; color: #fff;
        border: none; border-radius: 3px;
        padding: 15px; font-size: 11px; letter-spacing: 0.38em; text-transform: uppercase;
        font-family: 'Noto Sans TC', sans-serif;
        cursor: pointer;
        transition: background 0.3s, box-shadow 0.3s, transform 0.2s;
        position: relative; overflow: hidden;
      }
      .dvf-submit:hover {
        background: #1a1410;
        box-shadow: 0 8px 24px rgba(43,37,32,0.22);
        transform: translateY(-1px);
      }
      .dvf-submit:active { transform: translateY(0); }

      .dvf-cancel {
        display: flex; align-items: center; justify-content: center; gap: 5px;
        width: 100%; margin-top: 16px;
        background: none; border: none;
        font-size: 11px; letter-spacing: 0.25em; color: #9c8e7f;
        cursor: pointer; font-family: 'Noto Sans TC', sans-serif;
        transition: color 0.25s;
      }
      .dvf-cancel:hover { color: #C5A059; }
      .dvf-cancel .material-symbols-outlined { font-size: 14px; }

      /* 成功畫面 */
      #dvfViewSuccess { display: none; text-align: center; }
      #dvfViewSuccess.show { display: block; }
      .dvf-success-icon {
        width: 56px; height: 56px; border-radius: 50%;
        border: 1px solid rgba(197,160,89,0.3);
        background: rgba(197,160,89,0.05);
        display: flex; align-items: center; justify-content: center;
        margin: 0 auto 20px;
      }
      .dvf-success-title {
        font-family: 'Noto Serif TC', serif;
        font-size: 20px; font-weight: 300; color: #2b2520;
        letter-spacing: 0.06em; margin-bottom: 10px;
      }
      .dvf-success-desc {
        font-size: 12px; color: #9c8e7f;
        letter-spacing: 0.12em; line-height: 2; margin: 0 0 28px;
        font-family: 'Noto Sans TC', sans-serif;
      }
      .dvf-goto-login {
        display: inline-flex; align-items: center; gap: 6px;
        font-size: 11px; letter-spacing: 0.3em; text-transform: uppercase;
        color: #9c8e7f; background: none; border: none;
        cursor: pointer; font-family: 'Noto Sans TC', sans-serif;
        transition: color 0.25s;
      }
      .dvf-goto-login:hover { color: #C5A059; }
      .dvf-goto-login .material-symbols-outlined { font-size: 14px; }
    </style>

    <div id="dvForgotBox">

      <!-- 表單畫面 -->
      <div id="dvfViewForm">
        <span class="dvf-eyebrow">Forgot Password</span>
        <h2 class="dvf-title">忘記密碼？</h2>
        <p class="dvf-sub">請輸入您預約時使用的電子郵件<br>我們將寄送重設連結給您</p>
        <div class="dvf-divider"><div class="dvf-dot"></div></div>

        <div class="dvf-alert" id="dvfAlert">
          <span class="material-symbols-outlined">error_outline</span>
          <span id="dvfAlertMsg"></span>
        </div>

        <label class="dvf-label" for="dvfEmail">電子郵件</label>
        <div class="dvf-input-wrap" id="dvfInputWrap">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
            stroke="rgba(197,160,89,0.7)" stroke-width="1.6"
            stroke-linecap="round" stroke-linejoin="round">
            <rect x="2" y="4" width="20" height="16" rx="2"/>
            <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
          </svg>
          <input class="dvf-input" type="email" id="dvfEmail" placeholder="example@email.com" />
        </div>

        <button class="dvf-submit" id="dvfSendBtn">傳送重設連結</button>
        <button class="dvf-cancel" id="dvfCancelBtn">
          <span class="material-symbols-outlined">arrow_back</span>
          返回登入頁
        </button>
      </div>

      <!-- 成功畫面 -->
      <div id="dvfViewSuccess">
        <div class="dvf-success-icon">
          <span class="material-symbols-outlined"
            style="font-size:26px; color:#C5A059;
                   font-variation-settings:'FILL' 0,'wght' 200,'GRAD' 0,'opsz' 24;">
            mark_email_read
          </span>
        </div>
        <p class="dvf-success-title">郵件已寄出</p>
        <p class="dvf-success-desc">
          若此信箱已完成預約，<br>重設連結將在幾分鐘內寄達。
        </p>
        <button class="dvf-goto-login" id="dvfCloseSuccessBtn">
          <span class="material-symbols-outlined">arrow_back</span>
          返回登入頁
        </button>
      </div>

    </div>
    `;

  document.body.appendChild(overlay);

  // 點擊遮罩關閉
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) closeForgotModal();
  });

  document.getElementById('dvfCancelBtn').addEventListener('click', closeForgotModal);
  document.getElementById('dvfCloseSuccessBtn').addEventListener('click', closeForgotModal);
  document.getElementById('dvfSendBtn').addEventListener('click', submitForgotPassword);
  document.getElementById('dvfEmail').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') submitForgotPassword();
  });

  setTimeout(() => document.getElementById('dvfEmail')?.focus(), 100);
}

function closeForgotModal() {
  const overlay = document.getElementById('dvForgotOverlay');
  if (overlay) overlay.style.display = 'none';
}

async function submitForgotPassword() {
  const email = document.getElementById('dvfEmail')?.value.trim();
  const alertEl = document.getElementById('dvfAlert');
  const alertMsg = document.getElementById('dvfAlertMsg');
  const inputWrap = document.getElementById('dvfInputWrap');

  alertEl.classList.remove('show');
  inputWrap.classList.remove('is-error');

  if (!email || !email.includes('@')) {
    alertMsg.textContent = '請輸入有效的電子郵件格式';
    alertEl.classList.add('show');
    inputWrap.classList.add('is-error');
    return;
  }

  const btn = document.getElementById('dvfSendBtn');
  btn.disabled = true;
  btn.textContent = '傳送中...';

  try {
    await fetch('/api/customer/forgot-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email })
    });
    // 不管後端回什麼，一律顯示成功（安全模糊化）
    document.getElementById('dvfViewForm').style.display = 'none';
    document.getElementById('dvfViewSuccess').classList.add('show');
  } catch (err) {
    console.error('忘記密碼發生錯誤:', err);
    alertMsg.textContent = '系統連線失敗，請稍後再試';
    alertEl.classList.add('show');
  } finally {
    btn.disabled = false;
    btn.textContent = '傳送重設連結';
  }
}