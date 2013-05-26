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

package org.opentripplanner.api.standalone;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class OTPRunner {

    private static final Logger LOG = LoggerFactory.getLogger(OTPRunner.class);

    @Parameter(names = { "-h", "--help"}, description = "Print this help message and exit", help = true)
    private boolean help;
    
    @Parameter(names = { "-v", "--verbose" }, description = "Verbose output")
    private boolean verbose = false;
   
    @Parameter(names = { "-p", "--port"}, description = "server port")
    private int port = 8080;

    @Parameter(names = { "-g", "--graph"}, description = "path to graph directory")
    private String graphDirectory = "/var/otp/graphs";
    
    @Parameter(names = { "-r", "--router"}, description = "default router ID")
    private String defaultRouterId = "";

    @Parameter(names = { "-s", "--server"}, description = "whether or not to start a server")
    private boolean startServer = true;

    @Parameter(description = "Files") // the rest of the parameters in one array
    private List<String> files = new ArrayList<String>();

    public static void main(String[] args) {
        OTPRunner runner = new OTPRunner();
        JCommander jc = new JCommander(runner, args);
        if (runner.help) {
            jc.usage();
            System.exit(0);
        }
        runner.run();
    }
    
    private void run() {
        LOG.info("Starting OTP server on port {} using graphs at {}", port, graphDirectory);
        GrizzlyServer server = new GrizzlyServer();
        server.start(new String[] { graphDirectory, defaultRouterId, String.valueOf(port) });
    }
    
}


