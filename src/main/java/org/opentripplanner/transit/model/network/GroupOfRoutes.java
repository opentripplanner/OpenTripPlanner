package org.opentripplanner.transit.model.network;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.transit.model.basic.TransitEntity2;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * OTP model for NeTEx GroupOfLines. Not used for GTFS at the moment. This is used to categorize
 * lines based on their particular purposes such as fare harmonization or public presentation. For
 * exempel divide lines into commercial and non-commercial group.
 */
public class GroupOfRoutes extends TransitEntity2<GroupOfRoutes, GroupOfRoutesBuilder> {

  private final String privateCode;
  private final String shortName;
  private final String name;
  private final String description;

  public GroupOfRoutes(GroupOfRoutesBuilder builder) {
    super(builder.getId());
    this.privateCode = builder.getPrivateCode();
    this.shortName = builder.getShortName();
    this.name = builder.getName();
    this.description = builder.getDescription();
    // TODO - Either validate these and handle the exception in the mapping
    //      - fix the annotations as well
    // AssertUtils.assertHasValue(privateCode);
    // AssertUtils.assertHasValue(shortName);
    // AssertUtils.assertHasValue(name);
  }

  public static GroupOfRoutesBuilder of(FeedScopedId id) {
    return new GroupOfRoutesBuilder(id);
  }

  //@NotNull
  @Nullable
  public String getPrivateCode() {
    return privateCode;
  }

  //@NotNull
  @Nullable
  public String getShortName() {
    return shortName;
  }

  //@NotNull
  @Nullable
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public GroupOfRoutesBuilder copy() {
    return new GroupOfRoutesBuilder(this);
  }

  @Override
  public boolean sameValue(GroupOfRoutes other) {
    return (
      other != null &&
      getId().equals(other.getId()) &&
      Objects.equals(privateCode, other.privateCode) &&
      Objects.equals(shortName, other.shortName) &&
      Objects.equals(name, other.name) &&
      Objects.equals(description, other.description)
    );
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
