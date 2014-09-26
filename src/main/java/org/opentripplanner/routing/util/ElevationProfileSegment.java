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

package org.opentripplanner.routing.util;

import java.io.Serializable;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;

/**
 * This class is an helper for Edges and Vertexes to store various data about elevation profiles.
 * 
 */
public class ElevationProfileSegment implements Serializable {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(ElevationProfileSegment.class);

    private static final ElevationProfileSegment FLAT_PROFILE = new ElevationProfileSegment();

    private byte[] packedElevationProfile;

    private float slopeSpeedFactor;

    private float slopeWorkFactor;

    private float maxSlope;

    private boolean flattened;

    private ElevationProfileSegment() {
        slopeSpeedFactor = 1.0f;
        slopeWorkFactor = 1.0f;
    }

    public ElevationProfileSegment(SlopeCosts costs, CoordinateSequence elev) {
        packedElevationProfile = CompactElevationProfile.compactElevationProfile(elev);
        slopeSpeedFactor = (float)costs.slopeSpeedFactor;
        slopeWorkFactor = (float)costs.slopeWorkFactor;
        maxSlope = (float)costs.maxSlope;
        flattened = costs.flattened;
    }

    public static ElevationProfileSegment getFlatProfile() {
        return FLAT_PROFILE;
    }

    public double getMaxSlope() {
        return maxSlope;
    }

    public boolean isFlattened() {
        return flattened;
    }

    public double getSlopeSpeedFactor() {
        return slopeSpeedFactor;
    }

    public double getSlopeWorkFactor() {
        return slopeWorkFactor;
    }

    public PackedCoordinateSequence getElevationProfile() {
        return CompactElevationProfile.uncompactElevationProfile(packedElevationProfile);
    }

    public PackedCoordinateSequence getElevationProfile(double start, double end) {
        return ElevationUtils.getPartialElevationProfile(getElevationProfile(), start, end);
    }

    public String toString() {
        String out = "";
        PackedCoordinateSequence elevationProfile = getElevationProfile();
        if (elevationProfile == null || elevationProfile.size() == 0) {
            return "(empty elevation profile)";
        }
        for (int i = 0; i < elevationProfile.size(); ++i) {
            Coordinate coord = elevationProfile.getCoordinate(i);
            out += "(" + coord.x + "," + coord.y + "), ";
        }
        return out.substring(0, out.length() - 2);
    }
}
