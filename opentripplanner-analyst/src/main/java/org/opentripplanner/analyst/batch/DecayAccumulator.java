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

import lombok.Setter;
public class DecayAccumulator implements Accumulator {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdAccumulator.class);

    @Setter double lambda; 

    public void setHalfLifeMinutes(int halfLifeMinutes) {
        float halfLifeSeconds = halfLifeMinutes * 60;
        lambda = 1.0/halfLifeSeconds;
    }
    
    @Override
    public void accumulate(double amount, ResultSet current, ResultSet accumulated) {
        if (current.population != accumulated.population) {
            LOG.error("population mismatch.");
            return;
        }
        int n = accumulated.population.size();
        for (int i = 0; i < n; i++) {
            double t = current.results[i]; 
            if (t > 0) {
                double decay = Math.exp(-lambda * t);
                double decayed = amount * decay;
                accumulated.results[i] += decayed;
            }
        }
    }

    @Override
    public void finish() {
        // nothing to do
    }

}
