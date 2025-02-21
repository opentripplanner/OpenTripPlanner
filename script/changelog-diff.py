#!/usr/bin/env python3

import subprocess
import sys

if len(sys.argv) != 3:
    print("Expected arguments: <git-ref-A> <git-ref-B>", file=sys.stderr)
    exit(1)

v1 = sys.argv[1]
v2 = sys.argv[2]

cmd_args = ['git', 'diff', v1, v2, '--no-ext-diff', '-U0', '-a',  '--no-prefix',  '--',  'doc/user/Changelog.md']
p = subprocess.run(args=cmd_args, capture_output=True, text=True, timeout=20)

if p.returncode:
    print(p.stderr, file=sys.stderr)
    print(f"\nERROR! Execution failed: {cmd_args}", file=sys.stderr)
    exit(p.returncode)

changes = p.stdout
print(f"\n## Changelog {v1} vs {v2} \n")
added = []
removed = []

for line in changes.splitlines(keepends=True):
    if line.startswith("+- "):
        added.append(line[1:-1])
    elif line.startswith("-- "):
        removed.append(line[1:-1])

if len(added) == 0 and len(removed) == 0:
    print(f"No changes!")
elif len(added) > 0:
    print(f"### Added PRs")
    for line in added:
        print(line)
elif len(removed) > 0:
    print(f"### Removed PRs")
    for line in removed:
        print(line)

