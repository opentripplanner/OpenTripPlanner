package org.opentripplanner.annotation;

import static org.opentripplanner.annotation.ServiceType.GraphUpdater;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;

@Component(key = "test.compoent", type = GraphUpdater)
public class TestComponent implements org.opentripplanner.updater.GraphUpdater {

  @Override
  public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {

  }

  @Override
  public void setup(Graph graph) throws Exception {

  }

  @Override
  public void run() throws Exception {

  }

  @Override
  public void teardown() {

  }

  @Override
  public void configure(Graph graph, JsonNode jsonNode) throws Exception {

  }
}
