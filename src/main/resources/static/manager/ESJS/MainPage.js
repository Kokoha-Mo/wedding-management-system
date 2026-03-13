document.addEventListener("DOMContentLoaded", () => {
    const btn = document.getElementById("sidebar-hamburger");
    const sidebar = document.getElementById("sidebarN");
    const content = document.getElementById("content");
    const navbar = document.querySelector(".navbar");


    function toggleSidebar(isCollapse) {
        if (isCollapse) {
            sidebar.classList.add("toggled");
            content.classList.add("expand");
            navbar.classList.add("expand");
        } else {
            sidebar.classList.remove("toggled");
            content.classList.remove("expand");
            navbar.classList.remove("expand");
        }
    }

    //導覽自動展開閉合
    function checkWidth() {
        if (window.innerWidth < 768) {
            toggleSidebar(true);
        } else {
            toggleSidebar(false);
        }
    }

    //手動切換
    if (btn && sidebar && content) {

        btn.addEventListener("click", () => {
            sidebar.classList.toggle("toggled");
            content.classList.toggle("expand");
            navbar.classList.toggle("expand");
        });

        window.addEventListener("resize", checkWidth);

        checkWidth();
    }




    // 權限設定
    const permissions = {
        admin: ["add-announcement"],
        staff: ["view-announcement"]
    };

    // 目前登入者admin/staff
    const userRole = sessionStorage.getItem("userRole") || "staff";

    const userPermissions = permissions[userRole] || [];

    function applyPermissions() {
        document.querySelectorAll("[data-permission]").forEach(el => {
            const required = el.dataset.permission;
            if (!userPermissions.includes(required)) {
                el.style.display = "none";
            }
        });
    }
    applyPermissions();


    // 打卡邏輯
    const clockInBtn = document.getElementById('clockInBtn');
    const clockOutBtn = document.getElementById('clockOutBtn');
    const clockToastElement = document.getElementById('clockToast');
    const toastHeader = document.getElementById('toastHeader');
    const toastTitle = document.getElementById('toastTitle');
    const toastMessage = document.getElementById('toastMessage');

    const clockConfirmModalEl = document.getElementById('clockConfirmModal');
    const clockConfirmTime = document.getElementById('clockConfirmTime');
    const clockConfirmText = document.getElementById('clockConfirmText');
    const confirmSubmitBtn = document.getElementById('confirmSubmitBtn');

    if (clockConfirmModalEl && clockToastElement) {
        const clockConfirmModal = new bootstrap.Modal(clockConfirmModalEl);
        const toast = new bootstrap.Toast(clockToastElement, { delay: 3000 });

        let currentAction = '';

        function openConfirmModal(type) {
            currentAction = type;

            const now = new Date();
            const timeString = now.getHours().toString().padStart(2, '0') + ':' +
                now.getMinutes().toString().padStart(2, '0');

            if (clockConfirmTime) clockConfirmTime.innerText = timeString;

            if (type === 'in') {
                if (clockConfirmText) clockConfirmText.innerText = '確定要進行上班打卡嗎？';
                if (confirmSubmitBtn) confirmSubmitBtn.className = 'btn btn-success btn-sm px-4 shadow-sm';
            } else {
                if (clockConfirmText) clockConfirmText.innerText = '確定要進行下班打卡嗎？';
                if (confirmSubmitBtn) confirmSubmitBtn.className = 'btn btn-secondary btn-sm px-4 shadow-sm';
            }

            clockConfirmModal.show();
        }

        function showClockToast(type) {
            const timeString = clockConfirmTime ? clockConfirmTime.innerText : '';

            if (type === 'in') {
                if (toastHeader) toastHeader.className = 'toast-header text-white bg-success';
                if (toastTitle) toastTitle.innerText = '上班打卡成功';
                if (toastMessage) toastMessage.innerHTML = `您已於 <span class="text-success">${timeString}</span> 完成上班打卡！`;
            } else {
                if (toastHeader) toastHeader.className = 'toast-header text-white bg-secondary';
                if (toastTitle) toastTitle.innerText = '下班打卡成功';
                if (toastMessage) toastMessage.innerHTML = `您已於 <span class="text-danger">${timeString}</span> 完成下班打卡！`;
            }
            toast.show();
        }

        if (clockInBtn) clockInBtn.addEventListener('click', () => openConfirmModal('in'));
        if (clockOutBtn) clockOutBtn.addEventListener('click', () => openConfirmModal('out'));

        if (confirmSubmitBtn) {
            confirmSubmitBtn.addEventListener('click', () => {
                clockConfirmModal.hide();
                setTimeout(() => {
                    showClockToast(currentAction);
                }, 300);
            });
        }
    }
});






