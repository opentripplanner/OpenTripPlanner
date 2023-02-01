package org.opentripplanner.graph_builder.issue.report;

import com.google.common.collect.Multiset;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HTMLWriter {

  private static final Logger LOG = LoggerFactory.getLogger(HTMLWriter.class);
  private final DataSource target;
  private final Collection<DataImportIssue> issues;
  private final String issueTypeName;

  HTMLWriter(CompositeDataSource reportDirectory, String key, Collection<DataImportIssue> issues) {
    LOG.debug("Creating file: {}", key);
    this.target = reportDirectory.entry(key + ".html");
    this.issues = issues;
    this.issueTypeName = key;
  }

  HTMLWriter(CompositeDataSource reportDirectory, String filename) {
    LOG.debug("Creating index file: {}", filename);
    this.target = reportDirectory.entry(filename + ".html");
    this.issues = null;
    this.issueTypeName = filename;
  }

  void writeFile(Iterable<Multiset.Entry<String>> classes) {
    try (PrintWriter out = new PrintWriter(target.asOutputStream(), true, StandardCharsets.UTF_8)) {
      printPrelude(out);

      out.println(
        String.format("<h1>OpenTripPlanner data import issue log for %s</h1>", issueTypeName)
      );
      out.println("<h2>Graph report for <em>graph.obj</em></h2>");

      printCategoryLinks(classes, out);

      if (issues != null) {
        writeIssues(out);
      }

      printPostamble(out);
    }
  }

  private void printPrelude(PrintWriter out) {
    out.println("<html><head><title>Graph report for OTP Graph</title>");
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
  private void printCategoryLinks(Iterable<Multiset.Entry<String>> classes, PrintWriter out) {
    out.println("<p>");
    for (Multiset.Entry<String> htmlIssueType : classes) {
      String label_name = htmlIssueType.getElement();
      String label;
      int currentCount = 1;
      //it needs to add link to every file even if they are split
      while (currentCount <= htmlIssueType.getCount()) {
        label = label_name + currentCount;
        if (label.equals(issueTypeName)) {
          out.printf(
            "<button class='pure-button pure-button-disabled button-%s' style='background-color: %s;'>%s</button>%n",
            label_name.toLowerCase(),
            IssueColors.rgb(label_name),
            label
          );
        } else {
          out.printf(
            "<a class='pure-button button-%s' href=\"%s.html\" style='background-color: %s;'>%s</a>%n",
            label_name.toLowerCase(),
            label,
            IssueColors.rgb(label_name),
            label
          );
        }
        currentCount++;
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
