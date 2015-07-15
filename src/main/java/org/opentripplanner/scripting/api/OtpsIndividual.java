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
 * An individual is a point with coordinates, associated with some optional values (string, floats,
 * integers...)
 * 
 * @author laurent
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

    /**
     * @return The original location (lat/lon) of the individual, as created or loaded from input
     *         data.
     */
    public OtpsLatLon getLocation() {
        return new OtpsLatLon(lat, lon);
    }

    /**
     * @return The snapped location of the individual on the graph (ie a point on the nearest
     *         walkable/drivable street). This can be useful to output a more precise location for a
     *         generated grid point, as the returned location is the effective one used for
     *         path/time computations. Return NULL if the individual has never been evualuated (by a
     *         call to OtpsSPT.eval). Return the original location if the point can't be snapped
     *         (too far away from a street).
     */
    public OtpsLatLon getSnappedLocation() {
        if (cachedSample == null)
            return null;
        // Maybe the Sample should store the snapped location itself
        for (Edge e : cachedSample.v0.getOutgoingStreetEdges()) {
            if (e.getToVertex().equals(cachedSample.v1) && e.getGeometry() != null) {
                LineString geom = e.getGeometry();
                LengthIndexedLine liline = new LengthIndexedLine(geom);
                int d = cachedSample.d0 + cachedSample.d1;
                double k = d == 0 ? 0.0 : 1.0 * cachedSample.d0 / d;
                double x = liline.getStartIndex() + (liline.getEndIndex() - liline.getStartIndex())
                        * k;
                Coordinate p = liline.extractPoint(x);
                return new OtpsLatLon(p.y, p.x);
            }
        }
        return getLocation();
    }

    /**
     * Retrieve some data associated with this individual.
     * 
     * @param dataName The name of the data. For CSV population, this is the name of the column
     *        header (case sensitive).
     * @return The data as a string.
     */
    public String getStringData(String dataName) {
        if (data == null)
            return null;
        int index = population.getDataIndex(dataName);
        if (index >= 0 && index < data.length)
            return data[index];
        return null;
    }

    /**
     * Retrieve some data associated with this individual.
     * 
     * @param dataName The name of the data. For CSV population, this is the name of the column
     *        header (case sensitive).
     * @return The data as a float. Return NULL if the data is empty. Throw a NumberFormatException
     *         if the data can't be translated from a string.
     */
    public Double getFloatData(String dataName) {
        String str = getStringData(dataName);
        if (str == null)
            return null;
        return Double.parseDouble(str);
    }

    /**
     * Retrieve some data associated with this individual.
     * 
     * @param dataName The name of the data. For CSV population, this is the name of the column
     *        header (case sensitive).
     * @param def The default value in case the data is empty.
     * @return The data as a float. Return def if the data is empty. Throw a NumberFormatException
     *         if the data can't be translated from a string.
     */
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
        int boardings = cachedSample.evalBoardings(spt);
        double walkDistance = cachedSample.evalWalkDistance(spt);
        return new OtpsEvaluatedIndividual(this, time, boardings, walkDistance);
    }

    @Override
    public String toString() {
        return "Individual" + getLocation().toString();
    }
}
