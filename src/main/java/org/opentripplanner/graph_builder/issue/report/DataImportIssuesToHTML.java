package org.opentripplanner.graph_builder.issue.report;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class generates a nice HTML graph import data issue report.
 * <p>
 * They are created with the help of getHTMLMessage function in {@link DataImportIssue} derived
 * classes.
 *
 * @author mabu
 */
public class DataImportIssuesToHTML implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(DataImportIssuesToHTML.class);

  //Path to output folder
  private final CompositeDataSource reportDirectory;

  //If there are more then this number of issues the report are split into multiple files
  //This is because browsers aren't made for giant HTML files which can be made with 500k lines
  private final int maxNumberOfIssuesPerFile;

  //This counts all occurrences of HTML issue type
  //If one issue type is split into two files it has two entries in this Multiset
  //IT is used to show numbers in HTML files name and links
  private final Multiset<String> issueTypeOccurrences = HashMultiset.create();

  //List of writers which are used for actual writing issues to HTML
  private final List<HTMLWriter> writers = new ArrayList<>();

  //Key is classname, value is issue message
  //Multimap because there are multiple issues for each classname
  private final Multimap<String, String> issues = ArrayListMultimap.create();

  private final DataImportIssueStore issueStore;

  public DataImportIssuesToHTML(
    DataImportIssueStore issueStore,
    CompositeDataSource reportDirectory,
    int maxNumberOfIssuesPerFile
  ) {
    this.issueStore = issueStore;
    this.reportDirectory = reportDirectory;
    this.maxNumberOfIssuesPerFile = maxNumberOfIssuesPerFile;
  }

  @Override
  public void buildGraph() {
    try {
      // Delete all files in the report directory if it exist
      if (!deleteReportDirectoryAndContent()) {
        return;
      }

      //Groups issues in multimap according to issue type
      for (DataImportIssue it : issueStore.listIssues()) {
        //writer.println("<p>" + it.getHTMLMessage() + "</p>");
        // writer.println("<small>" + it.getTypeName()+"</small>");
        addIssue(it);
      }
      LOG.info("Creating data import issue log");

      //Creates list of HTML writers. Each writer has whole class of HTML issues
      //Or multiple HTML writers can have parts of one class of HTML issues if number
      // of issues is larger than maxNumberOfIssuesPerFile.
      for (Map.Entry<String, Collection<String>> entry : issues.asMap().entrySet()) {
        List<String> issueList;
        if (entry.getValue() instanceof List) {
          issueList = (List<String>) entry.getValue();
        } else {
          issueList = new ArrayList<>(entry.getValue());
        }
        addIssues(entry.getKey(), issueList);
      }

      //Actual writing to the file is made here since
      // this is the first place where actual number of files is known (because it depends on
      // the issue count)
      for (HTMLWriter writer : writers) {
        writer.writeFile(issueTypeOccurrences, false);
      }

      try {
        HTMLWriter indexFileWriter = new HTMLWriter("index", (Multimap<String, String>) null);
        indexFileWriter.writeFile(issueTypeOccurrences, true);
      } catch (Exception e) {
        LOG.error("Index file coudn't be created:{}", e.getMessage());
      }

      LOG.info("Data import issue logs are in {}", reportDirectory.path());
    } catch (Exception e) {
      // If the issue report fails due to a remote storage or network problem, then we log
      // the error an CONTINUE with the graph build process. Preventing OTP from saving the
      // Graph might have much bigger consequences than just failing to save the issue report.
      LOG.error("OTP failed to save issue report!", e);
    } finally {
      closeReportDirectory();
    }
  }

  @Override
  public void checkInputs() {}

  /**
   * Delete report if it exist, and return true if successful. Return {@code false} if the {@code
   * reportDirectory} is {@code null} or the directory can NOT be deleted.
   */
  private boolean deleteReportDirectoryAndContent() {
    if (reportDirectory == null) {
      LOG.error("Saving folder is empty!");
      return false;
    }
    if (reportDirectory.exists()) {
      //Removes all files from report directory
      try {
        reportDirectory.delete();
      } catch (Exception e) {
        LOG.error(
          "Failed to clean HTML report directory: " +
          reportDirectory.path() +
          ". HTML report won't be generated!",
          e
        );
        return false;
      }
    }
    // No need to create directories here, because the 'reportDirectory' is responsible for
    // creating paths (it they don´t exist) when saving files.
    return true;
  }

  /**
   * Creates file with given type of issues
   * <p>
   * If number of issues is larger then 'maxNumberOfIssuesPerFile' multiple files are generated. And
   * named issueClassName1,2,3 etc.
   *
   * @param issueTypeName name of import data issue class and then also filename
   * @param issues        list of all import data issue with that class
   */
  private void addIssues(String issueTypeName, List<String> issues) {
    HTMLWriter file_writer;
    if (issues.size() > 1.2 * maxNumberOfIssuesPerFile) {
      LOG.debug("Number of issues is very large. Splitting: {}", issueTypeName);
      List<List<String>> partitions = Lists.partition(issues, maxNumberOfIssuesPerFile);
      for (List<String> partition : partitions) {
        issueTypeOccurrences.add(issueTypeName);
        int labelCount = issueTypeOccurrences.count(issueTypeName);
        file_writer = new HTMLWriter(issueTypeName + labelCount, partition);
        writers.add(file_writer);
      }
    } else {
      issueTypeOccurrences.add(issueTypeName);
      int labelCount = issueTypeOccurrences.count(issueTypeName);
      file_writer = new HTMLWriter(issueTypeName + labelCount, issues);
      writers.add(file_writer);
    }
  }

  /**
   * Groups issues according to issue type, using the classname as type name.
   * <p>
   * All issues are saved together in multimap where key is issue classname and values are list of
   * issue with that class
   */
  private void addIssue(DataImportIssue issue) {
    issues.put(issue.getType(), issue.getHTMLMessage());
  }

  private void closeReportDirectory() {
    try {
      reportDirectory.close();
    } catch (IOException e) {
      LOG.warn(
        "Failed to close report directory: {}, details: {}. ",
        reportDirectory.path(),
        e.getLocalizedMessage(),
        e
      );
    }
  }

  class HTMLWriter {

    private final DataSource target;

    private final Multimap<String, String> writerIssues;

    private final String issueTypeName;

    HTMLWriter(String key, Collection<String> issues) {
      LOG.debug("Making file: {}", key);
      this.target = reportDirectory.entry(key + ".html");
      this.writerIssues = ArrayListMultimap.create();
      this.writerIssues.putAll(key, issues);
      this.issueTypeName = key;
    }

    HTMLWriter(String filename, Multimap<String, String> curMap) {
      LOG.debug("Making file: {}", filename);
      this.target = reportDirectory.entry(filename + ".html");
      this.writerIssues = curMap;
      this.issueTypeName = filename;
    }

    private void writeFile(Multiset<String> classes, boolean isIndexFile) {
      try (
        PrintWriter out = new PrintWriter(target.asOutputStream(), true, StandardCharsets.UTF_8)
      ) {
        out.println("<html><head><title>Graph report for OTP Graph</title>");
        out.println("\t<meta charset=\"utf-8\">");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("<script src='http://code.jquery.com/jquery-1.11.1.js'></script>");
        out.println(
          "<link rel='stylesheet' href='http://yui.yahooapis.com/pure/0.5.0/pure-min.css'>"
        );
        String css =
          "\t\t<style>\n" +
          "\n" +
          "\t\t\tbutton.pure-button {\n" +
          "\t\t\t\tmargin:5px;\n" +
          "\t\t\t}\n" +
          "\n" +
          "\t\t\tspan.pure-button {\n" +
          "\t\t\t\tcursor:default;\n" +
          "\t\t\t}\n" +
          "\n" +
          "\t\t\t.button-graphwide,\n" +
          "\t\t\t.button-parkandrideunlinked,\n" +
          "\t\t\t.button-graphconnectivity,\n" +
          "\t\t\t.button-turnrestrictionbad\t{\n" +
          "\t\t\t\tcolor:white;\n" +
          "\t\t\t\ttext-shadow: 0 1px 1px rgba(0, 0, 0, 0.2);\n" +
          "\t\t\t}\n" +
          "\n" +
          "\t\t\t.button-graphwide {\n" +
          "\t\t\t\tbackground: rgb(28, 184, 65); /* this is a green */\n" +
          "\t\t\t}\n" +
          "\n" +
          "\t\t\t.button-parkandrideunlinked {\n" +
          "\t\t\t\tbackground: rgb(202, 60, 60); /* this is a maroon */\n" +
          "\t\t\t}\n" +
          "\n" +
          "\t\t\t.button-graphconnectivity{\n" +
          "\t\t\t\tbackground: rgb(223, 117, 20); /* this is an orange */\n" +
          "\t\t\t}\n" +
          "\n" +
          "\t\t\t.button-turnrestrictionbad {\n" +
          "\t\t\t\tbackground: rgb(66, 184, 221); /* this is a light blue */\n" +
          "\t\t\t}\n" +
          "\n" +
          "\t\t</style>\n" +
          "";
        out.println(css);
        out.println("</head><body>");
        out.println(
          String.format("<h1>OpenTripPlanner data import issue log for %s</h1>", issueTypeName)
        );
        out.println("<h2>Graph report for <em>graph.obj</em></h2>");
        out.println("<p>");
        //adds links to the other HTML files
        for (Multiset.Entry<String> htmlIssueType : classes.entrySet()) {
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
        if (!isIndexFile) {
          out.println("<ul id=\"log\">");
          writeIssues(out);
          out.println("</ul>");
        }

        out.println("</body></html>");
      }
    }

    /**
     * Writes issues as LI html elements
     */
    private void writeIssues(PrintWriter out) {
      String FMT = "<li>%s</li>";
      for (Map.Entry<String, String> it : writerIssues.entries()) {
        out.printf(FMT, it.getValue());
      }
    }
  }
}
