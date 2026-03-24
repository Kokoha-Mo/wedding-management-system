const API_BASE = 'http://localhost:8080/api/employee';

// ════════════════════════════════════════
// Alert / Confirm Modal 函式
// ════════════════════════════════════════
function showAlert(message, icon = 'info') {
    document.getElementById('alert-message').textContent = message;
    document.getElementById('alert-icon').textContent = icon;
    document.getElementById('modal-alert').classList.remove('hidden');
}

function closeAlert() {
    document.getElementById('modal-alert').classList.add('hidden');
    if (window._alertCallback) { window._alertCallback(); window._alertCallback = null; }
}

let _confirmResolve = null;

function showConfirm(message, type = 'default') {
    document.getElementById('confirm-message').textContent = message;
    const iconWrap = document.getElementById('confirm-icon-wrap');
    const icon     = document.getElementById('confirm-icon');
    const okBtn    = document.getElementById('confirm-ok-btn');
    if (type === 'danger') {
        iconWrap.className = 'flex items-center justify-center w-14 h-14 rounded-full mb-4 bg-red-100 dark:bg-red-900/30 text-red-500';
        icon.textContent   = 'warning';
        okBtn.className    = 'flex-1 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-xl text-sm font-bold shadow-sm transition-colors';
    } else {
        iconWrap.className = 'flex items-center justify-center w-14 h-14 rounded-full mb-4 bg-indigo-100 dark:bg-indigo-900/30 text-indigo-500';
        icon.textContent   = 'help_outline';
        okBtn.className    = 'flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-sm font-bold shadow-sm transition-colors';
    }
    document.getElementById('modal-confirm').classList.remove('hidden');
    return new Promise(resolve => { _confirmResolve = resolve; });
}

function resolveConfirm(result) {
    document.getElementById('modal-confirm').classList.add('hidden');
    if (_confirmResolve) { _confirmResolve(result); _confirmResolve = null; }
}

document.addEventListener('DOMContentLoaded', () => {
    // 1. 取得「台灣當地時間」的今天日期，格式為 YYYY-MM-DD
    const today = new Date().toLocaleDateString('sv-SE');

    // 2. 限制「服務細項」的儀式日期
    document.querySelectorAll('input[id^="input-date-ceremony-"]').forEach(input => {
        input.min = today;
    });

    // 3. 限制「新增預約」的婚宴日期 (input-wedding-date)
    const inputWeddingDate = document.getElementById('input-wedding-date');
    if (inputWeddingDate) {
        inputWeddingDate.min = today;
    }

    // 4. 限制「修改資料」的婚宴日期 (edit-wedding-date)
    const editWeddingDate = document.getElementById('edit-wedding-date');
    if (editWeddingDate) {
        editWeddingDate.min = today;
    }
    loadStatusCounts();
    initSidebar();
    loadBooks('處理中');
    loadBooks('取消');

});

async function loadAllServices() {
    try {
        const res  = await fetch(`${API_BASE}/books/services`, { credentials: 'include' });
        if (!res.ok) return;
        const list = await res.json();
        window._serviceMap = {};
        list.forEach(s => {
            window._serviceMap[String(s.id)] = { name: s.name, price: s.price || 0 };
        });
    } catch (e) { /* 靜默失敗 */ }
}

// ════════════════════════════════════════
// 初始化 Sidebar 員工資訊
// ════════════════════════════════════════
function initSidebar() {
    const empId = sessionStorage.getItem('empId');
    const empName = sessionStorage.getItem('empName');
    const position = sessionStorage.getItem('position');
    const deptId = sessionStorage.getItem('deptId');
    const imgPath = sessionStorage.getItem('imgPath');

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

    // 顯示頭像 (側邊欄與右上角)
    if (imgPath) {
        // 確保路徑正確，如果不是完整 URL 則補上斜線
        const fullPath = imgPath.startsWith('http') ? imgPath : '/' + imgPath;
        const sidebarAv = document.getElementById('sidebar-avatar');
        const headerAv = document.getElementById('header-avatar');
        if (sidebarAv) sidebarAv.src = fullPath;
        if (headerAv) headerAv.src = fullPath;
    }

    // 側邊欄權限控制 (根據 dept_id 顯示對應功能)
    const navIds = ['nav-consultation', 'nav-book', 'nav-project', 'nav-task'];
    navIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none'; // 先隱藏所有
    });

    if (deptId === '1') {
        if (position === 'MANAGER') {
            // 營運部/管理層：可看預約管理 & 專案管理
            ['nav-book', 'nav-project'].forEach(id => {
                const el = document.getElementById(id);
                if (el) el.style.display = 'flex';
            });
        } else {
            // Department 1 Staff: See Task management
            const el = document.getElementById('nav-task');
            if (el) el.style.display = 'flex';
        }
    } else if (deptId === '7') {
        // 接待部：可看諮詢管理
        const el = document.getElementById('nav-consultation');
        if (el) el.style.display = 'flex';
    } else {
        // 其他 (如場地部、活動部等)：看任務管理
        const el = document.getElementById('nav-task');
        if (el) el.style.display = 'flex';
    }
}

