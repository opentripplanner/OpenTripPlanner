package org.opentripplanner.street.model;

import java.util.List;
import java.util.Set;
import org.opentripplanner.service.vehiclerental.street.CompositeRentalRestrictionExtension;
import org.opentripplanner.service.vehiclerental.street.NoRestriction;
import org.opentripplanner.street.search.state.State;

/**
 * An extension which defines rules for how rental vehicles may or may not traverse a vertex.
 */
public interface RentalRestrictionExtension {
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
    return CompositeRentalRestrictionExtension.of(this, other);
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

  enum RestrictionType {
    NO_TRAVERSAL,
    NO_DROP_OFF,
    BUSINESS_AREA_BORDER,
  }
}
