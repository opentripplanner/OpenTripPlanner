#!/usr/bin/env python3

import subprocess

import sys

args_len = len(sys.argv)

if (args_len < 3 or args_len > 4):
    print("Expected arguments: <git-ref-A> <git-ref-B> [title]", file=sys.stderr)
    print("  - git-ref-A/B : Git tag, branch or commit hash to identify the commit to use.", file=sys.stderr)
    print("  - title : An optional title to use, default is 'Changelog'.", file=sys.stderr)
    exit(1)

refA = sys.argv[1]
refB = sys.argv[2]
title = "Changelog"

if args_len > 3:
    title = f"{sys.argv[3]}"

cmd_args = ['git', 'diff', refA, refB, '--no-ext-diff', '-U0', '-a', '--no-prefix', '--', 'doc/user/Changelog.md']
p = subprocess.run(args=cmd_args, capture_output=True, text=True, timeout=20)

if p.returncode:
    print(p.stderr, file=sys.stderr)
    print(f"\nERROR! Execution failed: {cmd_args}", file=sys.stderr)
    exit(p.returncode)

changes = p.stdout
print(f"\n## {title} _{refA}_ vs _{refB}_\n")
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

