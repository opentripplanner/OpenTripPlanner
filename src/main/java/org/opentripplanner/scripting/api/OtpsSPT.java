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
import java.util.List;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * 
 */
public class OtpsSPT {

    private ShortestPathTree spt;

    private SampleFactory sampleFactory;

    protected OtpsSPT(ShortestPathTree spt, SampleFactory sampleFactory) {
        this.spt = spt;
        this.sampleFactory = sampleFactory;
    }

    public Long evalTime(double lat, double lon) {
        Sample sample = sampleFactory.getSample(lon, lat);
        if (sample == null)
            return null;
        long time = sample.eval(this.spt);
        if (time == Long.MAX_VALUE)
            return null;
        return time;
    }

    public Long evalTime(OtpsIndividual individual) {
        return individual.evalTime(spt, sampleFactory);
    }

    public List<Long> evalTime(Iterable<OtpsIndividual> population) {
        List<Long> retval = new ArrayList<>(); // Size?
        for (OtpsIndividual individual : population) {
            retval.add(evalTime(individual));
        }
        return retval;
    }
}
