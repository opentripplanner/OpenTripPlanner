package org.opentripplanner.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.glassfish.grizzly.http.server.Request;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.mapping.PlannerErrorMapper;
import org.opentripplanner.api.mapping.TripPlanMapper;
import org.opentripplanner.api.mapping.TripSearchMetadataMapper;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the primary entry point for the trip planning web service. All parameters are passed in
 * the query string. These parameters are defined as fields in the abstract RoutingResource
 * superclass, which also has methods for building routing requests from query parameters. This
 * allows multiple web services to have the same set of query parameters. In order for inheritance
 * to work, the REST resources are request-scoped (constructed at each request) rather than
 * singleton-scoped (a single instance existing for the lifetime of the OTP server).
 */
@Path("routers/{ignoreRouterId}/plan")
// final element needed here rather than on method to distinguish from routers API
public class PlannerResource extends RoutingResource {

  private static final Logger LOG = LoggerFactory.getLogger(PlannerResource.class);

  /**
   * @deprecated The support for multiple routers are removed from OTP2. See
   * https://github.com/opentripplanner/OpenTripPlanner/issues/2760
   */
  @Deprecated
  @PathParam("ignoreRouterId")
  private String ignoreRouterId;

  // We inject info about the incoming request so we can include the incoming query
  // parameters in the outgoing response. This is a TriMet requirement.
  // Jersey uses @Context to inject internal types and @InjectParam or @Resource for DI objects.
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public TripPlannerResponse plan(@Context UriInfo uriInfo, @Context Request grizzlyRequest) {
    /*
     * TODO: add Lang / Locale parameter, and thus get localized content (Messages & more...)
     * TODO: from/to inputs should be converted / geocoded / etc... here, and maybe send coords
     *       or vertex ids to planner (or error back to user)
     * TODO: org.opentripplanner.routing.module.PathServiceImpl has COOORD parsing. Abstract that
     *       out so it's used here too...
     */

    // Create response object, containing a copy of all request parameters. Maybe they should be in the debug section of the response.
    TripPlannerResponse response = new TripPlannerResponse(uriInfo);
    RouteRequest request = null;
    RoutingResponse res = null;
    try {
      /* Fill in request fields from query parameters via shared superclass method, catching any errors. */
      request = super.buildRequest(uriInfo.getQueryParameters());

      // Route
      res = serverContext.routingService().route(request);

      // Map to API
      // TODO VIA (Leonard) - we should store the default showIntermediateStops somewhere
      TripPlanMapper tripPlanMapper = new TripPlanMapper(request.locale(), showIntermediateStops);
      response.setPlan(tripPlanMapper.mapTripPlan(res.getTripPlan()));
      if (res.getPreviousPageCursor() != null) {
        response.setPreviousPageCursor(res.getPreviousPageCursor().encode());
      }
      if (res.getNextPageCursor() != null) {
        response.setNextPageCursor(res.getNextPageCursor().encode());
      }
      response.setMetadata(TripSearchMetadataMapper.mapTripSearchMetadata(res.getMetadata()));
      if (!res.getRoutingErrors().isEmpty()) {
        // The api can only return one error message, so the first one is mapped
        response.setError(PlannerErrorMapper.mapMessage(res.getRoutingErrors().get(0)));
      }

      /* Populate up the elevation metadata */
      response.elevationMetadata = new ElevationMetadata();
      response.elevationMetadata.ellipsoidToGeoidDifference =
        serverContext.graph().ellipsoidToGeoidDifference;
      response.elevationMetadata.geoidElevation = request.preferences().system().geoidElevation();

      response.debugOutput = res.getDebugTimingAggregator().finishedRendering();
    } catch (Exception e) {
      LOG.error("System error", e);
      PlannerError error = new PlannerError(Message.SYSTEM_ERROR);
      response.setError(error);
    }

    /* Log this request if such logging is enabled. */
    logRequest(grizzlyRequest, request, serverContext, res);

    return response;
  }

  private void logRequest(
    Request grizzlyRequest,
    RouteRequest request,
    OtpServerRequestContext serverContext,
    RoutingResponse res
  ) {
    if (request != null && serverContext != null && serverContext.requestLogger() != null) {
      StringBuilder sb = new StringBuilder();
      String clientIpAddress = grizzlyRequest.getRemoteAddr();
      //sb.append(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
      sb.append(clientIpAddress);
      sb.append(' ');
      sb.append(request.arriveBy() ? "ARRIVE" : "DEPART");
      sb.append(' ');
      sb.append(LocalDateTime.ofInstant(request.dateTime(), ZoneId.systemDefault()));
      sb.append(' ');
      sb.append(request.journey().modes());
      sb.append(' ');
      sb.append(request.from().lat);
      sb.append(' ');
      sb.append(request.from().lng);
      sb.append(' ');
      sb.append(request.to().lat);
      sb.append(' ');
      sb.append(request.to().lng);
      sb.append(' ');
      if (res != null) {
        for (Itinerary it : res.getTripPlan().itineraries) {
          sb.append(it.getDuration());
          sb.append(' ');
        }
      }
      serverContext.requestLogger().info(sb.toString());
    }
  }
}
