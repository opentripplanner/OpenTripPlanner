package org.opentripplanner.integration.graph_builder;

import java.io.IOException;

import org.opentripplanner.graph_builder.GraphBuilderMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphBuilderIntegrationMain {

  private static Logger _log = LoggerFactory.getLogger(GraphBuilderIntegrationMain.class);

  private static final String GRAPH_BUILDER_CONFIG_PATH = "graph_builder.config.path";

  public static void main(String[] args) throws IOException {

    String configPath = "src/main/resources/graph-builder.xml";

    String fromProperty = System.getProperty(GRAPH_BUILDER_CONFIG_PATH);
    if (fromProperty != null) {
      _log.info("reading graph builder config path from property "
          + GRAPH_BUILDER_CONFIG_PATH);
      configPath = fromProperty;
    }
    _log.info("graph builder config path: " + configPath);

    GraphBuilderMain.main(new String[] {configPath});
  }
}
