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

package org.opentripplanner.routing.algorithm.strategies;

import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.algorithm.strategies.DefaultExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.ExtraEdgesStrategy;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;

/**
 * A base implementation of {@link ExtraEdgesStrategy} that does actually add any extra edges, but
 * that can be selectively extended to add new behavior.
 * 
 * @author bdferris
 * @see ExtraEdgesStrategy
 * @see DefaultExtraEdgesStrategy
 */
public class EmptyExtraEdgesStrategy implements ExtraEdgesStrategy {

    @Override
    public void addIncomingEdgesForOrigin(Map<Vertex, List<Edge>> extraEdges, Vertex origin) {

    }

    @Override
    public void addIncomingEdgesForTarget(Map<Vertex, List<Edge>> extraEdges, Vertex target) {

    }

    @Override
    public void addOutgoingEdgesForOrigin(Map<Vertex, List<Edge>> extraEdges, Vertex origin) {

    }

    @Override
    public void addOutgoingEdgesForTarget(Map<Vertex, List<Edge>> extraEdges, Vertex target) {

    }
}
