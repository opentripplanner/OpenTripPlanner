package org.opentripplanner.ext.stopconsolidation;

import java.util.List;
import org.opentripplanner.ext.stopconsolidation.model.StopReplacement;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public interface StopConsolidationService {
  List<StopReplacement> replacements();

  List<FeedScopedId> stopIdsToReplace();

  boolean isSecondaryStop(StopLocation stop);

  boolean isActive();

  I18NString agencySpecificName(StopLocation stop);
}
