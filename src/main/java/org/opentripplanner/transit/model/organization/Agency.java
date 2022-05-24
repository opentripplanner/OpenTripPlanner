/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.organization;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity2;
import org.opentripplanner.util.lang.AssertUtils;

/**
 * This class is tha same as a GTFS Agency and Netex Authority.
 */
public final class Agency extends TransitEntity2<Agency, AgencyBuilder> {

  private final String name;
  private final String timezone;
  private final String url;
  private final String lang;
  private final String phone;
  private final String fareUrl;
  private final String brandingUrl;

  Agency(AgencyBuilder builder) {
    super(builder.getId());
    this.name = builder.getName();
    this.timezone = builder.getTimezone();
    this.url = builder.getUrl();
    this.lang = builder.getLang();
    this.phone = builder.getPhone();
    this.fareUrl = builder.getFareUrl();
    this.brandingUrl = builder.getBrandingUrl();

    AssertUtils.assertHasValue(getName());
  }

  public static AgencyBuilder of(FeedScopedId id) {
    return new AgencyBuilder(id);
  }

  public static AgencyBuilder ofNullable(Agency agency) {
    return new AgencyBuilder(agency);
  }

  public String getName() {
    return name;
  }

  @Nullable
  public String getTimezone() {
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

  @Nullable
  public String getBrandingUrl() {
    return brandingUrl;
  }

  public String toString() {
    return "<Agency " + getId() + ">";
  }

  @Override
  public AgencyBuilder copy() {
    return new AgencyBuilder(this);
  }

  @Override
  public boolean sameValue(Agency other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.name) &&
      Objects.equals(timezone, other.timezone) &&
      Objects.equals(url, other.url) &&
      Objects.equals(lang, other.lang) &&
      Objects.equals(phone, other.phone) &&
      Objects.equals(fareUrl, other.fareUrl) &&
      Objects.equals(brandingUrl, other.brandingUrl)
    );
  }
}
