import { createRouter } from "./router.js";

const inner = new Worker(new URL("./worker.js", import.meta.url), { type: "module" });
const writes = new BroadcastChannel("kagari-sqlite-writes");
const router = createRouter((message) => inner.postMessage(message), {
  sharedFiles: new Set(["app.db"]),
  onShared: (fileName) => writes.postMessage(fileName),
});

inner.onmessage = (event) => router.innerMessage(event.data);
inner.onerror = (event) => router.innerFailed(event.message);

onconnect = (event) => {
  const port = event.ports[0];
  const client = router.addClient((message) => port.postMessage(message));
  port.onmessage = (messageEvent) => {
    const message = messageEvent.data;
    if (message && typeof message.kagariClient === "string") {
      navigator.locks.request(message.kagariClient, () => router.removeClient(client));
      return;
    }
    router.receive(client, message);
  };
};
