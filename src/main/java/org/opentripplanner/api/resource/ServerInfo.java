package org.opentripplanner.api.resource;

import org.opentripplanner.common.MavenVersion;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

@Path("/")
public class ServerInfo {

    /** Quality value prioritizes MIME types */
    static final String Q = ";qs=0.5";
    
    private static final ServerInfo SERVER_INFO = new ServerInfo();

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public static ServerInfo getServerInfo() {
        return SERVER_INFO;
    }    
    
    // Fields must be public or have a public getter to be auto-serialized to JSON

    public MavenVersion serverVersion = MavenVersion.VERSION;
    
    public String cpuName = "unknown";
    
    public int nCores = 0;

    /* It would make sense to have one object containing maven, git, and hardware subobjects. */
    
    /**
     * Determine the OTP version and CPU type of the running server. This information should not
     * change while the server is up, so it can safely be cached at startup.
     */
    public ServerInfo() {
        try {
            InputStream fis = new FileInputStream("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("model name")) {
                    cpuName = line.split(": ")[1];
                    nCores += 1;
                }
            }
            fis.close();
        } 
        catch (Exception e) {
            cpuName = "unknown";
            nCores = 0;
        }
    }
}
