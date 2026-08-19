package org.opentripplanner.graph_builder.issue.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
 * Generates a single self-contained HTML report of all data import issues, plus individual GeoJSON
 * files for issue types that carry geometry.
 * <p>
 * The report is written as a single {@code index.html} file containing all issue data as embedded
 * JSON with inline CSS and JavaScript for interactive filtering, search, and pagination. This
 * replaces the previous multi-file approach where large issue types were split across many HTML
 * files.
 */
public class DataImportIssueReporter implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(DataImportIssueReporter.class);

  private final CompositeDataSource reportDirectory;
  private final DataImportIssueStore issueStore;
  private final ReportConfig config;

  public DataImportIssueReporter(
    DataImportIssueStore issueStore,
    CompositeDataSource reportDirectory,
    ReportConfig config
  ) {
    this.issueStore = issueStore;
    this.reportDirectory = reportDirectory;
    this.config = config;
  }

  /** Convenience constructor using default white-label config. */
  public DataImportIssueReporter(
    DataImportIssueStore issueStore,
    CompositeDataSource reportDirectory
  ) {
    this(issueStore, reportDirectory, ReportConfig.DEFAULT);
  }

  @Override
  public void buildGraph() {
    try {
      if (!deleteReportDirectoryAndContent()) {
        return;
      }

      List<DataImportIssue> allIssues = issueStore.listIssues();
      LOG.info("Writing data import issue report ({} issues)…", allIssues.size());

      // Group by type, sorted alphabetically, each group sorted by priority descending
      Map<String, List<DataImportIssue>> issuesByType = allIssues
        .stream()
        .sorted(Comparator.comparing(DataImportIssue::getType))
        .collect(
          Collectors.groupingBy(DataImportIssue::getType, LinkedHashMap::new, Collectors.toList())
        );

      issuesByType.replaceAll((type, issues) ->
        issues
          .stream()
          .sorted(Comparator.comparing(DataImportIssue::getPriority, Comparator.reverseOrder()))
          .toList()
      );

      // Write GeoJSON files and track which types have geometry
      List<String> typesWithGeoJson = writeGeoJsonFiles(issuesByType);

      // Write the single-page HTML report
      new SinglePageReportWriter(
        reportDirectory,
        config,
        issuesByType,
        typesWithGeoJson
      ).writeFile();

      LOG.info("Data import issue report written to {}", reportDirectory.path());
    } catch (Exception e) {
      // If the issue report fails (e.g. remote storage problem) log the error and continue.
      // Preventing OTP from saving the Graph has much bigger consequences than a missing report.
      LOG.error("OTP failed to save issue report!", e);
    } finally {
      closeReportDirectory();
    }
  }

  private List<String> writeGeoJsonFiles(Map<String, List<DataImportIssue>> issuesByType) {
    List<String> typesWithGeoJson = new ArrayList<>();
    for (var entry : issuesByType.entrySet()) {
      String type = entry.getKey();
      List<DataImportIssue> issues = entry.getValue();

      var withGeometry = issues
        .stream()
        .filter(i -> i.getGeometry() != null)
        .toList();
      if (withGeometry.isEmpty()) {
        continue;
      }
      var bucket = new Bucket(new BucketKey(type, null), withGeometry);
      boolean written = new GeoJsonWriter(reportDirectory, bucket).writeFile();
      if (written) {
        typesWithGeoJson.add(type);
      }
    }
    return typesWithGeoJson;
  }

  /**
   * Delete report directory if it exists. Returns {@code false} if the directory is {@code null}
   * or cannot be deleted (report generation will be skipped).
   */
  private boolean deleteReportDirectoryAndContent() {
    if (reportDirectory == null) {
      LOG.error("Saving folder is empty!");
      return false;
    }
    if (reportDirectory.exists()) {
      try {
        reportDirectory.delete();
      } catch (Exception e) {
        LOG.error(
          "Failed to clean HTML report directory: {}. Report won't be generated!",
          reportDirectory.path(),
          e
        );
        return false;
      }
    }
    return true;
  }

  private void closeReportDirectory() {
    try {
      reportDirectory.close();
    } catch (IOException e) {
      LOG.warn("Failed to close report directory: {}", reportDirectory.path(), e);
    }
  }
}
