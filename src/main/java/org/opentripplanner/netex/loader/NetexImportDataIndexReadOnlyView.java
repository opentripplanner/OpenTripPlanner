package org.opentripplanner.netex.loader;

import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalVersionMapById;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TariffZone;
import org.rutebanken.netex.model.TimetabledPassingTime;

import java.util.Collection;

public interface NetexImportDataIndexReadOnlyView {
    Network lookupNetworkForLine(String groupOfLineOrNetworkId);
    ReadOnlyHierarchicalMapById<Authority> getAuthoritiesById();
    ReadOnlyHierarchicalMapById<DayType> getDayTypeById();
    ReadOnlyHierarchicalMap<String, Collection<DayTypeAssignment>> getDayTypeAssignmentByDayTypeId();
    Iterable<DayTypeRefsToServiceIdAdapter> getDayTypeRefs();
    ReadOnlyHierarchicalMapById<DestinationDisplay> getDestinationDisplayById();
    ReadOnlyHierarchicalMapById<FlexibleStopPlace> getFlexibleStopPlacesById();
    ReadOnlyHierarchicalMapById<GroupOfStopPlaces> getGroupOfStopPlacesById();
    ReadOnlyHierarchicalMapById<JourneyPattern> getJourneyPatternsById();
    ReadOnlyHierarchicalMapById<FlexibleLine> getFlexibleLineById();
    ReadOnlyHierarchicalMapById<Line> getLineById();
    ReadOnlyHierarchicalMapById<StopPlace> getMultiModalStopPlaceById();
    ReadOnlyHierarchicalMapById<Notice> getNoticeById();
    ReadOnlyHierarchicalMapById<NoticeAssignment> getNoticeAssignmentById();
    ReadOnlyHierarchicalMapById<OperatingPeriod> getOperatingPeriodById();
    ReadOnlyHierarchicalMapById<Operator> getOperatorsById();
    ReadOnlyHierarchicalMap<String, Collection<TimetabledPassingTime>> getPassingTimeByStopPointId();
    ReadOnlyHierarchicalVersionMapById<Quay> getQuayById();
    ReadOnlyHierarchicalMap<String, String> getQuayIdByStopPointRef();
    ReadOnlyHierarchicalMapById<Route> getRouteById();
    ReadOnlyHierarchicalMap<String, Collection<ServiceJourney>> getServiceJourneyByPatternId();
    ReadOnlyHierarchicalMapById<ServiceLink> getServiceLinkById();
    ReadOnlyHierarchicalVersionMapById<StopPlace> getStopPlaceById();
    ReadOnlyHierarchicalMapById<TariffZone> getTariffZonesById();
    String getTimeZone();
}
