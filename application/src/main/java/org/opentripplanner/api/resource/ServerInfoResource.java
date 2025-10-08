package org.opentripplanner.api.resource;

import static org.opentripplanner.api.model.serverinfo.OtpBadgeGenerator.generateOtpBadgeSvg;
import static org.opentripplanner.api.model.serverinfo.OtpBadgeGenerator.isValidColor;
import static org.opentripplanner.api.model.serverinfo.OtpBadgeGenerator.isValidLabel;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.opentripplanner.api.model.serverinfo.ApiServerInfo;
import org.opentripplanner.model.projectinfo.OtpProjectInfo;

@Path("/")
public class ServerInfoResource {

  private static final ApiServerInfo SERVER_INFO = createServerInfo();

  /**
   * Determine the OTP version and CPU type of the running server. This information should not
   * change while the server is up, so it can safely be cached at startup. The project info is not
   * available before the graph is loaded, so for this to work this class should not be loaded
   * BEFORE that.
   */
  public static ApiServerInfo createServerInfo() {
    String cpuName = "unknown";
    int nCores = 0;
    try {
      InputStream fis = new FileInputStream("/proc/cpuinfo");
      BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("model name")) {
          cpuName = line.split(": ")[1];
          nCores += 1;
        }
      }
      fis.close();
    } catch (Exception ignore) {}

    return new ApiServerInfo(cpuName, nCores, OtpProjectInfo.projectInfo());
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiServerInfo getServerInfo() {
    return SERVER_INFO;
  }

  @GET
  @Path("version-badge.svg")
  @Produces("image/svg+xml;charset=utf-8")
  public Response getVersionBadge(
    @QueryParam("color") @DefaultValue("#E43") String color,
    @QueryParam("label") @DefaultValue("OTP Version") String label
  ) {
    if (!isValidColor(color)) {
      return Response.status(Response.Status.BAD_REQUEST)
        .entity("The 'color' parameter contains unexpected characters...")
        .build();
    }
    if (!isValidLabel(label)) {
      return Response.status(Response.Status.BAD_REQUEST)
        .entity("The 'label' parameter contains unexpected characters...")
        .build();
    }
    var version =
      "%s (%s)".formatted(SERVER_INFO.version.getVersion(), SERVER_INFO.otpSerializationVersionId);
    var svg = generateOtpBadgeSvg(label, color, version);
    var cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    // The "Cache-Control: no-cache" and "Last-Modified" is required by GitHub to avoid aggressive
    // caching.
    return Response.ok(svg).cacheControl(cacheControl).lastModified(new Date()).build();
  }
}
