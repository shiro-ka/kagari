export function createRouter(postToInner) {
  const queue = [];
  const openTransactions = new Map();
  const databaseClients = new Map();
  let inFlight = null;
  let nextInnerId = 0;
  let brokenReason = null;

  function transactionOwner() {
    for (const client of openTransactions.values()) return client;
    return null;
  }

  function nextEntry() {
    const owner = transactionOwner();
    const index = queue.findIndex((entry) => entry.client === null || owner === null || entry.client === owner);
    return index === -1 ? null : queue.splice(index, 1)[0];
  }

  function reply(client, message) {
    if (client && !client.gone) client.post(message);
  }

  function pump() {
    while (!inFlight && !brokenReason) {
      const entry = nextEntry();
      if (!entry) return;
      if (entry.client?.gone) continue;
      entry.innerId = nextInnerId++;
      inFlight = entry;
      postToInner({ id: entry.innerId, data: entry.data });
    }
  }

  function forgetDatabase(databaseId) {
    const client = databaseClients.get(databaseId);
    client?.databases.delete(databaseId);
    databaseClients.delete(databaseId);
    openTransactions.delete(databaseId);
  }

  function closeDatabase(databaseId) {
    queue.unshift({ client: null, data: { cmd: "close", databaseId } });
  }

  function addClient(post) {
    return { post, databases: new Set(), gone: false };
  }

  function receive(client, message) {
    if (client.gone) return;
    if (brokenReason) {
      reply(client, { id: message.id, error: brokenReason });
      return;
    }
    queue.push({ client, id: message.id, data: message.data });
    pump();
  }

  function innerMessage(message) {
    const entry = inFlight;
    if (!entry || message.id !== entry.innerId) return;
    inFlight = null;
    const { cmd, databaseId } = entry.data ?? {};
    if (cmd === "open" && message.data && message.data.databaseId != null) {
      const openedId = message.data.databaseId;
      if (entry.client && !entry.client.gone) {
        databaseClients.set(openedId, entry.client);
        entry.client.databases.add(openedId);
      } else {
        closeDatabase(openedId);
      }
    }
    if (cmd === "close" && databaseId != null) {
      forgetDatabase(databaseId);
    }
    if (message.databaseId != null && databaseClients.has(message.databaseId)) {
      if (message.autocommit === false) {
        openTransactions.set(message.databaseId, databaseClients.get(message.databaseId));
      } else {
        openTransactions.delete(message.databaseId);
      }
    }
    if (entry.client) {
      reply(entry.client, { id: entry.id, data: message.data, error: message.error });
    }
    pump();
  }

  function innerFailed(reason) {
    brokenReason = reason || "The SQLite worker stopped.";
    const waiting = inFlight ? [inFlight, ...queue] : [...queue];
    inFlight = null;
    queue.length = 0;
    for (const entry of waiting) {
      if (entry.client) reply(entry.client, { id: entry.id, error: brokenReason });
    }
  }

  function removeClient(client) {
    if (client.gone) return;
    client.gone = true;
    for (let i = queue.length - 1; i >= 0; i--) {
      if (queue[i].client === client) queue.splice(i, 1);
    }
    for (const databaseId of client.databases) {
      openTransactions.delete(databaseId);
      closeDatabase(databaseId);
    }
    pump();
  }

  return { addClient, receive, innerMessage, innerFailed, removeClient };
}
