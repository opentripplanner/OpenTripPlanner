package org.opentripplanner.transit.model.network;

import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * OTP model for NeTEx GroupOfLines. Not used for GTFS at the moment. This is used to categorize
 * lines based on their particular purposes such as fare harmonization or public presentation. For
 * example divide lines into commercial and non-commercial groups.
 */
public class GroupOfRoutesBuilder
  extends AbstractEntityBuilder<GroupOfRoutes, GroupOfRoutesBuilder> {

  private String privateCode;
  private String shortName;
  private String name;
  private String description;

  GroupOfRoutesBuilder(FeedScopedId id) {
    super(id);
  }

  GroupOfRoutesBuilder(GroupOfRoutes original) {
    super(original);
    this.privateCode = original.getPrivateCode();
    this.shortName = original.getShortName();
    this.name = original.getName();
    this.description = original.getDescription();
  }

  public String getPrivateCode() {
    return privateCode;
  }

  public GroupOfRoutesBuilder withPrivateCode(String privateCode) {
    this.privateCode = privateCode;
    return this;
  }

  public String getShortName() {
    return shortName;
  }

  public GroupOfRoutesBuilder withShortName(String shortName) {
    this.shortName = shortName;
    return this;
  }

  public String getName() {
    return name;
  }

  public GroupOfRoutesBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public GroupOfRoutesBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  @Override
  protected GroupOfRoutes buildFromValues() {
    return new GroupOfRoutes(this);
  }
}
