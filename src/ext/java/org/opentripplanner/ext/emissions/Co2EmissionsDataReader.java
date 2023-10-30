package org.opentripplanner.ext.emissions;

import com.csvreader.CsvReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This class handles reading the CO₂ emissions data from the files in the GTFS package
 * and saving it in a map.
 */
@Sandbox
public class Co2EmissionsDataReader {

  private final DataImportIssueStore issueStore;

  public Co2EmissionsDataReader(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  /**
   * Read files in a GTFS directory.
   * @param filePath
   * @return emissions data
   */
  public Map<FeedScopedId, Double> readGtfs(String filePath) {
    String feedId = "";
    Map<FeedScopedId, Double> emissionsData = new HashMap<>();
    try (InputStream feedInfoStream = new FileInputStream(filePath + "/feed_info.txt")) {
      feedId = readFeedId(feedInfoStream);
    } catch (IOException e) {
      issueStore.add("InvalidData", "Reading feed_info.txt failed.");
    }
    try (InputStream stream = new FileInputStream(filePath + "/emissions.txt")) {
      emissionsData = readEmissions(stream, feedId);
    } catch (IOException e) {
      issueStore.add("InvalidData", "Reading emissions.txt failed.");
    }
    return emissionsData;
  }

  /**
   * Read files in a GTFS zip file.
   * @param filePath
   * @return emissions data
   */
  public Map<FeedScopedId, Double> readGtfsZip(String filePath) {
    try {
      ZipFile zipFile = new ZipFile(new File(filePath), ZipFile.OPEN_READ);
      String feedId = readFeedId(zipFile.getInputStream(zipFile.getEntry("feed_info.txt")));
      InputStream stream = zipFile.getInputStream(zipFile.getEntry("emissions.txt"));
      Map<FeedScopedId, Double> emissionsData = readEmissions(stream, feedId);
      zipFile.close();
      return emissionsData;
    } catch (IOException e) {
      issueStore.add("InvalidData", "Reading emissions data failed.");
    }
    return null;
  }

  private Map<FeedScopedId, Double> readEmissions(InputStream stream, String feedId)
    throws IOException {
    Map<FeedScopedId, Double> emissionsData = new HashMap<>();
    CsvReader reader = new CsvReader(stream, StandardCharsets.UTF_8);
    reader.readHeaders();

    while (reader.readRecord()) {
      String routeId = reader.get("route_id");
      String avgCo2PerVehiclePerKmString = reader.get("avg_co2_per_vehicle_per_km");
      String avgPassengerCountString = reader.get("avg_passenger_count");

      if (avgCo2PerVehiclePerKmString.isEmpty()) {
        issueStore.add(
          "InvalidData",
          "Value for avg_co2_per_vehicle_per_km is missing in the Emissions.txt for route %s",
          routeId
        );
      }
      if (avgPassengerCountString.isEmpty()) {
        issueStore.add(
          "InvalidData",
          "Value for avg_passenger_count is missing in the Emissions.txt for route %s",
          routeId
        );
      }

      Double avgCo2PerVehiclePerMeter = Double.parseDouble(avgCo2PerVehiclePerKmString) / 1000;
      Double avgPassengerCount = Double.parseDouble(reader.get("avg_passenger_count"));
      Optional<Double> emissions = calculateEmissionsPerPassengerPerMeter(
        routeId,
        avgCo2PerVehiclePerMeter,
        avgPassengerCount
      );
      if (emissions.isPresent()) {
        emissionsData.put(new FeedScopedId(feedId, routeId), emissions.get());
      }
    }
    return emissionsData;
  }

  private String readFeedId(InputStream stream) {
    try {
      CsvReader reader = new CsvReader(stream, StandardCharsets.UTF_8);
      reader.readHeaders();
      reader.readRecord();
      return reader.get("feed_id");
    } catch (IOException e) {
      issueStore.add("InvalidData", "Reading emissions data failed.");
      throw new RuntimeException(e);
    }
  }

  private Optional<Double> calculateEmissionsPerPassengerPerMeter(
    String routeId,
    double avgCo2PerVehiclePerMeter,
    double avgPassengerCount
  ) {
    if (avgCo2PerVehiclePerMeter == 0) {
      // Passenger number is irrelevant when emissions is 0.
      return Optional.of(avgCo2PerVehiclePerMeter);
    }
    if (avgPassengerCount <= 0 || avgCo2PerVehiclePerMeter < 0) {
      issueStore.add(
        "InvalidData",
        "avgPassengerCount is 0 or less, but avgCo2PerVehiclePerMeter is nonzero or avgCo2PerVehiclePerMeter is negative for route %s",
        routeId
      );
      return Optional.empty();
    }
    return Optional.of(avgCo2PerVehiclePerMeter / avgPassengerCount);
  }
}
