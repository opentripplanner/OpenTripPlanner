package org.opentripplanner.emissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.opentripplanner.ext.emissions.DigitransitEmissions;
import org.opentripplanner.ext.emissions.DigitransitEmissionsService;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmissionsModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(EmissionsModule.class);

  private TransitModel transitModel;
  private Graph graph;
  private DataImportIssueStore issueStore;
  private BuildConfig config;

  public EmissionsModule(
    TransitModel transitModel,
    Graph graph,
    DataImportIssueStore issueStore,
    BuildConfig config
  ) {
    this.transitModel = transitModel;
    this.graph = graph;
    this.issueStore = issueStore;
    this.config = config;
  }

  @Override
  public void buildGraph() {
    LOG.info("Start emissions building!");
    String url = config.emissions.getUrl();
    if (url != null && !url.isEmpty()) {
      LOG.info("Fetching data from {}", url);
      try {
        var http = new OtpHttpClient();
        var data = http.getAndMapAsJsonNode(new URI(url), Map.of(), new ObjectMapper());
        if (data == null) {
          throw new IOException("Did not find any emissions data from url " + url);
        }

        Gson gson = new Gson();
        EmissionsService emissionsService;
        if (config.emissions.getConfigName().equals("digitransitEmissions")) {
          DigitransitEmissions[] digitransitEmissions = gson.fromJson(
            String.valueOf(data),
            DigitransitEmissions[].class
          );
          emissionsService = new DigitransitEmissionsService(digitransitEmissions);
          transitModel.setEmissionsService(emissionsService);
          graph.setEmissionsService(new DigitransitEmissionsService(digitransitEmissions));
        }
      } catch (Exception e) {
        LOG.error("ERROR " + e);
      }
    }
  }

  @Override
  public void checkInputs() {}
}
