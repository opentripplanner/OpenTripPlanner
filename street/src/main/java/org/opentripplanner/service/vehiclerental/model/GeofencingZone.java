package org.opentripplanner.service.vehiclerental.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * A geofencing zone describing restrictions for traversing with a rental vehicle or dropping it off
 * inside the zone.
 * <p>
 * Restriction fields ({@code dropOffBanned}, {@code traversalBanned}, {@code rideStartBanned}) are
 * nullable: {@code null} means the zone does not specify that field, allowing lower-priority zones
 * to contribute a value via per-field precedence resolution. {@code true} means banned,
 * {@code false} means explicitly allowed.
 * <p>
 * The {@code businessArea} flag is set at construction time. A zone is a business area when all
 * ride/traversal booleans are permissive — fields like {@code maximumSpeedKph} and
 * {@code vehicleTypeIds} are orthogonal to business area classification.
 * <p>
 * Equality uses {@code id} + {@code priority} only. The geometry and restriction fields are
 * excluded because JTS Geometry equality iterates all coordinates (expensive in hot paths like
 * set lookups during A* traversal), and id+priority uniquely identifies a zone within a feed.
 */
public final class GeofencingZone {

  private final FeedScopedId id;

  @Nullable
  private final I18NString name;

  private final Geometry geometry;

  @Nullable
  private final Boolean dropOffBanned;

  @Nullable
  private final Boolean traversalBanned;

  @Nullable
  private final Boolean rideStartBanned;

  private final boolean businessArea;

  @Nullable
  private final List<String> vehicleTypeIds;

  @Nullable
  private final Integer maximumSpeedKph;

  private final int priority;

  public GeofencingZone(
    FeedScopedId id,
    @Nullable I18NString name,
    Geometry geometry,
    @Nullable Boolean dropOffBanned,
    @Nullable Boolean traversalBanned,
    @Nullable Boolean rideStartBanned,
    boolean businessArea,
    @Nullable List<String> vehicleTypeIds,
    @Nullable Integer maximumSpeedKph,
    int priority
  ) {
    this.id = Objects.requireNonNull(id);
    this.name = name;
    this.geometry = geometry;
    this.dropOffBanned = dropOffBanned;
    this.traversalBanned = traversalBanned;
    this.rideStartBanned = rideStartBanned;
    this.businessArea = businessArea;
    this.vehicleTypeIds = vehicleTypeIds;
    this.maximumSpeedKph = maximumSpeedKph;
    this.priority = priority;
  }

  public FeedScopedId id() {
    return id;
  }

  @Nullable
  public I18NString name() {
    return name;
  }

  public Geometry geometry() {
    return geometry;
  }

  @Nullable
  public Boolean dropOffBanned() {
    return dropOffBanned;
  }

  @Nullable
  public Boolean traversalBanned() {
    return traversalBanned;
  }

  @Nullable
  public Boolean rideStartBanned() {
    return rideStartBanned;
  }

  @Nullable
  public List<String> vehicleTypeIds() {
    return vehicleTypeIds;
  }

  @Nullable
  public Integer maximumSpeedKph() {
    return maximumSpeedKph;
  }

  public int priority() {
    return priority;
  }

  /**
   * Whether the zone has any restriction that bans riding or dropping off.
   */
  public boolean hasRestriction() {
    return (
      Boolean.TRUE.equals(dropOffBanned) ||
      Boolean.TRUE.equals(traversalBanned) ||
      Boolean.TRUE.equals(rideStartBanned)
    );
  }

  /**
   * Whether this zone describes a general business operating area. Riders can travel freely inside
   * but cannot leave the area.
   * <p>
   * This flag is set explicitly at construction time rather than inferred from absence of
   * restrictions, because zones with only non-ride restrictions (e.g., speed limits) would
   * otherwise be misclassified as business areas.
   */
  public boolean isBusinessArea() {
    return businessArea;
  }

  /**
   * Deep equality check comparing all fields including geometry and restriction values. Use this
   * when you need to detect any change to a zone (e.g., to decide whether to re-apply zones after
   * a GBFS update). This is expensive due to JTS Geometry comparison and should not be used on hot
   * paths — use {@link #equals(Object)} (id+priority only) for set lookups during routing.
   */
  public boolean isEquivalentTo(GeofencingZone other) {
    if (this == other) {
      return true;
    }
    if (other == null) {
      return false;
    }
    return (
      priority == other.priority &&
      businessArea == other.businessArea &&
      id.equals(other.id) &&
      Objects.equals(name, other.name) &&
      Objects.equals(geometry, other.geometry) &&
      Objects.equals(dropOffBanned, other.dropOffBanned) &&
      Objects.equals(traversalBanned, other.traversalBanned) &&
      Objects.equals(rideStartBanned, other.rideStartBanned) &&
      Objects.equals(vehicleTypeIds, other.vehicleTypeIds) &&
      Objects.equals(maximumSpeedKph, other.maximumSpeedKph)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GeofencingZone other)) {
      return false;
    }
    return priority == other.priority && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return 31 * id.hashCode() + priority;
  }

  @Override
  public String toString() {
    return "GeofencingZone{id=" + id + ", priority=" + priority + "}";
  }

  /**
   * Resolve the effective value of a restriction field across overlapping zones for a given
   * network, using per-field precedence. For each field independently, the highest-priority zone
   * (lowest priority value) that specifies (non-null) the field wins.
   *
   * @param zones the set of zones currently containing the state
   * @param network the committed rental network (null returns false)
   * @param fieldAccessor accessor for the field to resolve
   * @return true if the resolved value is TRUE, false if no zone specifies the field or if the
   *     resolved value is FALSE
   */
  public static boolean resolveField(
    Set<GeofencingZone> zones,
    @Nullable String network,
    Function<GeofencingZone, Boolean> fieldAccessor
  ) {
    if (zones.isEmpty() || network == null) {
      return false;
    }
    Boolean bestValue = null;
    int bestPriority = Integer.MAX_VALUE;
    for (var zone : zones) {
      if (!zone.id().getFeedId().equals(network)) {
        continue;
      }
      Boolean value = fieldAccessor.apply(zone);
      if (value != null && zone.priority() < bestPriority) {
        bestPriority = zone.priority();
        bestValue = value;
      }
    }
    return Boolean.TRUE.equals(bestValue);
  }
}
