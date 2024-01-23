package org.opentripplanner.standalone.server;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.configure.ConstructApplicationFactory;
import org.opentripplanner.transit.service.TransitService;

public class JerseyToDaggerBridge extends AbstractBinder {

  private final ConstructApplicationFactory factory;

  public JerseyToDaggerBridge(ConstructApplicationFactory factory) {
    this.factory = factory;
  }

  @Override
  protected void configure() {
    bindFactory(factory::transitService).to(TransitService.class);
    bindFactory(factory::graph).to(Graph.class);
  }
}
