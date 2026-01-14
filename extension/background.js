// Background service worker for Pocket Clone extension

// Handle extension installation
chrome.runtime.onInstalled.addListener((details) => {
  if (details.reason === 'install') {
    // Open options page on first install
    chrome.runtime.openOptionsPage();
  }
});

// Listen for messages from popup
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'saveArticle') {
    saveArticle(message.url)
      .then(result => sendResponse({ success: true, article: result }))
      .catch(error => sendResponse({ success: false, error: error.message }));
    return true; // Keep channel open for async response
  }
});

async function saveArticle(url) {
  const { serverUrl } = await chrome.storage.sync.get(['serverUrl']);

  if (!serverUrl) {
    throw new Error('Server URL not configured. Please set it in extension options.');
  }

  const response = await fetch(`${serverUrl}/api/articles`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ url }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `HTTP ${response.status}`);
  }

  return response.json();
}
