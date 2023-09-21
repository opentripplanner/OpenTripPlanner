package org.opentripplanner.ext.digitransitemissions;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Module
public class DigitransitEmissionsModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(DigitransitEmissionsModule.class);
  private BuildConfig config;
  private EmissionsServiceRepository emissionsServiceRepository;
  private GraphBuilderDataSources dataSources;
  private Map<String, DigitransitEmissions> emissionsData = new HashMap<>();

  @Inject
  public DigitransitEmissionsModule(
    GraphBuilderDataSources dataSources,
    BuildConfig config,
    EmissionsServiceRepository emissionsServiceRepository
  ) {
    this.dataSources = dataSources;
    this.config = config;
    this.emissionsServiceRepository = emissionsServiceRepository;
  }

  public void buildGraph() {
    if (config.digitransitEmissions != null) {
      LOG.info("Start emissions building!");

      int carAvgCo2 = config.digitransitEmissions.getCarAvgCo2();
      double carAvgOccupancy = config.digitransitEmissions.getCarAvgOccupancy();
      double carAvgEmissions = carAvgCo2 / carAvgOccupancy;

      for (ConfiguredDataSource<GtfsFeedParameters> gtfsData : dataSources.getGtfsConfiguredDatasource()) {
        if (gtfsData.dataSource().name().contains(".zip")) {
          readGtfsZip(gtfsData.dataSource().path());
        } else {
          readGtfs(gtfsData.dataSource().path());
        }
      }

      this.emissionsServiceRepository.saveEmissionsService(
          new DigitransitEmissionsService(this.emissionsData, carAvgEmissions)
        );
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
      Double avgCo2PerVehiclePerKm = Double.parseDouble(reader.get("avg_co2_per_vehicle_per_km"));
      int avgPassengerCount = Integer.parseInt(reader.get("avg_passenger_count"));
      this.emissionsData.put(
          feedId + ":" + routeId,
          new DigitransitEmissions(avgCo2PerVehiclePerKm, avgPassengerCount)
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
}
