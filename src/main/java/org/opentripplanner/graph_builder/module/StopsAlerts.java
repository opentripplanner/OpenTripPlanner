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

package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Iterables;
import org.opentripplanner.graph_builder.module.stopsAlerts.IStopTester;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * It prints the stops that satisfy certain criteria.
 * The output is a list of stops, some of the stops attributes (coordinates and etc.) and the criteria it satisfies.
 */

public class StopsAlerts implements GraphBuilderModule {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(StopsAlerts.class);

    List<IStopTester> stopTesters = new ArrayList<IStopTester>();

    String logFile = "";

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        try {
            PrintWriter pw = new PrintWriter(new File(logFile));
            pw.printf("%s,%s,%s,%s\n","stopId","lon","lat","types");
            for (TransitStop ts : Iterables.filter(graph.getVertices(), TransitStop.class)) {
                StringBuilder types = new StringBuilder();
                for(IStopTester stopTester:stopTesters){
                    if(stopTester.fulfillDemands(ts,graph)){
                        if(types.length() > 0) types.append(";");
                        types.append(stopTester.getType());
                    }
                }
                if(types.length() > 0) {
                    pw.printf("%s,%f,%f,%s\n",ts.getStopId(), ts.getCoordinate().x,
                            ts.getCoordinate().y, types.toString());
                }
            }
            pw.close();            
        } catch (FileNotFoundException e) {
            LOG.error("Failed to write StopsAlerts log file", e);
        }
    }

    @Override
    public void checkInputs() {
        if(logFile.isEmpty())
            throw new RuntimeException("missing log file name");
    }
}
