package org.opentripplanner.ext.emissions;

import com.csvreader.CsvReader;
import com.esotericsoftware.minlog.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Co2EmissionsDataReader {

  private static final Logger LOG = LoggerFactory.getLogger(Co2EmissionsDataReader.class);
  private Map<FeedScopedId, Double> emissionsData = new HashMap<>();

  public Map<FeedScopedId, Double> readGtfs(String filePath) {
    try {
      InputStream feedInfoStream = new FileInputStream(filePath + "/feed_info.txt");
      String feedId = readFeedId(feedInfoStream);
      feedInfoStream.close();

      InputStream stream = new FileInputStream(filePath + "/emissions.txt");
      readEmissions(stream, feedId);
      stream.close();
      return this.emissionsData;
    } catch (IOException e) {
      LOG.error("Reading emissions data failed.", e);
    }
    return null;
  }

  public Map<FeedScopedId, Double> readGtfsZip(String filePath) {
    try {
      ZipFile zipFile = new ZipFile(new File(filePath), ZipFile.OPEN_READ);
      String feedId = readFeedId(zipFile.getInputStream(zipFile.getEntry("feed_info.txt")));
      InputStream stream = zipFile.getInputStream(zipFile.getEntry("emissions.txt"));
      readEmissions(stream, feedId);
      zipFile.close();
      return this.emissionsData;
    } catch (IOException e) {
      LOG.error("Reading emissions data failed.", e);
    }
    return null;
  }

  private void readEmissions(InputStream stream, String feedId) throws IOException {
    CsvReader reader = new CsvReader(stream, StandardCharsets.UTF_8);
    reader.readHeaders();

    while (reader.readRecord()) {
      String routeId = reader.get("route_id");
      String avgCo2PerVehiclePerKmString = reader.get("avg_co2_per_vehicle_per_km");
      String avgPassengerCountString = reader.get("avg_passenger_count");

      if (avgCo2PerVehiclePerKmString.isEmpty()) {
        LOG.error("Value for avg_co2_per_vehicle_per_km is missing in the Emissions.txt");
      }
      if (avgPassengerCountString.isEmpty()) {
        LOG.error("Value for avg_passenger_count is missing in the Emissions.txt");
      }

      Double avgCo2PerVehiclePerMeter = Double.parseDouble(avgCo2PerVehiclePerKmString) / 1000;
      Double avgPassengerCount = Double.parseDouble(reader.get("avg_passenger_count"));
      Optional<Double> emissions = calculateEmissionsPerPassengerPerMeter(
        routeId,
        avgCo2PerVehiclePerMeter,
        avgPassengerCount
      );
      if (emissions.isPresent()) {
        this.emissionsData.put(new FeedScopedId(feedId, routeId), emissions.get());
      }
    }
  }

  private String readFeedId(InputStream stream) {
    try {
      CsvReader reader = new CsvReader(stream, StandardCharsets.UTF_8);
      reader.readHeaders();
      reader.readRecord();
      return reader.get("feed_id");
    } catch (IOException e) {
      LOG.error("Reading feed id for emissions failed.", e);
      throw new RuntimeException(e);
    }
  }

  private static Optional<Double> calculateEmissionsPerPassengerPerMeter(
    String routeId,
    double avgCo2PerVehiclePerMeter,
    double avgPassengerCount
  ) {
    if (avgCo2PerVehiclePerMeter == 0) {
      // Passenger number is irrelevant when emissions is 0.
      return Optional.of(avgCo2PerVehiclePerMeter);
    }
    if (avgPassengerCount <= 0 || avgCo2PerVehiclePerMeter < 0) {
      Log.error(
        "Invalid data for route " +
        routeId +
        ": avgPassengerCount is 0 or less, but avgCo2PerVehiclePerMeter is nonzero or avgCo2PerVehiclePerMeter is negative."
      );
      return Optional.empty();
    }
    return Optional.of(avgCo2PerVehiclePerMeter / avgPassengerCount);
  }
}
