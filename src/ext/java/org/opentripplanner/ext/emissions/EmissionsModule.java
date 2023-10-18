package org.opentripplanner.ext.emissions;

import com.csvreader.CsvReader;
import dagger.Module;
import jakarta.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsFeedParameters;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Module
public class EmissionsModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(EmissionsModule.class);
  private BuildConfig config;
  private EmissionsDataModel emissionsDataModel;
  private GraphBuilderDataSources dataSources;
  private Map<FeedScopedId, Double> emissionsData = new HashMap<>();

  @Inject
  public EmissionsModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    EmissionsDataModel emissionsDataModel
  ) {
    this.dataSources = dataSources;
    this.config = config;
    this.emissionsDataModel = emissionsDataModel;
  }

  public void buildGraph() {
    if (config.emissions != null) {
      LOG.info("Start emissions building!");

      double carAvgCo2PerKm = config.emissions.getCarAvgCo2PerKm();
      double carAvgOccupancy = config.emissions.getCarAvgOccupancy();
      double carAvgEmissionsPerMeter = carAvgCo2PerKm / 1000 / carAvgOccupancy;

      for (ConfiguredDataSource<GtfsFeedParameters> gtfsData : dataSources.getGtfsConfiguredDatasource()) {
        if (gtfsData.dataSource().name().contains(".zip")) {
          readGtfsZip(gtfsData.dataSource().path());
        } else {
          readGtfs(gtfsData.dataSource().path());
        }
      }
      this.emissionsDataModel.setCo2Emissions(this.emissionsData);
      this.emissionsDataModel.setCarAvgCo2PerMeter(carAvgEmissionsPerMeter);
    }
  }

  private void readGtfs(String filePath) {
    try {
      InputStream feedInfoStream = new FileInputStream(filePath + "/feed_info.txt");
      String feedId = readFeedId(feedInfoStream);
      feedInfoStream.close();

      InputStream stream = new FileInputStream(filePath + "/emissions.txt");
      readEmissions(stream, feedId);
      stream.close();
    } catch (IOException e) {
      LOG.error("Reading emissions data failed.", e);
    }
  }

  private void readGtfsZip(String filePath) {
    try {
      ZipFile zipFile = new ZipFile(new File(filePath), ZipFile.OPEN_READ);
      String feedId = readFeedId(zipFile.getInputStream(zipFile.getEntry("feed_info.txt")));
      InputStream stream = zipFile.getInputStream(zipFile.getEntry("emissions.txt"));
      readEmissions(stream, feedId);
      zipFile.close();
    } catch (IOException e) {
      LOG.error("Reading emissions data failed.", e);
    }
  }

  private void readEmissions(InputStream stream, String feedId) throws IOException {
    CsvReader reader = new CsvReader(stream, StandardCharsets.UTF_8);
    reader.readHeaders();
    while (reader.readRecord()) {
      String routeId = reader.get("route_id");
      Double avgCo2PerVehiclePerMeter =
        Double.parseDouble(reader.get("avg_co2_per_vehicle_per_km")) / 1000;
      Double avgPassengerCount = Double.parseDouble(reader.get("avg_passenger_count"));
      this.emissionsData.put(
          new FeedScopedId(feedId, routeId),
          calculateEmissionsPerPassengerPerMeter(avgCo2PerVehiclePerMeter, avgPassengerCount)
        );
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

  private static double calculateEmissionsPerPassengerPerMeter(
    double avgCo2PerVehiclePerMeter,
    double avgPassengerCount
  ) {
    if (avgPassengerCount <= 1) {
      return avgCo2PerVehiclePerMeter;
    }
    return avgCo2PerVehiclePerMeter / avgPassengerCount;
  }
}
