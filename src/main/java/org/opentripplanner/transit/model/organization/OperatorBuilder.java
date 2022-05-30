package org.opentripplanner.transit.model.organization;

import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class OperatorBuilder extends AbstractEntityBuilder<Operator, OperatorBuilder> {

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
    this.name = original.getName();
    this.url = original.getUrl();
    this.phone = original.getPhone();
  }

  public String getName() {
    return name;
  }

  public OperatorBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public OperatorBuilder withUrl(String url) {
    this.url = url;
    return this;
  }

  public String getPhone() {
    return phone;
  }

  public OperatorBuilder withPhone(String phone) {
    this.phone = phone;
    return this;
  }

  @Override
  protected Operator buildFromValues() {
    return new Operator(this);
  }
}
