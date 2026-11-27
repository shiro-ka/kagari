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
