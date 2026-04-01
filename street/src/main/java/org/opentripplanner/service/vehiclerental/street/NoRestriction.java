package org.opentripplanner.service.vehiclerental.street;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.search.state.State;

/**
 * No restriction on traversal which is the default.
 */
public final class NoRestriction implements RentalRestrictionExtension {

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
    return EnumSet.noneOf(RestrictionType.class);
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
