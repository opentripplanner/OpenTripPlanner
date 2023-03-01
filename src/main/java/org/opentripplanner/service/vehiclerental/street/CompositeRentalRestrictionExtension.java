package org.opentripplanner.service.vehiclerental.street;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.search.state.State;

/**
 * Combines multiple restrictions into one.
 */
public final class CompositeRentalRestrictionExtension implements RentalRestrictionExtension {


    private final RentalRestrictionExtension[] exts;

    private CompositeRentalRestrictionExtension(RentalRestrictionExtension... exts) {
      for (var ext : exts) {
        if (ext instanceof CompositeRentalRestrictionExtension) {
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
      var set = EnumSet.noneOf(RestrictionType.class);
      for (var e : exts) {
        set.addAll(e.debugTypes());
      }
      return Set.copyOf(set);
    }

    @Override
    public RentalRestrictionExtension add(RentalRestrictionExtension other) {
      return CompositeRentalRestrictionExtension.of(this, other);
    }

    public static RentalRestrictionExtension of(RentalRestrictionExtension... exts) {
      var set = Arrays.stream(exts).flatMap(e -> e.toList().stream()).collect(Collectors.toSet());
      if (set.size() == 1) {
        return List.copyOf(set).get(0);
      } else {
        return new CompositeRentalRestrictionExtension(set.toArray(RentalRestrictionExtension[]::new));
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
        return CompositeRentalRestrictionExtension.of(newExts);
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
