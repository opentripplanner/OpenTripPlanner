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

/**
 * A simple exponential decay (gravity) model, as defined in, for example
 * Geurs, Karst T, and Bert van Wee. "Accessibility Evaluation of Land-use and Transport
 * Strategies: Review and Research Directions." Journal of Transport Geography 12, no. 2
 * (2004): 127–140. http://www.sciencedirect.com/science/article/pii/S0966692303000607, 133.
 * 
 * The equation is A_i = \sum{D_j e^{-\Beta c_{ij}}}, where A_i is the accessibility at location i,
 * D_j is the attractiveness of location j, \Beta is the cost sensitivity parameter, and c_{ij} is
 * the cost of going from i to j (Geurs and Wee 2004, 133).
 */
public class DecayAccumulator implements Accumulator {

    private static final Logger LOG = LoggerFactory.getLogger(DecayAccumulator.class);

    /**
     * This is the cost sensitivity parameter, the multiplier for the exponentiation of cost.
     * See Geurs and Wee 2004, 133. (Note that Geurs and Wee 2004 call it beta, but it's the same
     * variable)
     */
    public double lambda; 

    /**
     * This is a convenience function to set the cost sensitivity with more real-world values.
     * The value passed in is multiplied by 60 and the reciprocal is taken.
     * @param halfLifeMinutes
     */
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
            // TODO: is there any reason why t == 0 is invalid? (mattwigway)
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
