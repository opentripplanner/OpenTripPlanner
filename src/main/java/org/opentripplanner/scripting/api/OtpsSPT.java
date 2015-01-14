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

    public OtpsEvaluatedIndividual eval(double lat, double lon) {
        return eval(new OtpsIndividual(lat, lon, null, null));
    }

    public OtpsEvaluatedIndividual eval(OtpsIndividual individual) {
        return individual.eval(spt, sampleFactory);
    }

    public OtpsLatLon getSnappedOrigin() {
        return new OtpsLatLon(spt.getOptions().rctx.origin.getLat(),
                spt.getOptions().rctx.origin.getLon());
    }

    public List<OtpsEvaluatedIndividual> eval(Iterable<OtpsIndividual> population) {
        List<OtpsEvaluatedIndividual> retval = new ArrayList<>(); // Size?
        for (OtpsIndividual individual : population) {
            OtpsEvaluatedIndividual evaluated = eval(individual);
            if (evaluated != null)
                retval.add(evaluated);
        }
        return retval;
    }
}
