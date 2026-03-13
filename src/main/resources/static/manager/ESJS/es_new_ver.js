// ES-Helper.js - 系統共用互動邏輯

document.addEventListener('DOMContentLoaded', () => {
    
    // ================= 1. 側邊欄開合邏輯 =================
    const sidebar = document.getElementById('sidebar');
    const hamburgerBtn = document.getElementById('sidebar-hamburger');
    // 抓取黑色遮罩 (請確保 HTML 中有加上 id="sidebar-overlay")
    const overlay = document.getElementById('sidebar-overlay');

    if (hamburgerBtn && sidebar) {
        hamburgerBtn.addEventListener('click', function (e) {
            e.stopPropagation(); // 防止點擊事件冒泡影響其他元素

            const isDesktop = window.innerWidth >= 1024; // 1024px 對應 Tailwind 的 lg 斷點

            if (isDesktop) {
                // 【桌機版邏輯】：切換寬度 (w-64 展開, w-20 收縮保留圖示)
                if (sidebar.classList.contains('w-64')) {
                    sidebar.classList.remove('w-64');
                    sidebar.classList.add('w-20');
                } else {
                    sidebar.classList.remove('w-20');
                    sidebar.classList.add('w-64');
                }
            } else {
                // 【手機/平板版邏輯】：滑出/滑入 (Offcanvas 效果)
                sidebar.classList.toggle('-translate-x-full');
                sidebar.classList.toggle('translate-x-0');
                
                // 同時切換黑色遮罩的顯示狀態
                if (overlay) {
                    overlay.classList.toggle('hidden');
                }
            }
        });
    }

    // 【手機版專用】：點擊黑色遮罩時，關閉側邊欄
    if (overlay) {
        overlay.addEventListener('click', function () {
            sidebar.classList.add('-translate-x-full');
            sidebar.classList.remove('translate-x-0');
            overlay.classList.add('hidden');
        });
    }

    // 【視窗縮放防呆】：當使用者拖拉瀏覽器視窗大小時的自動重置
    window.addEventListener('resize', () => {
        if (window.innerWidth >= 1024) {
            // 拉大到桌機尺寸：隱藏遮罩，移除手機版的隱藏位移
            if (overlay) overlay.classList.add('hidden');
            sidebar.classList.remove('-translate-x-full');
        } else {
            // 縮小到手機尺寸：強制收合側邊欄，重置寬度為 w-64，確保下次滑出是正常寬度
            if (overlay) overlay.classList.add('hidden');
            sidebar.classList.add('-translate-x-full');
            sidebar.classList.remove('translate-x-0');
            sidebar.classList.remove('w-20');
            sidebar.classList.add('w-64');
        }
    });

    // ================= 2. 使用者下拉選單邏輯 =================
    const userMenuBtn = document.getElementById('user-menu-button');
    const userDropdown = document.getElementById('user-dropdown');

    if (userMenuBtn && userDropdown) {
        // 點擊頭像切換選單
        userMenuBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            userDropdown.classList.toggle('hidden');
        });

        // 點擊畫面其他地方自動關閉選單
        document.addEventListener('click', (e) => {
            if (!userMenuBtn.contains(e.target) && !userDropdown.contains(e.target)) {
                userDropdown.classList.add('hidden');
            }
        });
    }
});