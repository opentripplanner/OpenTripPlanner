package org.opentripplanner.netex.index.api;

import org.rutebanken.netex.model.*;

import java.util.Collection;

public interface NetexEntityIndexReadOnlyView {
    Network lookupNetworkForLine(String groupOfLineOrNetworkId);
    ReadOnlyHierarchicalMapById<Authority> getAuthoritiesById();
    ReadOnlyHierarchicalMapById<DayType> getDayTypeById();
    ReadOnlyHierarchicalMap<String, Collection<DayTypeAssignment>> getDayTypeAssignmentByDayTypeId();
    ReadOnlyHierarchicalMapById<DatedServiceJourney> getDatedServiceJourneys();
    ReadOnlyHierarchicalMapById<DestinationDisplay> getDestinationDisplayById();
    ReadOnlyHierarchicalMapById<FlexibleStopPlace> getFlexibleStopPlacesById();
    ReadOnlyHierarchicalMapById<GroupOfStopPlaces> getGroupOfStopPlacesById();
    ReadOnlyHierarchicalMapById<JourneyPattern> getJourneyPatternsById();
    ReadOnlyHierarchicalMapById<FlexibleLine> getFlexibleLineById();
    ReadOnlyHierarchicalMapById<Line> getLineById();
    ReadOnlyHierarchicalMapById<StopPlace> getMultiModalStopPlaceById();
    ReadOnlyHierarchicalMapById<Notice> getNoticeById();
    ReadOnlyHierarchicalMapById<NoticeAssignment> getNoticeAssignmentById();
    ReadOnlyHierarchicalMapById<OperatingDay> getOperatingDayById();
    ReadOnlyHierarchicalMapById<OperatingPeriod> getOperatingPeriodById();
    ReadOnlyHierarchicalMapById<Operator> getOperatorsById();
    ReadOnlyHierarchicalVersionMapById<Quay> getQuayById();
    ReadOnlyHierarchicalMap<String, String> getQuayIdByStopPointRef();
    ReadOnlyHierarchicalMap<String, String> getFlexibleStopPlaceByStopPointRef();
    ReadOnlyHierarchicalMapById<Route> getRouteById();
    ReadOnlyHierarchicalMapById<ServiceJourney> getServiceJourneyById();
    ReadOnlyHierarchicalMapById<ServiceJourneyInterchange> getServiceJourneyInterchangeById();
    ReadOnlyHierarchicalMapById<ServiceLink> getServiceLinkById();
    ReadOnlyHierarchicalVersionMapById<StopPlace> getStopPlaceById();
    ReadOnlyHierarchicalVersionMapById<TariffZone> getTariffZonesById();
    ReadOnlyHierarchicalMapById<Branding> getBrandingById();
    String getTimeZone();
}
