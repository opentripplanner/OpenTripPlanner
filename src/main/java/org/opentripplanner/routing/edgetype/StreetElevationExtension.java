package org.opentripplanner.routing.edgetype;

import java.io.Serializable;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.util.lang.ToStringBuilder;

public class StreetElevationExtension implements Serializable {

  private final double distanceMeters;

  private final byte[] compactedElevationProfile;

  private final PackedCoordinateSequence packedElevationProfile;

  private final double effectiveBicycleSafetyDistance;

  private final double effectiveBikeDistance;

  private final double effectiveBikeWorkCost;

  private final double effectiveWalkDistance;

  private final float maxSlope;

  private final boolean flattened;

  private StreetElevationExtension(
    double distanceMeters,
    boolean computed,
    PackedCoordinateSequence packedElevationProfile,
    float effectiveBicycleSafetyFactor,
    double effectiveBikeDistanceFactor,
    double effectiveBikeWorkFactor,
    double effectiveWalkDistanceFactor,
    float maxSlope,
    boolean flattened
  ) {
    this.distanceMeters = distanceMeters;
    this.effectiveBicycleSafetyDistance = effectiveBicycleSafetyFactor * distanceMeters;
    this.effectiveBikeDistance = effectiveBikeDistanceFactor * distanceMeters;
    this.effectiveBikeWorkCost = effectiveBikeWorkFactor * distanceMeters;
    this.effectiveWalkDistance = effectiveWalkDistanceFactor * distanceMeters;
    this.maxSlope = maxSlope;
    this.flattened = flattened;

    if (computed) {
      this.compactedElevationProfile = null;
      this.packedElevationProfile = packedElevationProfile;
    } else {
      this.compactedElevationProfile =
        CompactElevationProfile.compactElevationProfileWithRegularSamples(packedElevationProfile);
      this.packedElevationProfile = null;
    }
  }

  public static void addToEdge(
    StreetEdge streetEdge,
    PackedCoordinateSequence elevationProfile,
    boolean computed
  ) {
    if (elevationProfile != null && elevationProfile.size() >= 2) {
      if (!streetEdge.isSlopeOverride() || computed) {
        var extension = calculateForEdge(streetEdge, elevationProfile, computed);
        streetEdge.setElevationExtension(extension);
      }
    }
  }

  public PackedCoordinateSequence getElevationProfile() {
    if (compactedElevationProfile != null) {
      return CompactElevationProfile.uncompactElevationProfileWithRegularSamples(
        compactedElevationProfile,
        distanceMeters
      );
    } else {
      return packedElevationProfile;
    }
  }

  public double getEffectiveBicycleSafetyDistance() {
    return effectiveBicycleSafetyDistance;
  }

  public double getEffectiveBikeDistance() {
    return effectiveBikeDistance;
  }

  public double getEffectiveBikeWorkCost() {
    return effectiveBikeWorkCost;
  }

  public double getEffectiveWalkDistance() {
    return effectiveWalkDistance;
  }

  public float getMaxSlope() {
    return this.maxSlope;
  }

  public boolean isFlattened() {
    return this.flattened;
  }

  public String toString() {
    return ToStringBuilder
      .of(StreetElevationExtension.class)
      .addBoolIfTrue("flattened", flattened)
      .addNum("distanceMeters", distanceMeters)
      .addNum("effectiveBicycleSafetyFactor", effectiveBicycleSafetyDistance)
      .addNum("effectiveBikeDistance", effectiveBikeDistance)
      .addNum("effectiveBikeWorkCost", effectiveBikeWorkCost)
      .addNum("effectiveWalkDistance", effectiveWalkDistance)
      .addNum("maxSlope", maxSlope)
      .toString();
  }

  private static StreetElevationExtension calculateForEdge(
    StreetEdge streetEdge,
    PackedCoordinateSequence elevationProfile,
    boolean computed
  ) {
    boolean slopeLimit = streetEdge.getPermission().allows(StreetTraversalPermission.CAR);
    SlopeCosts costs = ElevationUtils.getSlopeCosts(elevationProfile, slopeLimit);

    var effectiveBikeDistanceFactor = costs.slopeSpeedFactor;
    var effectiveBikeWorkFactor = costs.slopeWorkFactor;
    var effectiveWalkDistanceFactor = costs.effectiveWalkFactor;
    var maxSlope = (float) costs.maxSlope;
    var flattened = costs.flattened;

    float effectiveBicycleSafetyFactor = (float) (
      streetEdge.getBicycleSafetyFactor() *
      costs.lengthMultiplier +
      costs.slopeSafetyCost /
      streetEdge.getDistanceMeters()
    );

    if (
      Double.isInfinite(effectiveBicycleSafetyFactor) || Double.isNaN(effectiveBicycleSafetyFactor)
    ) {
      throw new IllegalStateException(
        "Elevation updated bicycleSafetyFactor is " + effectiveBicycleSafetyFactor
      );
    }

    if (streetEdge.isStairs()) {
      // Ignore elevation related costs for stairs, RoutingRequest#stairsTimeFactor is used instead.
      effectiveBikeDistanceFactor = 1.0;
      effectiveWalkDistanceFactor = 1.0;
    }

    return new StreetElevationExtension(
      streetEdge.getDistanceMeters(),
      computed,
      elevationProfile,
      effectiveBicycleSafetyFactor,
      effectiveBikeDistanceFactor,
      effectiveBikeWorkFactor,
      effectiveWalkDistanceFactor,
      maxSlope,
      flattened
    );
  }
}
