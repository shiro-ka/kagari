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
            f.write(xml)
    return ET.fromstring(xml[xml.index("<"):]) if "<" in xml else None


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


def type_into(label, text):
    if not tap(label):
        return False
    adb("shell", "input", "text", text)
    time.sleep(1)
    hide_ime()
    return True


def main():
    os.makedirs(OUT, exist_ok=True)
    if not HANDLE or not APP_PASSWORD:
        print("BSKY_HANDLE or BSKY_APP_PASSWORD is empty", file=sys.stderr)
        return 1
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
    if tap("Quote posts allowed"):
        time.sleep(1)
        shot("quote-disabled")
    return 0


sys.exit(main())
