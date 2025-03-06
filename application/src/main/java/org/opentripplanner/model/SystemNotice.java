package org.opentripplanner.model;

import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.utils.tostring.ToStringBuilder;

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
public class SystemNotice {

  /**
   * An id or code identifying the notice. Use a descriptive tag like: 'transit-walking-filter'.
   */
  private final String tag;

  /**
   * An english text explaining why the element is tagged, and/or what the tag means.
   */
  private final String text;

  public SystemNotice(String tag, String text) {
    this.tag = tag;
    this.text = text;
  }

  public String tag() {
    return tag;
  }

  public String text() {
    return text;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SystemNotice.class)
      .addStr("tag", tag)
      .addStr("text", text)
      .toString();
  }
}
