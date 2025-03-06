package org.opentripplanner.ext.reportapi.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import org.opentripplanner.ext.reportapi.model.CachedValue;
import org.opentripplanner.ext.reportapi.model.GraphReportBuilder;
import org.opentripplanner.ext.reportapi.model.GraphReportBuilder.GraphStats;
import org.opentripplanner.ext.reportapi.model.TransfersReport;
import org.opentripplanner.ext.reportapi.model.TransitGroupPriorityReport;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

@Path("/report")
@Produces(MediaType.TEXT_PLAIN)
public class ReportResource {

  /** Since the computation is pretty expensive only allow it every 5 minutes */
  private static final CachedValue<GraphStats> cachedStats = new CachedValue<>(
    Duration.ofMinutes(5)
  );

  private final TransferService transferService;
  private final TransitService transitService;
  private final RouteRequest defaultRequest;

  @SuppressWarnings("unused")
  public ReportResource(@Context OtpServerRequestContext requestContext) {
    this.transferService = requestContext.transitService().getTransferService();
    this.transitService = requestContext.transitService();
    this.defaultRequest = requestContext.defaultRouteRequest();
  }

  @GET
  @Path("/transfers.csv")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public String getTransfersAsCsv() {
    return TransfersReport.export(transferService.listAll(), transitService);
  }

  @GET
  @Path("/transit/group/priorities")
  @Produces(MediaType.TEXT_PLAIN)
  public String getTransitGroupPriorities() {
    return TransitGroupPriorityReport.build(
      transitService.listTripPatterns(),
      defaultRequest.journey().transit()
    );
  }

  @GET
  @Path("/graph.json")
  public Response stats(@Context OtpServerRequestContext serverRequestContext) {
    return Response.status(Response.Status.OK)
      .entity(cachedStats.get(() -> GraphReportBuilder.build(serverRequestContext)))
      .type("application/json")
      .build();
  }
}
