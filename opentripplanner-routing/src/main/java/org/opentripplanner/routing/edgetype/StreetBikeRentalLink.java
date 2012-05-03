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

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.util.ElevationProfileSegment;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * This represents the connection between a street vertex and a bike rental station vertex.
 * 
 */
public class StreetBikeRentalLink extends AbstractEdge implements EdgeWithElevation {

    private static final long serialVersionUID = 1L;

    private static GeometryFactory _geometryFactory = new GeometryFactory();

    private BikeRentalStationVertex bikeRentalStationVertex;

    private ElevationProfileSegment elevationProfileSegment;

    public StreetBikeRentalLink(StreetVertex fromv, BikeRentalStationVertex tov) {
        super(fromv, tov);
        bikeRentalStationVertex = tov;
        elevationProfileSegment = new ElevationProfileSegment(getGeometry().getLength());
    }

    public StreetBikeRentalLink(BikeRentalStationVertex fromv, StreetVertex tov) {
        super(fromv, tov);
        bikeRentalStationVertex = fromv;
        elevationProfileSegment = new ElevationProfileSegment(getGeometry().getLength());
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return elevationProfileSegment.getLength();
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { fromv.getCoordinate(), tov.getCoordinate() };
        return _geometryFactory.createLineString(coordinates);
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    public String getName() {
        return bikeRentalStationVertex.getName();
    }

    public State traverse(State s0) {
        // disallow traversing two StreetBikeRentalLinks in a row.
        // prevents router using bike rental stations as shortcuts to get around
        // turn restrictions.
        if (s0.getBackEdge() instanceof StreetBikeRentalLink)
            return null;
        FixedModeEdge en = new FixedModeEdge(this, s0.getNonTransitMode(s0.getOptions()));
        StateEditor s1 = s0.edit(this, en);
        // TODO LG Increment time/weight based on real distance?
        s1.incrementTimeInSeconds(1);
        s1.incrementWeight(1);
        return s1.makeState();
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return options.getModes().contains(TraverseMode.BICYCLE) ? 0 : Double.POSITIVE_INFINITY;
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }

    public String toString() {
        return "StreetBikeRentalLink(" + fromv + " -> " + tov + ")";
    }

    @Override
    public PackedCoordinateSequence getElevationProfile() {
        return elevationProfileSegment.getElevationProfile();
    }

    @Override
    public PackedCoordinateSequence getElevationProfile(double start, double end) {
        return elevationProfileSegment.getElevationProfile(start, end);
    }

    @Override
    public boolean setElevationProfile(PackedCoordinateSequence elev, boolean computed) {
        return elevationProfileSegment.setElevationProfile(elev, computed, false);
    }

}
