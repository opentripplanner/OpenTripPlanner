package org.opentripplanner.routing.alertpatch;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

public class TransitAlert implements Serializable {

  private final Set<EntitySelector> entities = new HashSet<>();
  private String id;
  public I18NString alertHeaderText;
  public I18NString alertDescriptionText;
  public I18NString alertDetailText;
  public I18NString alertAdviceText;
  // TODO OTP2 we wanted to merge the GTFS single alertUrl and the SIRI multiple URLs.
  //      However, GTFS URLs are one-per-language in a single object, and SIRI URLs are N objects with no translation.
  public I18NString alertUrl;
  private List<AlertUrl> alertUrlList = new ArrayList<>();
  //null means unknown
  public String alertType;
  /**
   * The severity of the alert.
   */
  public AlertSeverity severity;
  /**
   * The cause of the disruption.
   */
  public AlertCause cause;
  /**
   * The effect of the disruption.
   */
  public AlertEffect effect;
  //null means unknown
  public Integer priority;
  private List<TimePeriod> timePeriods = new ArrayList<>();
  private String feedId;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<AlertUrl> getAlertUrlList() {
    return alertUrlList;
  }

  public void setAlertUrlList(List<AlertUrl> alertUrlList) {
    this.alertUrlList = alertUrlList;
  }

  public boolean displayDuring(long startTimeSeconds, long endTimeSeconds) {
    for (TimePeriod timePeriod : timePeriods) {
      if (endTimeSeconds >= timePeriod.startTime) {
        if (timePeriod.endTime == 0 || startTimeSeconds < timePeriod.endTime) {
          return true;
        }
      }
    }
    return false;
  }

  public void setTimePeriods(List<TimePeriod> periods) {
    timePeriods = periods;
  }

  public void addEntity(EntitySelector entitySelector) {
    entities.add(entitySelector);
  }

  public Set<EntitySelector> getEntities() {
    return entities;
  }

  public String getFeedId() {
    return feedId;
  }

  public void setFeedId(String feedId) {
    this.feedId = feedId;
  }

  /**
   * Finds the first validity startTime from all timePeriods for this alert.
   *
   * @return First startDate for this Alert, <code>null</code> if 0 (not set)
   */
  @Nullable
  public Instant getEffectiveStartDate() {
    return timePeriods
      .stream()
      .map(timePeriod -> timePeriod.startTime)
      .min(Comparator.naturalOrder())
      .filter(startTime -> startTime > 0) //If 0, null should be returned
      .map(Instant::ofEpochSecond)
      .orElse(null);
  }

  /**
   * Finds the last validity endTime from all timePeriods for this alert. Returns <code>null</code>
   * if the validity is open-ended
   *
   * @return Last endDate for this Alert, <code>null</code> if open-ended
   */
  @Nullable
  public Instant getEffectiveEndDate() {
    return timePeriods
      .stream()
      .map(timePeriod -> timePeriod.endTime)
      .max(Comparator.naturalOrder())
      .filter(endTime -> endTime < TimePeriod.OPEN_ENDED) //If open-ended, null should be returned
      .map(Instant::ofEpochSecond)
      .orElse(null);
  }

  @Override
  public String toString() {
    return (
      "Alert('" +
      (
        alertHeaderText != null
          ? alertHeaderText.toString()
          : alertDescriptionText != null
            ? alertDescriptionText.toString()
            : alertDetailText != null
              ? alertDetailText.toString()
              : alertAdviceText != null ? alertAdviceText.toString() : "?"
      ) +
      "')"
    );
  }
}
