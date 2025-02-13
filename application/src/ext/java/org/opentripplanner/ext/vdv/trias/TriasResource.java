package org.opentripplanner.ext.vdv.trias;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.Objects;
import org.opentripplanner.ext.vdv.VdvService;
import org.opentripplanner.ext.vdv.ojp.OjpMapper;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/trias/v1")
public class TriasResource {

  private static final Logger LOG = LoggerFactory.getLogger(TriasResource.class);

  private final VdvService vdvService;
  private final OjpMapper mapper;

  public TriasResource(@Context OtpServerRequestContext context) {
    this.vdvService = new VdvService(context.transitService());
    this.mapper = new OjpMapper(context.transitService().getTimeZone());
  }

  @POST
  @Path("/")
  @Produces("application/xml")
  public Response index() {
    try {
      var tripTimesOnDate = vdvService.findStopTimesInPattern();
      var ojp = mapper.mapStopTimesInPattern(tripTimesOnDate, Instant.now());

      StreamingOutput stream = os -> {
        Writer writer = new BufferedWriter(new OutputStreamWriter(os));
        OjpToTriasTransformer.transform(ojp, writer);
        writer.flush();
      };
      return Response.ok(stream).build();
    } catch (Exception e) {
      LOG.error("Error producing TRIAS response", e);
      return Response.serverError().build();
    }
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
