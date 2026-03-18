const API_BASE = 'http://localhost:8080/api/employee';

document.addEventListener('DOMContentLoaded', () => {
    loadStatusCounts();
    initSidebar();
    loadBooks('處理中');
});

// ════════════════════════════════════════
// 初始化 Sidebar 員工資訊
// ════════════════════════════════════════
function initSidebar() {
    const empName = sessionStorage.getItem('empName');
    const imgPath = sessionStorage.getItem('imgPath');
    const nameEl  = document.getElementById('sidebar-emp-name');
    const imgEl   = document.getElementById('sidebar-emp-img');
    if (nameEl && empName) nameEl.textContent = empName;
    if (imgEl  && imgPath) imgEl.src = imgPath;
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
        document.getElementById('badge-pending').textContent   = counts['處理中'] ?? 0;
        document.getElementById('badge-signed').textContent    = counts['已簽約'] ?? 0;
        document.getElementById('badge-cancelled').textContent = counts['取消']   ?? 0;
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
    try {
        const managerId = sessionStorage.getItem('empId');

        // --- 1. 發送請求 ---
        const res = await fetch(
            `${API_BASE}/books?status=${encodeURIComponent(status)}&managerId=${managerId}`,
            { credentials: 'include' }
        );

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
        result.sort((a, b) => new Date(b.createAt) - new Date(a.createAt));
        //console.log(`[載入列表 - ${status}] 後端回傳的原始資料:`, result);

        // --- 4. 檢查是否為陣列 (防護二) ---
        if (!Array.isArray(result)) {
            //console.warn(`[資料格式警告] 後端回傳的不是陣列！目前格式為:`, typeof result);

            // 嘗試從常見的分頁/包裝格式中取出陣列
            if (result && Array.isArray(result.data)) {
                //console.log('✔ 已從 result.data 中取出陣列');
                result = result.data;
            } else if (result && Array.isArray(result.content)) {
                //console.log('✔ 已從 result.content 中取出陣列');
                result = result.content;
            } else {
                //console.error('❌ 無法在回傳物件中找到陣列，強制設為空陣列 [] 避免報錯');
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
        alert('請填寫姓名與手機號碼');
        return;
    }

    let checkResult = [];
    if (email) {
        const checkRes = await fetch(
            `${API_BASE}/books/check-duplicate?email=${encodeURIComponent(email)}`,
            { credentials: 'include' }
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
        content:      document.getElementById('input-notes').value.trim()
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
        const res = await fetch(`${API_BASE}/books/${bookId}/update`, {
            method:  'PATCH',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body:    JSON.stringify({ status: newStatus })
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

let currentEditingBookId = null;

function findCheckbox(service) {
    if (service.selector) return document.querySelector(service.selector);
    const labels = Array.from(document.querySelectorAll('#modal-modify label'));
    const label = labels.find(l => l.textContent.includes(service.name));
    if (label) return label.querySelector('input') || document.getElementById(label.getAttribute('for'));
    return null;
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

        // Step B：再勾選子 checkbox（沒有 id 或不是 main- 開頭的）
        document.querySelectorAll('#modal-modify .modal-service-cb:not([id^="main-"])').forEach(cb => {
            const sid = cb.getAttribute('data-service-id');
            if (checkedIds.has(sid)) {
                cb.checked = true;
            }
        });

        // 4. 顯示已選服務清單
        const serviceListEl = document.getElementById('modify-service-list');
        if (serviceListEl) {
            if (data.services && data.services.length > 0) {
                serviceListEl.innerHTML = data.services.map(s =>
                    `<li class="text-sm text-gray-700 dark:text-gray-300 flex items-center gap-2">
                        <span class="material-icons text-primary text-base">check_circle</span>
                        ${s.serviceName}
                        <span class="ml-auto text-[11px] text-gray-400">NT$ ${(s.unitPrice || 0).toLocaleString()}</span>
                    </li>`
                ).join('');
            } else {
                serviceListEl.innerHTML = '<li class="text-sm text-gray-400">尚無服務細項</li>';
            }
        }

        // 5. 開啟 modal
        toggleModal('modal-modify');

    } catch (err) {
        console.error('[API] 載入細項失敗:', err);
        showToast('載入失敗，請稍後再試', 'error');
    }
}

// ════════════════════════════════════════
// API：儲存需求設定（PUT /books/{id}/details）
// ════════════════════════════════════════
async function saveBookDetails() {
    if (!currentViewBookId) return;

    // 收集備註
    const notes = document.getElementById('modify-notes')?.value.trim() || '';

    // 收集所有勾選的 checkbox（有 data-service-id 的）
    const details = [];
    document.querySelectorAll('#modal-modify .modal-service-cb:checked').forEach(cb => {
        details.push({
            service_id: parseInt(cb.getAttribute('data-service-id')),
            unit_price: parseInt(cb.getAttribute('data-price') || 0),
            ceremony_date: null
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
            toggleModal('modal-modify');
            showToast('需求設定已儲存！');
        } else {
            showToast('儲存失敗，請稍後再試', 'error');
        }
    } catch (err) {
        console.error('[API] 儲存失敗:', err);
        showToast('連線失敗', 'error');
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
        card.className = 'snap-start shrink-0 w-[calc(33.333%-11px)] min-w-[420px] bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 flex flex-col overflow-hidden hover:shadow-md transition-shadow';
        card.dataset.staff  = book.managerName || '';
        card.dataset.theme  = book.styles      || '';
        card.dataset.guests = book.guestScale  || '0';

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
                <button onclick="viewBookDetails(${bookId})"
                    class="flex-1 py-2.5 text-[12px] font-medium text-gray-500 hover:text-gray-700 border-r border-gray-100 transition-colors">
                    查看需求
                </button>
                <button onclick="updateBookStatus(${bookId}, '已簽約')"
                    class="flex-1 py-2.5 text-[12px] font-bold text-primary hover:bg-blue-50 border-r border-gray-100 transition-colors">
                    接案處理
                </button>
                <button onclick="updateBookStatus(${bookId}, '取消')"
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
            <button onclick="viewBookDetails(${book.bookId})"
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

    const countEl = document.getElementById('cancelled-count');
    if (countEl) countEl.textContent = `共 ${books.length} 筆取消記錄`;
}

// ════════════════════════════════════════
// 工具函式
// ════════════════════════════════════════
function showToast(message, type = 'success') {
    const colors = { success: '#22c55e', error: '#ef4444' };
    const toast  = document.createElement('div');
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