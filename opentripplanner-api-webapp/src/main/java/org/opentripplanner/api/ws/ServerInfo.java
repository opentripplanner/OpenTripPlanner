/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.ws;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.common.MavenVersion;

@Path("/serverinfo")
@XmlRootElement
public class ServerInfo {
    
    private static final ServerInfo SERVER_INFO = new ServerInfo();
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public static ServerInfo getServerInfo() throws JSONException {
        return SERVER_INFO;
    }    

    @XmlElement
    private MavenVersion serverVersion = MavenVersion.VERSION; 
    
    @XmlElement
    private String cpuName = "unknown";
    
    @XmlElement
    private int nCores = 0;

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
