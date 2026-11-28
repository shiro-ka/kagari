import { defineCustomClientStrategy, locales, type Locale } from '$lib/paraglide/runtime';

function matchLocale(tag: string): Locale | undefined {
	const lower = tag.toLowerCase();
	const base = lower.split('-')[0];
	return (
		locales.find((locale) => locale.toLowerCase() === lower) ??
		locales.find((locale) => locale.toLowerCase() === base) ??
		locales.find((locale) => locale.toLowerCase().split('-')[0] === base)
	);
}

defineCustomClientStrategy('custom-browserLanguage', {
	getLocale: () => {
		for (const tag of globalThis.navigator?.languages ?? []) {
			const locale = matchLocale(tag);
			if (locale) return locale;
		}
		return undefined;
	},
	setLocale: () => {}
});
