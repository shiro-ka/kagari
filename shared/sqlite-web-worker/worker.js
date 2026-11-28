/**
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sqlite3InitModule from "@sqlite.org/sqlite-wasm";

let sqlite3 = null;

const databases = new Map();
const statements = new Map();
const statementDatabases = new Map();
const databaseWrites = new Map();

let nextDatabaseId = 0;
let nextStatementId = 0;

const POOL_ATTEMPTS = 40;
const POOL_RETRY_MS = 250;

async function installPool(fileName) {
  let lastError = null;
  for (let attempt = 0; attempt < POOL_ATTEMPTS; attempt++) {
    try {
      return await sqlite3.installOpfsSAHPoolVfs({
        name: `kagari-${fileName}`,
        forceReinitIfPreviouslyFailed: true,
      });
    } catch (error) {
      lastError = error;
      await new Promise((resolve) => setTimeout(resolve, POOL_RETRY_MS));
    }
  }
  throw lastError;
}

async function createDatabase(fileName) {
  if (fileName) {
    try {
      const pool = await installPool(fileName);
      return new pool.OpfsSAHPoolDb(`/${fileName}`);
    } catch (error) {
      console.error("OPFS SAH pool unavailable", fileName, error);
    }
  }
  return new sqlite3.oo1.DB(fileName || ":memory:", "c");
}

async function openRequest(id, requestData) {
  try {
    const newDatabaseId = nextDatabaseId++;
    const newDatabase = await createDatabase(requestData.fileName);
    databases.set(newDatabaseId, newDatabase);
    watchWrites(newDatabaseId, newDatabase);
    postMessage({ id, data: { databaseId: newDatabaseId } });
  } catch (error) {
    postMessage({ id, error: error.message });
  }
}

function prepareRequest(id, requestData) {
  try {
    const newStatementId = nextStatementId++;
    const resultData = {
      statementId: newStatementId,
      parameterCount: 0,
      columnNames: [],
    };
    const database = databases.get(requestData.databaseId);
    if (!database) {
      postMessage({ id, error: `Invalid database ID: ${requestData.databaseId}` });
      return;
    }
    const statement = database.prepare(requestData.sql);
    statements.set(newStatementId, statement);
    statementDatabases.set(newStatementId, requestData.databaseId);
    resultData.parameterCount = sqlite3.capi.sqlite3_bind_parameter_count(statement);
    for (let i = 0; i < statement.columnCount; i++) {
      resultData.columnNames.push(sqlite3.capi.sqlite3_column_name(statement, i));
    }
    postMessage({ id, data: resultData });
  } catch (error) {
    postMessage({ id, error: error.message });
  }
}

function watchWrites(databaseId, database) {
  const writes = new Set();
  databaseWrites.set(databaseId, writes);
  sqlite3.capi.sqlite3_update_hook(
    database.pointer,
    (_context, _operation, schema, table) => {
      if (schema === "main") writes.add(table);
    },
    0,
  );
  sqlite3.capi.sqlite3_rollback_hook(database.pointer, () => writes.clear(), 0);
}

function takeWrites(databaseId) {
  const writes = databaseWrites.get(databaseId);
  if (!writes || writes.size === 0 || !autocommitOf(databaseId)) return undefined;
  const tables = Array.from(writes);
  writes.clear();
  return tables;
}

function touchRequest(id, requestData) {
  const database = databases.get(requestData.databaseId);
  try {
    const tracked = database?.selectValue(
      "SELECT 1 FROM temp.sqlite_master WHERE type = 'table' AND name = 'room_table_modification_log'",
    );
    if (tracked) {
      for (const table of requestData.tables) {
        database.exec({
          sql: `UPDATE temp.room_table_modification_log SET invalidated = 1 WHERE table_id IN (
            SELECT CAST(substr(sql, instr(sql, 'table_id = ') + 11) AS INTEGER) FROM temp.sqlite_master
            WHERE type = 'trigger' AND tbl_name = ? COLLATE NOCASE AND instr(sql, 'table_id = ') > 0)`,
          bind: [table],
        });
      }
    }
  } catch (error) {
    console.error("touch failed", requestData.tables, error);
  }
  postMessage({ id, data: null });
}

function autocommitOf(databaseId) {
  const database = databases.get(databaseId);
  return database ? sqlite3.capi.sqlite3_get_autocommit(database.pointer) !== 0 : true;
}

function stepRequest(id, requestData) {
  const statement = statements.get(requestData.statementId);
  if (!statement) {
    postMessage({ id, error: `Invalid statement ID: ${requestData.statementId}` });
    return;
  }
  const databaseId = statementDatabases.get(requestData.statementId);
  try {
    const resultData = {
      rows: [],
      columnTypes: [],
    };
    statement.reset();
    statement.clearBindings();
    for (let i = 0; i < requestData.bindings.length; i++) {
      statement.bind(i + 1, requestData.bindings[i]);
    }
    while (statement.step()) {
      if (!resultData.columnTypes.length) {
        for (let i = 0; i < statement.columnCount; i++) {
          resultData.columnTypes.push(sqlite3.capi.sqlite3_column_type(statement, i));
        }
      }
      resultData.rows.push(statement.get([]));
    }
    postMessage({
      id,
      data: resultData,
      databaseId,
      autocommit: autocommitOf(databaseId),
      writes: takeWrites(databaseId),
    });
  } catch (error) {
    postMessage({ id, error: error.message, databaseId, autocommit: autocommitOf(databaseId) });
  }
}

function closeRequest(id, requestData) {
  if (requestData.statementId !== undefined && requestData.statementId != null) {
    const statement = statements.get(requestData.statementId);
    if (!statement) {
      postMessage({ id, error: `Invalid statement ID: ${requestData.statementId}` });
      return;
    }
    try {
      statement.finalize();
      statements.delete(requestData.statementId);
      statementDatabases.delete(requestData.statementId);
    } catch (error) {
      postMessage({ id, error: error.message });
      return;
    }
  }

  if (requestData.databaseId !== undefined && requestData.databaseId != null) {
    const database = databases.get(requestData.databaseId);
    if (!database) {
      postMessage({ id, error: `Invalid database ID: ${requestData.databaseId}` });
      return;
    }
    try {
      for (const [statementId, databaseId] of Array.from(statementDatabases.entries())) {
        if (databaseId === requestData.databaseId) {
          const statement = statements.get(statementId);
          if (statement) {
            statement.finalize();
          }
          statements.delete(statementId);
          statementDatabases.delete(statementId);
        }
      }
      database.close();
      databases.delete(requestData.databaseId);
      databaseWrites.delete(requestData.databaseId);
    } catch (error) {
      postMessage({ id, error: error.message });
      return;
    }
  }
  postMessage({ id, data: null });
}

const commandMap = {
  open: openRequest,
  prepare: prepareRequest,
  step: stepRequest,
  close: closeRequest,
  touch: touchRequest,
};

function handleMessage(e) {
  const requestMsg = e.data;
  if (!Object.hasOwn(requestMsg, "data") && requestMsg.data == null) {
    postMessage({ id: requestMsg.id, error: "Invalid request, missing 'data'." });
    return;
  }
  if (!Object.hasOwn(requestMsg.data, "cmd") && requestMsg.data.cmd == null) {
    postMessage({ id: requestMsg.id, error: "Invalid request, missing 'cmd'." });
    return;
  }
  const command = requestMsg.data.cmd;
  const requestHandler = commandMap[command];
  if (requestHandler) {
    return requestHandler(requestMsg.id, requestMsg.data);
  } else {
    postMessage({ id: requestMsg.id, error: `Invalid request, unknown command: '${command}'.` });
  }
}

const ready = sqlite3InitModule().then((instance) => {
  sqlite3 = instance;
});
let queue = ready;
onmessage = (e) => {
  queue = queue.then(() => handleMessage(e)).catch((error) => console.error(error));
};
