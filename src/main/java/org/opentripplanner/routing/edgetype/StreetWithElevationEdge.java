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

    // The maximum resistive drag force component along this StreetWithElevationEdge. The difference of this resistive
    // drag force component is likely extremely small along the vast majority of edges in the graph. Therefore, don't
    // store all values in an array like the gradients and gradient lengths. Instead, use the maximum resistive drag
    // component which would correspond to drag resistive force at the minimum altitude seen on this edge. This is an
    // overestimate of aerodynamic drag.
    private double maximumDragResistiveForceComponent;

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
        maximumDragResistiveForceComponent = costs.maximumDragResistiveForceComponent;

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
     * Override the calculateSpeed method, but only do special calculations for Micromobility. There are separate
     * methods that are used to calculate speed for walking and bicycling. However, the calculations for bicycling are
     * questionable and this method could theoretically be used to calculate bicycling speeds.
     *
     * The elevation-dependent micromobility speed will differ according to calculated gradients. In order to save
     * computing time, the gradients along a road are pre-calculated and allocated into bins of 1% grade. Two different
     * arrays are analyzed. One array has information about the gradient and the other has information about the length
     * in meters of this gradient. All of the resulting travel times and meters of each gradient segment are added up
     * and then used to calculate an average speed for the entire road.
     */
    @Override
    public double calculateSpeed(RoutingRequest options, TraverseMode traverseMode, long timeMillis) {
        // use default StreetEdge method to calculate speed if the traverseMode is not micromobility
        if (traverseMode != TraverseMode.MICROMOBILITY) return super.calculateSpeed(options, traverseMode, timeMillis);

        // TODO: figure out why this is null sometimes
        if (gradients == null) return super.calculateSpeed(options, traverseMode, timeMillis);

        // calculate and accumulate the total travel time and distance it would take to traverse each gradient
        // these values will eventually be used to calculate an overall average speed.
        double distance = 0;
        double time = 0;

        // iterate over each gradient/distance entry and determine the travel time
        for (int i = 0; i < gradients.length; i++) {
            distance += gradientLengths[i];
            time += gradientLengths[i] / Math.min(
                calculateMicromobilitySpeed(
                    options.watts,
                    options.weight,
                    Math.atan(gradients[i] / 100.0),
                    this.getRollingResistanceCoefficient(),
                    maximumDragResistiveForceComponent,
                    options.minimumMicromobilitySpeed,
                    options.maximumMicromobilitySpeed
                ),
                // make sure the speed limit is obeyed
                getCarSpeed()
            );
        }

        if (time == 0) {
            // very short edges can cause divide-by-zero errors. Return the max speed a micromobility vehicle could
            // travel along the road
            return Math.min(options.maximumMicromobilitySpeed, getCarSpeed());
        }

        // return the overall average speed by dividing the accumulated distance and time
        return distance / time;
    }

    @Override
    public String toString() {
        return "StreetWithElevationEdge(" + getId() + ", " + getName() + ", " + fromv + " -> "
                + tov + " length=" + this.getDistance() + " carSpeed=" + this.getCarSpeed()
                + " permission=" + this.getPermission() + ")";
    }
}
