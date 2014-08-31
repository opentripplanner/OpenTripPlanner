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
 * An Aggregator that simply sums the data for all destination Individuals less than a given distance/weight away from the origin point. This can be
 * used for simple cumulative opportunity accessibility indicators.
 * 
 * @author andrewbyrd
 */
public class ThresholdSumAggregator implements Aggregator {

    public int threshold = 60 * 90; // 1.5 hours in seconds

    @Override
    public double computeAggregate(ResultSet rs) {
        double aggregate = 0;
        int i = 0;
        for (Individual target : rs.population) {
            double t = rs.results[i];
            if (t > 0 && t < threshold)
                aggregate += target.input;
            i++;
        }
        return aggregate;
    }

}
