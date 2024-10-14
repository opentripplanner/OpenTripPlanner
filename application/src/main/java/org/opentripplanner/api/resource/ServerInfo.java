package org.opentripplanner.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.opentripplanner.api.model.serverinfo.ApiServerInfo;
import org.opentripplanner.model.projectinfo.OtpProjectInfo;

@Path("/")
public class ServerInfo {

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
}
