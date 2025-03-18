/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.organization;

import static org.opentripplanner.utils.lang.StringUtils.assertHasValue;

import java.time.ZoneId;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;

/**
 * This class is tha same as a GTFS Agency and Netex Authority.
 */
public final class Agency extends AbstractTransitEntity<Agency, AgencyBuilder> implements LogInfo {

  private final String name;
  private final ZoneId timezone;
  private final String url;
  private final String lang;
  private final String phone;
  private final String fareUrl;

  Agency(AgencyBuilder builder) {
    super(builder.getId());
    // Required fields
    this.name = assertHasValue(
      builder.getName(),
      "Missing mandatory name on Agency %s",
      builder.getId()
    );

    this.timezone = ZoneId.of(
      assertHasValue(
        builder.getTimezone(),
        "Missing mandatory time zone on Agency %s",
        builder.getId()
      )
    );

    // Optional fields
    this.url = builder.getUrl();
    this.lang = builder.getLang();
    this.phone = builder.getPhone();
    this.fareUrl = builder.getFareUrl();
  }

  public static AgencyBuilder of(FeedScopedId id) {
    return new AgencyBuilder(id);
  }

  public String getName() {
    return logName();
  }

  public ZoneId getTimezone() {
    return timezone;
  }

  @Nullable
  public String getUrl() {
    return url;
  }

  @Nullable
  public String getLang() {
    return lang;
  }

  @Nullable
  public String getPhone() {
    return phone;
  }

  @Nullable
  public String getFareUrl() {
    return fareUrl;
  }

  @Override
  public AgencyBuilder copy() {
    return new AgencyBuilder(this);
  }

  @Override
  public String logName() {
    return name;
  }

  @Override
  public boolean sameAs(Agency other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.name) &&
      Objects.equals(timezone, other.timezone) &&
      Objects.equals(url, other.url) &&
      Objects.equals(lang, other.lang) &&
      Objects.equals(phone, other.phone) &&
      Objects.equals(fareUrl, other.fareUrl)
    );
  }
}
