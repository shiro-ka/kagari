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
