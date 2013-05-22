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

package org.opentripplanner.graph_builder;

import java.io.File;
import java.io.IOException;

import org.opentripplanner.gbannotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationsToHTML {

    private static Logger LOG = LoggerFactory.getLogger(AnnotationsToHTML .class); 

    public static void main(String[] args) throws IOException {
        // FIXME turn off all logging to avoid mixing log entries and HTML
//        @SuppressWarnings("unchecked")
//        List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
//        loggers.add(LogManager.getRootLogger());
//        for ( Logger logger : loggers ) {
//            logger.setLevel(Level.OFF);
//        }
        if (args.length < 1) {
            System.out.println("Usage: AnnotationsToHTML /path/to/graph");
        }

        String graphPath = args[0];

        File path = new File(graphPath);
        if (path.getName().equals("Graph.obj")) {
            path = path.getParentFile();
        }
        try {
            Graph graph = Graph.load(new File(graphPath), Graph.LoadLevel.BASIC);
            process(graph, graphPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void process(Graph graph, String path) {
        System.out.println("<html><head><title>Graph report for " + path + "</title></head><body>");

        for (GraphBuilderAnnotation annotation : graph.getBuilderAnnotations()) {
            System.out.println("<p>" + annotation.getMessage() + "</p>");
            
        }
        System.out.println("</body></html>");
    }

}
