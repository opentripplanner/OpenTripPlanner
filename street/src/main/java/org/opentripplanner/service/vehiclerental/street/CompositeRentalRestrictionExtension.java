package org.opentripplanner.service.vehiclerental.street;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.search.state.State;

/**
 * Combines multiple restrictions into one.
 */
public final class CompositeRentalRestrictionExtension implements RentalRestrictionExtension {

  private final RentalRestrictionExtension[] extensions;

  private CompositeRentalRestrictionExtension(RentalRestrictionExtension... extensions) {
    for (var ext : extensions) {
      if (ext instanceof CompositeRentalRestrictionExtension) {
        throw new IllegalArgumentException(
          "Composite extension cannot be nested into one another."
        );
      }
    }
    var set = new HashSet<>(Arrays.asList(extensions));
    this.extensions = set.toArray(RentalRestrictionExtension[]::new);
  }

  @Override
  public boolean traversalBanned(State state) {
    return getHighestPriorityApplicable(state)
      .map(ext -> ext.traversalBanned(state))
      .orElse(false);
  }

  @Override
  public boolean dropOffBanned(State state) {
    return getHighestPriorityApplicable(state)
      .map(ext -> ext.dropOffBanned(state))
      .orElse(false);
  }

  /**
   * Find the highest-priority restriction that applies to the given state.
   * When zones overlap, the one with the lowest priority value wins.
   */
  private Optional<RentalRestrictionExtension> getHighestPriorityApplicable(State state) {
    RentalRestrictionExtension best = null;
    int bestPriority = Integer.MAX_VALUE;

    for (var ext : extensions) {
      if (ext.appliesTo(state) && ext.priority() <= bestPriority) {
        best = ext;
        bestPriority = ext.priority();
      }
    }
    return Optional.ofNullable(best);
  }

  @Override
  public Set<RestrictionType> debugTypes() {
    var set = EnumSet.noneOf(RestrictionType.class);
    for (var ext : extensions) {
      set.addAll(ext.debugTypes());
    }
    return set;
  }

  @Override
  public RentalRestrictionExtension add(RentalRestrictionExtension other) {
    return CompositeRentalRestrictionExtension.of(this, other);
  }

  public static RentalRestrictionExtension of(RentalRestrictionExtension... exts) {
    var set = Arrays.stream(exts)
      .flatMap(e -> e.toList().stream())
      .collect(Collectors.toSet());
    if (set.size() == 1) {
      return List.copyOf(set).get(0);
    } else {
      return new CompositeRentalRestrictionExtension(
        set.toArray(RentalRestrictionExtension[]::new)
      );
    }
  }

  @Override
  public RentalRestrictionExtension remove(RentalRestrictionExtension toRemove) {
    var newExts = Arrays.stream(extensions)
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
    return List.copyOf(Arrays.asList(extensions));
  }

  @Override
  public boolean hasRestrictions() {
    return extensions.length > 0;
  }

  @Override
  public Set<String> noDropOffNetworks() {
    return Arrays.stream(extensions)
      .flatMap(e -> e.noDropOffNetworks().stream())
      .collect(Collectors.toSet());
  }

  @Override
  public List<String> networks() {
    return Arrays.stream(extensions)
      .flatMap(e -> e.networks().stream())
      .toList();
  }
}
