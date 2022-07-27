package org.opentripplanner.updater;

import java.util.List;
import java.util.Map;

/**
 * This is a read-only API for a GraphUpdaterManager, that can be safely used in the APIs to query
 * the state of the updaters
 */
public interface GraphUpdaterStatus {
  int numberOfUpdaters();

  List<String> listUnprimedUpdaters();

  Map<Integer, String> getUpdaterDescriptions();

  Class<?> getUpdaterClass(int id);
}
