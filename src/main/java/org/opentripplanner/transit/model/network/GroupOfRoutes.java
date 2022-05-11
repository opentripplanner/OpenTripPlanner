package org.opentripplanner.transit.model.network;

import javax.validation.constraints.NotNull;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * OTP model for NeTEx GroupOfLines. Not used for GTFS at the moment. This is used to categorize
 * lines based on their particular purposes such as fare harmonization or public presentation. For
 * exempel divide lines into commercial and non-commercial group.
 */
public class GroupOfRoutes extends TransitEntity {

  private final String privateCode;
  private final String shortName;
  private final String name;
  private final String description;

  public GroupOfRoutes(
    FeedScopedId id,
    String privateCode,
    String shortName,
    String name,
    String description
  ) {
    super(id);
    this.privateCode = privateCode;
    this.shortName = shortName;
    this.name = name;
    this.description = description;
  }

  @NotNull
  public String getPrivateCode() {
    return privateCode;
  }

  @NotNull
  public String getShortName() {
    return shortName;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(this.getClass())
      .addObj("id", this.getId())
      .addStr("privateCode", this.getPrivateCode())
      .addStr("shortName", this.getShortName())
      .addStr("name", this.getName())
      .addStr("description", this.getDescription())
      .toString();
  }
}
