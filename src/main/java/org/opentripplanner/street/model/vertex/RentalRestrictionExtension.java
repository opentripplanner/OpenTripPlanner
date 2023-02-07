package org.opentripplanner.street.model.vertex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.search.state.State;

/**
 * An extension which defines rules for how rental vehicles may or may not traverse a vertex.
 */
public sealed interface RentalRestrictionExtension {
  /**
   * The static default instance which doesn't have any restrictions at all.
   */
  public static final RentalRestrictionExtension NO_RESTRICTION = new NoRestriction();

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
  default RentalRestrictionExtension add(RentalRestrictionExtension other) {
    return Composite.of(this, other);
  }

  /**
   * Remove the extension from this one
   */
  default RentalRestrictionExtension remove(RentalRestrictionExtension toRemove) {
    return NO_RESTRICTION;
  }

  /**
   * Return all extensions contained in this one as a list.
   */
  List<RentalRestrictionExtension> toList();

  /**
   * List all networks that have a restriction in this extension.
   */
  List<String> networks();

  boolean hasRestrictions();

  Set<String> noDropOffNetworks();

  /**
   * No restriction on traversal which is the default.
   */
  final class NoRestriction implements RentalRestrictionExtension {

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
    public RentalRestrictionExtension add(RentalRestrictionExtension other) {
      return other;
    }

    @Override
    public List<RentalRestrictionExtension> toList() {
      return List.of();
    }

    @Override
    public List<String> networks() {
      return List.of();
    }

    @Override
    public boolean hasRestrictions() {
      return false;
    }

    @Override
    public Set<String> noDropOffNetworks() {
      return Set.of();
    }
  }

  /**
   * Traversal is restricted by the properties of the geofencing zone.
   */
  final class GeofencingZoneExtension implements RentalRestrictionExtension {

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
          zone.traversalBanned() &&
          (
            state.unknownRentalNetwork() ||
            zone.id().getFeedId().equals(state.getVehicleRentalNetwork())
          )
        );
      } else {
        return false;
      }
    }

    @Override
    public boolean dropOffBanned(State state) {
      if (state.isRentingVehicle()) {
        return (
          zone.dropOffBanned() &&
          (
            state.unknownRentalNetwork() ||
            zone.id().getFeedId().equals(state.getVehicleRentalNetwork())
          )
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
    public List<RentalRestrictionExtension> toList() {
      return List.of(this);
    }

    @Override
    public List<String> networks() {
      return List.of(zone.id().getFeedId());
    }

    @Override
    public boolean hasRestrictions() {
      return true;
    }

    @Override
    public Set<String> noDropOffNetworks() {
      if (zone.dropOffBanned()) {
        return Set.of(zone.id().getFeedId());
      } else {
        return Set.of();
      }
    }

    @Override
    public String toString() {
      return zone.id().toString();
    }
  }

  /**
   * Traversal is banned since this location is the border of a business area.
   */
  final class BusinessAreaBorder implements RentalRestrictionExtension {

    private final String network;

    public BusinessAreaBorder(String network) {
      this.network = network;
    }

    @Override
    public boolean traversalBanned(State state) {
      if (state.getRequest().departAt()) {
        return state.isRentingVehicle() && network.equals(state.getVehicleRentalNetwork());
      } else {
        return state.isRentingVehicle();
      }
    }

    @Override
    public boolean dropOffBanned(State state) {
      return false;
    }

    @Override
    public Set<RestrictionType> debugTypes() {
      return Set.of(RestrictionType.BUSINESS_AREA_BORDER);
    }

    @Override
    public List<RentalRestrictionExtension> toList() {
      return List.of(this);
    }

    @Override
    public boolean hasRestrictions() {
      return true;
    }

    @Override
    public Set<String> noDropOffNetworks() {
      return Set.of();
    }

    @Override
    public List<String> networks() {
      return List.of(network);
    }
  }

  /**
   * Combines multiple restrictions into one.
   */
  final class Composite implements RentalRestrictionExtension {

    private final RentalRestrictionExtension[] exts;

    private Composite(RentalRestrictionExtension... exts) {
      for (var ext : exts) {
        if (ext instanceof Composite) {
          throw new IllegalArgumentException(
            "Composite extension cannot be nested into one another."
          );
        }
      }
      var set = new HashSet<>(Arrays.asList(exts));
      this.exts = set.toArray(RentalRestrictionExtension[]::new);
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
    public RentalRestrictionExtension add(RentalRestrictionExtension other) {
      return Composite.of(this, other);
    }

    private static RentalRestrictionExtension of(RentalRestrictionExtension... exts) {
      var set = Arrays.stream(exts).flatMap(e -> e.toList().stream()).collect(Collectors.toSet());
      if (set.size() == 1) {
        return List.copyOf(set).get(0);
      } else {
        return new Composite(set.toArray(RentalRestrictionExtension[]::new));
      }
    }

    @Override
    public RentalRestrictionExtension remove(RentalRestrictionExtension toRemove) {
      var newExts = Arrays
        .stream(exts)
        .filter(e -> !e.equals(toRemove))
        .toArray(RentalRestrictionExtension[]::new);
      if (newExts.length == 0) {
        return null;
      } else {
        return Composite.of(newExts);
      }
    }

    @Override
    public List<RentalRestrictionExtension> toList() {
      return List.copyOf(Arrays.asList(exts));
    }

    @Override
    public boolean hasRestrictions() {
      return exts.length > 0;
    }

    @Override
    public Set<String> noDropOffNetworks() {
      return Arrays
        .stream(exts)
        .flatMap(e -> e.noDropOffNetworks().stream())
        .collect(Collectors.toSet());
    }

    @Override
    public List<String> networks() {
      return Arrays.stream(exts).flatMap(e -> e.networks().stream()).toList();
    }
  }

  enum RestrictionType {
    NO_TRAVERSAL,
    NO_DROP_OFF,
    BUSINESS_AREA_BORDER,
  }
}
