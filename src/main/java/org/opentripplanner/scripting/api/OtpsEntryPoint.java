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

package org.opentripplanner.scripting.api;

import java.io.IOException;

import org.opentripplanner.analyst.batch.RasterPopulation;
import org.opentripplanner.analyst.batch.SyntheticRasterPopulation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPMain;
import org.opentripplanner.standalone.OTPServer;

import com.beust.jcommander.JCommander;

/**
 * This is the main entry point (facade) for use by scripts. An instance of this facade is
 * accessible through the bean named "otp" in the various scripts, or by creating one if using OTP
 * as a library from an external script.
 * 
 * This facade allow a script to access / create OTP objects (routers, populations, ...)
 * 
 * @see OtpsRouter
 * @see OtpsPopulation
 * 
 * @author laurent
 */
public class OtpsEntryPoint {

    private OTPServer otpServer;

    private Object retval = null;

    public OtpsEntryPoint(OTPServer otpServer) {
        this.otpServer = otpServer;
    }

    /**
     * Create an OTP scripting entry point using the same command-line parameters as used by the OTP
     * application. This is useful when using scripting from an outside environment, using OTP as a
     * library. For example in jython the following script 'myscript.py':
     * 
     * <pre>
     *   #!/usr/bin/jython
     *   from org.opentripplanner.scripting.api import OtpsEntryPoint
     *   otp = OtpsEntryPoint.fromArgs([ "-g", "." ])
     *   ...
     * </pre>
     * 
     * One can then run jython with:
     * 
     * <pre>
     *   CLASSPATH=otp.jar jython myscript.py
     * </pre>
     * 
     * @param args Command-line arguments, as used by the application.
     * @return The "otp" scripting facade bean.
     * @throws Exception
     */
    public static OtpsEntryPoint fromArgs(String[] args) throws Exception {
        if (args == null)
            args = new String[0];
        CommandLineParameters params = new CommandLineParameters();
        @SuppressWarnings("unused")
        JCommander jc = new JCommander(params, args);
        params.analyst = true; // Force analyst
        params.infer();
        OTPMain otpMain = new OTPMain(params);
        if (params.build != null) // TODO Enable this
            throw new IllegalArgumentException("Building from script is not yet supported.");
        if (params.scriptFile != null) // This would be weird
            throw new IllegalArgumentException("Scripting from script is not supported.");
        if (params.visualize) // Useless
            throw new IllegalArgumentException("Vizualizer from script is not supported.");
        if (params.server) // Why not?
            throw new IllegalArgumentException("Server from script is not supported.");
        otpMain.run();
        return new OtpsEntryPoint(otpMain.otpServer);
    }

    public static OtpsEntryPoint fromArgs() throws Exception {
        return fromArgs(null);
    }

    /**
     * @return The default router. If there is one router, this will be the default.
     */
    public OtpsRouter getRouter() {
        return getRouter(null);
    }

    /**
     * @param routerId The ID of the router to request.
     * @return The router of the given ID.
     */
    public OtpsRouter getRouter(String routerId) {
        return new OtpsRouter(otpServer.getRouter(routerId));
    }

    /**
     * @return A new plan request that can be used to plan route / shortest path tree on a router.
     */
    public OtpsRoutingRequest createRequest() {
        RoutingRequest rreq = new RoutingRequest();
        return new OtpsRoutingRequest(rreq);
    }

    /**
     * @return A new empty population.
     */
    public OtpsPopulation createEmptyPopulation() {
        return new OtpsPopulation();
    }

    /**
     * Create a grid of evently spaced points in a rectangle. Coordinates are expressed in the given
     * CRS, by default WGS84 geographical.
     * 
     * @param top
     * @param bottom
     * @param left
     * @param right
     * @param rows
     * @param cols
     * @return A new grid population of rows x cols individuals, regularly spaced.
     */
    public OtpsPopulation createGridPopulation(double top, double bottom, double left,
            double right, int rows, int cols) {
        SyntheticRasterPopulation rasterPop = new SyntheticRasterPopulation();
        rasterPop.left = left;
        rasterPop.right = right;
        rasterPop.top = top;
        rasterPop.bottom = bottom;
        rasterPop.cols = cols;
        rasterPop.rows = rows;
        rasterPop.setup();
        return new OtpsPopulation(rasterPop);
    }

    /**
     * Load a population from a CSV file.
     * 
     * @param filename The filename to load data from.
     * @param latColName The name of the latitude column header.
     * @param lonColName The name of the longitude column header.
     * @return A population containing an individual per CSV row.
     * @throws IOException If something bad happens (file not found, invalid format...)
     */
    public OtpsPopulation loadCSVPopulation(String filename, String latColName, String lonColName)
            throws IOException {
        return OtpsPopulation.loadFromCSV(filename, latColName, lonColName);
    }

    /**
     * Load a population from a raster (GeoTIFF) file.
     * 
     * @param filename The filename to load data from.
     * @return A population containing an individual per cell.
     */
    public OtpsPopulation loadRasterPopulation(String filename) {
        RasterPopulation rasterPop = new RasterPopulation();
        rasterPop.sourceFilename = filename;
        rasterPop.setup();
        return new OtpsPopulation(rasterPop);
    }

    /**
     * @return A new CSV output.
     */
    public OtpsCsvOutput createCSVOutput() {
        return new OtpsCsvOutput();
    }

    /**
     * @param retval Set the return value of the script, which will be returned in web mode. For
     *        command-line mode, the return value is not used (for now).
     */
    public void setRetval(Object retval) {
        this.retval = retval;
    }

    public Object getRetval() {
        return retval;
    }
}
