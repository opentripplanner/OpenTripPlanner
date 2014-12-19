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

package org.opentripplanner.scripting.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;

/**
 * 
 */
public class OtpsPopulation implements Iterable<OtpsIndividual> {

    private List<OtpsIndividual> individuals;

    protected OtpsPopulation(Population population) {
        individuals = new ArrayList<>(population.size());
        int index = 0;
        for (Individual individual : population) {
            individuals.add(new OtpsIndividual(index, individual));
            index++;
        }
    }

    public void addIndividual(double lat, double lon) {
        this.addIndividual(lat, lon, null, 1);
    }

    public void addIndividual(double lat, double lon, double input) {
        this.addIndividual(lat, lon, null, input);
    }

    public void addIndividual(double lat, double lon, String label, double input) {
        int index = individuals.size();
        OtpsIndividual individual = new OtpsIndividual(index, lat, lon, label, input);
        individuals.add(individual);
    }

    @Override
    public Iterator<OtpsIndividual> iterator() {
        // This seems to work. What the use for Guava ForwardingIterator then?
        return individuals.iterator();
    }
}
