package org.opentripplanner.transit.model.organization;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.basic.TransitEntityBuilder;

public class OperatorBuilder extends TransitEntityBuilder<Operator, OperatorBuilder> {

  private String name;
  private String url;
  private String phone;

  OperatorBuilder(FeedScopedId id) {
    super(id);
  }

  /**
   * If given operator is null, the returned builder is marked for removal in the parent. If not,
   * all values are copied into the builder.
   */
  OperatorBuilder(Operator original) {
    super(original);
  }

  public String getName() {
    return name;
  }

  public OperatorBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public OperatorBuilder setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getPhone() {
    return phone;
  }

  public OperatorBuilder setPhone(String phone) {
    this.phone = phone;
    return this;
  }

  @Override
  protected void updateLocal(@Nonnull @NotNull Operator original) {
    this.name = original.getName();
    this.url = original.getUrl();
    this.phone = original.getPhone();
  }

  @Override
  protected Operator buildFromValues() {
    return new Operator(this);
  }
}
