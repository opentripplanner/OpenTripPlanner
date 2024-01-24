package org.opentripplanner.standalone.server;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.configure.OtpServerFactory;
import org.opentripplanner.transit.service.TransitService;

public class JerseyToDaggerBridge extends AbstractBinder {

  private final OtpServerFactory factory;

  public JerseyToDaggerBridge(OtpServerFactory factory) {
    this.factory = factory;
  }

  @Override
  protected void configure() {
    bindFactory(factory::transitService).to(TransitService.class);
    bindFactory(factory::graph).to(Graph.class);
  }
}
