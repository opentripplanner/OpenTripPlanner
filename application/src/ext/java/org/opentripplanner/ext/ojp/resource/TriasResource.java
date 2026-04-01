package org.opentripplanner.ext.ojp.resource;

import de.vdv.ojp20.OJP;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.Set;
import javax.xml.transform.TransformerException;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.api.model.transit.HideFeedIdMapper;
import org.opentripplanner.ext.ojp.RequestHandler;
import org.opentripplanner.ext.ojp.parameters.TriasApiParameters;
import org.opentripplanner.ext.ojp.service.CallAtStopService;
import org.opentripplanner.ext.ojp.service.OjpService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/trias/v1")
public class TriasResource {

  private static final Logger LOG = LoggerFactory.getLogger(TriasResource.class);
  private static final Set<String> ALLOWED_CLASSPATH_RESOURCES = Set.of(
    "stop-event-coordinates.xml",
    "stop-event-stop-point.xml"
  );

  private final RequestHandler handler;

  public TriasResource(@Context OtpServerRequestContext context) {
    var transitService = context.transitService();
    var zoneId = context.triasApiParameters().timeZone().orElse(transitService.getTimeZone());
    var service = new CallAtStopService(transitService, context.graphFinder());
    var idMapper = idMapper(context.triasApiParameters());
    var serviceMapper = new OjpService(service, context.routingService(), idMapper, zoneId);
    this.handler = new RequestHandler(serviceMapper, TriasResource::ojpToTrias, "TRIAS");
  }

  @POST
  @Produces(MediaType.APPLICATION_XML)
  public Response index(String triasInput) {
    try {
      var ojp = OjpToTriasTransformer.triasToOjp(triasInput);
      return handler.handleRequest(ojp, RouteRequest.defaultValue());
    } catch (JAXBException | TransformerException e) {
      LOG.error("Error reading TRIAS request", e);
      return handler.error("Could not read TRIAS request.");
    }
  }

  @GET
  @Path("/explorer")
  @Produces(MediaType.TEXT_HTML)
  public Response explorer() throws IOException {
    return classpathResource("explorer.html");
  }

  @GET
  @Path("/static/config.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response configJson() throws IOException {
    return classpathResource("config.json");
  }

  @GET
  @Path("/static/api_templates.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response templatesJson() throws IOException {
    return classpathResource("api_templates.json");
  }

  @GET
  @Path("/static/{fileName}")
  @Produces(MediaType.APPLICATION_XML)
  public Response stopEventXml(@PathParam("fileName") String fileName) throws IOException {
    if (ALLOWED_CLASSPATH_RESOURCES.contains(fileName)) {
      return classpathResource(fileName);
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  private static FeedScopedIdMapper idMapper(TriasApiParameters triasApiConfig) {
    if (triasApiConfig.hideFeedId()) {
      return new HideFeedIdMapper(triasApiConfig.hardcodedInputFeedId());
    } else {
      return new DefaultFeedIdMapper();
    }
  }

  private static StreamingOutput ojpToTrias(OJP ojpOutput) {
    return os -> {
      Writer writer = new OutputStreamWriter(os);
      OjpToTriasTransformer.ojpToTrias(ojpOutput, writer);
      writer.flush();
    };
  }

  private static Response classpathResource(String name) throws IOException {
    final String resource = "explorer/" + name;
    var res = Objects.requireNonNull(
      TriasResource.class.getResource(resource),
      "%s not found".formatted(resource)
    ).openStream();
    return Response.ok(res).build();
  }
}
