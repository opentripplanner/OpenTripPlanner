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

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 */
public class OtpsIndividual {

    protected int index;

    protected String label;

    protected double lon;

    protected double lat;

    protected double input;

    protected boolean isSampleSet = false;

    protected Sample cachedSample = null;

    protected Graph graph;

    protected OtpsIndividual(int index, Individual i) {
        this(index, i.lat, i.lon, i.label, i.input);
    }

    protected OtpsIndividual(int index, double lat, double lon, String label, double input) {
        this.index = index;
        this.label = label;
        this.lon = lon;
        this.lat = lat;
        this.input = input;
    }

    protected synchronized Long evalTime(ShortestPathTree spt, SampleFactory sampleFactory) {
        Graph sptGraph = spt.getOptions().getRoutingContext().graph;
        if (!isSampleSet || graph != sptGraph) {
            cachedSample = sampleFactory.getSample(lon, lat);
            // Note: sample can be null here
            graph = sptGraph;
            isSampleSet = true;
        }
        long time = cachedSample.eval(spt);
        if (time == Long.MAX_VALUE)
            return null;
        return time;
    }
}
