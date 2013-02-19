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

package org.opentripplanner.analyst.batch.aggregator;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.ResultSet;

/**
 * An Aggregator which calculates the weighted average of the shortest path lengths to all Individuals in the destination Population.
 * 
 * This can be used to find the average distance/time to all people or jobs in a metropolitan area from a given origin.
 * 
 * @author andrewbyrd
 */
public class WeightedAverageAggregator implements Aggregator {

    @Override
    public double computeAggregate(ResultSet rs) {
        double aggregate = 0;
        int i = 0;
        int n = 0;
        for (Individual target: rs.population) {
            double t = rs.results[i++];
            if (Double.isInfinite(target.input))
                continue;
            if (Double.isInfinite(t) || t < 0)
                continue;
            aggregate += target.input * t;
            n += target.input;
        }
        aggregate /= n;
        return aggregate;
    }

}
