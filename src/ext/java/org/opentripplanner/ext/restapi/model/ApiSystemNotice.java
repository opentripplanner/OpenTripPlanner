package org.opentripplanner.ext.restapi.model;

import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.basic.Notice;

/**
 * A system notice is used to tag elements with system information.
 * <p>
 * One use-case is to run a routing search in debug-filter-mode and instead of removing itineraries
 * from the result, the itineraries could be tagged instead. These notices are meant for system
 * testers and developers and should not be used for end user notification or alerts.
 *
 * @see TransitAlert for end user alerts
 * @see Notice for end user notices
 */
public class ApiSystemNotice {

  /**
   * An id or code identifying the notice. Use a descriptive tag like: 'transit-walking-filter'.
   */
  public final String tag;

  /**
   * An english text explaining why the element is tagged, and/or what the tag means.
   */
  public final String text;

  public ApiSystemNotice(String tag, String text) {
    this.tag = tag;
    this.text = text;
  }
}
