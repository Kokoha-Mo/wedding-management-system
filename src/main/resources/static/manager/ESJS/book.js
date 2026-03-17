const API_BASE = 'http://localhost:8080/api';

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    loadBooks('處理中');
});

function initSidebar() {
    const empId = sessionStorage.getItem('empId');
    const empName = sessionStorage.getItem('empName');
    const position = sessionStorage.getItem('position');
    const deptId = sessionStorage.getItem('deptId');

    if (!empId) {
        window.location.replace('login.html');
        return;
    }

    // 顯示人員資訊
    const sidebarEmpName = document.getElementById('sidebar-emp-name');
    const headerEmpName = document.getElementById('header-emp-name');
    const sidebarPosition = document.getElementById('sidebar-position');

    if (sidebarEmpName) sidebarEmpName.textContent = empName || '未知員工';
    if (headerEmpName) headerEmpName.textContent = empName || '未知員工';
    if (sidebarPosition) sidebarPosition.textContent = position === 'MANAGER' ? '業務管理' : '員工';

    // 顯示頭像
    const imgPath = sessionStorage.getItem('imgPath');
    if (imgPath) {
        const fullPath = imgPath.startsWith('http') ? imgPath : '/' + imgPath;
        const sidebarAv = document.getElementById('sidebar-avatar');
        const headerAv = document.getElementById('header-avatar');
        if (sidebarAv) sidebarAv.src = fullPath;
        if (headerAv) headerAv.src = fullPath;
    }

    // 側邊欄權限控制
    const navIds = ['nav-consultation', 'nav-book', 'nav-project', 'nav-task'];
    navIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });

    if (deptId === '1') {
        ['nav-book', 'nav-project'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'flex';
        });
    } else if (deptId === '7') {
        const el = document.getElementById('nav-consultation');
        if (el) el.style.display = 'flex';
    } else {
        const el = document.getElementById('nav-task');
        if (el) el.style.display = 'flex';
    }
}

// Tab 切換
function switchTab(tabId) {
    const tabs = ['tab-pending', 'tab-signed', 'tab-cancelled'];
    const btns = ['btn-tab-pending', 'btn-tab-signed', 'btn-tab-cancelled'];

    tabs.forEach((t, i) => {
        const tabEl = document.getElementById(t);
        const btnEl = document.getElementById(btns[i]);
        if (t === tabId) {
            tabEl.classList.remove('hidden');
            btnEl.classList.add('bg-primary', 'text-white', 'shadow-sm');
            btnEl.classList.remove('text-gray-500', 'dark:text-gray-400', 'hover:bg-gray-100', 'dark:hover:bg-gray-700');
        } else {
            tabEl.classList.add('hidden');
            btnEl.classList.remove('bg-primary', 'text-white', 'shadow-sm');
            btnEl.classList.add('text-gray-500', 'dark:text-gray-400', 'hover:bg-gray-100', 'dark:hover:bg-gray-700');
        }
    });

    // 切換 tab 時載入對應資料
    if (tabId === 'tab-pending') loadBooks('處理中');
    if (tabId === 'tab-signed') loadBooks('已簽約');
    if (tabId === 'tab-cancelled') loadBooks('取消');
}

async function loadStatusCounts() {
    try {
        const res = await fetch(`${API_BASE}/books/status-counts`);
        const counts = await res.json();

        document.getElementById('badge-pending').textContent = counts['處理中'] ?? 0;
        document.getElementById('badge-signed').textContent = counts['已簽約'] ?? 0;
        document.getElementById('badge-cancelled').textContent = counts['取消'] ?? 0;
    } catch (err) {
        console.error('[API] 載入狀態數量失敗:', err);
    }
}

// ════════════════════════════════════════
// Modal 控制
// ════════════════════════════════════════
function toggleModal(modalID) {
    document.getElementById(modalID).classList.toggle('hidden');
    const backdrop = document.getElementById(modalID + '-backdrop');
    if (backdrop) backdrop.classList.toggle('hidden');
}

function openPlanModal(id) {
    document.getElementById(id).classList.remove('hidden');
    document.getElementById(id + '-backdrop').classList.remove('hidden');
}

function closePlanModal(id) {
    document.getElementById(id).classList.add('hidden');
    document.getElementById(id + '-backdrop').classList.add('hidden');
}

