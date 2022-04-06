package org.opentripplanner.netex.index.hierarchy;

import static org.opentripplanner.netex.support.NetexVersionHelper.firstValidDateTime;
import static org.opentripplanner.netex.support.NetexVersionHelper.versionOf;

import java.time.LocalDateTime;
import org.rutebanken.netex.model.EntityInVersionStructure;

/**
 * Wrapper class to simplify getting the correct version of a Netex versioned entity.
 */
class ValidOnDate<T extends EntityInVersionStructure> {

  private final T entity;
  private final LocalDateTime time;

  /**
   * Wrap the given {@code entity} and find the first point in time the entity is valid after the
   * given {@code timeLimit}.
   */
  ValidOnDate(T entity, LocalDateTime timeLimit) {
    this.entity = entity;
    this.time = firstValidDateTime(entity.getValidBetween(), timeLimit);
  }

  T entity() {
    return entity;
  }

  boolean isValid() {
    return time != null;
  }

  /**
   * Return true if this is a better version of the entity based on the time limit passed in on the
   * constructor and, in case of a tie the version number is used.
   */
  boolean bestVersion(ValidOnDate<T> other) {
    if (time.isBefore(other.time)) {
      return true;
    }
    if (other.time.isBefore(time)) {
      return false;
    }
    return versionOf(entity) > versionOf(other.entity);
  }
}
