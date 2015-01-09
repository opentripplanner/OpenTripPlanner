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
import org.opentripplanner.standalone.OTPServer;

/**
 * 
 */
public class OtpsEntryPoint {

    private OTPServer otpServer;

    private Object retval = null;

    public OtpsEntryPoint(OTPServer otpServer) {
        this.otpServer = otpServer;
    }

    public OtpsRouter getRouter() {
        return getRouter(null);
    }

    public OtpsRouter getRouter(String routerId) {
        return new OtpsRouter(otpServer.getRouter(routerId));
    }

    public OtpsRoutingRequest createRequest() {
        RoutingRequest rreq = new RoutingRequest();
        return new OtpsRoutingRequest(rreq);
    }

    public OtpsPopulation createEmptyPopulation() {
        return new OtpsPopulation();
    }

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

    public OtpsPopulation loadCSVPopulation(String filename, String latColName, String lonColName)
            throws IOException {
        return OtpsPopulation.loadFromCSV(filename, latColName, lonColName);
    }

    public OtpsPopulation loadRasterPopulation(String filename) {
        RasterPopulation rasterPop = new RasterPopulation();
        rasterPop.sourceFilename = filename;
        rasterPop.setup();
        return new OtpsPopulation(rasterPop);
    }

    public OtpsCsvOutput createCSVOutput() {
        return new OtpsCsvOutput();
    }

    public void setRetval(Object retval) {
        this.retval = retval;
    }

    public Object getRetval() {
        return retval;
    }
}
