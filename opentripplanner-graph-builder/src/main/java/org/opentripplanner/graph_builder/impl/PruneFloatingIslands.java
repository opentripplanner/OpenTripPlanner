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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.*;
import org.apache.log4j.Logger;
import org.opentripplanner.common.StreetUtils;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.*;

public class PruneFloatingIslands implements GraphBuilder {

    private static org.slf4j.Logger _log = LoggerFactory.getLogger(PruneFloatingIslands.class);

    @Getter
    @Setter
    private int maxIslandSize = 40;

    @Getter
    @Setter
    private String islandLogFile = "";

    public List<String> provides() {
        return Collections.emptyList();
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        StreetUtils.pruneFloatingIslands(graph, maxIslandSize, createLogger());
    }

    @Override
    public void checkInputs() {
        //no inputs
    }

    private String createLogger() {
        if (islandLogFile.isEmpty()) return null;
        try{
            PatternLayout layout =  new PatternLayout("%m\n");
            FileAppender appender = new RollingFileAppender(layout, islandLogFile, false);
            Logger.getLogger("islands").addAppender(appender);
        }catch (IOException ioe){
            _log.warn("could not create file appender for " + islandLogFile + " duo to: " + ioe.getMessage());
            return null;
        }
        return "islands";

    }

}