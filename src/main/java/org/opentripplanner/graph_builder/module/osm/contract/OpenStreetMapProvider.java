package org.opentripplanner.graph_builder.module.osm.contract;

public interface OpenStreetMapProvider {
  void readOSM(PhaseAwareOSMEntityStore osmdb);
  void checkInputs();
}
