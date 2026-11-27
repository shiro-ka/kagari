export function appHref(path: string): string {
	return `#${path}`;
}

export function appUrl(url: Pick<URL, 'hash' | 'origin'>): URL {
	return new URL(url.hash.replace(/^#/, '') || '/', url.origin);
}
