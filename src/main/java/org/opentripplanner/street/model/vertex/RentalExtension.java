package org.opentripplanner.street.model.vertex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.search.state.State;

/**
 * An extension which defines rules for how rental vehicles may or may not traverse a vertex.
 */
public sealed interface RentalExtension {
  /**
   * The static default instance which doesn't have any restrictions at all.
   */
  public static final RentalExtension NO_RESTRICTIONS = new NoExtension();

  boolean traversalBanned(State state);

  boolean dropOffBanned(State state);

  Set<DebugInfo> debug();

  default RentalExtension add(RentalExtension other) {
    return Composite.of(this, other);
  }

  default RentalExtension remove(RentalExtension toRemove) {
    return NO_RESTRICTIONS;
  }

  final class NoExtension implements RentalExtension {

    @Override
    public boolean traversalBanned(State state) {
      return false;
    }

    @Override
    public boolean dropOffBanned(State state) {
      return false;
    }

    @Override
    public Set<DebugInfo> debug() {
      return Set.of();
    }

    @Override
    public RentalExtension add(RentalExtension other) {
      return other;
    }
  }

  final class GeofencingZoneExtension implements RentalExtension {

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
    public Set<DebugInfo> debug() {
      var set = new HashSet<DebugInfo>();
      if (zone.traversalBanned()) {
        set.add(DebugInfo.NO_TRAVERSAL);
      }
      if (zone.dropOffBanned()) {
        set.add(DebugInfo.NO_DROP_OFF);
      }
      return Set.copyOf(set);
    }

    @Override
    public String toString() {
      return zone.id().toString();
    }
  }

  final class BusinessAreaBorder implements RentalExtension {

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
    public Set<DebugInfo> debug() {
      return Set.of(DebugInfo.BUSINESS_AREA_BORDER);
    }
  }

  final class Composite implements RentalExtension {

    private final RentalExtension[] exts;

    private Composite(RentalExtension... exts) {
      var set = new HashSet<>(Arrays.asList(exts));
      this.exts = set.toArray(RentalExtension[]::new);
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
    public Set<DebugInfo> debug() {
      var set = new HashSet<DebugInfo>();
      for (var e : exts) {
        set.addAll(e.debug());
      }
      return Set.copyOf(set);
    }

    @Override
    public RentalExtension add(RentalExtension other) {
      return Composite.of(this, other);
    }

    private static RentalExtension of(RentalExtension... exts) {
      var set = new HashSet<>(Arrays.asList(exts));
      if (set.size() == 1) {
        return exts[0];
      } else {
        return new Composite(set.toArray(RentalExtension[]::new));
      }
    }

    @Override
    public RentalExtension remove(RentalExtension toRemove) {
      var newExts = Arrays
        .stream(exts)
        .filter(e -> !e.equals(toRemove))
        .toArray(RentalExtension[]::new);
      if (newExts.length == 0) {
        return null;
      } else {
        return Composite.of(newExts);
      }
    }
  }

  enum DebugInfo {
    NO_TRAVERSAL,
    NO_DROP_OFF,
    BUSINESS_AREA_BORDER,
  }
}
