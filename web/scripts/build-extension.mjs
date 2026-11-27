import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const webDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const buildDir = path.join(webDir, 'build');
const extensionDir = path.join(webDir, 'extension');

execFileSync('npx', ['vite', 'build'], {
	cwd: webDir,
	stdio: 'inherit',
	env: { ...process.env, FLARE_WEB_SHARED_LIBRARY: 'productionLibrary' }
});

for (const entry of await fs.readdir(extensionDir, { withFileTypes: true })) {
	if (!entry.isFile()) continue;
	await fs.copyFile(path.join(extensionDir, entry.name), path.join(buildDir, entry.name));
}

const htmlFiles = (await fs.readdir(buildDir, { recursive: true })).filter((name) => name.endsWith('.html'));
let count = 0;
for (const name of htmlFiles) {
	const file = path.join(buildDir, name);
	const scripts = [];
	const html = (await fs.readFile(file, 'utf8')).replace(/<script>([\s\S]*?)<\/script>/g, (_match, body) => {
		const scriptName = `inline-${count++}.js`;
		scripts.push([scriptName, body]);
		return `<script src="./${scriptName}"></script>`;
	});
	for (const [scriptName, body] of scripts) {
		await fs.writeFile(path.join(path.dirname(file), scriptName), body);
	}
	await fs.writeFile(file, html);
}
