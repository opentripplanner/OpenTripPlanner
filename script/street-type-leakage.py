#!/usr/bin/env python3
"""
Visualise how the street-module types Edge, Vertex and State leak into other Maven modules.

Scans every Java file outside the `street` module, finds the files that use one of the three types
(via import or by living in the same package) and counts references to the simple type name. The
result is aggregated by package area and rendered as a single SVG:

  * one summary tile per type: references, files, and how many of those files are production code
  * a heatmap, with one row per package area and one column per type, showing reference counts,
    plus a bar with the number of files per area split into production and test code

Usage:
  script/street-type-leakage.py [-o street-type-leakage.svg] [--top 20] [--depth 2] [--prod-only]
"""

import argparse
import re
import sys
from collections import defaultdict
from html import escape
from pathlib import Path

TYPES = {
  "Edge": "org.opentripplanner.street.model.edge",
  "Vertex": "org.opentripplanner.street.model.vertex",
  "State": "org.opentripplanner.street.search.state",
}
TYPE_NAMES = list(TYPES)
STREET_MODULE = "street"
PROD_SOURCE_SETS = {"main", "ext"}
BASE_PACKAGE = "org/opentripplanner/"

# Comments, string literals and text blocks are removed before counting references
STRIP_RE = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\\n])*"|/\*[\s\S]*?\*/|//[^\n]*')
PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
IMPORT_LINE_RE = re.compile(r"^\s*import\s+[^;]+;", re.M)


def find_repo_root():
  root = Path(__file__).resolve().parent.parent
  if not (root / STREET_MODULE / "pom.xml").exists():
    sys.exit(f"Could not find the '{STREET_MODULE}' module below {root}")
  return root


def java_files(root):
  for path in root.rglob("*.java"):
    rel = path.relative_to(root)
    parts = rel.parts
    if parts[0] == STREET_MODULE or "target" in parts or "node_modules" in parts:
      continue
    yield path, rel


def source_set_and_module(rel):
  """Returns (module, source set) for a path like application/src/<set>/java/..."""
  parts = rel.parts
  if "src" not in parts:
    return parts[0], "unknown"
  i = parts.index("src")
  module = "/".join(parts[:i])
  source_set = parts[i + 1] if i + 1 < len(parts) else "unknown"
  return module, source_set


def area_of(rel, module, depth):
  """Package area used for grouping, e.g. 'routing.algorithm' or 'ext.flex'."""
  path = rel.as_posix()
  idx = path.find(BASE_PACKAGE)
  if idx < 0:
    return module
  pkg = path[idx + len(BASE_PACKAGE):].split("/")[:-1]
  if not pkg:
    return module
  # Sandbox features are grouped by feature name
  n = depth + 1 if pkg[0] == "ext" else depth
  area = ".".join(pkg[:n])
  return area if module == "application" else f"{module}:{area}"


def analyse(root, depth, prod_only=False):
  # area -> type -> refs; area -> {"prod": files, "test": files}
  refs = defaultdict(lambda: defaultdict(int))
  files = defaultdict(lambda: {"prod": 0, "test": 0})
  per_type = {t: {"refs": 0, "files": 0, "prod_files": 0} for t in TYPE_NAMES}
  total_files = {"prod": 0, "test": 0}
  modules = set()

  for path, rel in java_files(root):
    if prod_only and source_set_and_module(rel)[1] not in PROD_SOURCE_SETS:
      continue
    text = path.read_text(encoding="utf-8", errors="replace")
    if "opentripplanner.street" not in text:
      continue
    code = STRIP_RE.sub("", text)
    m = PACKAGE_RE.search(code)
    package = m.group(1) if m else ""
    body = IMPORT_LINE_RE.sub("", code)

    used = {}
    for t, pkg in TYPES.items():
      imported = re.search(rf"^\s*import\s+{re.escape(pkg)}\.{t}\s*;", code, re.M)
      if imported or package == pkg:
        count = len(re.findall(rf"(?<![\w.]){t}\b", body))
        # Fully qualified usages without import
        count += len(re.findall(rf"{re.escape(pkg)}\.{t}\b", body)) if not imported else 0
        if count:
          used[t] = count
      else:
        fq = len(re.findall(rf"{re.escape(pkg)}\.{t}\b", body))
        if fq:
          used[t] = fq
    if not used:
      continue

    module, source_set = source_set_and_module(rel)
    kind = "prod" if source_set in PROD_SOURCE_SETS else "test"
    area = area_of(rel, module, depth)
    modules.add(module)
    files[area][kind] += 1
    total_files[kind] += 1
    for t, count in used.items():
      refs[area][t] += count
      per_type[t]["refs"] += count
      per_type[t]["files"] += 1
      if kind == "prod":
        per_type[t]["prod_files"] += 1

  return refs, files, per_type, total_files, sorted(modules)


