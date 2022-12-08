package org.opentripplanner.transit.model.site;

import java.util.Collection;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.lang.ObjectUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;

/**
 * A grouping of Stops referred to by the same name. No actual boarding or alighting happens at this
 * point, but rather at its underlying childStops.
 */
public interface StopLocationsGroup extends LogInfo {
  FeedScopedId getId();

  I18NString getName();

  /**
   * Implementations should go down the hierarchy and return all the underlying stops recursively.
   */
  Collection<StopLocation> getChildStops();

  default double getLat() {
    return getCoordinate().latitude();
  }

  default double getLon() {
    return getCoordinate().longitude();
  }

  /**
   * Representative location for the StopLocation. Can either be the actual location of the stop, or
   * the centroid of an area or line.
   */
  WgsCoordinate getCoordinate();

  @Override
  default String logName() {
    return ObjectUtils.ifNotNull(getName(), Object::toString, null);
  }
}
