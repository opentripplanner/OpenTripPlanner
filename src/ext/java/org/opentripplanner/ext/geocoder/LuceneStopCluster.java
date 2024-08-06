package org.opentripplanner.ext.geocoder;

import java.util.Collection;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * A package-private helper type for transporting data before serializing.
 */
record LuceneStopCluster(
  String primaryId,
  Collection<String> secondaryIds,
  Collection<I18NString> names,
  Collection<String> codes,
  StopCluster.Coordinate coordinate
) {}
