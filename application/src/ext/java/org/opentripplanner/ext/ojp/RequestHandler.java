package org.opentripplanner.ext.ojp;

import static java.util.Objects.requireNonNull;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.OJPTripRequestStructure;
import de.vdv.ojp20.siri.AbstractFunctionalServiceRequestStructure;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Function;
import org.opentripplanner.ext.ojp.mapping.ErrorMapper;
import org.opentripplanner.ext.ojp.service.OjpService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming OJP requests and routes them to the appropriate OJP service.
 */
public class RequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);
  private final OjpService ojpService;
  private final Function<OJP, StreamingOutput> responseMapper;
  private final String apiName;

  public RequestHandler(
    OjpService ojpService,
    Function<OJP, StreamingOutput> responseMapper,
    String apiName
  ) {
    this.ojpService = requireNonNull(ojpService);
    this.responseMapper = requireNonNull(responseMapper);
    this.apiName = requireNonNull(apiName);
  }

  public Response handleRequest(OJP ojp, RouteRequest defaultRequest) {
    try {
      var request = findRequest(ojp);

      if (request instanceof OJPStopEventRequestStructure ser) {
        var ojpResponse = ojpService.handleStopEventRequest(ser);
        StreamingOutput stream = responseMapper.apply(ojpResponse);
        return Response.ok(stream).build();
      } else if (request instanceof OJPTripRequestStructure tr) {
        var ojpResponse = ojpService.handleTripRequest(tr, defaultRequest);
        StreamingOutput stream = responseMapper.apply(ojpResponse);
        return Response.ok(stream).build();
      } else {
        return error(
          "Request type '%s' is not supported".formatted(request.getClass().getSimpleName())
        );
      }
    } catch (EntityNotFoundException | RoutingValidationException e) {
      return error(e.getMessage());
    } catch (Exception e) {
      LOG.error("Error processing %s request".formatted(apiName), e);
      return error(e.getMessage());
    }
  }

  public Response error(String value) {
    var output = responseMapper.apply(ErrorMapper.error(value, ZonedDateTime.now()));
    return Response.status(Response.Status.BAD_REQUEST).entity(output).build();
  }

  private AbstractFunctionalServiceRequestStructure findRequest(OJP ojp) {
    return Optional.ofNullable(ojp.getOJPRequest())
      .map(s -> s.getServiceRequest())
      .stream()
      .flatMap(s -> s.getAbstractFunctionalServiceRequest().stream())
      .findFirst()
      .orElseThrow(() ->
        new IllegalArgumentException("No request found in %s XML body.".formatted(apiName))
      )
      .getValue();
  }
}
