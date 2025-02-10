package org.opentripplanner.ext.vdv.trias;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Instant;
import org.opentripplanner.ext.vdv.VdvService;
import org.opentripplanner.ext.vdv.ojp.OjpMapper;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/trias/v1/")
@Produces("application/xml")
public class TriasResource {

  private static final Logger LOG = LoggerFactory.getLogger(TriasResource.class);

  private final VdvService vdvService;
  private final OjpMapper mapper;

  public TriasResource(@Context OtpServerRequestContext context) {
    this.vdvService = new VdvService(context.transitService());
    this.mapper = new OjpMapper(context.transitService().getTimeZone());
  }

  @GET
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
}
