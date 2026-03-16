document.addEventListener('DOMContentLoaded', () => {

    // ─── 元素選取 ───
    const navbar = document.querySelector('.navbar');
    const backToTopBtn = document.getElementById('backToTop');
    const searchBtn = document.getElementById('search-btn');
    const searchInput = document.getElementById('search-input');

    // ─── 變數設定 ───
    let lastScrollTop = 0;
    const delta = 10;

    // ─── 整合後的捲動事件 ───
    window.addEventListener('scroll', () => {
        const st = window.pageYOffset || document.documentElement.scrollTop;

        // 1. 導覽列捲動隱藏/顯示邏輯
        if (Math.abs(lastScrollTop - st) > delta) {
            if (st > lastScrollTop && st > 80) {
                navbar.classList.add('nav-hidden');
            } else {
                navbar.classList.remove('nav-hidden');
            }
            lastScrollTop = st;
        }

        // 2. 回到頂部按鈕顯示邏輯 (超過 400px 顯示)
        if (st > 400) {
            backToTopBtn?.classList.add('show');
        } else {
            backToTopBtn?.classList.remove('show');
        }
    });

    // ─── 滑鼠靠近頂部顯示 Navbar ───
    document.addEventListener('mousemove', (e) => {
        if (e.clientY < 50) navbar.classList.remove('nav-hidden');
    });

    // ─── 自動標示目前頁面導覽項目 ───
    const currentPath = window.location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('.nav-link-item').forEach(link => {
        const href = (link.getAttribute('href') || '').split('/').pop();
        if (href && href === currentPath) link.classList.add('active-page');
    });

    // ─── 桌面版搜尋列 ───
    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            if (!searchInput.classList.contains('open')) {
                searchInput.classList.add('open');
                searchInput.focus();
            } else {
                if (searchInput.value.trim()) alert('執行搜尋：' + searchInput.value);
                else searchInput.classList.remove('open');
            }
        });
    }

    // ─── 點擊按鈕回到最上方 ───
    backToTopBtn?.addEventListener('click', () => {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    /* ════ COOKIE 同意橫欄 ════ */
    (function () {
        // 會員頁面不顯示
        const excludePages = ['customer_system', 'customer_progress'];
        const currentPage = window.location.pathname;
        const isExcluded = excludePages.some(page => currentPage.includes(page));
        if (isExcluded) return;

        // 已同意過就不再顯示
        if (localStorage.getItem('dv_cookie_consent')) return;

        // 動態插入 HTML
        const banner = document.createElement('div');
        banner.id = 'cookieBanner';
        banner.style.cssText = /*css*/ `
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        z-index: 99998;
        background: rgba(250, 247, 242, 0.98);
        border-top: 1px solid rgba(197, 160, 89, 0.4);
        box-shadow: 0 -8px 40px rgba(0, 0, 0, 0.08);
        backdrop-filter: blur(8px);
        -webkit-backdrop-filter: blur(8px);
        padding: 18px 56px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 32px;
        transform: translateY(100%);
        transition: transform 0.5s ease;
    `;
        banner.innerHTML = /*html*/ `
        <div style="display:flex; align-items:center; gap:14px; flex:1; min-width:0;">
            <span style="font-size:18px; color:rgba(197,160,89,0.8); flex-shrink:0;">✦</span>
            <p style="margin:0; font-size:12.5px; color:#6a6053; letter-spacing:0.1em; line-height:1.9;">
                本網站使用 Cookie 以提供最佳瀏覽體驗，並分析流量與使用狀況。繼續使用即表示您同意我們的
                <a href="#" style="color:#b8922a; text-decoration:none; border-bottom:1px solid rgba(184,146,42,0.4);">隱私政策</a>。
            </p>
        </div>
        <div style="display:flex; align-items:center; gap:16px; flex-shrink:0;">
            <button id="cookieDeny" style="
                background: none;
                border: 1px solid rgba(140,126,111,0.4);
                color: #8c7e6f;
                font-size: 11px;
                letter-spacing: 0.35em;
                text-transform: uppercase;
                padding: 9px 22px;
                cursor: pointer;
                border-radius: 0;
                font-family: inherit;
                transition: background 0.3s, border-color 0.3s;
                white-space: nowrap;
            ">拒絕</button>
            <button id="cookieAccept" style="
                background: rgba(197,160,89,0.12);
                border: 1px solid rgba(197,160,89,0.6);
                color: #b8922a;
                font-size: 11px;
                letter-spacing: 0.35em;
                text-transform: uppercase;
                padding: 9px 22px;
                cursor: pointer;
                border-radius: 0;
                font-family: inherit;
                transition: background 0.3s, border-color 0.3s;
                white-space: nowrap;
            ">同意</button>
        </div>
    `;

        document.body.appendChild(banner);

        // 延遲 1.2 秒滑入
        setTimeout(() => {
            banner.style.transform = 'translateY(0)';
        }, 1200);

        function dismissBanner(accepted) {
            banner.style.transform = 'translateY(100%)';
            localStorage.setItem('dv_cookie_consent', accepted ? 'accepted' : 'denied');
        }

        document.getElementById('cookieAccept').addEventListener('click', () => dismissBanner(true));
        document.getElementById('cookieDeny').addEventListener('click', () => dismissBanner(false));
    })();



});