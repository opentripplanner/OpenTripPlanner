package org.opentripplanner.ext.reportapi.model;

import jakarta.inject.Inject;
import org.opentripplanner.framework.di.OtpServerRequest;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.service.TransitService;

@OtpServerRequest
public class ReportService {

  private final TransferService transferService;
  private final TransitService transitService;
  private final RouteRequest defaultRequest;

  @Inject
  public ReportService(TransitService transitService, RouteRequest defaultRequest) {
    this.transferService = transitService.getTransferService();
    this.transitService = transitService;
    this.defaultRequest = defaultRequest;
  }

  public TransferService getTransferService() {
    return transferService;
  }

  public TransitService getTransitService() {
    return transitService;
  }

  public RouteRequest getDefaultRequest() {
    return defaultRequest;
  }
}
