package org.opentripplanner.ext.reportapi.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.reportapi.model.CachedValue;
import org.opentripplanner.ext.reportapi.model.GraphReportBuilder;
import org.opentripplanner.ext.reportapi.model.GraphReportBuilder.GraphStats;
import org.opentripplanner.ext.reportapi.model.TransfersReport;
import org.opentripplanner.ext.reportapi.model.TransitGroupPriorityReport;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.constrained.ConstrainedTransferService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheReport;
import org.opentripplanner.transit.transfer.regular.api.report.TransferCacheReportEntry;

@Path("/report")
@Produces(MediaType.TEXT_PLAIN)
public class ReportResource {

  /** Since the computation is pretty expensive only allow it every 5 minutes */
  private static final CachedValue<GraphStats> CACHED_STATS = new CachedValue<>(
    Duration.ofMinutes(5)
  );

  private final ConstrainedTransferService transferService;
  private final TransitService transitService;
  private final RouteRequest defaultRequest;
  private final Graph graph;
  private final RegularTransferCacheReport regularTransferCacheReport;

  @SuppressWarnings("unused")
  public ReportResource(
    @Context TransitService transitService,
    @Context RouteRequest defaultRequest,
    @Context Graph graph,
    @Context RegularTransferCacheReport regularTransferCacheReport
  ) {
    this.transferService = transitService.getConstrainedTransferService();
    this.transitService = transitService;
    this.defaultRequest = defaultRequest;
    this.graph = graph;
    this.regularTransferCacheReport = regularTransferCacheReport;
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
  public Response stats() {
    return Response.status(Response.Status.OK)
      .entity(CACHED_STATS.get(() -> GraphReportBuilder.build(transitService, graph)))
      .type("application/json")
      .build();
  }

  /**
   * The keys of the cache of the Raptor transfers (the user preferences of the requests), with the
   * number of requests that used each, the most used first. A key is a profile, a warmup entry, or
   * created on demand.
   */
  @GET
  @Path("/regular-transfers/cache.json")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TransferCacheReportEntry> regularTransferCache() {
    return regularTransferCacheReport.entries();
  }
}
