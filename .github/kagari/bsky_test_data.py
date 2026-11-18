import json
import os
import re
import sys
import time
import urllib.error
import urllib.request

ENTRYWAY = "https://bsky.social"
APPVIEW = "did:web:api.bsky.app#bsky_appview"
CHAT = "did:web:api.bsky.chat#bsky_chat"
FEED_URI = "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.generator/whats-hot"
STARTER_PACK_ACTOR = "bsky.app"


def now():
    return time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())


class Session:
    def __init__(self, handle, password):
        data = self.call(ENTRYWAY, "com.atproto.server.createSession", {"identifier": handle, "password": password})
        self.did = data["did"]
        self.handle = data["handle"]
        self.jwt = data["accessJwt"]
        self.pds = ENTRYWAY
        for service in (data.get("didDoc") or {}).get("service", []):
            if service.get("id") == "#atproto_pds":
                self.pds = service["serviceEndpoint"]

    def call(self, base, method, body=None, params=None, proxy=None):
        url = f"{base}/xrpc/{method}"
        if params:
            url += "?" + "&".join(f"{k}={urllib.request.quote(str(v), safe='')}" for k, v in params.items())
        headers = {"Content-Type": "application/json"}
        if getattr(self, "jwt", None):
            headers["Authorization"] = f"Bearer {self.jwt}"
        if proxy:
            headers["atproto-proxy"] = proxy
        request = urllib.request.Request(
            url,
            data=json.dumps(body).encode() if body is not None else None,
            headers=headers,
            method="POST" if body is not None else "GET",
        )
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                text = response.read().decode()
                return json.loads(text) if text else {}
        except urllib.error.HTTPError as error:
            raise RuntimeError(f"{method}: {error.code} {error.read().decode()[:300]}") from None

    def xrpc(self, method, body=None, params=None, proxy=None):
        return self.call(self.pds, method, body, params, proxy)

    def create(self, collection, record):
        return self.xrpc(
            "com.atproto.repo.createRecord",
            {"repo": self.did, "collection": collection, "record": record},
        )

    def put(self, collection, rkey, record):
        return self.xrpc(
            "com.atproto.repo.putRecord",
            {"repo": self.did, "collection": collection, "rkey": rkey, "record": record},
        )

    def delete(self, uri):
        rkey = uri.rsplit("/", 1)[-1]
        collection = uri.split("/")[-2]
        self.xrpc("com.atproto.repo.deleteRecord", {"repo": self.did, "collection": collection, "rkey": rkey})

    def post(self, text, **extra):
        record = {"$type": "app.bsky.feed.post", "text": text, "createdAt": now(), "langs": ["ja"]}
        record.update(extra)
        return self.create("app.bsky.feed.post", record)


def byte_range(text, part):
    start = len(text[: text.index(part)].encode())
    return {"byteStart": start, "byteEnd": start + len(part.encode())}


def setup(out):
    main = Session(os.environ["BSKY_MAIN_HANDLE"], os.environ["BSKY_MAIN_APP_PASSWORD"])
    sub = Session(os.environ["BSKY_HANDLE"], os.environ["BSKY_APP_PASSWORD"])
    stamp = time.strftime("%m%d%H%M", time.gmtime())
    state = {"main": [], "sub": [], "stamp": stamp}

    target = sub.post(f"Claude Code: kagari test {stamp}")
    state["sub"].append(target["uri"])
    ref = {"uri": target["uri"], "cid": target["cid"]}

    reply = main.post(f"Claude Code: kagari reply test {stamp}", reply={"root": ref, "parent": ref})
    state["main"].append(reply["uri"])
    quote = main.post(
        f"Claude Code: kagari quote test {stamp}",
        embed={"$type": "app.bsky.embed.record", "record": ref},
    )
    state["main"].append(quote["uri"])

    feed = sub.xrpc("app.bsky.feed.getFeedGenerator", params={"feed": FEED_URI}, proxy=APPVIEW)["view"]
    feed_post = sub.post(
        f"Claude Code: kagari feed embed {stamp}",
        embed={"$type": "app.bsky.embed.record", "record": {"uri": feed["uri"], "cid": feed["cid"]}},
    )
    state["sub"].append(feed_post["uri"])
    packs = sub.xrpc(
        "app.bsky.graph.getActorStarterPacks",
        params={"actor": STARTER_PACK_ACTOR, "limit": 1},
        proxy=APPVIEW,
    ).get("starterPacks", [])
    if packs:
        pack_post = sub.post(
            f"Claude Code: kagari starter pack embed {stamp}",
            embed={"$type": "app.bsky.embed.record", "record": {"uri": packs[0]["uri"], "cid": packs[0]["cid"]}},
        )
        state["sub"].append(pack_post["uri"])

    follows = sub.xrpc("app.bsky.graph.getFollows", params={"actor": sub.did, "limit": 100}, proxy=APPVIEW)
    for follow in follows.get("follows", []):
        if follow["did"] == main.did and follow.get("viewer", {}).get("following"):
            sub.delete(follow["viewer"]["following"])
    sub.put("chat.bsky.actor.declaration", "self", {"$type": "chat.bsky.actor.declaration", "allowIncoming": "all"})
    try:
        convo = sub.xrpc("chat.bsky.convo.getConvoForMembers", params={"members": main.did}, proxy=CHAT)["convo"]
        sub.xrpc("chat.bsky.convo.leaveConvo", {"convoId": convo["id"]}, proxy=CHAT)
    except RuntimeError as error:
        state["leave_error"] = str(error)
    time.sleep(3)

    convo = main.xrpc("chat.bsky.convo.getConvoForMembers", params={"members": sub.did}, proxy=CHAT)["convo"]
    mention = f"@{sub.handle}"
    link = "https://example.com"
    text = f"Claude Code: kagari DM test {stamp} {mention} {link}"
    message = main.xrpc(
        "chat.bsky.convo.sendMessage",
        {
            "convoId": convo["id"],
            "message": {
                "text": text,
                "facets": [
                    {
                        "index": byte_range(text, mention),
                        "features": [{"$type": "app.bsky.richtext.facet#mention", "did": sub.did}],
                    },
                    {
                        "index": byte_range(text, link),
                        "features": [{"$type": "app.bsky.richtext.facet#link", "uri": link}],
                    },
                ],
            },
        },
        proxy=CHAT,
    )
    state["main_convo"] = convo["id"]
    state["main_message"] = message.get("id")
    with open(os.path.join(out, "state.json"), "w") as f:
        json.dump(state, f, indent=2)
    print("setup done", {k: v for k, v in state.items() if k != "stamp"})


def cleanup(out):
    path = os.path.join(out, "state.json")
    if not os.path.exists(path):
        return
    with open(path) as f:
        state = json.load(f)
    main = Session(os.environ["BSKY_MAIN_HANDLE"], os.environ["BSKY_MAIN_APP_PASSWORD"])
    for uri in state.get("main", []):
        try:
            main.delete(uri)
        except RuntimeError as error:
            print("delete failed", uri, re.sub(r"\s+", " ", str(error)))
    if state.get("main_convo") and state.get("main_message"):
        try:
            main.xrpc(
                "chat.bsky.convo.deleteMessageForSelf",
                {"convoId": state["main_convo"], "messageId": state["main_message"]},
                proxy=CHAT,
            )
        except RuntimeError as error:
            print("dm delete failed", re.sub(r"\s+", " ", str(error)))
    print("cleanup done")


if __name__ == "__main__":
    out = sys.argv[2]
    os.makedirs(out, exist_ok=True)
    {"setup": setup, "cleanup": cleanup}[sys.argv[1]](out)
