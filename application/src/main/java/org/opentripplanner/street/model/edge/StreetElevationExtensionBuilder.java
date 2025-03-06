package org.opentripplanner.street.model.edge;

import java.util.Optional;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * Build an elevation profile for a StreetEdge.
 */
public class StreetElevationExtensionBuilder {

  private StreetTraversalPermission permission;
  private PackedCoordinateSequence elevationProfile;

  private float bicycleSafetyFactor;
  private double distanceInMeters;
  private float walkSafetyFactor;
  private boolean isStairs;
  private boolean computed;
  private boolean isSlopeOverride;

  public static StreetElevationExtensionBuilder of(StreetEdge streetEdge) {
    return new StreetElevationExtensionBuilder()
      .withComputed(true)
      .withDistanceInMeters(streetEdge.getDistanceMeters())
      .withSlopeOverride(streetEdge.isSlopeOverride())
      .withStairs(streetEdge.isStairs())
      .withWalkSafetyFactor(streetEdge.getWalkSafetyFactor())
      .withBicycleSafetyFactor(streetEdge.getBicycleSafetyFactor())
      .withPermission(streetEdge.getPermission());
  }

  public static StreetElevationExtensionBuilder of(StreetEdgeBuilder<?> seb) {
    return new StreetElevationExtensionBuilder()
      .withComputed(true)
      .withSlopeOverride(seb.slopeOverride())
      .withStairs(seb.stairs())
      .withWalkSafetyFactor(seb.walkSafetyFactor())
      .withBicycleSafetyFactor(seb.bicycleSafetyFactor())
      .withPermission(seb.permission());
  }

  public Optional<StreetElevationExtension> build() {
    if (elevationProfileHasAtLeastTwoPoints() && (!isSlopeOverride || computed)) {
      return Optional.of(buildInternal());
    }
    return Optional.empty();
  }

  public StreetElevationExtensionBuilder withPermission(StreetTraversalPermission permission) {
    this.permission = permission;
    return this;
  }

  public StreetElevationExtensionBuilder withElevationProfile(
    PackedCoordinateSequence parentEdgeElevationProfile
  ) {
    this.elevationProfile = parentEdgeElevationProfile;
    return this;
  }

  public StreetElevationExtensionBuilder withBicycleSafetyFactor(float bicycleSafetyFactor) {
    this.bicycleSafetyFactor = bicycleSafetyFactor;
    return this;
  }

  public StreetElevationExtensionBuilder withDistanceInMeters(double distanceInMeters) {
    this.distanceInMeters = distanceInMeters;
    return this;
  }

  public StreetElevationExtensionBuilder withWalkSafetyFactor(float walkSafetyFactor) {
    this.walkSafetyFactor = walkSafetyFactor;
    return this;
  }

  public StreetElevationExtensionBuilder withStairs(boolean stairs) {
    isStairs = stairs;
    return this;
  }

  public StreetElevationExtensionBuilder withComputed(boolean computed) {
    this.computed = computed;
    return this;
  }

  public StreetElevationExtensionBuilder withSlopeOverride(boolean slopeOverride) {
    isSlopeOverride = slopeOverride;
    return this;
  }

  private boolean elevationProfileHasAtLeastTwoPoints() {
    return elevationProfile != null && elevationProfile.size() >= 2;
  }

  private StreetElevationExtension buildInternal() {
    boolean slopeLimit = permission.allows(StreetTraversalPermission.CAR);
    SlopeCosts costs = ElevationUtils.getSlopeCosts(elevationProfile, slopeLimit);

    var effectiveBikeDistanceFactor = costs.slopeSpeedFactor;
    var effectiveBikeWorkFactor = costs.slopeWorkFactor;
    var effectiveWalkDistanceFactor = costs.effectiveWalkFactor;
    var maxSlope = (float) costs.maxSlope;
    var flattened = costs.flattened;

    float effectiveBicycleSafetyFactor = (float) (bicycleSafetyFactor * costs.lengthMultiplier +
      costs.slopeSafetyCost / distanceInMeters);

    if (
      Double.isInfinite(effectiveBicycleSafetyFactor) || Double.isNaN(effectiveBicycleSafetyFactor)
    ) {
      throw new IllegalStateException(
        "Elevation updated bicycleSafetyFactor is " + effectiveBicycleSafetyFactor
      );
    }

    float effectiveWalkSafetyFactor = (float) (walkSafetyFactor * effectiveWalkDistanceFactor);

    if (Double.isInfinite(effectiveWalkSafetyFactor) || Double.isNaN(effectiveWalkSafetyFactor)) {
      throw new IllegalStateException(
        "Elevation updated walkSafetyFactor is " + effectiveWalkSafetyFactor
      );
    }

    if (isStairs) {
      // Ignore elevation related costs for stairs, RouteRequest#stairsTimeFactor is used instead.
      effectiveBikeDistanceFactor = 1.0;
      effectiveWalkDistanceFactor = 1.0;
    }

    return new StreetElevationExtension(
      distanceInMeters,
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
