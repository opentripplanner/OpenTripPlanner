package org.opentripplanner.graph_builder.module.osm.contract;

public interface OSMProvider {
  void readOSM(PhaseAwareOSMEntityStore osmdb);
  void checkInputs();
}
