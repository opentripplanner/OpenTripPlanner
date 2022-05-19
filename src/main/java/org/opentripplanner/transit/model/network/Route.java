/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.transit.model.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Branding;
import org.opentripplanner.transit.model.organization.Operator;

public final class Route extends TransitEntity {

  private static final long serialVersionUID = 1L;

  private Agency agency;

  private Operator operator;

  private Branding branding;

  private List<GroupOfRoutes> groupsOfRoutes = new ArrayList<>();

  private String shortName;

  private String longName;

  private TransitMode mode;

  // TODO: consolidate these
  private Integer gtfsType;

  private Integer gtfsSortOrder;

  private String netexSubmode;

  private String desc;

  private String url;

  private String color;

  private String textColor;

  private BikeAccess bikesAllowed = BikeAccess.UNKNOWN;

  private String flexibleLineType;

  public Route(FeedScopedId id) {
    super(id);
  }

  public Branding getBranding() {
    return branding;
  }

  public void setBranding(Branding branding) {
    this.branding = branding;
  }

  /**
   * The 'agency' property represent a GTFS Agency and NeTEx the Authority. Note that Agency does
   * NOT map 1-1 to Authority, it is rather a mix between Authority and Operator.
   */
  public Agency getAgency() {
    return agency;
  }

  public void setAgency(Agency agency) {
    this.agency = agency;
  }

  /**
   * NeTEx Operator, not in use when importing GTFS files.
   */
  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public String getLongName() {
    return longName;
  }

  public void setLongName(String longName) {
    this.longName = longName;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public Integer getGtfsType() {
    return gtfsType;
  }

  public void setGtfsType(int gtfsType) {
    this.gtfsType = gtfsType;
  }

  @Nullable
  public Integer getGtfsSortOrder() {
    return gtfsSortOrder;
  }

  public void setGtfsSortOrder(@Nullable Integer gtfsSortOrder) {
    this.gtfsSortOrder = gtfsSortOrder;
  }

  public TransitMode getMode() {
    return mode;
  }

  public void setMode(TransitMode mode) {
    this.mode = mode;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getTextColor() {
    return textColor;
  }

  public void setTextColor(String textColor) {
    this.textColor = textColor;
  }

  public BikeAccess getBikesAllowed() {
    return bikesAllowed;
  }

  public void setBikesAllowed(BikeAccess bikesAllowed) {
    this.bikesAllowed = bikesAllowed;
  }

  /**
   * Pass-through information from NeTEx FlexibleLineType. This information is not used by OTP.
   */
  public String getFlexibleLineType() {
    return flexibleLineType;
  }

  public void setFlexibleLineType(String flexibleLineType) {
    this.flexibleLineType = flexibleLineType;
  }

  /** @return the route's short name, or the long name if the short name is null. */
  public String getName() {
    return shortName != null ? shortName : longName;
  }

  @Override
  public String toString() {
    return "<Route " + getId() + " " + getName() + ">";
  }

  public String getNetexSubmode() {
    return netexSubmode;
  }

  public void setNetexSubmode(String netexSubmode) {
    this.netexSubmode = netexSubmode;
  }

  public List<GroupOfRoutes> getGroupsOfRoutes() {
    return groupsOfRoutes;
  }

  public void setGroupsOfRoutes(Collection<GroupOfRoutes> list) {
    groupsOfRoutes = List.copyOf(list);
  }
}
