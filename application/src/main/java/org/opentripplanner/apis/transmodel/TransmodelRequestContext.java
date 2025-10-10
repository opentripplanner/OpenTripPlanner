package org.opentripplanner.apis.transmodel;

import javax.annotation.Nullable;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.lang.Sandbox;

public class TransmodelRequestContext {

  private final OtpServerRequestContext serverContext;
  private final RoutingService routingService;
  private final TransitService transitService;
  private final @Nullable EmpiricalDelayService empiricalDelayService;

  public TransmodelRequestContext(
    OtpServerRequestContext serverContext,
    RoutingService routingService,
    TransitService transitService,
    EmpiricalDelayService empiricalDelayService
  ) {
    this.serverContext = serverContext;
    this.routingService = routingService;
    this.transitService = transitService;
    this.empiricalDelayService = empiricalDelayService;
  }

  public OtpServerRequestContext getServerContext() {
    return serverContext;
  }

  public RoutingService getRoutingService() {
    return routingService;
  }

  public TransitService getTransitService() {
    return transitService;
  }

  @Nullable
  @Sandbox
  public EmpiricalDelayService getEmpiricalDelayService() {
    return empiricalDelayService;
  }
}
