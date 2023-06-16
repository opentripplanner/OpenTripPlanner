package org.opentripplanner.emissions;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStreamReader;
import org.opentripplanner.ext.emissions.DigitransitEmissions;
import org.opentripplanner.ext.emissions.DigitransitEmissionsService;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.framework.io.HttpUtils;
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
        var data = HttpUtils.getData(url);
        System.out.println("YEE");
        if (data == null) {
          throw new IOException("Did not find any emissions data from url " + url);
        }

        var reader = new InputStreamReader(data);
        var emissionJson = CharStreams.toString(reader);
        Gson gson = new Gson();
        EmissionsService emissionsService;
        if (config.emissions.getConfigName().equals("digitransitEmissions")) {
          DigitransitEmissions[] digitransitEmissions = gson.fromJson(
            emissionJson,
            DigitransitEmissions[].class
          );
          emissionsService = new DigitransitEmissionsService(digitransitEmissions);
          transitModel.setEmissionsService(emissionsService);
        }
      } catch (Exception e) {
        LOG.error("ERROR " + e);
      }
    }
  }

  @Override
  public void checkInputs() {}
}
