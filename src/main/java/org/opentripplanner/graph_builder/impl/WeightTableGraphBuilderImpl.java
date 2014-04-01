/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.algorithm.strategies.WeightTable;
import org.opentripplanner.routing.graph.Graph;

/**
 * Add a weight table to a graph, which provides information to the
 * TableRemainingWeightHeuristic
 * This builder should be run after all transit and street data,
 * as well as transit-street links are in place.
 */
public class WeightTableGraphBuilderImpl implements GraphBuilder {
	private Double maxWalkSpeed = null;
        private Double maxWalkDistance = null;

        public List<String> provides() {
            return Arrays.asList("weightTable");
        }

        public List<String> getPrerequisites() {
            return Arrays.asList("linking");
        }
        
	@Override
	public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
		WeightTable wt = new WeightTable(graph);
		if (maxWalkSpeed != null) {
			wt.setMaxWalkSpeed(maxWalkSpeed);
		}
		if (maxWalkDistance != null) {
		    wt.setMaxWalkDistance(maxWalkDistance);
		}
		wt.buildTable();
		graph.putService(WeightTable.class, wt);
	}

	/**
	 * The maximum walk speed that the weight table can support.  Using higher values
	 * than this during trip planning will lead to slower planning.
	 * @param maxWalkSpeed
	 */
	public void setMaxWalkSpeed(double maxWalkSpeed) {
		this.maxWalkSpeed = maxWalkSpeed;
	}

        /**
         * The maximum walk distance that the weight table can support, in meters.  
         * For trip planning requests with higher values than this, the weight table
         * will be inaccurate and the search will fall back on a simpler heuristic.
         * Building the weight table with a higher maxWalkDistance will take longer and
         * the resulting tables will take up more memory.
         * @param maxWalkDistance
         */
        public void setMaxWalkDistance(double maxWalkDistance) {
                this.maxWalkDistance = maxWalkDistance;
        }

        public double getMaxWalkSpeed() {
		return maxWalkSpeed;
	}
        
        public double getMaxWalkDistance() {
            return maxWalkDistance;
        }

        @Override
        public void checkInputs() {
            //no inputs
        }
}
