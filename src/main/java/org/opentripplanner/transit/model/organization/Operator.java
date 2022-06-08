package org.opentripplanner.transit.model.organization;

import static org.opentripplanner.util.lang.AssertUtils.assertHasValue;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.LogInfo;
import org.opentripplanner.transit.model.framework.TransitEntity2;

/**
 * A company which is responsible for operating public transport services. The operator will often
 * operate under contract with an Authority (Agency).
 * <p/>
 * Netex ONLY. Operators are available only if the data source is Netex, not GTFS.
 *
 * @see Agency
 */
public class Operator extends TransitEntity2<Operator, OperatorBuilder> implements LogInfo {

  private final String name;
  private final String url;
  private final String phone;

  Operator(OperatorBuilder builder) {
    super(builder.getId());
    // Required fields
    this.name = assertHasValue(builder.getName());

    // Optional fields
    this.url = builder.getUrl();
    this.phone = builder.getPhone();
  }

  public static OperatorBuilder of(@Nonnull FeedScopedId id) {
    return new OperatorBuilder(id);
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
  public String getUrl() {
    return url;
  }

  @Nullable
  public String getPhone() {
    return phone;
  }

  @Override
  @Nonnull
  public OperatorBuilder copy() {
    return new OperatorBuilder(this);
  }

  @Override
  public boolean sameAs(@Nonnull Operator other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(name, other.name) &&
      Objects.equals(url, other.url) &&
      Objects.equals(phone, other.phone)
    );
  }
}
