package org.opentripplanner.ext.stopconsolidation;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;

public interface StopConsolidationRepository extends Serializable {
  void addGroups(Collection<ConsolidatedStopGroup> group);

  List<ConsolidatedStopGroup> groups();
}