// ════════════════════════════════════════
// 篩選功能
// ════════════════════════════════════════
function toggleFilterPanel() {
    document.getElementById('filter-panel').classList.toggle('hidden');
}

// 點擊外部關閉篩選面板
document.addEventListener('click', function (e) {
    const panel = document.getElementById('filter-panel');
    const btn = document.getElementById('btn-filter');
    if (!panel.contains(e.target) && !btn.contains(e.target)) {
        panel.classList.add('hidden');
    }
});

const activeFilters = { staff: new Set(), theme: new Set(), guests: new Set() };

function toggleFilter(el, group) {
    const val = el.dataset.value;
    if (activeFilters[group].has(val)) {
        activeFilters[group].delete(val);
        el.classList.remove('bg-primary', 'text-white', 'border-primary');
        el.classList.add('text-gray-500', 'border-gray-200');
    } else {
        activeFilters[group].add(val);
        el.classList.add('bg-primary', 'text-white', 'border-primary');
        el.classList.remove('text-gray-500', 'border-gray-200');
    }
    updateFilterDot();
    applyFilters();
}

function clearFilters() {
    ['staff', 'theme', 'guests'].forEach(g => activeFilters[g].clear());
    document.querySelectorAll('.filter-chip').forEach(el => {
        el.classList.remove('bg-primary', 'text-white', 'border-primary');
        el.classList.add('text-gray-500', 'border-gray-200');
    });
    updateFilterDot();
    applyFilters();
}

function updateFilterDot() {
    const hasActive = ['staff', 'theme', 'guests'].some(g => activeFilters[g].size > 0);
    document.getElementById('filter-active-dot').classList.toggle('hidden', !hasActive);
}

function applyFilters() {
    document.querySelectorAll('#card-slider > div[data-staff]').forEach(card => {
        const staff = card.dataset.staff || '';
        const theme = card.dataset.theme || '';
        const guests = parseInt(card.dataset.guests || '0');
        const bucket = guests < 100 ? 'small' : guests <= 200 ? 'medium' : 'large';
        const ok = (activeFilters.staff.size === 0 || activeFilters.staff.has(staff))
            && (activeFilters.theme.size === 0 || activeFilters.theme.has(theme))
            && (activeFilters.guests.size === 0 || activeFilters.guests.has(bucket));
        card.style.display = ok ? '' : 'none';
    });
}

// ════════════════════════════════════════
// 卡片滑動
// ════════════════════════════════════════
function scrollCards(direction) {
    const container = document.getElementById('card-slider');
    const card = container.querySelector('div[data-staff]') || container.querySelector('div.snap-start');
    const cardWidth = card ? card.offsetWidth + 16 : container.clientWidth / 3;
    container.scrollBy({ left: direction * cardWidth, behavior: 'smooth' });
}

// ════════════════════════════════════════
// 細項連動（modal-modify 用）
// ════════════════════════════════════════
function toggleSubItems(mainCheckbox, subContainerId) {
    const container = document.getElementById(subContainerId);
    if (!container) return;
    const inputs = container.querySelectorAll('input');
    if (mainCheckbox.checked) {
        container.classList.remove('opacity-50', 'pointer-events-none');
        inputs.forEach(input => input.disabled = false);
    } else {
        container.classList.add('opacity-50', 'pointer-events-none');
        inputs.forEach(input => { input.disabled = true; input.checked = false; });
    }
}

// ════════════════════════════════════════
// API：載入預約列表
// ════════════════════════════════════════
async function loadBooks(status = '處理中') {
    try {
        const res = await fetch(`${API_BASE}/books?status=${encodeURIComponent(status)}`);
        const result = await res.json();

        if (status === '處理中') renderPendingCards(result);
        if (status === '已簽約') renderSignedTable(result);
        if (status === '取消') renderCancelledTable(result);

    } catch (err) {
        console.error('[API] 載入列表失敗:', err);
    }
}

