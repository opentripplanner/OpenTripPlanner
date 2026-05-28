package org.opentripplanner.transit.model.filter.selector;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A filter for using select/not semantics.
 * <p>
 * Select: an item must match at least one select criterion (OR between selects).
 * Not: an item is excluded if it matches any not criterion.
 * A filter with no select and no not is not allowed.
 */
public class FilterRequest<TSelectRequest> {

  @Nullable
  private final List<TSelectRequest> select;

  @Nullable
  private final List<TSelectRequest> not;

  private FilterRequest(Builder<TSelectRequest> builder) {
    this.select = builder.select.isEmpty() ? null : List.copyOf(builder.select);
    this.not = builder.not.isEmpty() ? null : List.copyOf(builder.not);
  }

  public static <T> Builder<T> of() {
    return new Builder<>();
  }

  public List<TSelectRequest> select() {
    return select;
  }

  public List<TSelectRequest> not() {
    return not;
  }

  @Override
  public String toString() {
    if (select == null && not == null) {
      return "ALL";
    }
    return ToStringBuilder.ofEmbeddedType().addCol("select", select).addCol("not", not).toString();
  }

  public static class Builder<TSelectRequest> {

    private final List<TSelectRequest> select = new ArrayList<>();
    private final List<TSelectRequest> not = new ArrayList<>();

    public Builder<TSelectRequest> addSelect(TSelectRequest selectRequest) {
      this.select.add(selectRequest);
      return this;
    }

    public Builder<TSelectRequest> addNot(TSelectRequest selectRequest) {
      this.not.add(selectRequest);
      return this;
    }

    public FilterRequest<TSelectRequest> build() {
      return new FilterRequest<>(this);
    }
  }
}
