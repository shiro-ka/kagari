package dev.dimension.flare.data.database

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

public actual fun createDatabaseDriver(): SQLiteDriver =
    SerialWebSQLiteDriver(
        delegate = WebWorkerSQLiteDriver(createSQLiteWorker()),
    )

@OptIn(ExperimentalWasmJsInterop::class)
private fun createSQLiteWorker(): Worker =
    js(
        "globalThis.location?.protocol === 'moz-extension:' ? (() => { " +
            "const worker = new SharedWorker(new URL('@androidx/sqlite-web-worker/shared-worker.js', import.meta.url), { type: 'module', name: 'kagari-sqlite' }); " +
            "const port = worker.port; " +
            "worker.onerror = (event) => port.onerror?.(event); " +
            "const lockName = 'kagari-sqlite-' + crypto.randomUUID(); " +
            "navigator.locks.request(lockName, () => { port.postMessage({ kagariClient: lockName }); return new Promise(() => {}); }); " +
            "return port; " +
            "})() : new Worker(new URL('@androidx/sqlite-web-worker/worker.js', import.meta.url), { type: 'module' })",
    )

internal actual fun RoomDatabase.refreshOnWritesFromOtherPages() {
    listenSQLiteWrites { invalidationTracker.refreshAsync() }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun listenSQLiteWrites(onWrite: () -> Unit): Unit =
    js("void (new BroadcastChannel('kagari-sqlite-writes').onmessage = () => onWrite())")
