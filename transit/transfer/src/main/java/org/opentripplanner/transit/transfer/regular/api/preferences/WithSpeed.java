package org.opentripplanner.transit.transfer.regular.api.preferences;

import org.opentripplanner.core.model.basic.Speed;

/**
 * The preferences of a profile type where the traveler sets the speed, like walking and riding a
 * bicycle. It is not meaningful for all types, the speed of a car is the speed of the street.
 */
public interface WithSpeed {
  Speed speed();
}
