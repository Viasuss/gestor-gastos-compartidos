/**
 * NEXUS App Shell Controller (UX 1.0)
 * - Sidebar colapsable persistente en escritorio
 * - Drawer lateral responsivo en tablet/móvil
 * - Modal accesible Panel de Control de Cuenta (Escape, Tab-trap, inert)
 * - Cierre automático de toasts
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'nexus.sidebar.collapsed';

    function initSidebar() {
        const toggleBtn = document.getElementById('btnToggleSidebar');
        if (window.innerWidth >= 1024) {
            try {
                if (localStorage.getItem(STORAGE_KEY) === 'true') {
                    document.body.classList.add('nx-sidebar-collapsed');
                }
            } catch (e) {
                // Storage deshabilitado: continuar con estado expandido
            }
        }

        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                const collapsed = document.body.classList.toggle('nx-sidebar-collapsed');
                toggleBtn.setAttribute('aria-expanded', String(!collapsed));
                try {
                    localStorage.setItem(STORAGE_KEY, String(collapsed));
                } catch (e) {
                    // Ignorar si storage no disponible
                }
            });
        }
    }

    function initMobileDrawer() {
        const sidebar = document.getElementById('nxSidebar');
        const backdrop = document.getElementById('nxDrawerBackdrop');
        const openBtns = document.querySelectorAll('[data-open-drawer]');
        const closeBtns = document.querySelectorAll('[data-close-drawer]');

        if (!sidebar || !backdrop) return;

        function openDrawer() {
            sidebar.classList.add('is-open');
            backdrop.classList.add('is-open');
            document.body.style.overflow = 'hidden';
        }

        function closeDrawer() {
            sidebar.classList.remove('is-open');
            backdrop.classList.remove('is-open');
            document.body.style.overflow = '';
        }

        openBtns.forEach((btn) => btn.addEventListener('click', openDrawer));
        closeBtns.forEach((btn) => btn.addEventListener('click', closeDrawer));
        backdrop.addEventListener('click', closeDrawer);
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && sidebar.classList.contains('is-open')) {
                closeDrawer();
            }
        });
    }

    function initAccountModal() {
        const modal = document.getElementById('panelControlBackdrop');
        const btnOpen = document.getElementById('btnAbrirPanelControl');
        const shell = document.getElementById('nxAppShell');
        if (!modal || !btnOpen) return;

        let lastTrigger = null;

        function getFocusables() {
            return Array.from(modal.querySelectorAll(
                'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
            )).filter((el) => !el.disabled && el.offsetParent !== null);
        }

        function openModal(trigger) {
            lastTrigger = trigger || btnOpen;
            modal.classList.add('is-open');
            modal.setAttribute('aria-hidden', 'false');
            if (shell) shell.setAttribute('inert', '');
            const focusables = getFocusables();
            if (focusables.length > 0) {
                focusables[0].focus();
            }
        }

        function closeModal() {
            modal.classList.remove('is-open');
            modal.setAttribute('aria-hidden', 'true');
            if (shell) shell.removeAttribute('inert');
            if (lastTrigger && typeof lastTrigger.focus === 'function') {
                lastTrigger.focus();
            }
        }

        btnOpen.addEventListener('click', () => openModal(btnOpen));

        modal.querySelectorAll('[data-close-account-modal]').forEach((el) => {
            el.addEventListener('click', closeModal);
        });

        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal();
        });

        document.addEventListener('keydown', (e) => {
            if (!modal.classList.contains('is-open')) return;
            if (e.key === 'Escape') {
                closeModal();
                return;
            }
            if (e.key === 'Tab') {
                const focusables = getFocusables();
                if (focusables.length === 0) return;
                const first = focusables[0];
                const last = focusables[focusables.length - 1];
                if (e.shiftKey && document.activeElement === first) {
                    e.preventDefault();
                    last.focus();
                } else if (!e.shiftKey && document.activeElement === last) {
                    e.preventDefault();
                    first.focus();
                }
            }
        });
    }

    function initToasts() {
        document.querySelectorAll('.nx-toast[data-autohide], .toast[data-autohide]').forEach((toast) => {
            setTimeout(() => {
                toast.style.opacity = '0';
                toast.style.transition = 'opacity 250ms ease';
                setTimeout(() => toast.remove(), 260);
            }, 5000);
        });
        document.querySelectorAll('[data-dismiss-toast]').forEach((btn) => {
            btn.addEventListener('click', () => {
                const toast = btn.closest('.toast, .nx-toast');
                if (toast) toast.remove();
            });
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        initSidebar();
        initMobileDrawer();
        initAccountModal();
        initToasts();
    });
})();
