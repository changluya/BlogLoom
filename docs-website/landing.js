// BlogLoom landing page interactions (tab switching + copy buttons).
// Delegated handlers also work after Mintlify navigates without a full reload.
(() => {
  if (window.__blogloomLanding) return;
  window.__blogloomLanding = true;

  document.addEventListener('click', async (event) => {
    if (!(event.target instanceof Element)) return;

    // Tab switching inside code windows.
    const tab = event.target.closest('.blogloom-landing .hs-tab[data-panel]');
    if (tab) {
      const windowElement = tab.closest('.hs-window');
      if (!windowElement) return;
      windowElement.querySelectorAll('.hs-tab').forEach((item) => {
        const selected = item === tab;
        item.classList.toggle('active', selected);
        item.setAttribute('aria-pressed', String(selected));
      });
      windowElement.querySelectorAll('.hs-code-panel').forEach((panel) => {
        panel.style.display = panel.id === tab.dataset.panel ? '' : 'none';
      });
    }

    // Copy buttons.
    const button = event.target.closest('.blogloom-landing .hs-copy-btn');
    if (!button) return;
    const original = button.textContent;
    try {
      await navigator.clipboard.writeText(
        button.dataset.copyBase64 ? atob(button.dataset.copyBase64) : button.dataset.copy || ''
      );
      button.textContent = 'Copied';
    } catch {
      button.textContent = 'Select and copy manually';
    }
    window.setTimeout(() => {
      button.textContent = original;
    }, 1800);
  });
})();
