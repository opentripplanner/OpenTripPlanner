package org.opentripplanner.ext.ojp.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.api.model.transit.HideFeedIdMapper;
import org.opentripplanner.ext.ojp.RequestHandler;
import org.opentripplanner.ext.ojp.parameters.OjpApiParameters;
import org.opentripplanner.ext.ojp.service.CallAtStopService;
import org.opentripplanner.ext.ojp.service.OjpService;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/ojp/v2")
public class OjpResource {

  private static final Logger LOG = LoggerFactory.getLogger(OjpResource.class);

  private static final String EXPLORER_HTML = """
      <!doctype html>
      <html lang="en">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <title>OJP Explorer</title>
          <script defer="defer" src="https://leonardehrenfried.github.io/ojp-mini-client/static/js/main.1a443aab.js"></script>
          <link href="https://leonardehrenfried.github.io/ojp-mini-client/static/css/main.320826b9.css" rel="stylesheet">
        </head>
        <body><noscript>You need to enable JavaScript to run this app.</noscript><div id="root"></div></body>
      </html>
    """;

  private final RequestHandler handler;
  private final RouteRequest defaultRouteRequest;

  public OjpResource(
    @Context TransitService transitService,
    @Context Graph graph,
    @Context LinkingContextFactory linkingContextFactory,
    @Context OjpApiParameters ojpApiParameters,
    @Context RouteRequest defaultRouteRequest,
    @Context RoutingService routingService
  ) {
    NearbyStopFinder nearbyStopFinder = graph.hasStreets
      ? StreetNearbyStopFinder.of(linkingContextFactory).build()
      : new StraightLineNearbyStopFinder(transitService::findRegularStopsByBoundingBox);
    var callAtStopService = new CallAtStopService(transitService, nearbyStopFinder);
    var idMapper = idMapper(ojpApiParameters);
    var ojpService = new OjpService(
      callAtStopService,
      routingService,
      idMapper,
      transitService.getTimeZone()
    );
    this.handler = new RequestHandler(ojpService, OjpCodec::serialize, "OJP");
    this.defaultRouteRequest = defaultRouteRequest;
  }

  @POST
  @Produces(MediaType.APPLICATION_XML)
  public Response index(String xmlString) {
    try {
      var ojp = OjpCodec.deserialize(xmlString);
      return handler.handleRequest(ojp, defaultRouteRequest);
    } catch (JAXBException | TransformerException e) {
      LOG.error("Error reading OJP request", e);
      return handler.error("Could not read OJP request.");
    }
  }

  @GET
  @Path("/explorer")
  @Produces(MediaType.TEXT_HTML)
  public Response index() {
    return Response.ok(EXPLORER_HTML).build();
  }

  private static FeedScopedIdMapper idMapper(OjpApiParameters ojpParams) {
    if (ojpParams.hideFeedId()) {
      return new HideFeedIdMapper(ojpParams.hardcodedInputFeedId());
    } else {
      return new DefaultFeedIdMapper();
    }
  }
}
