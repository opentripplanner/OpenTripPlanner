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
import java.util.Objects;
import javax.xml.transform.TransformerException;
import org.opentripplanner.ext.vdv.VdvService;
import org.opentripplanner.ext.vdv.ojp.ErrorMapper;
import org.opentripplanner.ext.vdv.ojp.StopEventResponseMapper;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/trias/v1")
public class TriasResource {

  private static final Logger LOG = LoggerFactory.getLogger(TriasResource.class);

  private final VdvService vdvService;
  private final StopEventResponseMapper mapper;

  public TriasResource(@Context OtpServerRequestContext context) {
    this.vdvService = new VdvService(context.transitService());
    this.mapper = new StopEventResponseMapper(context.transitService().getTimeZone());
  }

  @POST
  @Produces("application/xml")
  public Response index(String triasInput) {
    try {
      var ojp = OjpToTriasTransformer.readTrias(triasInput);

      var request = ojp
        .getOJPRequest()
        .getServiceRequest()
        .getAbstractFunctionalServiceRequest()
        .getFirst()
        .getValue();

      if (request instanceof OJPStopEventRequestStructure ser) {
        var stopId = ser.getLocation().getPlaceRef().getStopPointRef().getValue();
        var time = ser.getLocation().getDepArrTime();
        var numResults = ser.getParams().getNumberOfResults().intValue();
        var tripTimesOnDate = vdvService.findStopTimesInPattern(stopId, time, numResults);
        var ojpOutput = mapper.mapStopTimesInPattern(tripTimesOnDate, Instant.now());
        final StreamingOutput stream = ojpToTrias(ojpOutput);
        return Response.ok(stream).build();
      }
      else {
        var error = ErrorMapper.error("Request %s is not supported".formatted(request.getClass().getSimpleName()));
        return Response.serverError().entity(error).build();
      }
    } catch (JAXBException | TransformerException e) {
      var error = ErrorMapper.error("Could not read TRIAS request.");
      return Response.serverError().entity(error).build();
    } catch (Exception e) {
      LOG.error("Error producing TRIAS response", e);
      return Response.serverError().build();
    }
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
