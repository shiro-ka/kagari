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

browser.webRequest.onBeforeSendHeaders.addListener(
	(details) => {
		if (!details.originUrl?.startsWith(extensionOrigin)) return {};
		const carried = details.requestHeaders.filter((header) =>
			header.name.toLowerCase().startsWith(carriedHeaderPrefix)
		);
		if (carried.length === 0) return {};
		const restored = new Map(
			carried.map((header) => [header.name.toLowerCase().slice(carriedHeaderPrefix.length), header.value])
		);
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