// ════════════════════════════════════════
// API：建立預約
// ════════════════════════════════════════
async function submitCreateBook() {
    const nameA = document.getElementById('input-nameA').value.trim();
    const nameB = document.getElementById('input-nameB').value.trim();
    const name = nameB ? `${nameA} & ${nameB}` : nameA;  // 合併成 "A & B"，B 選填
    const tel = document.getElementById('input-tel').value.trim();
    const email = document.getElementById('input-email').value.trim();

    if (!nameA || !tel) {
        alert('請填寫新郎/新娘姓名與手機號碼');
        return;
    }

    // Step 1: 查相似客戶
    let checkResult = [];
    if (email) {
        const checkRes = await fetch(
            `${API_BASE}/books/check-duplicate?email=${encodeURIComponent(email)}`
        );
        checkResult = await checkRes.json();

        if (checkResult.length > 0) {
            const confirmed = window.confirm(
                `此 Email 已有客戶資料：${checkResult[0].name}\n` +
                `手機：${checkResult[0].tel}\n\n` +
                `確定要覆蓋舊預約嗎？`
            );
            if (!confirmed) return;
        }
    }

    // Step 2: 建立預約
    const themeRadio = document.querySelector('input[name="theme"]:checked');
    const styles = themeRadio
        ? themeRadio.closest('label').querySelector('span').textContent.trim()
        : '';

    const payload = {
        name, tel, email,
        line_id: document.getElementById('input-lineid').value.trim(),
        wedding_date: document.getElementById('input-wedding-date').value || null,
        guest_scale: parseInt(document.getElementById('input-guest-count').value) || null,
        place: document.getElementById('input-venue').value.trim(),
        styles,
        content: document.getElementById('input-notes').value.trim()
    };

    try {
        const res = await fetch(`${API_BASE}/books`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const result = await res.json();

        if (res.ok) {
            toggleModal('modal-create-customer');
            showToast(checkResult.length > 0 ? '預約已覆蓋更新！' : '新客戶預約已建立！');
            loadBooks('處理中');
        } else {
            alert('建立失敗：' + (result.message || '請確認欄位'));
        }
    } catch (err) {
        console.error(err);
        alert('連線失敗，請確認後端服務是否啟動');
    }
}

// ════════════════════════════════════════
// API：更新預約狀態
// ════════════════════════════════════════
async function updateBookStatus(bookId, newStatus) {
    try {
        const res = await fetch(`${API_BASE}/books/${bookId}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });

        if (res.ok) {
            showToast('狀態已更新');
            loadStatusCounts();
            loadBooks('處理中');
        }
    } catch (err) {
        console.error('[API] 更新狀態失敗:', err);
    }
}

// ════════════════════════════════════════
// 渲染：處理中卡片
// ════════════════════════════════════════
function renderPendingCards(books) {
    const slider = document.getElementById('card-slider');
    slider.innerHTML = '';

    if (books.length === 0) {
        slider.innerHTML = `<p class="text-gray-400 text-sm py-4">目前沒有處理中的預約</p>`;
        return;
    }

    books.forEach(book => {
        const card = document.createElement('div');
        card.className = 'snap-start shrink-0 w-[calc(33.333%-11px)] min-w-[420px] bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 flex flex-col overflow-hidden hover:shadow-md transition-shadow';
        card.dataset.staff = book.managerName || '';
        card.dataset.theme = book.styles || '';
        card.dataset.guests = book.guestScale || '0';

        card.innerHTML = `
            <div class="p-4 flex-1">
                <div class="flex justify-between items-center mb-2">
                    <span class="px-2 py-0.5 rounded text-[9px] font-bold bg-blue-50 text-blue-600">
                        ${book.managerName || '未分配'}
                    </span>
                    <span class="text-[9px] text-gray-400 font-medium">
                        ${book.createAt ? book.createAt.split('T')[0] : ''}
                    </span>
                </div>
                <h4 class="text-[16px] font-bold text-gray-900 dark:text-white mb-3 truncate">
                    ${book.customerName || ''}
                </h4>
                <div class="space-y-1.5">
                    <div class="text-[12px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">聯絡手機</span>
                        <span class="font-medium truncate">${book.tel || '-'}</span>
                    </div>
                    <div class="text-[12px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">LINE ID</span>
                        <span class="font-medium truncate">${book.lineId || '-'}</span>
                    </div>
                    <div class="text-[12px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">預計婚期</span>
                        <span class="font-medium truncate">${book.weddingDate || '-'}</span>
                    </div>
                    <div class="text-[12px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">賓客規模</span>
                        <span class="font-medium truncate">${book.guestScale ? book.guestScale + ' 人' : '-'}</span>
                    </div>
                    <div class="text-[12px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">宴客場地</span>
                        <span class="font-medium truncate">${book.place || '-'}</span>
                    </div>
                    <div class="text-[12px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">視覺定調</span>
                        <span class="font-medium truncate">${book.styles || '-'}</span>
                    </div>
                </div>
                <div class="mt-3 p-2 bg-gray-50 rounded text-[12px] text-gray-500 italic border border-gray-100 line-clamp-2">
                    "${book.content || '無備註'}"
                </div>
            </div>
            <div class="flex border-t border-gray-100 bg-gray-50/30">
                <button class="flex-1 py-2.5 text-[12px] font-medium text-gray-500 hover:text-gray-700 border-r border-gray-100 transition-colors">
                    查看需求
                </button>
                <button onclick="updateBookStatus(${book.bookId}, '已簽約')"
                    class="flex-1 py-2.5 text-[12px] font-bold text-primary hover:bg-blue-50 border-r border-gray-100 transition-colors">
                    轉為簽約
                </button>
                <button onclick="updateBookStatus(${book.bookId}, '取消')"
                    class="flex-1 py-2.5 text-[12px] font-bold text-red-400 hover:bg-red-50 transition-colors">
                    取消
                </button>
            </div>
        `;
        slider.appendChild(card);
    });
}

// ════════════════════════════════════════
// 渲染：已簽約 table
// ════════════════════════════════════════
function renderSignedTable(books) {
    const container = document.getElementById('signed-list');
    container.innerHTML = '';

    if (books.length === 0) {
        container.innerHTML = `<p class="text-gray-400 text-sm px-5 py-4">目前沒有已簽約的預約</p>`;
        return;
    }

    books.forEach(book => {
        const row = document.createElement('div');
        row.className = 'grid grid-cols-[2fr_1.5fr_2fr_1.5fr_1.5fr_1.5fr] px-5 py-3.5 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors items-center';
        row.innerHTML = `
            <span class="text-[12px] font-medium text-gray-800 dark:text-gray-200">${book.customerName || '-'}</span>
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.weddingDate || '-'}</span>
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.place || '-'}</span>
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.styles || '-'}</span>
            <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-600 w-fit">${book.managerName || '-'}</span>
            <button onclick="viewBookDetail(${book.bookId})"
                class="text-[12px] text-primary hover:underline font-medium text-left">
                查看詳細
            </button>
        `;
        container.appendChild(row);
    });
}

// ════════════════════════════════════════
// 渲染：取消 table
// ════════════════════════════════════════
function renderCancelledTable(books) {
    const container = document.getElementById('cancelled-list');
    container.innerHTML = '';

    if (books.length === 0) {
        container.innerHTML = `<p class="text-gray-400 text-sm px-5 py-4">目前沒有取消的預約</p>`;
        return;
    }

    books.forEach(book => {
        const row = document.createElement('div');
        row.className = 'grid grid-cols-[2fr_2fr_2.5fr_2fr_1.5fr] px-5 py-3.5 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors items-center';
        row.innerHTML = `
            <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-gray-100 text-gray-500 border border-gray-200 w-fit">已取消</span>
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.createAt ? book.createAt.split('T')[0] : '-'}</span>
            <span class="text-[12px] font-medium text-gray-800 dark:text-gray-200">${book.customerName || '-'}</span>
            <span class="text-[12px] text-gray-500 dark:text-gray-400">${book.tel || '-'}</span>
            <button onclick="updateBookStatus(${book.bookId}, '處理中')"
                class="text-[12px] font-bold text-primary hover:underline">
                恢復預約
            </button>
        `;
        container.appendChild(row);
    });

    // 更新筆數
    const countEl = document.getElementById('cancelled-count');
    if (countEl) countEl.textContent = `共 ${books.length} 筆取消記錄`;
}

// ════════════════════════════════════════
// 工具函式
// ════════════════════════════════════════
function showToast(message, type = 'success') {
    const colors = { success: '#22c55e', error: '#ef4444' };
    const toast = document.createElement('div');
    toast.textContent = message;
    toast.style.cssText = `
        position:fixed; bottom:24px; right:24px; z-index:9999;
        padding:12px 20px; border-radius:10px; color:#fff;
        background:${colors[type]}; font-size:14px; font-weight:600;
        box-shadow:0 4px 12px rgba(0,0,0,.15);
    `;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}