def rows_for(refs, files, top):
  areas = sorted(refs, key=lambda a: (-sum(refs[a].values()), a))
  rows = [(a, dict(refs[a]), files[a]) for a in areas[:top]]
  rest = areas[top:]
  if rest:
    agg = defaultdict(int)
    agg_files = {"prod": 0, "test": 0}
    for a in rest:
      for t, c in refs[a].items():
        agg[t] += c
      for k in agg_files:
        agg_files[k] += files[a][k]
    rows.append((f"other ({len(rest)} areas)", dict(agg), agg_files))
  return rows


# Sequential blue ramp (light -> dark), steps 100..700
BLUE_RAMP = [
  "#cce3fd", "#b6d5f8", "#9dc8f7", "#84b9f3", "#6aabf1", "#519ced", "#338ced",
  "#257de0", "#2370cd", "#1b64be", "#1757ac", "#0f4a96", "#0e3d7e",
]


def cell_color(value, max_value):
  if value == 0:
    return None
  # sqrt scale so that small areas are still distinguishable from zero
  f = (value / max_value) ** 0.5 if max_value else 0
  return BLUE_RAMP[min(len(BLUE_RAMP) - 1, int(f * (len(BLUE_RAMP) - 1)))]


def text_on(color):
  return "#ffffff" if color and BLUE_RAMP.index(color) >= 6 else "#191919"


