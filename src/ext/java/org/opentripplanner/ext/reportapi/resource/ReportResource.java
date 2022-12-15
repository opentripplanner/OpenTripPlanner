package org.opentripplanner.ext.reportapi.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opentripplanner.ext.reportapi.model.BicycleSafetyReport;
import org.opentripplanner.ext.reportapi.model.CachedValue;
import org.opentripplanner.ext.reportapi.model.GraphReportBuilder;
import org.opentripplanner.ext.reportapi.model.GraphReportBuilder.GraphStats;
import org.opentripplanner.ext.reportapi.model.TransfersReport;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource;
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

  @SuppressWarnings("unused")
  public ReportResource(@Context OtpServerRequestContext requestContext) {
    this.transferService = requestContext.transitService().getTransferService();
    this.transitService = requestContext.transitService();
  }

  @GET
  @Path("/transfers.csv")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public String getTransfersAsCsv() {
    return TransfersReport.export(transferService.listAll(), transitService);
  }

  @GET
  @Path("/bicycle-safety.html")
  @Produces(MediaType.TEXT_HTML)
  public Response getBicycleSafetyPage() {
    try (var is = getClass().getResourceAsStream("/reportapi/report.html")) {
      return Response.ok(new String(is.readAllBytes(), StandardCharsets.UTF_8)).build();
    } catch (IOException e) {
      return Response.serverError().build();
    }
  }

  @GET
  @Path("/bicycle-safety.csv")
  @Produces("text/csv")
  public Response getBicycleSafetyAsCsv(
    @DefaultValue("default") @QueryParam("osmWayPropertySet") String osmWayPropertySet
  ) {
    var source = OsmTagMapperSource.valueOf(osmWayPropertySet);
    return Response
      .ok(BicycleSafetyReport.makeCsv(source))
      .header(
        "Content-Disposition",
        "attachment; filename=\"" + osmWayPropertySet + "-bicycle-safety.csv\""
      )
      .build();
  }

  @GET
  @Path("/graph.json")
  public Response stats(@Context OtpServerRequestContext serverRequestContext) {
    return Response
      .status(Response.Status.OK)
      .entity(cachedStats.get(() -> GraphReportBuilder.build(serverRequestContext)))
      .type("application/json")
      .build();
  }
}