// ════════════════════════════════════════
// API：載入狀態數量（badge）
// ════════════════════════════════════════
async function loadStatusCounts() {
    try {
        const managerId = sessionStorage.getItem('empId');
        const res    = await fetch(`${API_BASE}/books/status-counts?managerId=${managerId}`, {
            credentials: 'include'
        });
        const counts = await res.json();
        const resCancel = await fetch(`${API_BASE}/books/status-counts`, {
            credentials: 'include'
        });
        const countsCancel = await resCancel.json();
        document.getElementById('badge-pending').textContent   = counts['處理中'] ?? 0;
        document.getElementById('badge-signed').textContent    = counts['已簽約'] ?? 0;
        document.getElementById('badge-cancelled').textContent = countsCancel['取消']   ?? 0;
    } catch (err) {
        console.error('[API] 載入狀態數量失敗:', err);
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

    if (tabId === 'tab-pending')   loadBooks('處理中');
    if (tabId === 'tab-signed')    loadBooks('已簽約');
    if (tabId === 'tab-cancelled') loadBooks('取消');
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

document.addEventListener('click', function(e) {
    const panel = document.getElementById('filter-panel');
    const btn   = document.getElementById('btn-filter');
    if (!panel || !btn) return;
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
        const staff  = card.dataset.staff  || '';
        const theme  = card.dataset.theme  || '';
        const guests = parseInt(card.dataset.guests || '0');
        const bucket = guests < 100 ? 'small' : guests <= 200 ? 'medium' : 'large';
        const ok = (activeFilters.staff.size  === 0 || activeFilters.staff.has(staff))
            && (activeFilters.theme.size  === 0 || activeFilters.theme.has(theme))
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
// API：載入預約列表 (已加入錯誤捕捉與陣列檢查)
// ════════════════════════════════════════
async function loadBooks(status = '處理中') {
    // ── 顯示 loading ──
    if (status === '處理中') {
        document.getElementById('card-slider').innerHTML = `
            <div class="flex flex-col items-center justify-center w-full py-12 text-gray-400">
                <svg style="width:36px;height:36px;animation:spin 0.8s linear infinite;margin-bottom:12px;"
                     viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2.5">
                    <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
                </svg>
                <p class="text-sm font-medium">正在載入預約資料...</p>
            </div>
        `;
    }
    if (status === '已簽約') {
        document.getElementById('signed-list').innerHTML = `
            <div class="flex flex-col items-center justify-center w-full py-12 text-gray-400">
                <svg style="width:36px;height:36px;animation:spin 0.8s linear infinite;margin-bottom:12px;"
                     viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2.5">
                    <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
                </svg>
                <p class="text-sm font-medium">正在載入資料...</p>
            </div>
        `;
    }
    if (status === '取消') {
        document.getElementById('cancelled-list').innerHTML = `
            <div class="flex flex-col items-center justify-center w-full py-12 text-gray-400">
                <svg style="width:36px;height:36px;animation:spin 0.8s linear infinite;margin-bottom:12px;"
                     viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2.5">
                    <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
                </svg>
                <p class="text-sm font-medium">正在載入資料...</p>
            </div>
        `;
    }
    try {
        const managerId = sessionStorage.getItem('empId');

        let url = `${API_BASE}/books?status=${encodeURIComponent(status)}`;
        if (status !== '取消') {
            url += `&managerId=${managerId}`;
        }

        // --- 1. 發送請求 ---
        const res = await fetch(url, { credentials: 'include' });

        // --- 2. 檢查 HTTP 狀態碼 (防護一) ---
        if (!res.ok) {
            // 💡 加上這段：如果遇到 401 或 403，代表憑證過期或沒權限
            if (res.status === 401 || res.status === 403) {
                alert("您的登入已過期，請重新登入！");
                sessionStorage.clear(); // 清除使用者的暫存資料
                window.location.replace("login.html"); // 自動踢回登入頁
                return;
            }

            console.error(`[API 錯誤] 載入列表失敗，狀態碼: ${res.status}`);
            // ... 下面保留你原本寫的空陣列防護 ...
            if (status === '處理中') renderPendingCards([]);
            if (status === '已簽約') renderSignedTable([]);
            if (status === '取消')   renderCancelledTable([]);
            return;
        }

        // --- 3. 解析 JSON 資料 ---
        let result = await res.json();
        console.log('loadBooks 回傳:', result);
        //console.log("從後端拿到的第一筆資料時間是：", result[0].createAt);

        // ★★★ 關鍵：印出後端回傳的原始資料 ★★★
        result.sort((a, b) => {
            // 優先使用 updateAt，如果沒有才用 createAt，確保用來排序的時間跟畫面上顯示的一樣
            const timeA = new Date(a.updateAt || a.createAt || 0).getTime();
            const timeB = new Date(b.updateAt || b.createAt || 0).getTime();

            // timeB - timeA 代表「降冪排列」，也就是最新/數字越大的排越前面
            return timeB - timeA;
        });

        // --- 4. 檢查是否為陣列 (防護二) ---
        if (!Array.isArray(result)) {

            // 嘗試從常見的分頁/包裝格式中取出陣列
            if (result && Array.isArray(result.data)) {
                //console.log('✔ 已從 result.data 中取出陣列');
                result = result.data;
            } else if (result && Array.isArray(result.content)) {
                //console.log('✔ 已從 result.content 中取出陣列');
                result = result.content;
            } else {
                result = [];
            }
        }

        // --- 5. 渲染畫面 ---
        if (status === '處理中') renderPendingCards(result);
        if (status === '已簽約') renderSignedTable(result);
        if (status === '取消')   renderCancelledTable(result);

    } catch (err) {
        console.error('[API] 載入列表發生網路錯誤或例外:', err);
    }
}

// ════════════════════════════════════════
// API：建立預約
// ════════════════════════════════════════
async function submitCreateBook() {
    const nameA = document.getElementById('input-nameA').value.trim();
    const nameB = document.getElementById('input-nameB').value.trim();
    const name = nameB ? `${nameA} & ${nameB}` : nameA;
    const tel   = document.getElementById('input-tel').value.trim();
    const email = document.getElementById('input-email').value.trim();

    if (!name || !tel) {
        showToast('請填寫姓名與手機號碼');
        return;
    }

    if (!/^\d{10}$/.test(tel)) {
        showToast('手機號碼錯誤，必須為 10 位數字！', 'error');
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showToast('Email 格式不正確（例如：example@gmail.com）', 'error');
        return;
    }

    let checkResult = [];

    // 先查 email 是否重複
    if (email) {
        const checkRes = await fetch(
            `${API_BASE}/books/check-duplicate?email=${encodeURIComponent(email)}`,
            { credentials: 'include' }
        );
        checkResult = await checkRes.json();
    }

    // 再查 tel 是否重複（若 email 沒查到）
    if (checkResult.length === 0 && tel) {
        const checkTelRes = await fetch(
            `${API_BASE}/books/check-duplicate?tel=${encodeURIComponent(tel)}`,
            { credentials: 'include' }
        );
        checkResult = await checkTelRes.json();
    }

    // 有重複 → 提示確認
    if (checkResult.length > 0) {
        const confirmed = await showConfirm(
            `此客戶已有資料：${checkResult[0].name}\n` +
            `手機：${checkResult[0].tel}\n` +
            `Email：${checkResult[0].email || '-'}\n\n` +
            `確定要覆蓋舊預約嗎？`
        );
        if (!confirmed) return;
    }

    const themeRadio = document.querySelector('input[name="theme"]:checked');
    const styles = themeRadio
        ? themeRadio.closest('label').querySelector('span').textContent.trim()
        : '';

    const payload = {
        name, tel, email,
        line_id:      document.getElementById('input-lineid').value.trim(),
        wedding_date: document.getElementById('input-wedding-date').value || null,
        guest_scale:  parseInt(document.getElementById('input-guest-count').value) || null,
        place:        document.getElementById('input-venue').value.trim(),
        styles,
        content:      document.getElementById('input-notes').value.trim(),
        manager_id:   parseInt(sessionStorage.getItem('empId')) || null
    };

    try {
        const res = await fetch(`${API_BASE}/books`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body:    JSON.stringify(payload)
        });
        const result = await res.json();

        if (res.ok) {
            toggleModal('modal-create-customer');
            showToast(checkResult.length > 0 ? '預約已覆蓋更新！' : '新客戶預約已建立！');
            loadStatusCounts();
            loadBooks('處理中');
        } else {
            showToast('建立失敗：' + (result.message || '請確認欄位'));
        }
    } catch (err) {
        console.error(err);
        showToast('連線失敗，請確認後端服務是否啟動');
    }
}

// ════════════════════════════════════════
// API：更新預約狀態
// ════════════════════════════════════════
async function updateBookStatus(bookId, newStatus) {
// --- 【新增的驗證邏輯：如果是轉簽約，先檢查婚宴日期】 ---
    if (newStatus === '已簽約') {
        // 從畫面上找出對應的那張卡片
        const card = document.querySelector(`div[data-book-id="${bookId}"]`);

        if (card) {
            // 讀取你在 render 裡面綁好的婚宴日期
            const weddingDate = card.dataset.weddingDate;

            // 檢查是否為空 (有時候沒資料會變成字串 'null'，所以一併擋掉)
            if (!weddingDate || weddingDate === 'null' || weddingDate.trim() === '') {
                showToast('請先點擊「修改資料」填寫婚宴日期，才能轉為簽約喔！', 'error');
                return;
            }
        }
        const customerName = card?.dataset.name || '此客戶';
        const confirmed = await showConfirm(
            `確定要將「${customerName}」的預約轉為已簽約嗎？\n\n轉簽約後將自動建立專案與任務。`
        );
        if (!confirmed) return;
    }
    if (newStatus === '取消') {
        const card = document.querySelector(`div[data-book-id="${bookId}"]`);
        const customerName = card?.dataset.name || '此客戶';
        const confirmed = await showConfirm(`確定要取消「${customerName}」的預約嗎？`,'danger');
        if (!confirmed) return;
    }

    if (newStatus === '處理中') {
        const confirmed = await showConfirm('確定要恢復這筆預約嗎？\n恢復後，此案件將由您接手處理！');
        if (!confirmed) return;
    }
    const payload = { status: newStatus };

    if (newStatus === '處理中') {
        payload.managerId = sessionStorage.getItem('empId');
    }

    try {
        const res = await fetch(`${API_BASE}/books/${bookId}/update`, {
            method:  'PATCH',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body:    JSON.stringify({ status: newStatus,managerId: parseInt(sessionStorage.getItem('empId')) })
        });

        if (res.ok) {
            showToast('狀態已更新');
            loadStatusCounts();
            loadBooks('處理中');
            loadBooks('取消');
        }
    } catch (err) {
        console.error('[API] 更新狀態失敗:', err);
    }
}

// ════════════════════════════════════════
// API：修改新人基本資料
// ════════════════════════════════════════
let currentEditBookId = null;

function openEditModal(btn) {

    const card = btn.closest('.snap-start');
    if (!card) return;

    currentEditBookId = card.dataset.bookId;

    document.getElementById('edit-name').value         = card.dataset.name        || '';
    document.getElementById('edit-tel').value          = card.dataset.tel         || '';
    document.getElementById('edit-email').value        = card.dataset.email       || '';
    document.getElementById('edit-lineid').value       = card.dataset.lineId      || '';
    document.getElementById('edit-wedding-date').value = card.dataset.weddingDate || '';
    document.getElementById('edit-guest-scale').value  = card.dataset.guestScale  || '';
    document.getElementById('edit-place').value        = card.dataset.place       || '';

    document.querySelectorAll('input[name="edit-theme"]').forEach(radio => {
        radio.checked = (radio.value === card.dataset.styles);
    });

    toggleModal('modal-edit-book');
}

async function saveBookInfo() {
    if (!currentEditBookId) return;
    const saveBtn = document.querySelector('#modal-edit-book button[onclick="saveBookInfo()"]');

    // disabled + 轉圈
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = `
            <svg style="display:inline-block; width:16px; height:16px; vertical-align:middle; margin-right:8px; animation:spin 0.8s linear infinite;" 
                 viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
            </svg>
            儲存中...
        `;
    }

    const selectedTheme = document.querySelector('input[name="edit-theme"]:checked');
    const email = document.getElementById('edit-email')?.value.trim() || '';
    const tel   = document.getElementById('edit-tel')?.value.trim() || '';

    if (!email) {
        showToast('Email 為必填', 'error');
        return;
    }

    if (!tel || !/^\d{10}$/.test(tel)) {
        showToast('手機號碼錯誤，必須為 10 位數字！', 'error');
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showToast('Email 格式不正確（例如：example@gmail.com）', 'error');
        return;
    }

    // ── 檢查重複（排除自己這筆）──
    let checkResult = [];

    if (email) {
        const res = await fetch(
            `${API_BASE}/books/check-duplicate?email=${encodeURIComponent(email)}`,
            { credentials: 'include' }
        );
        const result = await res.json();
        // 排除自己這筆（同一個 bookId 的不算重複）
        checkResult = result.filter(c => String(c.bookId) !== String(currentEditBookId));
    }

    if (checkResult.length === 0 && tel) {
        const res = await fetch(
            `${API_BASE}/books/check-duplicate?tel=${encodeURIComponent(tel)}`,
            { credentials: 'include' }
        );
        const result = await res.json();
        checkResult = result.filter(c => String(c.bookId) !== String(currentEditBookId));
    }

    if (checkResult.length > 0) {
        const confirmed = showConfirm(
            `此 Email 或手機已有其他客戶資料：${checkResult[0].name}\n` +
            `手機：${checkResult[0].tel}\n` +
            `Email：${checkResult[0].email || '-'}\n\n` +
            `確定要繼續儲存嗎？`
        );
        if (!confirmed) return;
    }

    const payload = {
        name:         document.getElementById('edit-name').value.trim(),
        tel:          document.getElementById('edit-tel').value.trim(),
        email:        document.getElementById('edit-email')?.value.trim() || null,
        line_id:      document.getElementById('edit-lineid').value.trim(),
        wedding_date: document.getElementById('edit-wedding-date').value || null,
        guest_scale:  parseInt(document.getElementById('edit-guest-scale').value) || null,
        place:        document.getElementById('edit-place').value.trim(),
        styles:       selectedTheme ? selectedTheme.value : null,
    };

    try {
        const res = await fetch(`${API_BASE}/books/${currentEditBookId}/info`, {
            method:  'PATCH',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            toggleModal('modal-edit-book');
            showToast('資料已更新！');
            loadStatusCounts();
            loadBooks('處理中');
        } else {
            const err = await res.json().catch(() => ({}));
            showToast(err.message || '儲存失敗', 'error');
        }
    } catch (err) {
        console.error('[API] 修改資料失敗:', err);
        showToast('連線失敗', 'error');
    }
    finally {
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.textContent = '儲存資料';
        }
    }

}

// ════════════════════════════════════════
// API：查看 book_details（查看需求按鈕）
// ════════════════════════════════════════

// 記住目前開啟的 bookId，儲存時使用
let currentViewBookId = null;

async function viewBookDetails(bookId) {
    currentViewBookId = bookId;
    try {
        const res  = await fetch(`${API_BASE}/books/${bookId}/details`, { credentials: 'include' });
        const data = await res.json();

        // 1. 填入備註
        const notesEl = document.getElementById('modify-notes');
        if (notesEl) notesEl.value = data.notes || '';

        // 2. 先把所有 modal checkbox 取消勾選
        document.querySelectorAll('#modal-modify .modal-service-cb').forEach(cb => {
            cb.checked = false;
            // 同步關閉子項目
            const targetId = cb.id.replace('main-', 'sub-');
            const subBox = document.getElementById(targetId);
            if (subBox) {
                subBox.classList.add('opacity-50', 'pointer-events-none');
                subBox.querySelectorAll('input').forEach(i => { i.disabled = true; i.checked = false; });
            }
        });

        // 3. 根據 API 回傳的 serviceId 自動勾選
        const checkedIds = new Set((data.services || []).map(s => String(s.serviceId)));

        // Step A：先勾選主 checkbox（main-*），並開啟對應子項目
        document.querySelectorAll('#modal-modify .modal-service-cb[id^="main-"]').forEach(cb => {
            const sid = cb.getAttribute('data-service-id');
            if (checkedIds.has(sid)) {
                cb.checked = true;
                const targetId = cb.id.replace('main-', 'sub-');
                const subBox = document.getElementById(targetId);
                if (subBox) {
                    subBox.classList.remove('opacity-50', 'pointer-events-none');
                    subBox.querySelectorAll('input').forEach(i => i.disabled = false);
                }
            }
        });

        // Step B：勾選子 checkbox + 回填日期
        document.querySelectorAll('#modal-modify .modal-service-cb:not([id^="main-"])').forEach(cb => {
            const sid = cb.getAttribute('data-service-id');
            if (checkedIds.has(sid)) {
                cb.checked = true;

                // 回填儀式日期
                const service = (data.services || []).find(s => String(s.serviceId) === sid);
                if (service?.ceremonyDate) {
                    const dateInput = document.getElementById(`input-date-ceremony-${sid}`);
                    if (dateInput) {
                        dateInput.value = service.ceremonyDate;
                        const container = document.getElementById(`date-${sid}`);
                        if (container) container.classList.remove('hidden');
                    }
                }
            }
        });

        // 顯示已選服務清單
        const serviceListEl = document.getElementById('modify-service-list');
        if (serviceListEl) {
            if (data.services && data.services.length > 0) {
                serviceListEl.innerHTML = data.services.map(s =>{
                // service_id 1 和 2 是 A方案基底，不顯示在清單
                if (s.serviceId === 1 || s.serviceId === 2) return '';
                return `<li class="text-sm text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <span class="material-icons text-primary text-base">check_circle</span>
                    ${s.serviceName}
                 <span class="ml-auto text-[11px] text-gray-400">NT$ ${(s.unitPrice || 0).toLocaleString()}</span>
                 </li>`;
            }).join('');

                // ★ 補上總價計算
                const addonsTotal = data.services
                    .filter(s => s.serviceId !== 1 && s.serviceId !== 2)
                    .reduce((sum, s) => sum + (s.unitPrice || 0), 0);
                const total = 43800 + addonsTotal;
                const totalEl = document.getElementById('modify-total-price');
                if (totalEl) totalEl.textContent = 'NT$ ' + total.toLocaleString();

            } else {
                serviceListEl.innerHTML = '<li class="text-sm text-gray-400">尚無服務細項</li>';
                const totalEl = document.getElementById('modify-total-price');
                if (totalEl) totalEl.textContent = 'NT$ 0';
            }
        }

        // 5. 建立 serviceId -> {name, price} Map 並綁定即時更新事件
        window._serviceMap = {};
        (data.services || []).forEach(s => {
            window._serviceMap[String(s.serviceId)] = { name: s.serviceName, price: s.unitPrice || 0 };
        });

        // 6. 開啟 modal
        toggleModal('modal-modify');

    } catch (err) {
        console.error('[API] 載入細項失敗:', err);
        showToast('載入失敗，請稍後再試', 'error');
    }
}



// ════════════════════════════════════════
// API：查看已簽約詳細摘要（純文字）
// ════════════════════════════════════════
async function viewBookDetail(bookId) {
    try {
        const res  = await fetch(`${API_BASE}/books/${bookId}/details`, { credentials: 'include' });
        const data = await res.json();

        // 找對應的 book 資料（從已簽約 table 的 row）
        const row = document.querySelector(`[data-book-id="${bookId}"]`);

        // 填入基本資料
        document.getElementById('detail-customer-name').textContent = data.customerName || row?.dataset.customerName || '-';
        document.getElementById('detail-book-meta').textContent     = `預約編號 #${bookId}`;
        document.getElementById('detail-wedding-date').textContent  = data.weddingDate  || '-';
        document.getElementById('detail-guest-scale').textContent   = data.guestScale   ? data.guestScale + ' 人' : '-';
        document.getElementById('detail-place').textContent         = data.place        || '-';
        document.getElementById('detail-styles').textContent        = data.styles       || '-';
        document.getElementById('detail-tel').textContent           = formatPhone(data.tel)|| '-';
        document.getElementById('detail-lineid').textContent        = data.lineId       || '-';

        // 服務項目清單
        const listEl = document.getElementById('detail-services-list');
        if (data.services && data.services.length > 0) {
            listEl.innerHTML = data.services.map(s => `
                <li class="flex justify-between items-center px-4 py-3">
                    <span class="text-sm text-gray-700 dark:text-gray-300 flex items-center gap-2">
                        <span class="material-icons text-primary text-base">check_circle</span>
                        ${s.serviceName}
                    </span>
                    <span class="text-sm font-medium text-gray-600">NT$ ${(s.unitPrice || 0).toLocaleString()}</span>
                </li>`).join('');
        } else {
            listEl.innerHTML = '<li class="px-4 py-3 text-sm text-gray-400">尚無服務細項</li>';
        }

        // 總價
        document.getElementById('detail-total').textContent = 'NT$ ' + (data.totalPrice || 0).toLocaleString();

        // 備註
        const notesWrap = document.getElementById('detail-notes-wrap');
        const notesEl   = document.getElementById('detail-notes');
        if (data.notes) {
            notesEl.textContent = data.notes;
            notesWrap.classList.remove('hidden');
        } else {
            notesWrap.classList.add('hidden');
        }

        toggleModal('modal-view-detail');

    } catch (err) {
        console.error('[API] 載入詳細失敗:', err);
        showToast('載入失敗，請稍後再試', 'error');
    }
}
// ════════════════════════════════════════
// UI：切換專屬儀式日期的顯示與隱藏
// ════════════════════════════════════════
function toggleCeremonyDate(checkbox, dateContainerId) {
    const container = document.getElementById(dateContainerId);
    if (!container) return;

    const dateInput = container.querySelector('input[type="date"]');

    if (checkbox.checked) {
        // 打勾時：顯示日期框
        container.classList.remove('hidden');
    } else {
        // 取消打勾時：隱藏日期框，並且清空裡面原本填的值
        container.classList.add('hidden');
        if (dateInput) dateInput.value = '';
    }
}


// ════════════════════════════════════════
// API：儲存需求設定（PUT /books/{id}/details）
// ════════════════════════════════════════
async function saveBookDetails() {
    if (!currentViewBookId) return;

    const saveBtn = document.querySelector('#modal-modify button[onclick="saveBookDetails()"]');
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = `
        <svg style="display:inline-block;width:14px;height:14px;vertical-align:middle;margin-right:6px;animation:spin 0.8s linear infinite;"
             viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
        </svg>
        儲存中...
    `;
    }

    const notes = document.getElementById('modify-notes')?.value.trim() || '';
    const details = [];
    document.querySelectorAll('#modal-modify .modal-service-cb:checked').forEach(cb => {
        const serviceId = parseInt(cb.getAttribute('data-service-id'));
        if (!serviceId) return;

        // 找對應的日期 input（只有特定 service 才有）
        const dateInput = document.getElementById(`input-date-ceremony-${serviceId}`);
        const ceremonyDate = dateInput?.value || null;

        console.log(`serviceId: ${serviceId}, ceremonyDate: ${ceremonyDate}`);

        details.push({
            service_id:    serviceId,
            unit_price:    parseInt(cb.getAttribute('data-price') || 0),
            ceremony_date: ceremonyDate
        });
    });

    try {
        const res = await fetch(`${API_BASE}/books/${currentViewBookId}/details`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ notes, details })
        });

        if (res.ok) {
            showToast('需求設定已儲存！');

            // ★ 儲存後重新從 API 撈資料，刷新上方已選清單
            await viewBookDetails(currentViewBookId);

            // 同步更新卡片備註
            const card = document.querySelector(`[data-book-id="${currentViewBookId}"]`);
            if (card) {
                const notesEl = card.querySelector('.line-clamp-2');
                if (notesEl) notesEl.textContent = '"' + (notes || '無備註') + '"';
                card.dataset.notes = notes;
            }
        } else {
            showToast('儲存失敗，請稍後再試', 'error');
        }
    } catch (err) {
        console.error('[API] 儲存失敗:', err);
        showToast('連線失敗', 'error');
    }finally {
        // 2. 不管成功失敗，最後都恢復按鈕
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.textContent = '儲存需求設定';  // 改成你按鈕原本的文字
        }
    }
}

