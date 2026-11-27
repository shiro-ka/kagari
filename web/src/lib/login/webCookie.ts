type BrowserCookie = {
    name: string;
    value: string;
};

type ExtensionApi = {
    cookies: {
        getAll(details: Record<string, unknown>): Promise<BrowserCookie[]>;
    };
    tabs: {
        create(properties: { url: string }): Promise<{ id?: number }>;
        remove(tabId: number): Promise<void>;
        onRemoved: {
            addListener(listener: (tabId: number) => void): void;
            removeListener(listener: (tabId: number) => void): void;
        };
    };
};

const POLL_MS = 1000;

function extensionApi(): ExtensionApi | null {
    const api = (globalThis as { browser?: Partial<ExtensionApi> }).browser;
    return api?.cookies && api.tabs ? (api as ExtensionApi) : null;
}

export function webCookieLoginAvailable(): boolean {
    return extensionApi() !== null;
}

async function readCookies(api: ExtensionApi, url: string): Promise<string> {
    const topLevelSite = new URL(url).origin;
    const lists = await Promise.all([
        api.cookies.getAll({ url }),
        api.cookies.getAll({ url, partitionKey: { topLevelSite } }),
    ]);
    const cookies = new Map<string, string>();
    for (const cookie of lists.flat()) cookies.set(cookie.name, cookie.value);
    return Array.from(cookies, ([name, value]) => `${name}=${value}`).join("; ");
}

export async function captureWebCookies(
    url: string,
    ready: (cookie: string) => boolean,
): Promise<string | null> {
    const api = extensionApi();
    if (!api) return null;
    const tab = await api.tabs.create({ url });
    const tabId = tab.id;
    if (tabId === undefined) return null;
    let closed = false;
    const onRemoved = (removedId: number) => {
        if (removedId === tabId) closed = true;
    };
    api.tabs.onRemoved.addListener(onRemoved);
    try {
        while (!closed) {
            await new Promise((resolve) => setTimeout(resolve, POLL_MS));
            const cookie = await readCookies(api, url);
            if (ready(cookie)) {
                if (!closed) await api.tabs.remove(tabId).catch(() => {});
                return cookie;
            }
        }
        return null;
    } finally {
        api.tabs.onRemoved.removeListener(onRemoved);
    }
}
