package org.opentripplanner.routing.api.request.via;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public abstract class AbstractViaLocation implements ViaLocation {

  private final String label;
  private final List<FeedScopedId> stopLocationIds;

  public AbstractViaLocation(String label, Collection<FeedScopedId> stopLocationIds) {
    this.label = label;
    this.stopLocationIds = List.copyOf(stopLocationIds);
  }

  @Nullable
  @Override
  public String label() {
    return label;
  }

  @Override
  public List<FeedScopedId> stopLocationIds() {
    return stopLocationIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractViaLocation that = (AbstractViaLocation) o;
    return (
      Objects.equals(label, that.label) && Objects.equals(stopLocationIds, that.stopLocationIds)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, stopLocationIds);
  }
}
