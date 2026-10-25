package org.opentripplanner.transit.transfer.regular.api.parameters;

/**
 * The type of a transfer profile. The configuration that is shared by all profiles of a type, like
 * {@link MaxDurationsPreference}, is set per type. Each type has its own user preferences class,
 * for example {@link WalkPreferences} for {@link #WALK}.
 */
public enum TransferProfileType {
  WALK,
  WHEELCHAIR,
  BICYCLE,
}
