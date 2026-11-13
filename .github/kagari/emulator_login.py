import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

OUT = sys.argv[1]
HANDLE = os.environ.get("BSKY_HANDLE", "")
APP_PASSWORD = os.environ.get("BSKY_APP_PASSWORD", "")
step_no = 0


def adb(*args, capture=False):
    result = subprocess.run(["adb", *args], capture_output=True, check=False)
    return result.stdout if capture else None


def shot(name):
    global step_no
    step_no += 1
    path = os.path.join(OUT, f"{step_no:02d}-{name}.png")
    with open(path, "wb") as f:
        f.write(adb("exec-out", "screencap", "-p", capture=True))


def dump(name=None):
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("exec-out", "cat", "/sdcard/ui.xml", capture=True).decode("utf-8", "replace")
    if name:
        with open(os.path.join(OUT, f"{step_no:02d}-{name}.xml"), "w") as f:
            f.write(redact(xml))
    return ET.fromstring(xml[xml.index("<"):]) if "<" in xml else None


def redact(xml):
    def clean(match):
        node = match.group(0)
        if 'password="true"' in node:
            node = re.sub(r'text="[^"]*"', 'text=""', node)
        if APP_PASSWORD:
            pieces = {APP_PASSWORD[i:i + 4] for i in range(0, max(len(APP_PASSWORD) - 3, 1))}

            def scrub(value):
                text = value.group(2)
                for piece in pieces:
                    text = text.replace(piece, "")
                return value.group(1) + text + '"'

            node = re.sub(r'((?:text|content-desc)=")([^"]*)"', scrub, node)
        return node

    return re.sub(r"<node [^>]*>", clean, xml)


def disable_ime():
    out = adb("shell", "ime", "list", "-s", capture=True).decode("utf-8", "replace")
    for ime in out.split():
        adb("shell", "ime", "disable", ime)


def type_text(text, delay):
    for ch in text:
        adb("shell", "input", "text", "%s" if ch == " " else ch)
        time.sleep(delay)


def field_texts():
    root = dump()
    if root is None:
        return []
    return [n.get("text") or "" for n in root.iter("node") if n.get("class") == "android.widget.EditText"]


def clear_field():
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    for _ in range(4):
        adb("shell", "input", "keyevent", *(["KEYCODE_DEL"] * 15))


def center(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def hide_ime():
    ime = adb("shell", "dumpsys", "input_method", capture=True).decode("utf-8", "replace")
    if "mInputShown=true" in ime:
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)


def matches(node, label, exact):
    texts = [node.get("text") or "", node.get("content-desc") or ""]
    if exact:
        return label in texts
    return any(label in t for t in texts)


def find(label, last=False, timeout=20, exact=False):
    deadline = time.time() + timeout
    while time.time() < deadline:
        root = dump()
        if root is not None:
            hits = [n for n in root.iter("node") if matches(n, label, exact)]
            if hits:
                return hits[-1] if last else hits[0]
        time.sleep(1)
    return None


def tap(label, last=False, timeout=20, exact=False):
    node = find(label, last=last, timeout=timeout, exact=exact)
    if node is None:
        dump(f"missing-{re.sub(r'[^A-Za-z0-9]+', '_', label)}")
        shot(f"missing-{re.sub(r'[^A-Za-z0-9]+', '_', label)}")
        return False
    x, y = center(node)
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(1)
    return True


