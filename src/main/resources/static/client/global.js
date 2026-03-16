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

    /* ════ COOKIE 同意卡片（右下角浮動 RWD） ════ */
    (function () {
        const excludePages = ['customer_system', 'customer_progress'];
        const currentPage = window.location.pathname;
        const isExcluded = excludePages.some(page => currentPage.includes(page));
        if (isExcluded) return;

        if (localStorage.getItem('dv_cookie_consent')) return;

        const style = document.createElement('style');
        style.textContent = `
        #cookieBanner {
            position: fixed;
            bottom: 80px;
            right: 24px;
            z-index: 99998;
            width: 300px;
            background: rgba(250, 247, 242, 0.98);
            border: 1px solid rgba(197, 160, 89, 0.35);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.10);
            backdrop-filter: blur(8px);
            -webkit-backdrop-filter: blur(8px);
            padding: 20px 20px 16px;
            border-radius: 4px;
            opacity: 0;
            transform: translateY(20px);
            transition: transform 0.45s ease, opacity 0.45s ease;
        }
        @media (max-width: 480px) {
            #cookieBanner {
                width: auto;
                left: 12px;
                right: 12px;
                bottom: 12px;
                border-radius: 4px;
            }
        }
    `;
        document.head.appendChild(style);

        const banner = document.createElement('div');
        banner.id = 'cookieBanner';
        banner.innerHTML = /*html*/`
        <div style="display:flex; align-items:center; gap:10px; margin-bottom:10px;">
            <span style="font-size:14px; color:rgba(197,160,89,0.85);">✦</span>
            <span style="font-size:11px; letter-spacing:0.25em; color:#b8922a; text-transform:uppercase;">Cookie 通知</span>
        </div>
        <p style="margin:0 0 16px; font-size:12px; color:#6a6053; letter-spacing:0.05em; line-height:1.85;">
            本網站使用 Cookie 以提供最佳瀏覽體驗，並分析流量與使用狀況。繼續使用即表示您同意我們的
            <a href="privacy.html" style="color:#b8922a; text-decoration:none; border-bottom:1px solid rgba(184,146,42,0.35);">隱私政策</a>。
        </p>
        <div style="display:flex; flex-direction:column; gap:10px; margin-top:4px;">
            <button id="cookieAccept" style="
                background: rgba(197,160,89,0.15);
                border: 1px solid rgba(197,160,89,0.55);
                color: #b8922a;
                font-size: 14px;
                letter-spacing: 0.3em;
                text-transform: uppercase;
                padding: 13px 16px;
                cursor: pointer;
                border-radius: 0;
                font-family: inherit;
                width: 100%;
                font-weight: 400;
            ">同意</button>
            <button id="cookieDeny" style="
                background: none;
                border: none;
                color: #8c7e6f;
                font-size: 13px;
                letter-spacing: 0.2em;
                text-transform: uppercase;
                padding: 8px 16px;
                cursor: pointer;
                border-radius: 0;
                font-family: inherit;
                width: 100%;
                text-decoration: underline;
                text-underline-offset: 3px;
            ">拒絕</button>
        </div>
    `;

        document.body.appendChild(banner);

        setTimeout(() => {
            banner.style.opacity = '1';
            banner.style.transform = 'translateY(0)';
        }, 1200);

        function dismissBanner(accepted) {
            banner.style.opacity = '0';
            banner.style.transform = 'translateY(20px)';
            setTimeout(() => banner.remove(), 450);
            localStorage.setItem('dv_cookie_consent', accepted ? 'accepted' : 'denied');
        }

        document.getElementById('cookieAccept').addEventListener('click', () => dismissBanner(true));
        document.getElementById('cookieDeny').addEventListener('click', () => dismissBanner(false));
    })();


});