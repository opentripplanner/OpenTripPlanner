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

package org.opentripplanner.gbannotation;

import java.io.Serializable;

import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents noteworthy events or errors that occur during the graphbuilding process.
 * 
 * This is in the routing subproject (rather than graphbuilder) to avoid making routing depend on the entire graphbuilder subproject. Graphbuilder
 * already depends on routing.
 * 
 * Typically we want to create an annotation object, store it in the graph that is being built, and log it at the same time. Automatically logging in
 * the annotation object constructor or in the Graph will lead to the wrong compilation unit/line number being reported in the logs. It seems that we
 * cannot modify the behavior of the logger to report a log event one stack frame higher than usual because the true logging mechanism is behind a
 * facade. We cannot invert the process and log an annotation object which would attach itself to a graph upon creation because the logger methods
 * only accept strings. Thus, a static register method on this class that creates an annotation, adds it to a graph, and returns a message string for
 * that annotation.
 * 
 * @author andrewbyrd
 */
public abstract class GraphBuilderAnnotation implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(GraphBuilderAnnotation.class);

    private static final long serialVersionUID = 20121004L;

    public static String register(Graph graph, GraphBuilderAnnotation gba) {
        //graph.addBuilderAnnotation(gba);
        return gba.getMessage() + " (annotation registered)";
    }

    public String toString() {
        return "GraphBuilderAnnotation: " + this.getMessage();
    }

    public abstract String getMessage();

    public static void logSummary(Iterable<GraphBuilderAnnotation> gbas) {
        // an EnumMap would be nice, but Integers are immutable...
//        int[] counts = new int[Variety.values().length];
//        LOG.info("Summary (number of each type of annotation):");
//        for (GraphBuilderAnnotation gba : gbas)
//            ++counts[gba.variety.ordinal()];
//        for (Variety v : Variety.values())
//            LOG.info("    {} - {}", v.toString(), counts[v.ordinal()]);
    }

}
