import re
import subprocess
import sys


def next_version(base, kind, tags):
    prefixes = (f"android-{base}-kagari.", f"v{base}-kagari.")
    releases = []
    prereleases = []
    for tag in tags:
        prefix = next((p for p in prefixes if tag.startswith(p)), None)
        if prefix is None:
            continue
        rest = tag[len(prefix):]
        if re.fullmatch(r"[0-9]+", rest):
            releases.append(1)
            continue
        match = re.fullmatch(r"([0-9]+)\.([0-9]+)", rest)
        if not match:
            continue
        major, minor = int(match.group(1)), int(match.group(2))
        if minor == 0:
            releases.append(major)
        else:
            prereleases.append((major, minor))
    current = max(releases, default=0)
    if kind == "release":
        return f"{base}-kagari.{current + 1}.0"
    minor = max((m for p, m in prereleases if p == current), default=0)
    return f"{base}-kagari.{current}.{minor + 1}"


def main():
    base, kind = sys.argv[1], sys.argv[2]
    tags = subprocess.run(
        ["git", "tag", "--list"], check=True, capture_output=True, text=True
    ).stdout.split()
    print(next_version(base, kind, tags))


if __name__ == "__main__":
    main()
