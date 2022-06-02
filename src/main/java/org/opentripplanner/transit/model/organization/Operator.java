package org.opentripplanner.transit.model.organization;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntity2;
import org.opentripplanner.util.lang.AssertUtils;

/**
 * A company which is responsible for operating public transport services. The operator will often
 * operate under contract with an Authority (Agency).
 * <p/>
 * Netex ONLY. Operators are available only if the data source is Netex, not GTFS.
 *
 * @see Agency
 */
public class Operator extends TransitEntity2<Operator, OperatorBuilder> {

  private final String name;
  private final String url;
  private final String phone;

  Operator(OperatorBuilder builder) {
    super(builder.getId());
    this.name = builder.getName();
    this.url = builder.getUrl();
    this.phone = builder.getPhone();

    // name is required
    AssertUtils.assertHasValue(this.name);
  }

  public static OperatorBuilder of(FeedScopedId id) {
    return new OperatorBuilder(id);
  }

  /** if given operator is null, the returned builder is marked for removal in the parent */
  @Nonnull
  public static OperatorBuilder of(@Nullable Operator operator) {
    return new OperatorBuilder(operator);
  }

  @Nonnull
  public String getName() {
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

  public String toString() {
    return "<Operator " + getId() + ">";
  }

  @Override
  public OperatorBuilder copy() {
    return new OperatorBuilder(this);
  }

  @Override
  public boolean sameValue(Operator other) {
    return (
      other != null &&
      getId().equals(other.getId()) &&
      Objects.equals(name, other.name) &&
      Objects.equals(url, other.url) &&
      Objects.equals(phone, other.phone)
    );
  }
}
