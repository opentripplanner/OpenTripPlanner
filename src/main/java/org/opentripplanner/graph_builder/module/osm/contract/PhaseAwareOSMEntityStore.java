package org.opentripplanner.graph_builder.module.osm.contract;

public interface PhaseAwareOSMEntityStore extends RelationalOSMEntityStore {
  void doneFirstPhaseRelations();
  void doneSecondPhaseWays();
  void doneThirdPhaseNodes();
  void postLoad();
}
