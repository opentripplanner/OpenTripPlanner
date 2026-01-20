package org.opentripplanner.framework.graphql;

import graphql.relay.DefaultConnection;
import graphql.relay.Edge;
import graphql.relay.PageInfo;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class extends the {@link DefaultConnection} with a somewhat standard {@code totalCount}
 * field.
 */
public class DefaultCountedConnection<T>
  extends DefaultConnection<T>
  implements CountedConnection<T> {

  private final Integer totalCount;

  public DefaultCountedConnection(List<Edge<T>> edges, PageInfo pageInfo, Integer totalCount) {
    super(edges, pageInfo);
    this.totalCount = totalCount;
  }

  @Override
  public int getTotalCount() {
    return totalCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultCountedConnection<?> that = (DefaultCountedConnection<?>) o;

    return (
      Objects.equals(getEdges(), that.getEdges()) &&
      Objects.equals(getPageInfo(), that.getPageInfo()) &&
      totalCount.equals(that.totalCount)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEdges(), getPageInfo(), totalCount);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
      .addColSize("edges", getEdges())
      .addObj("pageInfo", getPageInfo())
      .addNum("totalCount", totalCount)
      .toString();
  }
}
