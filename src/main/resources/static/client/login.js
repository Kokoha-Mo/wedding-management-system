/**
 * 功能：同步電腦/手機導覽列、處理 JWT 與 記住帳號
 */

const SESSION_TIMEOUT = 60 * 60 * 1000; // 1 小時過期

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
function checkAuthStatus() {
    const user = sessionStorage.getItem('dv_username') || localStorage.getItem('dv_username');
    const loginTime = sessionStorage.getItem('dv_login_time') || localStorage.getItem('dv_login_time');
    const now = Date.now();

    if (user) {
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
    const deskLogin = document.getElementById('loginLinkDesktop');
    const deskUserWrap = document.getElementById('userDropdownWrap');
    const deskUserBtn = document.getElementById('userDropdownBtn');
    const mobLogin = document.getElementById('loginLinkMobile');
    const mobUserMenu = document.getElementById('mobileUserMenu');
    const mobWelcome = document.getElementById('mobileWelcome');
    const mobLogout = document.getElementById('logoutBtnMobile');

    if (isLoggedIn) {
        if (deskLogin) deskLogin.style.setProperty('display', 'none', 'important');
        if (mobLogin) mobLogin.style.setProperty('display', 'none', 'important');
        if (deskUserWrap) deskUserWrap.style.display = 'block';
        if (deskUserBtn) deskUserBtn.textContent = `歡迎, ${username} ▾`;
        if (mobUserMenu) mobUserMenu.style.display = 'block';
        if (mobWelcome) mobWelcome.textContent = `歡迎, ${username}`;
        if (mobLogout) mobLogout.style.display = 'block';
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

        // 🌟 2. 判斷是否被後端標記為「首次登入強制修改密碼」
        if (data.forcePasswordChange) {

            // 顯示強制修改密碼的 Modal (此時還不把 username 寫進 localStorage，避免他亂跳頁)
            const forceResetOverlay = document.getElementById('forceResetOverlay');
            if (forceResetOverlay) forceResetOverlay.classList.add('show');

            // 綁定修改密碼表單的送出事件
            const forceResetForm = document.getElementById('forceResetForm');
            if (forceResetForm) {
                forceResetForm.onsubmit = async function (e) {
                    e.preventDefault(); // 防止表單重整頁面

                    const newPwd = document.getElementById('newPasswordInput').value;
                    const confirmPwd = document.getElementById('confirmPasswordInput').value;

                    if (newPwd !== confirmPwd) {
                        alert('兩次輸入的密碼不一致！');
                        return;
                    }

                    try {
                        // 呼叫更新密碼 API (記得也要帶 credentials: 'include' 才能傳送剛登入的 Cookie)
                        const updateRes = await fetch('http://127.0.0.1:8080/api/customer/update-password', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            credentials: 'include',
                            body: JSON.stringify({ newPassword: newPwd })
                        });

                        if (updateRes.ok) {
                            // 🌟 修改成功！關閉強制重設視窗，繼續走正常的登入成功流程
                            forceResetOverlay.classList.remove('show');

                            completeLoginProcess(data, email, rememberMe);

                        } else {
                            const errData = await updateRes.json();
                            alert('修改失敗：' + (errData.message || '請稍後再試'));
                        }
                    } catch (err) {
                        console.error('修改密碼發生錯誤:', err);
                        alert('伺服器連線失敗，請稍後再試');
                    }
                };
            }
            return; // 卡在這裡，不繼續往下執行
        }

        // 🌟 3. 如果是正常的老客戶登入，直接執行成功流程
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
            credentials: 'include' // 帶 Cookie 才能讓後端識別並覆蓋cookie
        });
    } catch (e) {
        // 網路失敗，前端仍繼續清除本地
    }

    // 清除localStorage
    localStorage.removeItem('dv_token');
    localStorage.removeItem('dv_username');
    localStorage.removeItem('dv_login_time');
    localStorage.removeItem('dv_customer_id');
    sessionStorage.clear();
    alert('您已成功登出');
    window.location.href = './index.html';
}

/* 初始化：記住帳號、綁定按鈕與 Enter */
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
    document.getElementById('forgotBtn')?.addEventListener('click', async (e) => {
        e.preventDefault(); // 防止網頁亂跳

        // 1. 跳出瀏覽器內建的輸入框，請客人輸入 Email
        const email = prompt('請輸入您預約時使用的電子郵件：\n(我們將寄送重設密碼的連結給您)');

        if (!email) return; // 如果客人按取消或沒輸入，就什麼都不做
        if (!email.includes('@')) return alert('請輸入有效的電子郵件格式！');

        try {
            // 2. 呼叫我們後端寫好的忘記密碼 API
            const res = await fetch('http://127.0.0.1:8080/api/customer/forgot-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email })
            });

            const data = await res.json();

            // 3. 顯示後端回傳的訊息 (也就是那句完美的模糊化回覆)
            alert(data.message);

        } catch (error) {
            console.error('忘記密碼發生錯誤:', error);
            alert('系統連線失敗，請稍後再試。');
        }
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