def render_svg(rows, per_type, total_files, modules, prod_only):
  W = 900
  pad = 32
  label_w = 230
  cell_w = 110
  cell_h = 26
  gap = 2
  bar_x = pad + label_w + len(TYPE_NAMES) * cell_w + 28
  bar_max_w = W - pad - bar_x - 40
  out = []

  def add(s):
    out.append(s)

  # --- header ---------------------------------------------------------------
  y = pad + 18
  total_refs = sum(p["refs"] for p in per_type.values())
  n_files = total_files["prod"] + total_files["test"]
  scope = "production code" if prod_only else "production and test code"
  head = [
    f'<text x="{pad}" y="{y}" class="title">How Edge, Vertex and State leak out of the street module</text>',
    f'<text x="{pad}" y="{y + 22}" class="sub">{total_refs:,} references in {n_files:,} files '
    f'({total_files["prod"]:,} production, {total_files["test"]:,} test) in '
    f'{escape(", ".join(modules) or "no modules")} — {scope}</text>',
  ]
  out.extend(head)

  # --- summary tiles ----------------------------------------------------------
  y += 44
  tile_w = (W - 2 * pad - 2 * 16) / 3
  tile_h = 78
  for i, t in enumerate(TYPE_NAMES):
    p = per_type[t]
    x = pad + i * (tile_w + 16)
    add(f'<rect x="{x:.1f}" y="{y}" width="{tile_w:.1f}" height="{tile_h}" rx="8" class="tile"/>')
    add(f'<text x="{x + 16:.1f}" y="{y + 24}" class="tile-label">{t}</text>')
    add(f'<text x="{x + 16:.1f}" y="{y + 54}" class="hero">{p["refs"]:,}</text>')
    add(f'<text x="{x + 16:.1f}" y="{y + 70}" class="sub">references in {p["files"]:,} files '
        f'({p["prod_files"]:,} production)</text>')
  y += tile_h + 40

  # --- heatmap header ---------------------------------------------------------
  add(f'<text x="{pad}" y="{y}" class="section">References by package area</text>')
  y += 26
  for i, t in enumerate(TYPE_NAMES):
    cx = pad + label_w + i * cell_w + cell_w / 2
    add(f'<text x="{cx:.1f}" y="{y}" class="col" text-anchor="middle">{t}</text>')
  add(f'<text x="{bar_x}" y="{y}" class="col">Files</text>')
  # legend for the file bar
  lx = bar_x + 44
  add(f'<rect x="{lx}" y="{y - 9}" width="10" height="10" rx="2" class="prod"/>')
  add(f'<text x="{lx + 14}" y="{y}" class="legend">production</text>')
  if not prod_only:
    add(f'<rect x="{lx + 88}" y="{y - 9}" width="10" height="10" rx="2" class="test"/>')
    add(f'<text x="{lx + 102}" y="{y}" class="legend">test</text>')
  y += 10

  max_cell = max((c for _, r, _ in rows for c in r.values()), default=1)
  max_files = max((f["prod"] + f["test"] for _, _, f in rows), default=1)

  for area, r, f in rows:
    add(f'<text x="{pad + label_w - 12}" y="{y + cell_h / 2 + 4:.1f}" class="row" '
        f'text-anchor="end">{escape(area)}</text>')
    for i, t in enumerate(TYPE_NAMES):
      v = r.get(t, 0)
      x = pad + label_w + i * cell_w
      color = cell_color(v, max_cell)
      fill = color or "#f0f0f0"
      add(f'<g><title>{escape(area)} · {t}: {v} references</title>'
          f'<rect x="{x + gap / 2}" y="{y + gap / 2}" width="{cell_w - gap}" '
          f'height="{cell_h - gap}" rx="3" fill="{fill}"/>')
      if v:
        add(f'<text x="{x + cell_w / 2}" y="{y + cell_h / 2 + 4:.1f}" class="cell" '
            f'text-anchor="middle" fill="{text_on(color)}">{v}</text>')
      add("</g>")
    # files bar
    total = f["prod"] + f["test"]
    scale = bar_max_w / max_files if max_files else 0
    pw = f["prod"] * scale
    tw = f["test"] * scale
    by = y + 6
    bh = cell_h - 12
    add(f'<g><title>{escape(area)}: {f["prod"]} production, {f["test"]} test files</title>')
    if pw:
      add(f'<rect x="{bar_x}" y="{by}" width="{max(pw - (gap if tw else 0), 1):.1f}" '
          f'height="{bh}" rx="2" class="prod"/>')
    if tw:
      add(f'<rect x="{bar_x + pw:.1f}" y="{by}" width="{max(tw, 1):.1f}" height="{bh}" '
          f'rx="2" class="test"/>')
    add(f'<text x="{bar_x + pw + tw + 6:.1f}" y="{y + cell_h / 2 + 4:.1f}" class="legend">'
        f'{total}</text></g>')
    y += cell_h

  y += 24
  add(f'<text x="{pad}" y="{y}" class="note">Cell = occurrences of the simple type name in files '
      f'that import it (comments, strings and imports excluded). Darker = more references.</text>')
  H = y + pad

  style = """
  <style>
    svg { font-family: system-ui, -apple-system, "Segoe UI", sans-serif; }
    .bg { fill: #ffffff; }
    .tile { fill: #f5f5f5; }
    .title { font-size: 18px; font-weight: 600; fill: #191919; }
    .section { font-size: 14px; font-weight: 600; fill: #191919; }
    .sub, .legend, .note { font-size: 12px; fill: #666666; }
    .tile-label { font-size: 13px; font-weight: 600; fill: #666666; }
    .hero { font-size: 26px; font-weight: 600; fill: #191919; }
    .col { font-size: 12px; font-weight: 600; fill: #666666; }
    .row { font-size: 12px; fill: #191919; font-family: ui-monospace, monospace; }
    .cell { font-size: 12px; font-variant-numeric: tabular-nums; }
    .prod { fill: #257de0; }
    .test { fill: #f47230; }
  </style>"""
  return (
    f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'
    f"{style}\n<rect class=\"bg\" width=\"{W}\" height=\"{H}\"/>\n" + "\n".join(out) + "\n</svg>\n"
  )


def main():
  ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
  ap.add_argument("-o", "--output", default="street-type-leakage.svg", help="SVG output file")
  ap.add_argument("--top", type=int, default=20, help="number of package areas to show")
  ap.add_argument("--depth", type=int, default=2, help="package depth used to group areas")
  ap.add_argument("--prod-only", action="store_true", help="ignore test source sets")
  args = ap.parse_args()

  root = find_repo_root()
  refs, files, per_type, total_files, modules = analyse(root, args.depth, args.prod_only)
  rows = rows_for(refs, files, args.top)
  Path(args.output).write_text(render_svg(rows, per_type, total_files, modules, args.prod_only))

  for t in TYPE_NAMES:
    p = per_type[t]
    print(f"{t:7} {p['refs']:6} refs  {p['files']:5} files  ({p['prod_files']} production)")
  print(f"Wrote {args.output}")


if __name__ == "__main__":
  main()
