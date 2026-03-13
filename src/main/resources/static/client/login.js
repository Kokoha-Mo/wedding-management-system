/**
 * 功能：同步電腦/手機導覽列、處理 JWT 與 Session、記住帳號
 */

const SESSION_TIMEOUT = 60 * 60 * 1000; // 1 小時過期

document.addEventListener('DOMContentLoaded', () => {
    checkAuthStatus();
    initLoginFeatures();

    document.getElementById('logoutBtn')?.addEventListener('click', handleLogout);
    document.getElementById('logoutBtnMobile')?.addEventListener('click', handleLogout);
});

/* 檢查現在是誰在線 */
function checkAuthStatus() {
    const user = sessionStorage.getItem('dv_username') || localStorage.getItem('dv_username');
    const token = sessionStorage.getItem('dv_token') || localStorage.getItem('dv_token');
    const loginTime = sessionStorage.getItem('dv_login_time') || localStorage.getItem('dv_login_time');
    const now = Date.now();

    if (user && token) {
        if (loginTime && (now - loginTime > SESSION_TIMEOUT)) {
            forceLogout();
            return;
        }
        syncNavbarUI(true, user);

        // 已登入卻在登入頁 → 直接導回首頁
        if (window.location.pathname.includes('client_login')) {
            window.location.href = './index.html';
            return;
        }
    } else {
        syncNavbarUI(false);
    }
}

/* UI：同步更新電腦與手機的導覽列 */
function syncNavbarUI(isLoggedIn, username = "") {
    const deskLogin    = document.getElementById('loginLinkDesktop');
    const deskUserWrap = document.getElementById('userDropdownWrap');
    const deskUserBtn  = document.getElementById('userDropdownBtn');
    const mobLogin     = document.getElementById('loginLinkMobile');
    const mobUserMenu  = document.getElementById('mobileUserMenu');
    const mobWelcome   = document.getElementById('mobileWelcome');
    const mobLogout    = document.getElementById('logoutBtnMobile');

    if (isLoggedIn) {
        if (deskLogin)    deskLogin.style.setProperty('display', 'none', 'important');
        if (mobLogin)     mobLogin.style.setProperty('display', 'none', 'important');
        if (deskUserWrap) deskUserWrap.style.display = 'block';
        if (deskUserBtn)  deskUserBtn.textContent = `歡迎, ${username} ▾`;
        if (mobUserMenu)  mobUserMenu.style.display = 'block';
        if (mobWelcome)   mobWelcome.textContent = `歡迎, ${username}`;
        if (mobLogout)    mobLogout.style.display = 'block';
    } else {
        if (deskLogin)    deskLogin.style.display = 'block';
        if (mobLogin)     mobLogin.style.display = 'block';
        if (deskUserWrap) deskUserWrap.style.display = 'none';
        if (mobUserMenu)  mobUserMenu.style.display = 'none';
        if (mobLogout)    mobLogout.style.display = 'none';
    }
}

/* 登入執行 */
async function performLoginAction() {
    const email     = document.getElementById('loginName')?.value.trim();
    const pass      = document.getElementById('passwordInput')?.value.trim();
    const rememberMe = document.getElementById('rememberMe')?.checked;

    if (!email || !pass) return alert('請完整填寫電子郵件與密碼');

    // ── Mock API（之後換成真實 fetch）──
    const mockResponse = {
        success: true,
        token: "eyJhbGciOiJIUzI1Ni...",
        username: email.split('@')[0]
    };

    if (mockResponse.success) {
        const storage = rememberMe ? localStorage : sessionStorage;
        storage.setItem('dv_token',      mockResponse.token);
        storage.setItem('dv_username',   mockResponse.username);
        storage.setItem('dv_login_time', Date.now());

        if (rememberMe) {
            localStorage.setItem('dv_remember_email', email);
        } else {
            localStorage.removeItem('dv_remember_email');
        }

        // 呼叫 modal，不再 alert / 直接跳頁
        showSuccessModal(mockResponse.username);

        // modal 顯示 2.2 秒後自動跳頁
        setTimeout(() => {
            window.location.href = './index.html';
        }, 1800);
    }
}

/* 登出處理 */
function handleLogout(e) {
    e.preventDefault();
    forceLogout();
}

function forceLogout() {
    localStorage.removeItem('dv_token');
    localStorage.removeItem('dv_username');
    localStorage.removeItem('dv_login_time');
    sessionStorage.clear();
    alert('您已成功登出');
    window.location.href = './index.html';
}

/* 初始化：記住帳號、綁定按鈕與 Enter */
function initLoginFeatures() {
    const savedEmail = localStorage.getItem('dv_remember_email');
    const emailField = document.getElementById('loginName');
    if (savedEmail && emailField) emailField.value = savedEmail;

    // 只在登入頁才綁定（避免其他頁面找不到元素報錯）
    document.getElementById('loginSubmitBtn')?.addEventListener('click', performLoginAction);
    document.getElementById('passwordInput')?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') performLoginAction();
    });
}

/* 密碼眼睛切換 */
function togglePassword(btn) {
    const input = document.getElementById('passwordInput');
    const icon  = btn.querySelector('.material-symbols-outlined');
    if (!input) return;
    const isPass = input.type === 'password';
    input.type = isPass ? 'text' : 'password';
    icon.textContent = isPass ? 'visibility' : 'visibility_off';
}