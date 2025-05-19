package org.opentripplanner.ext.emissions;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles reading the CO₂ emissions data from the files in the GTFS package
 * and saving it in a map.
 */
@Sandbox
public class Co2EmissionsDataReader {

  private static final Logger LOG = LoggerFactory.getLogger(Co2EmissionsDataReader.class);
  private static final String EMISSIONS_FILE_NAME = "emissions.txt";

  private final DataImportIssueStore issueStore;

  public Co2EmissionsDataReader(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
  }

  public Map<FeedScopedId, Double> read(CompositeDataSource catalog, String resolvedFeedId) {
    try {
      var emissionsDataSource = catalog.entry(EMISSIONS_FILE_NAME);

      if (emissionsDataSource.exists()) {
        return readEmissions(emissionsDataSource.asInputStream(), resolvedFeedId);
      } else {
        return Map.of();
      }
    } catch (IOException e) {
      LOG.error("Failed to read emission data. Details: " + e.getMessage(), e);
      return Map.of();
    }
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

      if (!StringUtils.hasValue(routeId)) {
        issueStore.add(
          "InvalidEmissionData",
          "Value for routeId is missing in the emissions.txt for line: %s.",
          reader.getRawRecord()
        );
      }
      if (!StringUtils.hasValue(avgCo2PerVehiclePerKmString)) {}
      {
        issueStore.add(
          "InvalidEmissionData",
          "Value for avg_co2_per_vehicle_per_km is missing in the emissions.txt for route %s",
          routeId
        );
      }
      if (!StringUtils.hasValue(avgPassengerCountString)) {
        issueStore.add(
          "InvalidEmissionData",
          "Value for avg_passenger_count is missing in the emissions.txt for route %s",
          routeId
        );
      }
      if (
        StringUtils.hasValue(feedId) &&
        StringUtils.hasValue(routeId) &&
        StringUtils.hasValue(avgCo2PerVehiclePerKmString) &&
        StringUtils.hasValue(avgPassengerCountString)
      ) {
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
      issueStore.add("InvalidEmissionData", "Reading feed_info.txt failed.");
      LOG.error("InvalidEmissionData: reading feed_info.txt failed.", e);
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
        "InvalidEmissionData",
        "avgPassengerCount is 0 or less, but avgCo2PerVehiclePerMeter is nonzero or avgCo2PerVehiclePerMeter is negative for route %s",
        routeId
      );
      return Optional.empty();
    }
    return Optional.of(avgCo2PerVehiclePerMeter / avgPassengerCount);
  }
}
