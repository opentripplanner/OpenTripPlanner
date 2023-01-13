package org.opentripplanner.street.model.edge;

import java.util.ArrayList;
import java.util.Arrays;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.search.state.State;

public sealed interface StreetEdgeRentalExtension {
  /**
   * The static default instance which doesn't have any restrictions at all.
   */
  public static final StreetEdgeRentalExtension NO_EXTENSION = new NoExtension();

  boolean traversalBanned(State state);

  boolean dropOffBanned(State state);

  default StreetEdgeRentalExtension add(StreetEdgeRentalExtension other) {
    return new Composite(this, other);
  }

  default StreetEdgeRentalExtension remove(StreetEdgeRentalExtension toRemove) {
    return NO_EXTENSION;
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
  }

  final class Composite implements StreetEdgeRentalExtension {

    private final StreetEdgeRentalExtension[] exts;

    public Composite(StreetEdgeRentalExtension... exts) {
      this.exts = exts;
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
    public StreetEdgeRentalExtension add(StreetEdgeRentalExtension other) {
      var list = new ArrayList<>(Arrays.asList(exts));
      list.add(other);
      var array = list.toArray(StreetEdgeRentalExtension[]::new);
      return new Composite(array);
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
        return new Composite(newExts);
      }
    }
  }
}