/// ════════════════════════════════════════
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
        const bookId = book.bookId;  // ← 先抽出來避免 template literal 問題
        const card = document.createElement('div');
        card.className = 'snap-start shrink-0 w-[calc(50%-12px)] min-w-[400px] max-w-[480px] min-h-[500px] bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 flex flex-col overflow-hidden hover:shadow-md transition-shadow';
        card.dataset.staff   = book.managerName  || '';
        card.dataset.theme   = book.styles       || '';
        card.dataset.guests  = book.guestScale   || '0';
        card.dataset.bookId  = book.bookId        || '';
        card.dataset.name    = book.customerName  || '';
        card.dataset.tel     = book.tel           || '';
        card.dataset.lineId  = book.lineId        || '';
        card.dataset.weddingDate = book.weddingDate || '';
        card.dataset.guestScale  = book.guestScale  || '';
        card.dataset.place   = book.place         || '';
        card.dataset.styles  = book.styles        || '';
        card.dataset.email = book.email || '';

        card.innerHTML = `
            <div class="p-6 flex-1 flex flex-col">
                <div class="flex justify-between items-center mb-4">
                    <span class="px-2.5 py-1 rounded text-[14px] font-bold bg-blue-50 text-blue-600">
                        ${book.managerName || '未分配'}
                    </span>
                    <span class="text-[15px] text-gray-400 font-medium">
                         ${book.updateAt ? book.updateAt.split(' ')[0]  : (book.createAt ? book.createAt.split('T')[0] : '')}
                    </span>
                </div>
                <h4 class="text-[18px] font-bold text-gray-900 dark:text-white mb-3 truncate">
                    ${book.customerName || ''}
                </h4>
                <div class="space-y-2">
                    <div class="text-[13px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">電子郵件</span>
                        <span class="font-medium truncate">${book.email || '-'}</span>
                    </div>
                    <div class="text-[13px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">聯絡手機</span>
                        <span class="font-medium truncate">${formatPhone(book.tel) || '-'}</span>
                    </div>
                    <div class="text-[13px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">LINE ID</span>
                        <span class="font-medium truncate">${book.lineId || '-'}</span>
                    </div>
                    <div class="text-[13px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">預計婚期</span>
                        <span class="font-medium truncate">${book.weddingDate || '-'}</span>
                    </div>
                    <div class="text-[13px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">賓客規模</span>
                        <span class="font-medium truncate">${book.guestScale ? book.guestScale + ' 人' : '-'}</span>
                    </div>
                    <div class="text-[13px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">宴客場地</span>
                        <span class="font-medium truncate">${book.place || '-'}</span>
                    </div>
                    <div class="text-[13px] flex items-center">
                        <span class="text-gray-400 w-16 shrink-0 font-medium">視覺定調</span>
                        <span class="font-medium truncate">${book.styles || '-'}</span>
                    </div>
                </div>
                <div class="mt-5 p-3 bg-gray-50 rounded text-[12px] text-gray-500 italic border border-gray-100 line-clamp-2">
                    "${book.content || '無備註'}"
                </div>
            </div>
            <div class="flex border-t border-gray-100 bg-gray-50/30 min-h-[56px]">
                <button onclick="viewBookDetails(${bookId})"
                    class="flex-1 py-4 text-[13px] font-bold text-gray-500 hover:text-gray-700 hover:bg-gray-200 border-r border-gray-100 transition-colors">
                    查看需求
                </button>
                <button onclick="openEditModal(this)"
                    class="flex-1 py-2.5 text-[13px] font-bold text-indigo-500 hover:bg-indigo-50 border-r border-gray-100 transition-colors">
                    修改資料
                </button>
                <button onclick="updateBookStatus(${bookId}, '已簽約')"
                    class="flex-1 py-2.5 text-[13px] font-bold text-primary hover:bg-blue-50 border-r border-gray-100 transition-colors">
                    轉為簽約
                </button>
                <button onclick="updateBookStatus(${bookId}, '取消')"
                    class="flex-1 py-2.5 text-[13px] font-bold text-red-400 hover:bg-red-50 transition-colors">
                    取消預約
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
        row.className = 'grid grid-cols-[1.5fr_2fr_1.5fr_2fr_1.5fr_1.5fr_1.5fr] px-5 py-3.5 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors justify-items-center text-center';
        row.innerHTML = `
            <span class="text-[12px] font-medium text-gray-800 dark:text-gray-200">${book.signAt || '-'}</span>
            <span class="text-[12px] font-medium text-gray-800 dark:text-gray-200">${book.customerName || '-'}</span>
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.weddingDate || '-'}</span>
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.place || '-'}</span>
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.styles || '-'}</span>
            <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-600 w-fit">${book.managerName || '-'}</span>
            <button onclick="viewBookDetail(${book.bookId})"
                class="text-[12px] text-primary hover:underline font-medium">
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
        row.className = 'grid grid-cols-[2fr_2.5fr_2.5fr_2.5fr_1.5fr_1.5fr] px-5 py-3.5 hover:bg-gray-50 dark:hover:bg-gray-700/30 transition-colors justify-items-center text-center';
        row.innerHTML = `
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.updateAt ? book.updateAt.split(' ')[0] : '-'}</span>
            <span class="text-[12px] font-medium text-gray-800 dark:text-gray-200">${book.customerName || '-'}</span>
            <span class="text-[12px] text-gray-600 dark:text-gray-300">${book.email || '-'}</span>
            <span class="text-[12px] text-gray-500 dark:text-gray-400">${formatPhone(book.tel) || '-'}</span>
            <span class="px-2 py-0.5 rounded text-[12px] font-bold bg-blue-50 text-blue-600 w-fit">${book.managerName || '-'}</span>
            <button onclick="updateBookStatus(${book.bookId}, '處理中')"
                class="text-[12px] font-bold text-primary hover:underline">
                恢復預約
            </button>
        `;
        container.appendChild(row);
    });

    const countEl = document.getElementById('cancelled-count');
    if (countEl) countEl.textContent = `共 ${books.length} 筆取消記錄`;
}

// ════════════════════════════════════════
// 工具函式
// ════════════════════════════════════════
function showToast(message, type = 'success') {
    const colors = {success: '#22c55e', error: '#ef4444'};
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
    // 將 0912345678 轉換成 0912-345-678
    function formatPhone(tel) {
        if (!tel) return '-';
        // 確保只處理 10 碼數字，否則原樣輸出
        if (/^09\d{8}$/.test(tel)) {
            return tel.replace(/(\d{4})(\d{3})(\d{3})/, '$1-$2-$3');
        }
        return tel;
}