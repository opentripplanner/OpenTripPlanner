package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A StreetEdge with elevation data.
 * 
 * @author laurent
 */
public class StreetWithElevationEdge extends StreetEdge {

    private static final long serialVersionUID = 1L;

    private double effectiveWalkDistanceFactor = 1.0;

    private double effectiveBikeDistanceFactor = 1.0;

    private double effectiveBikeWorkFactor = 1.0;

    private byte[] packedElevationProfile;

    private float maxSlope;

    private boolean flattened;

    /**
     * Remember to call the {@link #setElevationProfile(PackedCoordinateSequence, boolean)} to initiate elevation data.
     */
    public StreetWithElevationEdge(StreetVertex v1, StreetVertex v2, LineString geometry,
            I18NString name, double length, StreetTraversalPermission permission, boolean back) {
        super(v1, v2, geometry, name, length, permission, back);

    }

    public StreetWithElevationEdge(StreetVertex v1, StreetVertex v2, LineString geometry,
            String name, double length, StreetTraversalPermission permission, boolean back) {
        this(v1, v2, geometry, new NonLocalizedString(name), length, permission, back);
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

        packedElevationProfile = CompactElevationProfile.compactElevationProfileWithRegularSamples(elev);
        effectiveBikeDistanceFactor = costs.slopeSpeedFactor;
        effectiveBikeWorkFactor = costs.slopeWorkFactor;
        maxSlope = (float)costs.maxSlope;
        flattened = costs.flattened;
        effectiveWalkDistanceFactor = costs.effectiveWalkFactor;

        bicycleSafetyFactor *= costs.lengthMultiplier;
        bicycleSafetyFactor += costs.slopeSafetyCost / getDistanceMeters();
        return costs.flattened;
    }

    @Override
    public PackedCoordinateSequence getElevationProfile() {
        return CompactElevationProfile.uncompactElevationProfileWithRegularSamples(
                packedElevationProfile,
                getEffectiveWalkDistance()
        );
    }

    public boolean hasPackedElevationProfile () { return packedElevationProfile != null; }

    @Override
    public boolean isElevationFlattened() {
        return flattened;
    }

    @Override
    public float getMaxSlope() {
        return maxSlope;
    }

    @Override
    public double getEffectiveBikeDistance() {
        return effectiveBikeDistanceFactor * getDistanceMeters();
    }

    @Override
    public double getEffectiveBikeWorkCost() {
        return effectiveBikeWorkFactor * getDistanceMeters();
    }

    @Override
    public double getEffectiveWalkDistance() {
        return effectiveWalkDistanceFactor * getDistanceMeters();
    }

    @Override
    public String toString() {
        return "StreetWithElevationEdge(" + getName() + ", " + fromv + " -> "
                + tov + " length=" + this.getDistanceMeters() + " carSpeed=" + this.getCarSpeed()
                + " permission=" + this.getPermission() + ")";
    }
}
