package org.opentripplanner.netex.index.api;

import java.util.Collection;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Branding;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.Parking;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.TariffZone_VersionStructure;

public interface NetexEntityIndexReadOnlyView {
  Network lookupNetworkForLine(String groupOfLineOrNetworkId);

  ReadOnlyHierarchicalMapById<GroupOfLines> getGroupsOfLinesById();

  ReadOnlyHierarchicalMapById<Authority> getAuthoritiesById();

  ReadOnlyHierarchicalMapById<DayType> getDayTypeById();

  ReadOnlyHierarchicalMap<String, Collection<DayTypeAssignment>> getDayTypeAssignmentByDayTypeId();

  ReadOnlyHierarchicalMapById<DatedServiceJourney> getDatedServiceJourneys();

  ReadOnlyHierarchicalMapById<DestinationDisplay> getDestinationDisplayById();

  ReadOnlyHierarchicalMapById<FlexibleStopPlace> getFlexibleStopPlacesById();

  ReadOnlyHierarchicalMapById<GroupOfStopPlaces> getGroupOfStopPlacesById();

  ReadOnlyHierarchicalMapById<JourneyPattern_VersionStructure> getJourneyPatternsById();

  ReadOnlyHierarchicalMapById<FlexibleLine> getFlexibleLineById();

  ReadOnlyHierarchicalMapById<Line> getLineById();

  ReadOnlyHierarchicalMapById<StopPlace> getMultiModalStopPlaceById();

  ReadOnlyHierarchicalMapById<Notice> getNoticeById();

  ReadOnlyHierarchicalMapById<NoticeAssignment> getNoticeAssignmentById();

  ReadOnlyHierarchicalMapById<OperatingDay> getOperatingDayById();

  ReadOnlyHierarchicalMapById<OperatingPeriod_VersionStructure> getOperatingPeriodById();

  ReadOnlyHierarchicalMapById<Operator> getOperatorsById();

  ReadOnlyHierarchicalVersionMapById<Quay> getQuayById();

  ReadOnlyHierarchicalMap<String, String> getQuayIdByStopPointRef();

  ReadOnlyHierarchicalMap<String, String> getFlexibleStopPlaceByStopPointRef();

  ReadOnlyHierarchicalMapById<Route> getRouteById();

  ReadOnlyHierarchicalMapById<ServiceJourney> getServiceJourneyById();

  ReadOnlyHierarchicalMapById<ServiceJourneyInterchange> getServiceJourneyInterchangeById();

  ReadOnlyHierarchicalMapById<ServiceLink> getServiceLinkById();

  ReadOnlyHierarchicalVersionMapById<StopPlace> getStopPlaceById();

  ReadOnlyHierarchicalMapById<Parking> getParkingsById();

  ReadOnlyHierarchicalVersionMapById<TariffZone_VersionStructure> getTariffZonesById();

  ReadOnlyHierarchicalMapById<Branding> getBrandingById();

  String getTimeZone();
}
