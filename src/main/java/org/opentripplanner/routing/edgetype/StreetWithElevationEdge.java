package org.opentripplanner.routing.edgetype;

import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
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

    // an array of gradients as % incline
    private byte[] gradients;

    // an array of the length in meters of the corresponding gradient at the same index
    private short[] gradientLengths;

    // an array of the approximate resistive drag force component of the corresponding gradient at the same index. Since
    // there can be numerous elevation differences of gradients within an edge, these are approximations based off of
    // the minimum altitude seen for the corresonding gradient on this edge. This is an overestimate of aerodynamic
    // drag.
    // TODO just use one Cda for the minimum altitude, this is overkill
    private float[] gradientCdas;

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

        gradients = costs.gradients;
        gradientLengths = costs.gradientLengths;
        gradientCdas = costs.gradientCdas;

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

    /**
     * Override the calculateSpeed method, but only do special calculations for Micromobility. The elevation-dependent
     * micromobility speed will differ according to calculated gradients. In order to save computing time, the gradients
     * along a road are pre-calculated and allocated into bins of 1% grade. Two different arrays are analyzed. One array
     * has information about the gradient and the other has information about the length in meters of this gradient. All
     * of the resulting travel times and meters of each gradient segment are added up and then used to calculate an
     * average speed for the entire road.
     */
    @Override
    public double calculateSpeed(RoutingRequest options, TraverseMode traverseMode, long timeMillis) {
        if (traverseMode != TraverseMode.MICROMOBILITY) return super.calculateSpeed(options, traverseMode, timeMillis);
        // calculate the travel time it would tak to traverse each gradient
        double distance = 0;
        double time = 0;

        // TODO: figure out why this is null sometimes
        if (gradients == null) return super.calculateSpeed(options, traverseMode, timeMillis);

        for (int i = 0; i < gradients.length; i++) {
            distance += gradientLengths[i];
            time += gradientLengths[i] / Math.min(
                calculateMicromobilitySpeed(
                    options.watts,
                    options.weight,
                    Math.atan(gradients[i] / 100.0),
                    this.getRollingResistanceCoefficient(),
                    gradientCdas[i],
                    options.minimumMicromobilitySpeed,
                    options.maximumMicromobilitySpeed
                ),
                // make sure the speed limit is obeyed
                getCarSpeed()
            );
        }

        return Math.min(
            distance / time,
            // make sure the speed limit is obeyed
            getCarSpeed()
        );
    }

    @Override
    public String toString() {
        return "StreetWithElevationEdge(" + getId() + ", " + getName() + ", " + fromv + " -> "
                + tov + " length=" + this.getDistance() + " carSpeed=" + this.getCarSpeed()
                + " permission=" + this.getPermission() + ")";
    }
}
