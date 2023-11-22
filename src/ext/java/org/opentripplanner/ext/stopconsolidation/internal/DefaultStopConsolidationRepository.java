package org.opentripplanner.ext.stopconsolidation.internal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;

@Singleton
public class DefaultStopConsolidationRepository
  implements Serializable, StopConsolidationRepository {

  private final List<ConsolidatedStopGroup> groups = new ArrayList<>();

  @Inject
  public DefaultStopConsolidationRepository() {}

  public void addGroups(Collection<ConsolidatedStopGroup> group) {
    groups.addAll(group);
  }

  public List<ConsolidatedStopGroup> groups() {
    return List.copyOf(groups);
  }
}
