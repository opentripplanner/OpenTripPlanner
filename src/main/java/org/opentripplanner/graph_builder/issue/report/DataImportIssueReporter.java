package org.opentripplanner.graph_builder.issue.report;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.framework.logging.ProgressTracker;
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
      // Delete all files in the report directory if it exists
      if (!deleteReportDirectoryAndContent()) {
        return;
      }
      List<Bucket> buckets = partitionIssues(issueStore.listIssues(), maxNumberOfIssuesPerFile);
      List<BucketKey> keys = buckets.stream().map(Bucket::key).sorted().toList();

      var progress = ProgressTracker.track("Creating data import issue report", 50, buckets.size());

      LOG.info(progress.startMessage());

      for (Bucket bucket : buckets) {
        boolean addGeoJSONLink = new GeoJsonWriter(reportDirectory, bucket).writeFile();
        new HTMLWriter(reportDirectory, bucket, keys, addGeoJSONLink).writeFile();
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      }

      try {
        HTMLWriter indexFileWriter = new HTMLWriter(reportDirectory, "index", keys);
        indexFileWriter.writeFile();
      } catch (Exception e) {
        LOG.error("Index file couldn't be created:{}", e.getMessage());
      }
      LOG.info(progress.completeMessage());
      LOG.info("Data import issue report is in {}", reportDirectory.path());
    } catch (Exception e) {
      // If the issue report fails due to a remote storage or network problem, then we log
      // the error an CONTINUE with the graph build process. Preventing OTP from saving the
      // Graph might have much bigger consequences than just failing to save the issue report.
      LOG.error("OTP failed to save issue report!", e);
    } finally {
      closeReportDirectory();
    }
  }

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
   * Creates buckets, where each bucket has only a single issue type and max approximately
   * {@link this#maxNumberOfIssuesPerFile} issues
   */
  static List<Bucket> partitionIssues(List<DataImportIssue> issues, int maxNumberOfIssuesPerFile) {
    //Groups issues according to issue type
    Map<String, List<DataImportIssue>> issuesByType = issues
      .stream()
      .collect(Collectors.groupingBy(DataImportIssue::getType));

    List<Bucket> buckets = new ArrayList<>();

    for (Map.Entry<String, List<DataImportIssue>> entry : issuesByType.entrySet()) {
      var key = entry.getKey();

      // Sort each issue type by priority
      var sortedIssues = entry
        .getValue()
        .stream()
        .sorted(Comparator.comparing(DataImportIssue::getPriority, Comparator.reverseOrder()))
        .toList();

      // Split the issues to buckets if needed
      if (sortedIssues.size() > 1.2 * maxNumberOfIssuesPerFile) {
        List<List<DataImportIssue>> partitions = Lists.partition(
          sortedIssues,
          maxNumberOfIssuesPerFile
        );
        for (int i = 0; i < partitions.size(); i++) {
          buckets.add(new Bucket(new BucketKey(key, i + 1), partitions.get(i)));
        }
      } else {
        buckets.add(new Bucket(new BucketKey(key, null), sortedIssues));
      }
    }

    return buckets;
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
