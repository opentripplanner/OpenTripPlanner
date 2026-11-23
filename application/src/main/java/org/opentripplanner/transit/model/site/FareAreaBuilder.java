package org.opentripplanner.transit.model.site;

import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.TransitEntity;

public final class FareAreaBuilder extends AbstractEntityBuilder<FareArea, FareAreaBuilder> {

  @Nullable
  private String name;

  FareAreaBuilder(FeedScopedId id) {
    super(id);
  }

  FareAreaBuilder(FareArea original) {
    super(original);
    this.name = original.getName();
  }

  @Nullable
  public String name() {
    return name;
  }

  public FareAreaBuilder withName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @Override
  protected FareArea buildFromValues() {
    return new FareArea(this);
  }

  /**
   * A read-only list of the unique {@code areas} sorted by id, so the order is deterministic. An
   * empty input gives the shared empty list.
   */
  static List<FareArea> uniqueSortedById(Collection<FareArea> areas) {
    if (areas.isEmpty()) {
      return List.of();
    }
    return areas.stream().distinct().sorted(TransitEntity.idComparator()).toList();
  }
}
