package org.opentripplanner.transit.model.network;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;

/**
 * OTP model for NeTEx GroupOfLines. Not used for GTFS at the moment. This is used to categorize
 * lines based on their particular purposes such as fare harmonization or public presentation. For
 * example divide lines into commercial and non-commercial groups.
 */
public class GroupOfRoutes
  extends AbstractTransitEntity<GroupOfRoutes, GroupOfRoutesBuilder>
  implements LogInfo {

  private final String name;
  private final String privateCode;
  private final String shortName;
  private final String description;

  GroupOfRoutes(GroupOfRoutesBuilder builder) {
    super(builder.getId());
    // Optional fields
    this.name = builder.getName();
    this.privateCode = builder.getPrivateCode();
    this.shortName = builder.getShortName();
    this.description = builder.getDescription();
  }

  public static GroupOfRoutesBuilder of(FeedScopedId id) {
    return new GroupOfRoutesBuilder(id);
  }

  @Nonnull
  public String getName() {
    return logName();
  }

  @Override
  @Nonnull
  public String logName() {
    return name;
  }

  @Nullable
  public String getShortName() {
    return shortName;
  }

  @Nullable
  public String getPrivateCode() {
    return privateCode;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  @Override
  public GroupOfRoutesBuilder copy() {
    return new GroupOfRoutesBuilder(this);
  }

  @Override
  public boolean sameAs(@Nonnull GroupOfRoutes other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.name) &&
      Objects.equals(shortName, other.shortName) &&
      Objects.equals(privateCode, other.privateCode) &&
      Objects.equals(description, other.description)
    );
  }
}
