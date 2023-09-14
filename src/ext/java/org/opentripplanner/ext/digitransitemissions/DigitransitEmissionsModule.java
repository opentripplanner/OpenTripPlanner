package org.opentripplanner.ext.digitransitemissions;

import com.csvreader.CsvReader;
import dagger.Module;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
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

      this.emissionsServiceRepository.saveEmissionsService(
          new DigitransitEmissionsService(readGtfs(), carAvgEmissions)
        );
    }
  }

  private Map<String, DigitransitEmissions> readGtfs() {
    Map<String, DigitransitEmissions> emissionsData = new HashMap<>();

    try {
      for (ConfiguredDataSource<GtfsFeedParameters> gtfsData : dataSources.getGtfsConfiguredDatasource()) {
        ZipFile zipFile = new ZipFile(new File(gtfsData.dataSource().path()), ZipFile.OPEN_READ);
        String feedId = readFeedId(zipFile);
        ZipEntry entry = zipFile.getEntry("emissions.txt");
        InputStream stream = zipFile.getInputStream(entry);
        CsvReader reader = new CsvReader(stream, ';', StandardCharsets.UTF_8);
        reader.readHeaders();
        while (reader.readRecord()) {
          emissionsData.put(getKey(reader, feedId), getEmissions(reader));
        }
        zipFile.close();
      }
    } catch (IOException e) {
      LOG.error("Reading emissions data failed.", e);
    }
    return emissionsData;
  }

  private String readFeedId(ZipFile zipFile) {
    try {
      ZipEntry feedInfoEntry = zipFile.getEntry("feed_info.txt");
      InputStream stream = zipFile.getInputStream(feedInfoEntry);
      CsvReader reader = new CsvReader(stream, StandardCharsets.UTF_8);
      reader.readHeaders();
      reader.readRecord();
      return reader.get("feed_id");
    } catch (IOException e) {
      LOG.error("Reading feed id for emissions failed.", e);
      throw new RuntimeException(e);
    }
  }

  private String getKey(CsvReader reader, String feedId) throws IOException {
    String routeId = reader.get("route_id");
    String agencyId = reader.get("agency_id");
    String routeShortName = reader.get("route_short_name");
    String type = reader.get("type");
    return feedId + ":" + agencyId + ":" + routeId + ":" + routeShortName + ":" + type;
  }

  private DigitransitEmissions getEmissions(CsvReader reader) throws IOException {
    Double avg = Double.parseDouble(reader.get("avg"));
    int pAvg = Integer.parseInt(reader.get("p_avg"));
    return new DigitransitEmissions(avg, pAvg);
  }
}
