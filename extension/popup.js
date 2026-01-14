// Popup script - saves current tab URL when popup opens

document.addEventListener('DOMContentLoaded', async () => {
  const savingEl = document.getElementById('saving');
  const successEl = document.getElementById('success');
  const errorEl = document.getElementById('error');
  const titleEl = document.getElementById('article-title');
  const errorTextEl = document.getElementById('error-text');
  const openAppBtn = document.getElementById('open-app');
  const settingsBtn = document.getElementById('open-settings');

  // Get server URL for the "Open" button
  const { serverUrl } = await chrome.storage.sync.get(['serverUrl']);

  if (!serverUrl) {
    showError('Server URL not configured. Click Settings to configure.');
    return;
  }

  // Set up the Open button
  openAppBtn.href = serverUrl;
  openAppBtn.addEventListener('click', (e) => {
    e.preventDefault();
    chrome.tabs.create({ url: serverUrl });
  });

  // Settings button
  settingsBtn.addEventListener('click', () => {
    chrome.runtime.openOptionsPage();
  });

  // Get current tab and save
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });

    if (!tab?.url) {
      showError('Cannot save this page.');
      return;
    }

    // Skip chrome:// and extension pages
    if (tab.url.startsWith('chrome://') || tab.url.startsWith('chrome-extension://')) {
      showError('Cannot save browser internal pages.');
      return;
    }

    // Send message to background script to save
    chrome.runtime.sendMessage(
      { action: 'saveArticle', url: tab.url },
      (response) => {
        if (chrome.runtime.lastError) {
          showError(chrome.runtime.lastError.message);
          return;
        }

        if (response.success) {
          showSuccess(response.article);
        } else {
          showError(response.error);
        }
      }
    );
  } catch (err) {
    showError(err.message);
  }

  function showSuccess(article) {
    savingEl.classList.add('hidden');
    successEl.classList.remove('hidden');
    openAppBtn.classList.remove('hidden');

    if (article?.title) {
      titleEl.textContent = article.title;
      titleEl.classList.remove('hidden');
    }
  }

  function showError(message) {
    savingEl.classList.add('hidden');
    errorEl.classList.remove('hidden');
    errorTextEl.textContent = message;
    errorTextEl.classList.remove('hidden');
  }
});
