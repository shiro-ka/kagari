import assert from 'node:assert/strict';
import test from 'node:test';
import { createRouter } from '../../shared/sqlite-web-worker/router.js';

function setup() {
	const sent = [];
	const router = createRouter((message) => sent.push(message));
	const client = () => {
		const received = [];
		return { handle: router.addClient((message) => received.push(message)), received };
	};
	const answer = (extra = {}) => {
		const message = sent.at(-1);
		router.innerMessage({ id: message.id, data: null, ...extra });
	};
	return { router, sent, client, answer };
}

test('the same request id from two pages comes back to each page', () => {
	const { router, sent, client, answer } = setup();
	const a = client();
	const b = client();
	router.receive(a.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	router.receive(b.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	assert.equal(sent.length, 1);
	answer({ data: { databaseId: 0 } });
	assert.equal(sent.length, 2);
	assert.notEqual(sent[0].id, sent[1].id);
	answer({ data: { databaseId: 1 } });
	assert.deepEqual(a.received, [{ id: 0, data: { databaseId: 0 }, error: undefined }]);
	assert.deepEqual(b.received, [{ id: 0, data: { databaseId: 1 }, error: undefined }]);
});

test('another page waits while a page is inside a transaction', () => {
	const { router, sent, client, answer } = setup();
	const a = client();
	const b = client();
	router.receive(a.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	answer({ data: { databaseId: 0 } });
	router.receive(b.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	answer({ data: { databaseId: 1 } });

	router.receive(a.handle, { id: 1, data: { cmd: 'step', statementId: 0, bindings: [] } });
	answer({ data: { rows: [] }, databaseId: 0, autocommit: false });

	router.receive(b.handle, { id: 1, data: { cmd: 'step', statementId: 5, bindings: [] } });
	router.receive(a.handle, { id: 2, data: { cmd: 'step', statementId: 1, bindings: [] } });
	assert.equal(sent.at(-1).data.statementId, 1);
	answer({ data: { rows: [] }, databaseId: 0, autocommit: true });

	assert.equal(sent.at(-1).data.statementId, 5);
});

test('an error that ends the transaction lets the other page continue', () => {
	const { router, sent, client, answer } = setup();
	const a = client();
	const b = client();
	router.receive(a.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	answer({ data: { databaseId: 0 } });
	router.receive(a.handle, { id: 1, data: { cmd: 'step', statementId: 0, bindings: [] } });
	answer({ data: { rows: [] }, databaseId: 0, autocommit: false });
	router.receive(b.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	router.receive(a.handle, { id: 2, data: { cmd: 'step', statementId: 1, bindings: [] } });
	answer({ error: 'constraint failed', databaseId: 0, autocommit: true });
	assert.equal(a.received.at(-1).error, 'constraint failed');
	assert.equal(sent.at(-1).data.cmd, 'open');
});

test('a page closed inside a transaction is rolled back and the other page continues', () => {
	const { router, sent, client, answer } = setup();
	const a = client();
	const b = client();
	router.receive(a.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	answer({ data: { databaseId: 0 } });
	router.receive(a.handle, { id: 1, data: { cmd: 'step', statementId: 0, bindings: [] } });
	answer({ data: { rows: [] }, databaseId: 0, autocommit: false });
	router.receive(b.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	assert.equal(sent.length, 2);

	router.removeClient(a.handle);
	assert.deepEqual(sent.at(-1).data, { cmd: 'close', databaseId: 0 });
	answer();
	assert.equal(sent.at(-1).data.cmd, 'open');
	answer({ data: { databaseId: 1 } });
	assert.deepEqual(b.received, [{ id: 0, data: { databaseId: 1 }, error: undefined }]);
});

test('a reply for a page that closed meanwhile is dropped and its database is closed', () => {
	const { router, sent, client, answer } = setup();
	const a = client();
	router.receive(a.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	router.removeClient(a.handle);
	answer({ data: { databaseId: 3 } });
	assert.deepEqual(a.received, []);
	assert.deepEqual(sent.at(-1).data, { cmd: 'close', databaseId: 3 });
});

test('when the worker stops, every waiting request gets an error', () => {
	const { router, client } = setup();
	const a = client();
	const b = client();
	router.receive(a.handle, { id: 0, data: { cmd: 'open', fileName: 'app.db' } });
	router.receive(b.handle, { id: 7, data: { cmd: 'open', fileName: 'app.db' } });
	router.innerFailed('boom');
	assert.equal(a.received[0].error, 'boom');
	assert.equal(b.received[0].id, 7);
	assert.equal(b.received[0].error, 'boom');
	router.receive(a.handle, { id: 1, data: { cmd: 'prepare', databaseId: 0, sql: 'SELECT 1' } });
	assert.equal(a.received.at(-1).error, 'boom');
});
