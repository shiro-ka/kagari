import os
import sys
import time

import emulator_login as e


def main():
    os.makedirs(e.OUT, exist_ok=True)
    e.shot("start")
    if not e.tap("Log in"):
        return 1
    if e.find("Find a server by URL", timeout=10) is None:
        return 1
    e.shot("service-select")
    if not e.tap("Twitter"):
        return 1
    time.sleep(5)
    e.shot("selected")
    e.dump("selected")
    if not e.tap("Log in", last=True, exact=True):
        return 1
    time.sleep(10)
    e.shot("after-login-tap")
    e.dump("after-login-tap")
    return 0


if __name__ == "__main__":
    sys.exit(main())
