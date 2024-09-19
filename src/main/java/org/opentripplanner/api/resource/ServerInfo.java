package org.opentripplanner.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import org.opentripplanner.api.model.serverinfo.ApiServerInfo;
import org.opentripplanner.model.projectinfo.OtpProjectInfo;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

@Path("/")
public class ServerInfo {

  private final ZoneId timeZone;
  private static final String cpuName;
  private static final int nCores ;

  public ServerInfo(@Context OtpServerRequestContext serverContext) {
    this.timeZone = serverContext.transitService().getTimeZone();
  }


  /**
   * Determine the OTP version and CPU type of the running server. This information should not
   * change while the server is up, so it can safely be cached at startup. The project info is not
   * available before the graph is loaded, so for this to work this class should not be loaded
   * BEFORE that.
   */
  static {
    var cpu = "unknown";
    int cores = 0;
    try {
      InputStream fis = new FileInputStream("/proc/cpuinfo");
      BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("model name")) {
          cpu = line.split(": ")[1];
          cores+= 1;
        }
      }
      fis.close();
    } catch (Exception ignore) {}
    cpuName = cpu;
    nCores = cores;
  }



  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiServerInfo getServerInfo() {
    return new ApiServerInfo(cpuName, nCores, OtpProjectInfo.projectInfo(), timeZone);
  }
}
