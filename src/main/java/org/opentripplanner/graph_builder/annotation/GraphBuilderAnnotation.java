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

package org.opentripplanner.graph_builder.annotation;

import java.io.Serializable;
import org.opentripplanner.graph_builder.AnnotationsToHTML;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
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
 * {@link #getHTMLMessage() } is used in {@link AnnotationsToHTML} to create HTML annotations.
 * It is useful to put links to OSM in annotations.
 * 
 * @author andrewbyrd
 */
public abstract class GraphBuilderAnnotation implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(GraphBuilderAnnotation.class);

    private static final long serialVersionUID = 20121004L;

//    /** Generally, this should return the Vertex or Edge that is most relevant to this annotation. */
//    public abstract Object getReferencedObject();
    
    public String toString() {
        return "GraphBuilderAnnotation: " + this.getMessage();
    }

    public abstract String getMessage();

    public String getHTMLMessage() {
        return this.getMessage().replace("<", "&lt;").replace(">", "&gt;");
    }

    public Edge getReferencedEdge() {
        return null;
    }

    public Vertex getReferencedVertex() {
        return null;
    }

}
