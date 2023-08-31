package org.opentripplanner.ext.digitransitemissions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.search.TraverseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Module
public class DigitransitEmissionsModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(DigitransitEmissionsModule.class);
  private BuildConfig config;
  private EmissionsServiceRepository emissionsServiceRepository;

  @Inject
  public DigitransitEmissionsModule(
    BuildConfig config,
    EmissionsServiceRepository emissionsServiceRepository
  ) {
    this.config = config;
    this.emissionsServiceRepository = emissionsServiceRepository;
  }

  public void buildGraph() {
    if (config.digitransitEmissions != null) {
      LOG.info("Start emissions building!");
      String url = config.digitransitEmissions.getUrl();

      if (url != null && !url.isEmpty()) {
        LOG.info("Fetching data from {}", url);
        try {
          var http = new OtpHttpClient();
          JsonNode data = http.getAndMapAsJsonNode(new URI(url), Map.of(), new ObjectMapper());
          if (data == null) {
            throw new IOException("Did not find any emissions data from url " + url);
          }
          Map<String, DigitransitEmissions> digitransitEmissions = parseDigitransitEmissions(data);
          this.emissionsServiceRepository.saveEmissionsService(
              new DigitransitEmissionsService(digitransitEmissions)
            );
        } catch (Exception e) {
          LOG.error("ERROR ", e);
        }
      }
    }
  }

  private Map<String, DigitransitEmissions> parseDigitransitEmissions(JsonNode data) {
    Map<String, DigitransitEmissions> digitransitEmissions = new HashMap<>();
    for (JsonNode line : data) {
      digitransitEmissions.put(
        getKey(line),
        new DigitransitEmissions(line.get("avg").asDouble(), line.get("p_avg").intValue())
      );
    }
    return digitransitEmissions;
  }

  private String getKey(JsonNode data) {
    if (data.get("mode").asText().equals(TraverseMode.CAR.toString())) {
      return TraverseMode.CAR.toString();
    }
    String key =
      data.get("db").asText() +
      ":" +
      data.get("agency_id").asText() +
      ":" +
      data.get("mode").asText();
    return key;
  }
}
