package org.opentripplanner.graph_builder.issue.report;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HTMLWriter {

  private static final Logger LOG = LoggerFactory.getLogger(HTMLWriter.class);
  private final DataSource target;
  private final Collection<DataImportIssue> issues;
  private final BucketKey bucketKey;
  private final boolean addGeoJSONLink;
  private final List<BucketKey> keys;

  HTMLWriter(
    CompositeDataSource reportDirectory,
    Bucket bucket,
    List<BucketKey> keys,
    boolean addGeoJSONLink
  ) {
    LOG.debug("Creating file: {}", bucket.key().key());
    this.bucketKey = bucket.key();
    this.addGeoJSONLink = addGeoJSONLink;
    this.target = reportDirectory.entry(bucketKey.key() + ".html");
    this.keys = keys;
    this.issues = bucket.issues();
  }

  HTMLWriter(CompositeDataSource reportDirectory, String filename, List<BucketKey> keys) {
    LOG.debug("Creating index file: {}", filename);
    this.target = reportDirectory.entry(filename + ".html");
    this.keys = keys;
    this.issues = null;
    this.bucketKey = new BucketKey(filename, null);
    this.addGeoJSONLink = false;
  }

  void writeFile() {
    try (PrintWriter out = new PrintWriter(target.asOutputStream(), true, StandardCharsets.UTF_8)) {
      printPrelude(out);

      String title = bucketKey.label();
      out.println(String.format("<h1>OpenTripPlanner data import issue log %s</h1>", title));
      out.println("<h2>Graph report for <em>graph.obj</em></h2>");

      printCategoryLinks(out);

      if (issues != null) {
        if (addGeoJSONLink) {
          out.printf(
            "<a class=\"pure-button\" href=\"./%s.geojson\">Open issues in a GeoJSON file</a>",
            bucketKey.key()
          );
        }

        writeIssues(out);
      }

      printPostamble(out);
    }
  }

  private void printPrelude(PrintWriter out) {
    out.println("<!DOCTYPE html>");
    out.println("<html lang=\"en\"><head>");
    out.println("<title>Graph report for OTP Graph</title>");
    out.println("<meta charset=\"utf-8\">");
    out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
    out.println(
      "<link " +
      "rel=\"stylesheet\" " +
      "href=\"https://cdn.jsdelivr.net/npm/purecss@3.0.0/build/pure-min.css\" " +
      "integrity=\"sha384-X38yfunGUhNzHpBaEBsWLO+A0HDYOQi8ufWDkZ0k9e0eXz/tH3II7uKZ9msv++Ls\" " +
      "crossorigin=\"anonymous\">"
    );
    out.println("<style>.pure-button{margin-bottom:4px;}</style>");
    out.println("</head><body>");
  }

  /**
   * Adds links to the other HTML files.
   */
  private void printCategoryLinks(PrintWriter out) {
    out.println("<p>");
    for (BucketKey linkKey : keys) {
      String linkIssueType = linkKey.issueType();
      String label = linkKey.label();
      if (linkKey.equals(bucketKey)) {
        out.printf(
          "<button class='pure-button pure-button-disabled button-%s' style='background-color: %s;'>%s</button>%n",
          linkIssueType.toLowerCase(),
          IssueColors.rgb(linkIssueType),
          label
        );
      } else {
        out.printf(
          "<a class='pure-button button-%s' href=\"%s.html\" style='background-color: %s;'>%s</a>%n",
          linkIssueType.toLowerCase(),
          linkKey.key(),
          IssueColors.rgb(linkIssueType),
          label
        );
      }
    }
    out.println("</p>");
  }

  /**
   * Writes issues as LI html elements
   */
  private void writeIssues(PrintWriter out) {
    out.println("<ul id=\"log\">");
    for (DataImportIssue it : issues) {
      out.printf("<li>%s</li>%n", it.getHTMLMessage());
    }
    out.println("</ul>");
  }

  private static void printPostamble(PrintWriter out) {
    out.println("</body></html>");
  }
}
