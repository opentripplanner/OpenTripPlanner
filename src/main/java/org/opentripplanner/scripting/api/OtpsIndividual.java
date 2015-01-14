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

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

/**
 */
public class OtpsIndividual {

    protected double lon;

    protected double lat;

    protected String[] data;

    protected boolean isSampleSet = false;

    protected Sample cachedSample = null;

    protected Graph graph;

    protected OtpsPopulation population;

    protected OtpsIndividual(double lat, double lon, String[] data, OtpsPopulation population) {
        this.lon = lon;
        this.lat = lat;
        this.data = data;
        this.population = population;
    }

    public OtpsLatLon getLocation() {
        return new OtpsLatLon(lat, lon);
    }

    public OtpsLatLon getSnappedLocation() {
        if (cachedSample == null)
            return null;
        // Maybe the Sample should store the snapped location itself
        for (Edge e : cachedSample.v0.getOutgoingStreetEdges()) {
            if (e.getToVertex().equals(cachedSample.v1) && e.getGeometry() != null) {
                LineString geom = e.getGeometry();
                LengthIndexedLine liline = new LengthIndexedLine(geom);
                int t = cachedSample.t0 + cachedSample.t1;
                double k = t == 0 ? 0.0 : 1.0 * cachedSample.t0 / t;
                double x = liline.getStartIndex() + (liline.getEndIndex() - liline.getStartIndex())
                        * k;
                Coordinate p = liline.extractPoint(x);
                return new OtpsLatLon(p.y, p.x);
            }
        }
        return getLocation();
    }

    public String getStringData(String dataName) {
        if (data == null)
            return null;
        int index = population.getDataIndex(dataName);
        if (index >= 0 && index < data.length)
            return data[index];
        return null;
    }

    public Double getFloatData(String dataName) {
        String str = getStringData(dataName);
        if (str == null)
            return null;
        return Double.parseDouble(str);
    }

    public Double getFloatData(String dataName, double def) {
        Double val = getFloatData(dataName);
        return val == null ? def : val;
    }

    protected synchronized OtpsEvaluatedIndividual eval(ShortestPathTree spt,
            SampleFactory sampleFactory) {
        Graph sptGraph = spt.getOptions().getRoutingContext().graph;
        if (!isSampleSet || graph != sptGraph) {
            cachedSample = sampleFactory.getSample(lon, lat);
            // Note: sample can be null here
            graph = sptGraph;
            isSampleSet = true;
        }
        if (cachedSample == null)
            return null;
        long time = cachedSample.eval(spt);
        if (time == Long.MAX_VALUE)
            return null;
        return new OtpsEvaluatedIndividual(this, time);
    }

    @Override
    public String toString() {
        return "Individual" + getLocation().toString();
    }
}
