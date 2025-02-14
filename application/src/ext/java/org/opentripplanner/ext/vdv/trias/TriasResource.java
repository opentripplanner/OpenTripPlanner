package org.opentripplanner.ext.vdv.trias;

import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPStopEventRequestStructure;
import de.vdv.ojp20.siri.AbstractFunctionalServiceRequestStructure;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import javax.xml.transform.TransformerException;
import org.opentripplanner.ext.vdv.VdvService;
import org.opentripplanner.ext.vdv.ojp.ErrorMapper;
import org.opentripplanner.ext.vdv.ojp.OjpService;
import org.opentripplanner.ext.vdv.ojp.StopEventResponseMapper;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/trias/v1")
public class TriasResource {

  private static final Logger LOG = LoggerFactory.getLogger(TriasResource.class);

  private final OjpService ojpService;

  public TriasResource(@Context OtpServerRequestContext context) {
    var zoneId = context.transitService().getTimeZone();
    var vdvService = new VdvService(context.transitService());
    var mapper = new StopEventResponseMapper(zoneId);
    this.ojpService = new OjpService(vdvService, mapper, zoneId);
  }

  @POST
  @Produces("application/xml")
  public Response index(String triasInput) {
    try {
      var ojp = OjpToTriasTransformer.triasToOjp(triasInput);

      var request = getValue(ojp);

      if (request instanceof OJPStopEventRequestStructure ser) {
        var ojpResponse = ojpService.handleStopEvenRequest(ser);
        final StreamingOutput stream = ojpToTrias(ojpResponse);
        return Response.ok(stream).build();
      } else {
        return error(
          "Request type '%s' is not supported".formatted(request.getClass().getSimpleName())
        );
      }
    } catch (EntityNotFoundException e) {
      return error(e.getMessage());
    } catch (JAXBException | TransformerException e) {
      LOG.error("Error reading TRIAS request", e);
      return error("Could not read TRIAS request.");
    } catch (Exception e) {
      LOG.error("Error processing TRIAS request", e);
      return error(e.getMessage());
    }
  }

  private static AbstractFunctionalServiceRequestStructure getValue(OJP ojp) {
    return Optional
      .ofNullable(ojp.getOJPRequest())
      .map(s -> s.getServiceRequest())
      .stream()
      .flatMap(s -> s.getAbstractFunctionalServiceRequest().stream())
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("No request found in TRIAS XML body."))
      .getValue();
  }

  private static Response error(String value) {
    var trias = ojpToTrias(ErrorMapper.error(value, ZonedDateTime.now()));
    return Response.status(Response.Status.BAD_REQUEST).entity(trias).build();
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