def open_own_post_menu(more="More", markers=("Reaction settings",), banner="New Post"):
    tried = set()
    for _ in range(12):
        root = dump()
        if root is None:
            time.sleep(1)
            continue
        ok = [n for n in root.iter("node") if (n.get("text") or "") == "OK"]
        if ok:
            x, y = center(ok[0])
            adb("shell", "input", "tap", str(x), str(y))
            time.sleep(3)
            continue
        banners = [n for n in root.iter("node") if banner in (n.get("text") or "")]
        if banners:
            x, y = center(banners[0])
            adb("shell", "input", "tap", str(x), str(y))
            time.sleep(4)
            continue
        candidates = []
        for node in root.iter("node"):
            if node.get("content-desc") == more:
                x, y = center(node)
                if 100 < y < 560 and (x, y) not in tried:
                    candidates.append((x, y))
        for x, y in candidates:
            tried.add((x, y))
            adb("shell", "input", "tap", str(x), str(y))
            time.sleep(2)
            shot("menu-try")
            dump("menu-try")
            if any(find(m, timeout=3) is not None for m in markers):
                return True
            adb("shell", "input", "keyevent", "KEYCODE_BACK")
            time.sleep(1)
        adb("shell", "input", "swipe", "160", "450", "160", "150", "400")
        time.sleep(2)
        shot("scrolled")
        tried.clear()
    return False


def type_into(label, text):
    for attempt in range(3):
        if not tap(label):
            return False
        clear_field()
        type_text(text, 0.15 + attempt * 0.2)
        time.sleep(1)
        hide_ime()
        if text in field_texts():
            return True
    return False


def main():
    os.makedirs(OUT, exist_ok=True)
    if not HANDLE or not APP_PASSWORD:
        print("BSKY_HANDLE or BSKY_APP_PASSWORD is empty", file=sys.stderr)
        return 1
    disable_ime()
    shot("start")
    if not tap("Log in"):
        return 1
    shot("service-select")
    if not type_into("Server URL", "bsky.social"):
        return 1
    time.sleep(5)
    shot("server")
    use_password = find("Use password", timeout=5)
    if use_password is not None:
        x, y = center(use_password)
        adb("shell", "input", "tap", str(x), str(y))
        time.sleep(1)
    if not type_into("Username", HANDLE):
        return 1
    if not type_into("Password", APP_PASSWORD):
        return 1
    shot("filled")
    if not tap("Log in", last=True, exact=True):
        return 1
    time.sleep(20)
    shot("after-login")
    dump("after-login")
    if not tap("Compose"):
        return 1
    time.sleep(5)
    shot("compose")
    dump("compose")
    if tap("Replies:"):
        time.sleep(3)
        shot("reply-dialog")
        dump("reply-dialog")
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)
    if os.environ.get("KAGARI_TEST_POST") == "1":
        stamp = time.strftime("%Y%m%d%H%M", time.gmtime())
        if not tap("happening"):
            return 1
        type_text(f"Claude Code: kagari threadgate test {stamp}", 0.15)
        time.sleep(1)
        hide_ime()
        shot("post-filled")
        if not tap("Send post"):
            return 1
        time.sleep(15)
        shot("after-post")
        shot("timeline")
        dump("timeline")
        if open_own_post_menu():
            shot("post-menu")
            dump("post-menu")
            if tap("Reaction settings", timeout=5):
                time.sleep(6)
                shot("edit-dialog")
                dump("edit-dialog")
                if tap("Allow quote posts", timeout=5):
                    time.sleep(1)
                    shot("edit-toggled")
                    if tap("Save", timeout=5, exact=True):
                        time.sleep(8)
                        shot("after-save")
                        if open_own_post_menu():
                            shot("post-menu-after-save")
                            dump("post-menu-after-save")
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        adb("shell", "cmd", "locale", "set-app-locales", "lab49.kagari", "--locales", "ja-JP")
        time.sleep(6)
        shot("ja-home")
        if open_own_post_menu(more="その他", markers=("リアクションの設定",), banner="新しい投稿"):
            shot("ja-post-menu")
            dump("ja-post-menu")
            if tap("リアクションの設定", timeout=5):
                time.sleep(6)
                shot("ja-edit-dialog")
        return 0
    if tap("Quote posts", timeout=5):
        time.sleep(1)
        shot("quote-toggled")
    return 0


sys.exit(main())
