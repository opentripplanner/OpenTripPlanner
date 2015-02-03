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

package org.opentripplanner.graph_builder.services;

import java.util.HashMap;
import java.util.List;

import org.opentripplanner.routing.graph.Graph;

/** Modules that add elements to a graph. These are plugins to the GraphBuilder. */
public interface GraphBuilderModule {

    /** Process whatever inputs were supplied to this module and add the resulting elements to the given graph. */
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra);

    /** Check that all inputs to the graphbuilder are valid; throw an exception if not. */
    public void checkInputs();

}
