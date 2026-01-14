// Options page script

document.addEventListener('DOMContentLoaded', async () => {
  const serverUrlInput = document.getElementById('server-url');
  const saveBtn = document.getElementById('save-btn');
  const statusEl = document.getElementById('status');

  // Load saved settings
  const { serverUrl } = await chrome.storage.sync.get(['serverUrl']);
  if (serverUrl) {
    serverUrlInput.value = serverUrl;
  }

  // Save settings
  saveBtn.addEventListener('click', async () => {
    const url = serverUrlInput.value.trim();

    // Validate URL
    if (!url) {
      showStatus('Please enter a server URL.', 'error');
      return;
    }

    try {
      new URL(url);
    } catch {
      showStatus('Please enter a valid URL.', 'error');
      return;
    }

    // Remove trailing slash
    const normalizedUrl = url.replace(/\/+$/, '');

    // Test connection
    saveBtn.disabled = true;
    saveBtn.textContent = 'Testing connection...';

    try {
      const response = await fetch(`${normalizedUrl}/api/articles?limit=1`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      });

      if (!response.ok) {
        throw new Error(`Server returned ${response.status}`);
      }

      // Save to storage
      await chrome.storage.sync.set({ serverUrl: normalizedUrl });
      showStatus('Settings saved! Connection to server verified.', 'success');
    } catch (err) {
      showStatus(`Cannot connect to server: ${err.message}`, 'error');
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = 'Save Settings';
    }
  });

  function showStatus(message, type) {
    statusEl.textContent = message;
    statusEl.className = `status ${type}`;
    statusEl.classList.remove('hidden');
  }
});
