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

package org.opentripplanner.graph_builder.impl;

import lombok.Setter;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.graph_builder.impl.stopsAlerts.IStopTester;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StopsAlerts implements GraphBuilder {

    @Setter
    List<IStopTester> stopTesters = new ArrayList<IStopTester>();

    @Setter
    String logFile = "";

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
//        if(graph.getService(StreetVertexIndexService.class) == null) {
//            //TODO:check if necessary and throw expansion
//            throw new RuntimeException("could not find Street Vertex Index Service");
//        }
        Logger stopsLog = LoggerFactory.getLogger(LoggerAppenderProvider.createCsvFile4LoggerCat(logFile,"stops"));
        stopsLog.info(String.format("%s,%s,%s","lon","lat","types"));
        for (TransitStop ts : IterableLibrary.filter(graph.getVertices(), TransitStop.class)) {
            StringBuilder types = new StringBuilder();
            for(IStopTester stopTester:stopTesters){
                if(stopTester.fulfillDemands(ts,graph)){
                    if(types.length() > 0) types.append(",");
                    types.append(stopTester.getType());
                }
            }
            if(types.length() > 0) {
                stopsLog.info(String.format("%f,%f,%s",ts.getCoordinate().x,ts.getCoordinate().y,types.toString()));
            }
        }
    }

    @Override
    public List<String> provides() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getPrerequisites() {
        return Arrays.asList("transit","streets");
    }

    @Override
    public void checkInputs() {
        if(logFile.isEmpty())
            throw new RuntimeException("missing log file name");
    }
}
