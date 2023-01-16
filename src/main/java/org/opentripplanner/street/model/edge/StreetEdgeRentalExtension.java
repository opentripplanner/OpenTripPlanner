package org.opentripplanner.street.model.edge;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.search.state.State;

public sealed interface StreetEdgeRentalExtension {
  /**
   * The static default instance which doesn't have any restrictions at all.
   */
  public static final StreetEdgeRentalExtension NO_RESTRICTIONS = new NoExtension();

  boolean traversalBanned(State state);

  boolean dropOffBanned(State state);

  Set<DebugInfo> debug();

  default StreetEdgeRentalExtension add(StreetEdgeRentalExtension other) {
    return Composite.of(this, other);
  }

  default StreetEdgeRentalExtension remove(StreetEdgeRentalExtension toRemove) {
    return NO_RESTRICTIONS;
  }

  final class NoExtension implements StreetEdgeRentalExtension {

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
    public StreetEdgeRentalExtension add(StreetEdgeRentalExtension other) {
      return other;
    }
  }

  final class GeofencingZoneExtension implements StreetEdgeRentalExtension {

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
          zone.id().getFeedId().equals(state.getVehicleRentalNetwork()) &&
          zone.passingThroughBanned()
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
      if (zone.passingThroughBanned()) {
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

  final class BusinessAreaBorder implements StreetEdgeRentalExtension {

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

  final class Composite implements StreetEdgeRentalExtension {

    private final StreetEdgeRentalExtension[] exts;

    private Composite(StreetEdgeRentalExtension... exts) {
      var set = new HashSet<>(Arrays.asList(exts));
      this.exts = set.toArray(StreetEdgeRentalExtension[]::new);
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
    public StreetEdgeRentalExtension add(StreetEdgeRentalExtension other) {
      return Composite.of(this, other);
    }

    private static StreetEdgeRentalExtension of(StreetEdgeRentalExtension... exts) {
      var set = new HashSet<>(Arrays.asList(exts));
      if (set.size() == 1) {
        return exts[0];
      } else {
        return new Composite(set.toArray(StreetEdgeRentalExtension[]::new));
      }
    }

    @Override
    public StreetEdgeRentalExtension remove(StreetEdgeRentalExtension toRemove) {
      var newExts = Arrays
        .stream(exts)
        .filter(e -> !e.equals(toRemove))
        .toArray(StreetEdgeRentalExtension[]::new);
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
