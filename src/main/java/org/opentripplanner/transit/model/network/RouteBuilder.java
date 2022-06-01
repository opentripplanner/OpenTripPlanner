package org.opentripplanner.transit.model.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntityBuilder;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Branding;
import org.opentripplanner.transit.model.organization.Operator;

@SuppressWarnings("UnusedReturnValue")
public final class RouteBuilder extends TransitEntityBuilder<Route, RouteBuilder> {

  private Agency agency;
  private Operator operator;
  private Branding branding;
  private List<GroupOfRoutes> groupsOfRoutes;
  private String shortName;
  private String longName;
  private TransitMode mode;
  private Integer gtfsType;
  private Integer gtfsSortOrder;
  private String netexSubmode;
  private String flexibleLineType;
  private String desc;
  private String url;
  private String color;
  private String textColor;
  private BikeAccess bikesAllowed = BikeAccess.UNKNOWN;

  public RouteBuilder(FeedScopedId id) {
    super(id);
  }

  public RouteBuilder(Route route) {
    super(route);
  }

  public Agency getAgency() {
    return agency;
  }

  public RouteBuilder withAgency(Agency agency) {
    this.agency = agency;
    return this;
  }

  public Operator getOperator() {
    return operator;
  }

  public RouteBuilder withOperator(Operator operator) {
    this.operator = operator;
    return this;
  }

  public Branding getBranding() {
    return branding;
  }

  public RouteBuilder withBranding(Branding branding) {
    this.branding = branding;
    return this;
  }

  public String getName() {
    return Objects.requireNonNullElse(shortName, longName);
  }

  public String getShortName() {
    return shortName;
  }

  public RouteBuilder withShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  public String getLongName() {
    return longName;
  }

  public RouteBuilder withLongName(String longName) {
    this.longName = longName;
    return this;
  }

  public String getDesc() {
    return desc;
  }

  public RouteBuilder withDescription(String desc) {
    this.desc = desc;
    return this;
  }

  public Integer getGtfsType() {
    return gtfsType;
  }

  public RouteBuilder withGtfsType(int gtfsType) {
    this.gtfsType = gtfsType;
    return this;
  }

  public Integer getGtfsSortOrder() {
    return gtfsSortOrder;
  }

  public RouteBuilder withGtfsSortOrder(Integer gtfsSortOrder) {
    this.gtfsSortOrder = gtfsSortOrder;
    return this;
  }

  public TransitMode getMode() {
    return mode;
  }

  public RouteBuilder withMode(TransitMode mode) {
    this.mode = mode;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public RouteBuilder withUrl(String url) {
    this.url = url;
    return this;
  }

  public String getColor() {
    return color;
  }

  public RouteBuilder withColor(String color) {
    this.color = color;
    return this;
  }

  public String getTextColor() {
    return textColor;
  }

  public RouteBuilder withTextColor(String textColor) {
    this.textColor = textColor;
    return this;
  }

  public BikeAccess getBikesAllowed() {
    return bikesAllowed;
  }

  public RouteBuilder withBikesAllowed(BikeAccess bikesAllowed) {
    this.bikesAllowed = bikesAllowed;
    return this;
  }

  public String getFlexibleLineType() {
    return flexibleLineType;
  }

  public RouteBuilder withFlexibleLineType(String flexibleLineType) {
    this.flexibleLineType = flexibleLineType;
    return this;
  }

  public String getNetexSubmode() {
    return netexSubmode;
  }

  public RouteBuilder withNetexSubmode(String netexSubmode) {
    this.netexSubmode = netexSubmode;
    return this;
  }

  public List<GroupOfRoutes> getGroupsOfRoutes() {
    if (groupsOfRoutes == null) {
      groupsOfRoutes = new ArrayList<>();
    }
    return groupsOfRoutes;
  }

  @Override
  public String toString() {
    return "<Route " + getId() + " " + getName() + ">";
  }

  @Override
  protected Route buildFromValues() {
    return new Route(this);
  }

  @Override
  protected void updateLocal(@Nonnull Route original) {
    this.agency = original.getAgency();
    this.operator = original.getOperator();
    this.branding = original.getBranding();
    this.groupsOfRoutes = new ArrayList<>(original.getGroupsOfRoutes());
    this.shortName = original.getShortName();
    this.longName = original.getLongName();
    this.mode = original.getMode();
    this.gtfsType = original.getGtfsType();
    this.netexSubmode = original.getNetexSubmode();
    this.flexibleLineType = original.getFlexibleLineType();
    this.desc = original.getDesc();
    this.url = original.getUrl();
    this.color = original.getColor();
    this.textColor = original.getTextColor();
    this.bikesAllowed = original.getBikesAllowed();
  }
}
