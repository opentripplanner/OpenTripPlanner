package org.opentripplanner.annotation;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;

public abstract class StaticClassComponent implements GraphUpdater {

  @Component(key = "test.staticClassComponent", type = ServiceType.GraphUpdater)
  public static class FinalStaticClassComponent extends StaticClassComponent {

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
}
