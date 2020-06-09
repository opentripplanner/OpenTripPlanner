package org.opentripplanner.annotation;

import static org.opentripplanner.annotation.ServiceType.GraphUpdater;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;

@Component(key = "test.compoent", type = GraphUpdater, init = TestParameter.class)
public class TestComponent implements org.opentripplanner.updater.GraphUpdater {

  public TestComponent(TestParameter testParameter) {
    //nothing
  }

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
  public String getName() {
    return this.getClass().getName();
  }

  public boolean isPrimed() {
    return false;
  }
}
