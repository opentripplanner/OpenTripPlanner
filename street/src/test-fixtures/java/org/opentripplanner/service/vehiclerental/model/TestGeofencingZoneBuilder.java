package org.opentripplanner.service.vehiclerental.model;

import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * Test builder for {@link GeofencingZone}. Provides readable test zone construction without
 * exposing a test-only convenience constructor on the production class.
 */
public class TestGeofencingZoneBuilder {

  private final FeedScopedId id;

  @Nullable
  private I18NString name;

  @Nullable
  private Geometry geometry;

  @Nullable
  private Boolean dropOffBanned;

  @Nullable
  private Boolean traversalBanned;

  @Nullable
  private Boolean rideStartBanned;

  private boolean businessArea;

  @Nullable
  private List<String> vehicleTypeIds;

  @Nullable
  private Integer maximumSpeedKph;

  private int priority = 0;

  private TestGeofencingZoneBuilder(FeedScopedId id) {
    this.id = id;
  }

  public static TestGeofencingZoneBuilder of(FeedScopedId id) {
    return new TestGeofencingZoneBuilder(id);
  }

  public static TestGeofencingZoneBuilder of(String network, String zoneId) {
    return new TestGeofencingZoneBuilder(new FeedScopedId(network, zoneId));
  }

  public TestGeofencingZoneBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  public TestGeofencingZoneBuilder withGeometry(Geometry geometry) {
    this.geometry = geometry;
    return this;
  }

  public TestGeofencingZoneBuilder withDropOffBanned(Boolean dropOffBanned) {
    this.dropOffBanned = dropOffBanned;
    return this;
  }

  public TestGeofencingZoneBuilder withTraversalBanned(Boolean traversalBanned) {
    this.traversalBanned = traversalBanned;
    return this;
  }

  public TestGeofencingZoneBuilder withRideStartBanned(Boolean rideStartBanned) {
    this.rideStartBanned = rideStartBanned;
    return this;
  }

  public TestGeofencingZoneBuilder withBusinessArea(boolean businessArea) {
    this.businessArea = businessArea;
    return this;
  }

  public TestGeofencingZoneBuilder withVehicleTypeIds(List<String> vehicleTypeIds) {
    this.vehicleTypeIds = vehicleTypeIds;
    return this;
  }

  public TestGeofencingZoneBuilder withMaximumSpeedKph(Integer maximumSpeedKph) {
    this.maximumSpeedKph = maximumSpeedKph;
    return this;
  }

  public TestGeofencingZoneBuilder withPriority(int priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Shorthand for a no-drop-off zone (drop-off banned, traversal allowed).
   */
  public TestGeofencingZoneBuilder noDropOff() {
    this.dropOffBanned = true;
    this.traversalBanned = false;
    return this;
  }

  /**
   * Shorthand for a no-traversal zone (traversal banned, drop-off allowed).
   */
  public TestGeofencingZoneBuilder noTraversal() {
    this.dropOffBanned = false;
    this.traversalBanned = true;
    return this;
  }

  /**
   * Shorthand for a business area (all restrictions permissive).
   */
  public TestGeofencingZoneBuilder asBusinessArea() {
    this.dropOffBanned = false;
    this.traversalBanned = false;
    this.rideStartBanned = false;
    this.businessArea = true;
    return this;
  }

  public GeofencingZone build() {
    return new GeofencingZone(
      id,
      name,
      geometry,
      dropOffBanned,
      traversalBanned,
      rideStartBanned,
      businessArea,
      vehicleTypeIds,
      maximumSpeedKph,
      priority
    );
  }
}
