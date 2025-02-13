package org.opentripplanner.ext.vdv.trias;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.xml.bind.JAXBException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import javax.xml.transform.TransformerException;
import org.opentripplanner.ext.vdv.VdvService;
import org.opentripplanner.ext.vdv.ojp.ErrorMapper;
import org.opentripplanner.ext.vdv.ojp.StopEventResponseMapper;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/trias/v1")
public class TriasResource {

  private static final Logger LOG = LoggerFactory.getLogger(TriasResource.class);

  private final VdvService vdvService;
  private final StopEventResponseMapper mapper;
  private final ZoneId zoneId;

  public TriasResource(@Context OtpServerRequestContext context) {
    this.vdvService = new VdvService(context.transitService());
    this.mapper = new StopEventResponseMapper(context.transitService().getTimeZone());
    this.zoneId = context.transitService().getTimeZone();
  }

  @POST
  @Produces("application/xml")
  public Response index(String triasInput) {
    OJP ojpResponse;
    try {
      var ojp = OjpToTriasTransformer.triasToOjp(triasInput);

      var request = ojp
        .getOJPRequest()
        .getServiceRequest()
        .getAbstractFunctionalServiceRequest()
        .getFirst()
        .getValue();

      if (request instanceof OJPStopEventRequestStructure ser) {
        ojpResponse = handleStopEvenRequest(ser);
      } else {
        ojpResponse =
          ErrorMapper.error(
            "Request type '%s' is not supported".formatted(request.getClass().getSimpleName())
          );
      }
    } catch (JAXBException | TransformerException e) {
      ojpResponse = ErrorMapper.error("Could not read TRIAS request.");
    } catch (EntityNotFoundException | IllegalArgumentException e) {
      ojpResponse = ErrorMapper.error(e.getMessage());
    } catch (Exception e) {
      LOG.error("Error producing TRIAS response", e);
      ojpResponse = ErrorMapper.error(e.getMessage());
    }
    final StreamingOutput stream = ojpToTrias(ojpResponse);
    return Response.ok(stream).build();
  }

  private OJP handleStopEvenRequest(OJPStopEventRequestStructure ser) {
    var stopId = FeedScopedId.parse(ser.getLocation().getPlaceRef().getStopPointRef().getValue());
    var time = Optional
      .ofNullable(ser.getLocation().getDepArrTime().atZone(zoneId))
      .orElse(ZonedDateTime.now(zoneId));
    var numResults = Optional
      .ofNullable(ser.getParams())
      .map(s -> s.getNumberOfResults())
      .map(i -> i.intValue())
      .orElse(1);
    var tripTimesOnDate = vdvService.findStopTimesInPattern(stopId, time.toInstant(), numResults);
    return mapper.mapStopTimesInPattern(tripTimesOnDate, Instant.now());
  }

  private static StreamingOutput ojpToTrias(OJP ojpOutput) {
    StreamingOutput stream = os -> {
      Writer writer = new BufferedWriter(new OutputStreamWriter(os));
      OjpToTriasTransformer.transform(ojpOutput, writer);
      writer.flush();
    };
    return stream;
  }

  @GET
  @Path("/explorer")
  @Produces("text/html")
  public Response explorer() throws IOException {
    return classpathResource("explorer.html");
  }

  @GET
  @Path("/static/config.json")
  @Produces("application/json")
  public Response configJson() throws IOException {
    return classpathResource("config.json");
  }

  @GET
  @Path("/static/api_templates.json")
  @Produces("application/json")
  public Response templatesJson() throws IOException {
    return classpathResource("api_templates.json");
  }

  @GET
  @Path("/static/stop_event.xml")
  @Produces("application/xml")
  public Response stopEventXml() throws IOException {
    return classpathResource("stop_event.xml");
  }

  private static Response classpathResource(String name) throws IOException {
    final String resource = "explorer/" + name;
    var res = Objects
      .requireNonNull(TriasResource.class.getResource(resource), "%s not found".formatted(resource))
      .openStream();
    return Response.ok(res).build();
  }
}
