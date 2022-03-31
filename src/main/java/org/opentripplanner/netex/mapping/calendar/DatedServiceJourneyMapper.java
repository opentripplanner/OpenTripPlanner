package org.opentripplanner.netex.mapping.calendar;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDayRefStructure;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opentripplanner.netex.mapping.support.ServiceAlterationFilter.isRunning;


/**
 * This class is responsible for indexing and mapping {@link DatedServiceJourney}.
 */
public class DatedServiceJourneyMapper {


  /**
   * Map a list of DSJs to a set of service days.
   */
  public static Collection<ServiceDate> mapToServiceDates(
      Iterable<DatedServiceJourney> dsjs,
      ReadOnlyHierarchicalMapById<OperatingDay> operatingDayById
  ) {
    List<ServiceDate> result = new ArrayList<>();
    for (DatedServiceJourney dsj : dsjs) {

      // TODO This currently skips mapping of any trips containing ServiceAlteration CANCELLATION
      //      or REPLACED. In the future we will want to import these and allow them to be routed
      //      on if a parameter is set.
      if (!isRunning(dsj.getServiceAlteration())) { continue; }

      OperatingDay opDay = operatingDay(dsj, operatingDayById);
      if(opDay != null) {
        result.add(OperatingDayMapper.map(opDay));
      }
      else {
        // There is a validation on this, so we should never get here
        throw new NullPointerException(
            "DatedServiceJourney operating-day not found. DSJ Id: " + dsj.getId()
        );
      }
    }
    return result;
  }

  @Nullable
  private static OperatingDay operatingDay(
      DatedServiceJourney dsj,
      ReadOnlyHierarchicalMapById<OperatingDay> operatingDayById
  ) {
    OperatingDayRefStructure operatingDayRef = dsj.getOperatingDayRef();
    return operatingDayRef == null ? null : operatingDayById.lookup(operatingDayRef.getRef());
  }
}
