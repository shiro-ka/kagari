import os
import time

import emulator_login as e

PACKAGE = "lab49.kagari"
ACTIVITY = "dev.dimension.flare.MainActivity"


def back(times=1):
    for _ in range(times):
        e.adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(1)


def reset_home():
    back(4)
    e.adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/{ACTIVITY}")
    time.sleep(6)


def record(name):
    e.shot(name)
    e.dump(name)


def open_drawer():
    if not e.tap("Open navigation menu", timeout=10):
        return False
    time.sleep(2)
    if e.find("Me", timeout=3, exact=True) is None:
        e.tap(e.HANDLE.lstrip("@"), timeout=5)
        time.sleep(2)
    record("drawer")
    return True


def notifications():
    if not e.tap("Notifications", timeout=10, exact=True):
        return
    time.sleep(6)
    record("notifications")
    if e.tap("Mentions", timeout=5):
        time.sleep(6)
        record("notifications-mentions")


def profile_tabs():
    if not open_drawer():
        return
    if not e.tap("Me", timeout=5, exact=True):
        return
    time.sleep(8)
    record("profile")
    for label, name in (
        ("Videos", "profile-videos"),
        ("Feeds", "profile-feeds"),
        ("Starter packs", "profile-starter-packs"),
        ("Lists", "profile-lists"),
    ):
        node = e.find(label, timeout=4, exact=True)
        if node is None:
            e.adb("shell", "input", "swipe", "900", "1000", "200", "1000", "300")
            time.sleep(1)
            node = e.find(label, timeout=4, exact=True)
        if node is None:
            continue
        x, y = e.center(node)
        e.adb("shell", "input", "tap", str(x), str(y))
        time.sleep(6)
        record(name)
    if e.tap("Edit profile", timeout=5, exact=True):
        time.sleep(6)
        record("profile-edit")
        back()
        time.sleep(2)
        record("profile-edit-closed")


def account_menu(item, name):
    if not open_drawer():
        return False
    if not e.tap("Settings", timeout=5, exact=True):
        return False
    time.sleep(3)
    if not e.tap("Accounts", timeout=5, exact=True):
        return False
    time.sleep(3)
    record(f"{name}-accounts")
    if not e.tap("More", timeout=5, exact=True):
        return False
    time.sleep(2)
    record(f"{name}-menu")
    if not e.tap(item, timeout=5, exact=True):
        return False
    time.sleep(6)
    record(name)
    return True


def muted_words():
    if not account_menu("Muted words", "muted-words"):
        return
    word = "kagaritest"
    if e.type_into("Word or #tag", word):
        if e.tap("Add", timeout=5, exact=True):
            time.sleep(6)
            record("muted-words-added")
            node = e.find(word, timeout=5, exact=True)
            if node is not None:
                root = e.dump()
                _, y = e.center(node)
                buttons = [
                    n for n in root.iter("node")
                    if n.get("clickable") == "true" and abs(e.center(n)[1] - y) < 80 and e.center(n)[0] > 800
                ]
                if buttons:
                    x, y = e.center(buttons[-1])
                    e.adb("shell", "input", "tap", str(x), str(y))
                    time.sleep(6)
                    record("muted-words-removed")


def moderation_lists():
    if account_menu("Muted accounts", "muted-accounts"):
        reset_home()
    if account_menu("Blocked accounts", "blocked-accounts"):
        pass


def direct_messages():
    if not open_drawer():
        return
    if not e.tap("Direct Messages", timeout=5):
        return
    time.sleep(8)
    record("dm-list")
    root = e.dump()
    if root is None:
        return
    rows = [
        n for n in root.iter("node")
        if n.get("clickable") == "true" and 400 < e.center(n)[1] < 2000 and e.center(n)[0] > 300
    ]
    if not rows:
        return
    x, y = e.center(rows[0])
    e.adb("shell", "input", "tap", str(x), str(y))
    time.sleep(8)
    record("dm-conversation")


def main():
    os.makedirs(e.OUT, exist_ok=True)
    reset_home()
    for step in (notifications, profile_tabs, muted_words, moderation_lists, direct_messages):
        try:
            step()
        except Exception as error:
            e.shot(f"{step.__name__}-error")
            with open(f"{e.OUT}/{step.__name__}-error.txt", "w") as f:
                f.write(repr(error))
        reset_home()


if __name__ == "__main__":
    main()
