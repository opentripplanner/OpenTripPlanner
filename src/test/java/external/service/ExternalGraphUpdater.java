package external.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.annotation.Component;
import org.opentripplanner.annotation.ServiceType;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;

@Component(key = "external.updater", type = ServiceType.GraphUpdater)
public class ExternalGraphUpdater implements GraphUpdater {

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
