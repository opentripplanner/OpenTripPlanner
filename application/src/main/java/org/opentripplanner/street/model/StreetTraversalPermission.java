package org.opentripplanner.street.model;

import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * Who can traverse a street in a single direction.
 */
public enum StreetTraversalPermission {
  NONE(0),
  PEDESTRIAN(1),
  BICYCLE(2),
  PEDESTRIAN_AND_BICYCLE(2 | 1),
  CAR(4),
  PEDESTRIAN_AND_CAR(4 | 1),
  BICYCLE_AND_CAR(4 | 2),
  ALL(4 | 2 | 1);

  private static final StreetTraversalPermission[] lookup =
    new StreetTraversalPermission[StreetTraversalPermission.values().length];
  public final int code;

  static {
    for (StreetTraversalPermission s : StreetTraversalPermission.values()) {
      lookup[s.code] = s;
    }
  }

  StreetTraversalPermission(int code) {
    this.code = code;
  }

  public static StreetTraversalPermission get(int code) {
    return lookup[code];
  }

  public StreetTraversalPermission add(StreetTraversalPermission perm) {
    return get(this.code | perm.code);
  }

  /**
   * Returns intersection of allowed permissions between current permissions and given permissions
   */
  public StreetTraversalPermission intersection(StreetTraversalPermission perm) {
    return get(this.code & perm.code);
  }

  public StreetTraversalPermission remove(StreetTraversalPermission perm) {
    return get(this.code & ~perm.code);
  }

  public StreetTraversalPermission modify(boolean permissive, StreetTraversalPermission perm) {
    return permissive ? add(perm) : remove(perm);
  }

  public boolean allows(StreetTraversalPermission perm) {
    return (code & perm.code) != 0;
  }

  /**
   * Returns true if any of the specified modes are allowed to use this street.
   */
  public boolean allows(TraverseModeSet modes) {
    if (modes.getWalk() && allows(StreetTraversalPermission.PEDESTRIAN)) {
      return true;
    } else if (modes.getBicycle() && allows(StreetTraversalPermission.BICYCLE)) {
      return true;
    } else return modes.getCar() && allows(StreetTraversalPermission.CAR);
  }

  /**
   * Returns true if the given mode is allowed to use this street.
   */
  public boolean allows(TraverseMode mode) {
    if (mode == TraverseMode.WALK && allows(StreetTraversalPermission.PEDESTRIAN)) {
      return true;
    } else if (mode.isCyclingIsh() && allows(StreetTraversalPermission.BICYCLE)) {
      return true;
    } else return mode == TraverseMode.CAR && allows(StreetTraversalPermission.CAR);
  }

  /**
   * Returns true if there are any modes allowed by this permission.
   */
  public boolean allowsAnything() {
    return !this.allowsNothing();
  }

  /**
   * Returns true if this permission allows nothing to traverse
   */
  public boolean allowsNothing() {
    return this == StreetTraversalPermission.NONE;
  }
}
