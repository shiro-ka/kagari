browser.action.onClicked.addListener(async () => {
	const url = browser.runtime.getURL('index.html');
	const [tab] = await browser.tabs.query({ url });
	if (tab) {
		await browser.tabs.update(tab.id, { active: true });
		await browser.windows.update(tab.windowId, { focused: true });
		return;
	}
	await browser.tabs.create({ url });
});

browser.webRequest.onBeforeRequest.addListener(
	(details) => {
		const target = `index.html#/login/callback/oauth${new URL(details.url).search}`;
		void browser.tabs.update(details.tabId, { url: browser.runtime.getURL(target) });
		return { cancel: true };
	},
	{ urls: ['https://kagari.pages.dev/callback*'], types: ['main_frame'] },
	['blocking']
);

const carriedHeaderPrefix = 'x-kagari-h-';
const extensionOrigin = browser.runtime.getURL('');
const imageReferers = new Map([['i.pximg.net', 'https://www.pixiv.net/']]);

browser.webRequest.onBeforeSendHeaders.addListener(
	(details) => {
		if (!details.originUrl?.startsWith(extensionOrigin)) return {};
		const carried = details.requestHeaders.filter((header) =>
			header.name.toLowerCase().startsWith(carriedHeaderPrefix)
		);
		const restored = new Map(
			carried.map((header) => [header.name.toLowerCase().slice(carriedHeaderPrefix.length), header.value])
		);
		const referer = imageReferers.get(new URL(details.url).hostname);
		if (referer && !restored.has('referer')) restored.set('referer', referer);
		if (restored.size === 0) return {};
		const requestHeaders = details.requestHeaders.filter((header) => {
			const name = header.name.toLowerCase();
			return !name.startsWith(carriedHeaderPrefix) && !restored.has(name);
		});
		for (const [name, value] of restored) requestHeaders.push({ name, value });
		return { requestHeaders };
	},
	{ urls: ['<all_urls>'] },
	['blocking', 'requestHeaders']
);

browser.webRequest.onHeadersReceived.addListener(
	(details) => {
		const location = details.responseHeaders?.find((header) => header.name.toLowerCase() === 'location')?.value;
		if (!location?.toLowerCase().startsWith('pixiv://account/login')) return {};
		const target = `index.html#/login/callback/oauth${new URL(location).search}`;
		void browser.tabs.update(details.tabId, { url: browser.runtime.getURL(target) });
		return { cancel: true };
	},
	{ urls: ['https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback*'], types: ['main_frame'] },
	['blocking', 'responseHeaders']
);
