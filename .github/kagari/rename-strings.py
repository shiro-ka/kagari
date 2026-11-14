import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".")
patterns = [
    "app/src/main/res/values*/strings.xml",
    "compose-ui/src/commonMain/composeResources/values*/strings.xml",
]
word = re.compile(r"\bFlare\b")
total = 0
for pattern in patterns:
    for path in sorted(root.glob(pattern)):
        text = path.read_text(encoding="utf-8")
        new, count = word.subn("kagari", text)
        if count:
            path.write_text(new, encoding="utf-8")
            total += count
print(f"replaced {total}")
if total == 0:
    sys.exit(1)
