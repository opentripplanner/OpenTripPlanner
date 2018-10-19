package org.opentripplanner.routing.edgetype;

import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A StreetEdge with elevation data.
 * 
 * @author laurent
 */
public class StreetWithElevationEdge extends StreetEdge {

    private static final long serialVersionUID = 1L;

    private byte[] packedElevationProfile;

    private float slopeSpeedFactor = 1.0f;

    private float slopeWorkFactor = 1.0f;

    private float maxSlope;

    private boolean flattened;

    public StreetWithElevationEdge(StreetVertex v1, StreetVertex v2, LineString geometry,
            I18NString name, double length, StreetTraversalPermission permission, boolean back) {
        super(v1, v2, geometry, name, length, permission, back);
    }

    public StreetWithElevationEdge(StreetVertex v1, StreetVertex v2, LineString geometry,
            String name, double length, StreetTraversalPermission permission, boolean back) {
        super(v1, v2, geometry, new NonLocalizedString(name), length, permission, back);
    }

    @Override
    public StreetWithElevationEdge clone() {
        return (StreetWithElevationEdge) super.clone();
    }

    public boolean setElevationProfile(PackedCoordinateSequence elev, boolean computed) {
        if (elev == null || elev.size() < 2) {
            return false;
        }
        if (super.isSlopeOverride() && !computed) {
            return false;
        }
        boolean slopeLimit = getPermission().allows(StreetTraversalPermission.CAR);
        SlopeCosts costs = ElevationUtils.getSlopeCosts(elev, slopeLimit);

        packedElevationProfile = CompactElevationProfile.compactElevationProfile(elev);
        slopeSpeedFactor = (float)costs.slopeSpeedFactor;
        slopeWorkFactor = (float)costs.slopeWorkFactor;
        maxSlope = (float)costs.maxSlope;
        flattened = costs.flattened;

        bicycleSafetyFactor *= costs.lengthMultiplier;
        bicycleSafetyFactor += costs.slopeSafetyCost / getDistance();
        return costs.flattened;
    }

    @Override
    public PackedCoordinateSequence getElevationProfile() {
        return CompactElevationProfile.uncompactElevationProfile(packedElevationProfile);
    }

    @Override
    public boolean isElevationFlattened() {
        return flattened;
    }

    @Override
    public float getMaxSlope() {
        return maxSlope;
    }

    @Override
    public double getSlopeSpeedEffectiveLength() {
        return slopeSpeedFactor * getDistance();
    }

    @Override
    public double getSlopeWorkCostEffectiveLength() {
        return slopeWorkFactor * getDistance();
    }

    @Override
    public String toString() {
        return "StreetWithElevationEdge(" + getId() + ", " + getName() + ", " + fromv + " -> "
                + tov + " length=" + this.getDistance() + " carSpeed=" + this.getCarSpeed()
                + " permission=" + this.getPermission() + ")";
    }
}
