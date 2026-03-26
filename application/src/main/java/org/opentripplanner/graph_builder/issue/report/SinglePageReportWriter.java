package org.opentripplanner.graph_builder.issue.report;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a single self-contained HTML file containing all data import issues as embedded JSON,
 * with inline CSS and JS for an interactive, searchable, paginated report.
 */
class SinglePageReportWriter {

  private static final Logger LOG = LoggerFactory.getLogger(SinglePageReportWriter.class);

  private final DataSource target;
  private final ReportConfig config;
  private final Map<String, List<DataImportIssue>> issuesByType;
  private final List<String> typesWithGeoJson;

  SinglePageReportWriter(
    CompositeDataSource reportDirectory,
    ReportConfig config,
    Map<String, List<DataImportIssue>> issuesByType,
    List<String> typesWithGeoJson
  ) {
    this.target = reportDirectory.entry("index.html");
    this.config = config;
    this.issuesByType = issuesByType;
    this.typesWithGeoJson = typesWithGeoJson;
  }

  void writeFile() {
    try (var out = new PrintWriter(target.asOutputStream(), true, StandardCharsets.UTF_8)) {
      writeHtml(out);
    }
  }

  private void writeHtml(PrintWriter w) {
    w.println("<!DOCTYPE html>");
    w.println("<html lang=\"en\">");
    writeHead(w);
    w.println("<body>");
    writeHeader(w);
    writeToolbar(w);
    w.println("<main id=\"main\"></main>");
    writeDataScript(w);
    writeAppScript(w);
    w.println("</body></html>");
  }

  // ---- <head> ----

