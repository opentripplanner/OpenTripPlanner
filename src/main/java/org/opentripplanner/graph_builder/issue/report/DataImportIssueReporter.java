package org.opentripplanner.graph_builder.issue.report;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.datastore.api.CompositeDataSource;
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
public class DataImportIssueReporter implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(DataImportIssueReporter.class);

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

  private final DataImportIssueStore issueStore;

  public DataImportIssueReporter(
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
      LOG.info("Creating data import issue log");

      //Groups issues according to issue type
      Map<String, List<DataImportIssue>> sortedIssuesByType = issueStore
        .listIssues()
        .stream()
        .collect(Collectors.groupingBy(DataImportIssue::getType));

      // Sort each issue type by priority
      for (Map.Entry<String, List<DataImportIssue>> entry : sortedIssuesByType.entrySet()) {
        addIssues(
          entry.getKey(),
          entry
            .getValue()
            .stream()
            .sorted(Comparator.comparing(DataImportIssue::getPriority))
            .collect(
              Collectors.collectingAndThen(
                Collectors.toList(),
                l -> {
                  Collections.reverse(l);
                  return l;
                }
              )
            )
        );
      }

      Iterable<Multiset.Entry<String>> sortedIssueTypes = ImmutableSortedMultiset
        .copyOf(issueTypeOccurrences)
        .entrySet();

      //Actual writing to the file is made here since
      // this is the first place where actual number of files is known (because it depends on
      // the issue count)
      for (HTMLWriter writer : writers) {
        writer.writeFile(sortedIssueTypes);
      }

      try {
        HTMLWriter indexFileWriter = new HTMLWriter(reportDirectory, "index");
        indexFileWriter.writeFile(sortedIssueTypes);
      } catch (Exception e) {
        LOG.error("Index file couldn't be created:{}", e.getMessage());
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
   * Delete report if it exists, and return true if successful. Return {@code false} if the {@code
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
  private void addIssues(String issueTypeName, List<DataImportIssue> issues) {
    HTMLWriter file_writer;
    if (issues.size() > 1.2 * maxNumberOfIssuesPerFile) {
      LOG.debug("Number of issues is very large. Splitting: {}", issueTypeName);
      List<List<DataImportIssue>> partitions = Lists.partition(issues, maxNumberOfIssuesPerFile);
      for (List<DataImportIssue> partition : partitions) {
        issueTypeOccurrences.add(issueTypeName);
        int labelCount = issueTypeOccurrences.count(issueTypeName);
        file_writer = new HTMLWriter(reportDirectory, issueTypeName + labelCount, partition);
        writers.add(file_writer);
      }
    } else {
      issueTypeOccurrences.add(issueTypeName);
      int labelCount = issueTypeOccurrences.count(issueTypeName);
      file_writer = new HTMLWriter(reportDirectory, issueTypeName + labelCount, issues);
      writers.add(file_writer);
    }
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
}
