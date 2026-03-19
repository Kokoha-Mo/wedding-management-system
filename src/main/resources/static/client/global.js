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

    // ─── 點擊按鈕回到最上方 ───
    backToTopBtn?.addEventListener('click', () => {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });


});