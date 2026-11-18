import os
import re
import time

import emulator_login as e
import emulator_screens as s


def record(name):
    s.record(name)


def long_press(node, ms=900):
    x1, y1, _, _ = map(int, re.findall(r"\d+", node.get("bounds")))
    x, y = x1 + 24, y1 + 24
    e.adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), str(ms))
    time.sleep(2)


def menu_for(text):
    target = e.find(text, timeout=10)
    if target is None:
        return False
    _, ty = e.center(target)
    root = e.dump()
    buttons = sorted(
        (e.center(n) for n in root.iter("node") if n.get("content-desc") == "その他" and e.center(n)[1] > ty),
        key=lambda p: p[1],
    )
    if not buttons:
        return False
    x, y = buttons[0]
    e.adb("shell", "input", "tap", str(x), str(y))
    time.sleep(2)
    return True


def direct_message(stamp):
    if not s.open_drawer():
        return
    if not e.tap("メッセージ", timeout=5, exact=True):
        return
    time.sleep(8)
    record("two-dm-list")
    if not e.tap("チャットの要求", timeout=8):
        return
    time.sleep(8)
    record("two-dm-requests")
    room = e.find("kagari DM test", timeout=8)
    if room is None:
        return
    x, y = e.center(room)
    e.adb("shell", "input", "tap", str(x), str(y))
    time.sleep(8)
    record("two-dm-conversation")
    message = e.find(f"kagari DM test {stamp}", timeout=8)
    if message is None:
        return
    long_press(message)
    record("two-dm-long-press")
    if e.tap("❤️", timeout=5, exact=True):
        time.sleep(6)
        record("two-dm-reacted")
    if e.tap("承認", timeout=5, exact=True):
        time.sleep(6)
        record("two-dm-accepted")


def detach_quote(stamp):
    if not e.tap("通知", timeout=10, exact=True):
        return
    time.sleep(8)
    e.tap("メンション", timeout=5, exact=True)
    time.sleep(8)
    record("two-notifications")
    if not menu_for(f"kagari quote test {stamp}"):
        return
    record("two-quote-menu")
    if not e.tap("引用を外す", timeout=5, exact=True):
        return
    time.sleep(3)
    record("two-detach-dialog")
    if e.tap("外す", timeout=5, exact=True):
        time.sleep(8)
        record("two-detached")


def hide_reply(stamp):
    if not e.tap("通知", timeout=10, exact=True):
        return
    time.sleep(8)
    e.tap("メンション", timeout=5, exact=True)
    time.sleep(8)
    if not menu_for(f"kagari reply test {stamp}"):
        return
    record("two-reply-menu")
    if not e.tap("返信を非表示にする", timeout=5, exact=True):
        return
    time.sleep(5)
    record("two-hide-dialog")
    if e.tap("非表示にする", timeout=5, exact=True):
        time.sleep(8)
        record("two-hidden")


def embeds(stamp):
    if not s.open_drawer():
        return
    if not e.tap("自分", timeout=5, exact=True):
        return
    time.sleep(8)
    for _ in range(3):
        e.adb("shell", "input", "swipe", "540", "1800", "540", "800", "500")
        time.sleep(2)
    record("two-own-posts")


def main():
    os.makedirs(e.OUT, exist_ok=True)
    stamp = os.environ.get("KAGARI_TEST_STAMP", "")
    s.reset_home()
    for step in (direct_message, detach_quote, hide_reply, embeds):
        try:
            step(stamp)
        except Exception as error:
            e.shot(f"{step.__name__}-error")
            with open(f"{e.OUT}/{step.__name__}-error.txt", "w") as f:
                f.write(repr(error))
        s.reset_home()


if __name__ == "__main__":
    main()
