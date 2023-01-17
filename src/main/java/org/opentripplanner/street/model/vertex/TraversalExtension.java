package org.opentripplanner.street.model.vertex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.search.state.State;

/**
 * An extension which defines rules for how rental vehicles may or may not traverse a vertex.
 */
public sealed interface TraversalExtension {
  /**
   * The static default instance which doesn't have any restrictions at all.
   */
  public static final TraversalExtension NO_RESTRICTION = new NoRestriction();

  /**
   * If the current state is banned from traversing the location.
   */
  boolean traversalBanned(State state);

  /**
   * If the current state is allowed to drop its free-floating vehicle.
   */
  boolean dropOffBanned(State state);

  /**
   * Return the types of restrictions in this extension for debugging purposes.
   */
  Set<RestrictionType> debugTypes();

  /**
   * Add another extension to this one and returning the combined one.
   */
  default TraversalExtension add(TraversalExtension other) {
    return Composite.of(this, other);
  }

  /**
   * Remove the extension from this one
   */
  default TraversalExtension remove(TraversalExtension toRemove) {
    return NO_RESTRICTION;
  }

  /**
   * No restriction on traversal which is the default.
   */
  final class NoRestriction implements TraversalExtension {

    @Override
    public boolean traversalBanned(State state) {
      return false;
    }

    @Override
    public boolean dropOffBanned(State state) {
      return false;
    }

    @Override
    public Set<RestrictionType> debugTypes() {
      return Set.of();
    }

    @Override
    public TraversalExtension add(TraversalExtension other) {
      return other;
    }
  }

  /**
   * Traversal is restricted by the properties of the geofencing zone.
   */
  final class GeofencingZoneExtension implements TraversalExtension {

    private final GeofencingZone zone;

    public GeofencingZoneExtension(GeofencingZone zone) {
      this.zone = zone;
    }

    public GeofencingZone zone() {
      return zone;
    }

    @Override
    public boolean traversalBanned(State state) {
      if (state.isRentingVehicle()) {
        return (
          zone.id().getFeedId().equals(state.getVehicleRentalNetwork()) && zone.traversalBanned()
        );
      } else {
        return false;
      }
    }

    @Override
    public boolean dropOffBanned(State state) {
      if (state.isRentingVehicle()) {
        return (
          zone.id().getFeedId().equals(state.getVehicleRentalNetwork()) && zone.dropOffBanned()
        );
      } else {
        return false;
      }
    }

    @Override
    public Set<RestrictionType> debugTypes() {
      var set = new HashSet<RestrictionType>();
      if (zone.traversalBanned()) {
        set.add(RestrictionType.NO_TRAVERSAL);
      }
      if (zone.dropOffBanned()) {
        set.add(RestrictionType.NO_DROP_OFF);
      }
      return Set.copyOf(set);
    }

    @Override
    public String toString() {
      return zone.id().toString();
    }
  }

  /**
   * Traversal is banned since this location is the border of a business area.
   */
  final class BusinessAreaBorder implements TraversalExtension {

    private final String network;

    public BusinessAreaBorder(String network) {
      this.network = network;
    }

    @Override
    public boolean traversalBanned(State state) {
      return state.isRentingVehicle() && network.equals(state.getVehicleRentalNetwork());
    }

    @Override
    public boolean dropOffBanned(State state) {
      return traversalBanned(state);
    }

    @Override
    public Set<RestrictionType> debugTypes() {
      return Set.of(RestrictionType.BUSINESS_AREA_BORDER);
    }
  }

  /**
   * Combines multiple restrictions into one.
   */
  final class Composite implements TraversalExtension {

    private final TraversalExtension[] exts;

    private Composite(TraversalExtension... exts) {
      var set = new HashSet<>(Arrays.asList(exts));
      this.exts = set.toArray(TraversalExtension[]::new);
    }

    @Override
    public boolean traversalBanned(State state) {
      for (var ext : exts) {
        if (ext.traversalBanned(state)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean dropOffBanned(State state) {
      for (var ext : exts) {
        if (ext.dropOffBanned(state)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Set<RestrictionType> debugTypes() {
      var set = new HashSet<RestrictionType>();
      for (var e : exts) {
        set.addAll(e.debugTypes());
      }
      return Set.copyOf(set);
    }

    @Override
    public TraversalExtension add(TraversalExtension other) {
      return Composite.of(this, other);
    }

    private static TraversalExtension of(TraversalExtension... exts) {
      var set = new HashSet<>(Arrays.asList(exts));
      if (set.size() == 1) {
        return exts[0];
      } else {
        return new Composite(set.toArray(TraversalExtension[]::new));
      }
    }

    @Override
    public TraversalExtension remove(TraversalExtension toRemove) {
      var newExts = Arrays
        .stream(exts)
        .filter(e -> !e.equals(toRemove))
        .toArray(TraversalExtension[]::new);
      if (newExts.length == 0) {
        return null;
      } else {
        return Composite.of(newExts);
      }
    }
  }

  enum RestrictionType {
    NO_TRAVERSAL,
    NO_DROP_OFF,
    BUSINESS_AREA_BORDER,
  }
}
