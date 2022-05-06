package org.opentripplanner.transit.model.organization;

import org.opentripplanner.model.FeedScopedId;

public class AgencyBuilder {

  private FeedScopedId id;

  private String name;

  private String timezone;

  private String url;

  private String lang;

  private String phone;

  private String fareUrl;

  private String brandingUrl;

  AgencyBuilder(FeedScopedId id) {
    this.id = id;
  }

  AgencyBuilder(Agency agency) {
    this.id = agency.getId();
    this.name = agency.getName();
    this.timezone = agency.getTimezone();
    this.url = agency.getUrl();
    this.lang = agency.getLang();
    this.phone = agency.getPhone();
    this.fareUrl = agency.getFareUrl();
    this.brandingUrl = agency.getBrandingUrl();
  }

  public Agency build() {
    return new Agency(this);
  }

  public FeedScopedId getId() {
    return id;
  }

  public AgencyBuilder setId(FeedScopedId id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public AgencyBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public String getTimezone() {
    return timezone;
  }

  public AgencyBuilder setTimezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public AgencyBuilder setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getLang() {
    return lang;
  }

  public AgencyBuilder setLang(String lang) {
    this.lang = lang;
    return this;
  }

  public String getPhone() {
    return phone;
  }

  public AgencyBuilder setPhone(String phone) {
    this.phone = phone;
    return this;
  }

  public String getFareUrl() {
    return fareUrl;
  }

  public AgencyBuilder setFareUrl(String fareUrl) {
    this.fareUrl = fareUrl;
    return this;
  }

  public String getBrandingUrl() {
    return brandingUrl;
  }

  public AgencyBuilder setBrandingUrl(String brandingUrl) {
    this.brandingUrl = brandingUrl;
    return this;
  }
}
