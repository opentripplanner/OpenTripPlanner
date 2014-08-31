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

package org.opentripplanner.analyst.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThresholdAccumulator implements Accumulator {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdAccumulator.class);

    public int thresholdSeconds = 60 * 90; // 1.5 hours in seconds

    public void setThresholdMinutes(int minutes) {
        this.thresholdSeconds = minutes * 60;
    }
    
    @Override
    public void accumulate(double amount, ResultSet current, ResultSet accumulated) {
        if (current.population != accumulated.population) {
            return;
        }
        int n = accumulated.population.size();
        for (int i = 0; i < n; i++) {
            double t = current.results[i]; 
            if (t > 0 && t < thresholdSeconds) {
                accumulated.results[i] += amount;
            }
        }
    }

    @Override
    public void finish() {
        // nothing to do
    }

}