  private void writeHead(PrintWriter w) {
    w.println("<head>");
    w.println("<meta charset=\"utf-8\">");
    w.println("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
    w.printf("<title>Data Import Issues \u2014 %s</title>%n", esc(config.appName()));
    w.println("<style>");
    writeCss(w);
    w.println("</style>");
    w.println("</head>");
  }

  private void writeCss(PrintWriter w) {
    w.printf(
      """
      :root {
        --primary: %s;
        --primary-light: color-mix(in srgb, var(--primary) 12%%, white);
        --font: %s;
        --bg: #f8f9fa;
        --card-bg: #fff;
        --border: #dee2e6;
        --text: #212529;
        --text-muted: #6c757d;
        --radius: 8px;
      }
      """,
      esc(config.primaryColor()),
      esc(config.fontFamily())
    );
    w.println(
      """
      *, *::before, *::after { box-sizing: border-box; }
      body { margin:0; font-family:var(--font); background:var(--bg); color:var(--text);
             font-size:14px; line-height:1.5; }

      .header { background:var(--primary); color:#fff; padding:16px 24px;
                display:flex; align-items:center; gap:16px; }
      .header img { height:36px; }
      .header h1 { margin:0; font-size:20px; font-weight:600; }
      .header .meta { margin-left:auto; font-size:12px; opacity:.8; }

      .toolbar { position:sticky; top:0; z-index:10; background:var(--card-bg);
                 border-bottom:1px solid var(--border); padding:12px 24px;
                 display:flex; flex-wrap:wrap; gap:10px; align-items:center; }
      .toolbar input[type=search] { flex:1; min-width:200px; padding:8px 12px;
                 border:1px solid var(--border); border-radius:var(--radius);
                 font-size:14px; font-family:var(--font); }
      .toolbar input[type=search]:focus { outline:none; border-color:var(--primary);
                 box-shadow:0 0 0 3px var(--primary-light); }
      .search-hint { font-size:11px; color:var(--text-muted); width:100%; }

      .sort-bar { display:flex; align-items:center; gap:6px; }
      .sort-label { font-size:12px; color:var(--text-muted); white-space:nowrap; }
      .sort-btn { padding:5px 12px; border:1px solid var(--border); border-radius:var(--radius);
                  background:var(--card-bg); cursor:pointer; font-size:12px;
                  font-family:var(--font); color:var(--text-muted); transition:all .15s; }
      .sort-btn:hover { border-color:var(--primary); color:var(--primary); }
      .sort-btn.active { background:var(--primary); color:#fff; border-color:var(--primary); }

      .summary { padding:8px 24px; font-size:13px; color:var(--text-muted);
                 border-bottom:1px solid var(--border); background:var(--card-bg); }

      main { max-width:1200px; margin:0 auto; padding:16px 24px 60px; }

      .group { background:var(--card-bg); border:1px solid var(--border);
               border-radius:var(--radius); margin-bottom:8px; overflow:hidden; }
      .group-header { display:flex; align-items:center; gap:12px; padding:12px 16px;
                      cursor:pointer; user-select:none; transition:background .1s; }
      .group-header:hover { background:#f1f3f5; }
      .group-header .arrow { transition:transform .2s; font-size:12px; color:var(--text-muted); }
      .group-header.open .arrow { transform:rotate(90deg); }
      .group-header .type-name { font-weight:600; font-size:14px; }
      .group-header .count { font-size:12px; color:var(--text-muted); }
      .group-header .actions { display:flex; align-items:center; gap:6px; margin-left:auto; }
      .icon-btn { background:none; border:1px solid var(--border); border-radius:var(--radius);
                  cursor:pointer; padding:3px 8px; font-size:13px; line-height:1;
                  color:var(--text-muted); transition:all .15s; font-family:var(--font); }
      .icon-btn:hover { border-color:var(--primary); color:var(--primary); }
      .geojson-btn { font-size:12px; color:var(--primary); text-decoration:none;
                     padding:3px 8px; border:1px solid var(--primary);
                     border-radius:var(--radius); white-space:nowrap; }
      .geojson-btn:hover { background:var(--primary); color:#fff; }

      .group-body { display:none; border-top:1px solid var(--border); }
      .group-body.open { display:block; }

      .issue-list { list-style:none; margin:0; padding:0; }
      .issue-list li { padding:8px 16px; border-bottom:1px solid #f1f3f5; font-size:13px; }
      .issue-list li:last-child { border-bottom:none; }
      .issue-list li:hover { background:#f8f9fa; }
      .issue-list li mark { background:#fef08a; border-radius:2px; padding:0 1px; }

      .pager { display:flex; align-items:center; justify-content:center; gap:8px;
               padding:10px 16px; border-top:1px solid var(--border); }
      .pager button { padding:5px 12px; border:1px solid var(--border);
                      border-radius:var(--radius); background:var(--card-bg);
                      cursor:pointer; font-size:12px; font-family:var(--font); }
      .pager button:disabled { opacity:.4; cursor:default; }
      .pager button:not(:disabled):hover { border-color:var(--primary); color:var(--primary); }
      .pager .info { font-size:12px; color:var(--text-muted); }

      .empty { text-align:center; padding:48px 24px; color:var(--text-muted); }
      .empty .icon { font-size:48px; margin-bottom:12px; }

      @media (max-width:600px) {
        .toolbar { padding:10px 12px; }
        main { padding:12px; }
        .header { padding:12px 16px; }
      }
      """
    );
  }

  // ---- Header ----

  private void writeHeader(PrintWriter w) {
    w.println("<div class=\"header\">");
    if (!config.logoUrl().isBlank()) {
      w.printf("  <img src=\"%s\" alt=\"logo\">%n", esc(config.logoUrl()));
    }
    w.printf("  <h1>%s \u2014 Data Import Issues</h1>%n", esc(config.appName()));
    w.printf("  <span class=\"meta\">Generated %s</span>%n", Instant.now().toString());
    w.println("</div>");
  }

  // ---- Toolbar ----

  private void writeToolbar(PrintWriter w) {
    w.println("<div class=\"toolbar\">");
    w.println(
      "  <input type=\"search\" id=\"search\" placeholder=\"Filter by type name or message content\u2026\" autocomplete=\"off\">"
    );
    w.println("  <div class=\"sort-bar\">");
    w.println("    <span class=\"sort-label\">Sort:</span>");
    w.println(
      "    <button class=\"sort-btn active\" id=\"sort-alpha\" title=\"Sort alphabetically\">A\u2192Z</button>"
    );
    w.println(
      "    <button class=\"sort-btn\" id=\"sort-count\" title=\"Sort by number of issues\"># Issues</button>"
    );
    w.println("  </div>");
    w.println("  <div class=\"search-hint\" id=\"search-hint\"></div>");
    w.println("</div>");
    w.println("<div class=\"summary\" id=\"summary\"></div>");
  }

  // ---- Embedded issue data ----

  private void writeDataScript(PrintWriter w) {
    w.println("<script>");
    w.printf("const PAGE_SIZE = %d;%n", config.pageSize());
    w.println("const DATA = {");
    boolean first = true;
    for (var entry : issuesByType.entrySet()) {
      if (!first) {
        w.println(",");
      }
      first = false;
      String type = entry.getKey();
      boolean hasGeo = typesWithGeoJson.contains(type);
      w.printf("  %s: {%n", jsString(type));
      w.printf("    hasGeo: %s,%n", hasGeo);
      w.println("    messages: [");
      List<DataImportIssue> issues = entry.getValue();
      for (int i = 0; i < issues.size(); i++) {
        String msg = issues.get(i).getHTMLMessage();
        w.printf("      %s", jsString(msg));
        if (i < issues.size() - 1) {
          w.println(",");
        } else {
          w.println();
        }
      }
      w.println("    ]");
      w.print("  }");
    }
    w.println();
    w.println("};");
    w.println("</script>");
  }

  // ---- Application JS ----

  private void writeAppScript(PrintWriter w) {
    w.println("<script>");
    w.println(
      """
      (function() {
        const main = document.getElementById('main');
        const searchInput = document.getElementById('search');
        const summaryEl = document.getElementById('summary');
        const hintEl = document.getElementById('search-hint');

        let searchTerm = '';
        let sortBy = 'alpha';
        const pageState = {};

        // ---- Sort buttons ----

        document.getElementById('sort-alpha').addEventListener('click', () => {
          sortBy = 'alpha';
          document.getElementById('sort-alpha').classList.add('active');
          document.getElementById('sort-count').classList.remove('active');
          renderGroups();
        });
        document.getElementById('sort-count').addEventListener('click', () => {
          sortBy = 'count';
          document.getElementById('sort-count').classList.add('active');
          document.getElementById('sort-alpha').classList.remove('active');
          renderGroups();
        });

        // Index: lowercase plain-text of all messages for fast search
        const INDEX = {};
        for (const [type, data] of Object.entries(DATA)) {
          INDEX[type] = data.messages.map(m => m.replace(/<[^>]*>/g, '').toLowerCase());
        }

        // ---- Content search helpers ----

        function matchingIndices(type, term) {
          if (!term) return null;
          const idx = INDEX[type];
          const matches = [];
          for (let i = 0; i < idx.length; i++) {
            if (idx[i].includes(term)) matches.push(i);
          }
          return matches;
        }

        function highlight(html, term) {
          if (!term) return html;
          return html.replace(/(<[^>]*>)|([^<]+)/g, (match, tag, text) => {
            if (tag) return tag;
            const re = new RegExp('(' + term.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&') + ')', 'gi');
            return text.replace(re, '<mark>$1</mark>');
          });
        }

        // ---- Groups ----

        function getVisibleTypes() {
          const term = searchTerm.toLowerCase();
          return Object.entries(DATA)
            .filter(([type, data]) => {
              if (!term) return true;
              if (type.toLowerCase().includes(term)) return true;
              const idx = INDEX[type];
              for (let i = 0; i < idx.length; i++) {
                if (idx[i].includes(term)) return true;
              }
              return false;
            })
            .sort((a, b) =>
              sortBy === 'count'
                ? b[1].messages.length - a[1].messages.length
                : a[0].localeCompare(b[0])
            );
        }

        function renderGroups() {
          const visible = getVisibleTypes();
          const term = searchTerm.toLowerCase();

          let totalIssues = 0;
          let totalMatches = 0;
          for (const [type, data] of visible) {
            totalIssues += data.messages.length;
            if (term) {
              const m = matchingIndices(type, term);
              if (m) totalMatches += m.length;
            }
          }

          let summaryText = `Showing ${visible.length} issue type${visible.length !== 1 ? 's' : ''} with ${totalIssues.toLocaleString()} total issues`;
          if (term && totalMatches > 0) {
            summaryText += ` (${totalMatches.toLocaleString()} matching messages)`;
          }
          summaryEl.textContent = summaryText;
          hintEl.textContent = term
            ? 'Searching type names and message content. Types with matching messages auto-expand.'
            : '';

          if (visible.length === 0) {
            main.innerHTML = `<div class="empty"><div class="icon">&#x1F50D;</div>
              <p>No issue types match your filter.</p></div>`;
            return;
          }

          let html = '';
          for (const [type, data] of visible) {
            const geoLink = data.hasGeo
              ? `<a class="geojson-btn" href="./${type}.geojson" title="Download GeoJSON"
                    onclick="event.stopPropagation()">&#x1F4BE; GeoJSON</a>`
              : '';
            html += `<div class="group" data-type="${type}">
              <div class="group-header" data-type="${type}">
                <span class="arrow">&#x25B6;</span>
                <span class="type-name">${type}</span>
                <span class="count">${data.messages.length.toLocaleString()} issues</span>
                <div class="actions">
                  ${geoLink}
                  <button class="icon-btn" data-action="download" data-type="${type}"
                    onclick="event.stopPropagation()" title="Download issues as text file">&#x2B07; .txt</button>
                </div>
              </div>
              <div class="group-body" id="body-${type}"></div>
            </div>`;
          }
          main.innerHTML = html;

          main.querySelectorAll('.group-header').forEach(hdr => {
            hdr.addEventListener('click', () => toggleGroup(hdr.dataset.type));
          });
          main.querySelectorAll('[data-action="download"]').forEach(btn => {
            btn.addEventListener('click', () => downloadTxt(btn.dataset.type));
          });

          // Auto-expand types that matched on content (not type name)
          if (term) {
            for (const [type] of visible) {
              if (!type.toLowerCase().includes(term)) {
                toggleGroup(type);
              }
            }
          }
        }

        function toggleGroup(type) {
          const hdr = main.querySelector(`.group-header[data-type="${type}"]`);
          const body = document.getElementById('body-' + type);
          const isOpen = hdr.classList.toggle('open');
          body.classList.toggle('open', isOpen);
          if (isOpen) {
            pageState[type] = pageState[type] || 0;
            renderPage(type);
          }
        }

        // ---- Download as .txt ----

        function downloadTxt(type) {
          const data = DATA[type];
          const lines = data.messages.map(m => m.replace(/<[^>]*>/g, ''));
          const text =
            type + '\\n' + '='.repeat(type.length) + '\\n\\n' +
            lines.map((l, i) => (i + 1) + '. ' + l).join('\\n');
          const blob = new Blob([text], { type: 'text/plain' });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = type + '.txt';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          URL.revokeObjectURL(url);
        }

        // ---- Paginated issue list ----

        function renderPage(type) {
          const data = DATA[type];
          const term = searchTerm.toLowerCase();
          const page = pageState[type] || 0;

          let indices;
          if (term) {
            const mi = matchingIndices(type, term);
            indices = mi && mi.length > 0 ? mi : data.messages.map((_, i) => i);
          } else {
            indices = data.messages.map((_, i) => i);
          }

          const total = indices.length;
          const totalPages = Math.ceil(total / PAGE_SIZE);
          if (page >= totalPages) pageState[type] = Math.max(0, totalPages - 1);
          const p = pageState[type] || 0;
          const start = p * PAGE_SIZE;
          const end = Math.min(start + PAGE_SIZE, total);

          const body = document.getElementById('body-' + type);
          let html = '<ul class="issue-list">';
          for (let i = start; i < end; i++) {
            const msg = term
              ? highlight(data.messages[indices[i]], searchTerm)
              : data.messages[indices[i]];
            html += `<li>${msg}</li>`;
          }
          html += '</ul>';

          if (totalPages > 1) {
            html += `<div class="pager">
              <button ${p === 0 ? 'disabled' : ''} data-type="${type}" data-dir="-1">&#x25C0; Prev</button>
              <span class="info">Page ${p + 1} of ${totalPages}
                (${start + 1}\\u2013${end} of ${total.toLocaleString()}${term ? ' matches' : ''})</span>
              <button ${p >= totalPages - 1 ? 'disabled' : ''} data-type="${type}" data-dir="1">Next &#x25B6;</button>
            </div>`;
          }

          body.innerHTML = html;
          body.querySelectorAll('.pager button').forEach(btn => {
            btn.addEventListener('click', e => {
              e.stopPropagation();
              pageState[type] = (pageState[type] || 0) + parseInt(btn.dataset.dir);
              renderPage(type);
            });
          });
        }

        // ---- Search with debounce ----

        let searchTimeout;
        searchInput.addEventListener('input', () => {
          clearTimeout(searchTimeout);
          searchTimeout = setTimeout(() => {
            searchTerm = searchInput.value.trim();
            for (const k in pageState) pageState[k] = 0;
            renderGroups();
          }, 300);
        });

        // ---- Init ----
        renderGroups();
      })();
      """
    );
    w.println("</script>");
  }

  // ---- Utilities ----

  /**
   * Escape a string for safe use in an HTML attribute value (double-quoted).
   */
  private static String esc(String s) {
    if (s == null) {
      return "";
    }
    return s
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;");
  }

  /**
   * Encode a Java string as a JavaScript string literal (including the surrounding quotes).
   * Escapes characters that would otherwise break out of a JS string or a surrounding
   * {@code <script>} block, including the Unicode line terminators U+2028 and U+2029.
   */
  private static String jsString(String s) {
    if (s == null) {
      return "\"\"";
    }
    return (
      "\"" +
      s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
        .replace("<", "\\x3c")
        .replace(">", "\\x3e") +
      "\""
    );
  }
}
