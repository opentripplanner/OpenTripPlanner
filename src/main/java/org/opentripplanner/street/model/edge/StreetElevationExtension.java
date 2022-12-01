package org.opentripplanner.street.model.edge;

import java.io.Serializable;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.framework.geometry.CompactElevationProfile;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class StreetElevationExtension implements Serializable {

  /**
   * Distance of the edge projected on a 2d plane.
   */
  private final double distanceMeters;

  private final byte[] compactedElevationProfile;

  private final PackedCoordinateSequence packedElevationProfile;

  private final double effectiveBicycleSafetyDistance;

  private final double effectiveBikeDistance;

  private final double effectiveBikeDistanceForWorkCost;

  private final double effectiveWalkDistance;

  private final double effectiveWalkSafetyDistance;

  private final double distanceWithElevation;

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
    double effectiveWalkSafetyFactor,
    double lengthMultiplier,
    float maxSlope,
    boolean flattened
  ) {
    this.distanceMeters = distanceMeters;
    this.effectiveBicycleSafetyDistance = effectiveBicycleSafetyFactor * distanceMeters;
    this.effectiveBikeDistance = effectiveBikeDistanceFactor * distanceMeters;
    this.effectiveBikeDistanceForWorkCost = effectiveBikeWorkFactor * distanceMeters;
    this.effectiveWalkDistance = effectiveWalkDistanceFactor * distanceMeters;
    this.effectiveWalkSafetyDistance = effectiveWalkSafetyFactor * distanceMeters;
    this.distanceWithElevation = lengthMultiplier * distanceMeters;
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

  /**
   * The distance multiplied by the {@link StreetEdge#bicycleSafetyFactor}, but also considering the
   * increased length caused by the slope.
   */
  public double getEffectiveBicycleSafetyDistance() {
    return effectiveBicycleSafetyDistance;
  }

  /**
   * The distance multiplied by a factor considering how much faster/slower it is to bile the edge,
   * compared to if it was flat.
   */
  public double getEffectiveBikeDistance() {
    return effectiveBikeDistance;
  }

  /**
   * The distance multiplied by a factor considering how much more/less convenient it is to bike
   * the edge, compared to if it was flat. This is calculated form the energy usage of the cyclist.
   */
  public double getEffectiveBikeDistanceForWorkCost() {
    return effectiveBikeDistanceForWorkCost;
  }

  /**
   * The distance multiplied by a factor considering how much faster/slower it is to walk the edge,
   * compared to if it was flat.
   */
  public double getEffectiveWalkDistance() {
    return effectiveWalkDistance;
  }

  /**
   * The distance multiplied by the {@link StreetEdge#walkSafetyFactor}, but also considering the
   * increased length caused by the slope.
   */
  public double getEffectiveWalkSafetyDistance() {
    return effectiveWalkSafetyDistance;
  }

  /**
   * Physical distance which takes the elevation into account. The distanceMeters is a 2d distance.
   */
  public double getDistanceWithElevation() {
    return distanceWithElevation;
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
      .addNum("effectiveBikeDistanceForWorkCost", effectiveBikeDistanceForWorkCost)
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

    float effectiveWalkSafetyFactor = (float) (
      streetEdge.getWalkSafetyFactor() * effectiveWalkDistanceFactor
    );

    if (Double.isInfinite(effectiveWalkSafetyFactor) || Double.isNaN(effectiveWalkSafetyFactor)) {
      throw new IllegalStateException(
        "Elevation updated walkSafetyFactor is " + effectiveWalkSafetyFactor
      );
    }

    if (streetEdge.isStairs()) {
      // Ignore elevation related costs for stairs, RouteRequest#stairsTimeFactor is used instead.
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
      effectiveWalkSafetyFactor,
      costs.lengthMultiplier,
      maxSlope,
      flattened
    );
  }
}
