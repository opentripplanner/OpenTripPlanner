package org.opentripplanner.netex.index.api;

import java.util.Collection;
import org.entur.netex2.model.Authority;
import org.entur.netex2.model.Branding;
import org.entur.netex2.model.DatedServiceJourney;
import org.entur.netex2.model.DayType;
import org.entur.netex2.model.DayTypeAssignment;
import org.entur.netex2.model.DestinationDisplay;
import org.entur.netex2.model.FlexibleLine;
import org.entur.netex2.model.FlexibleStopPlace;
import org.entur.netex2.model.GroupOfLines;
import org.entur.netex2.model.GroupOfStopPlaces;
import org.entur.netex2.model.JourneyPattern_VersionStructure;
import org.entur.netex2.model.Line;
import org.entur.netex2.model.Network;
import org.entur.netex2.model.Notice;
import org.entur.netex2.model.NoticeAssignment;
import org.entur.netex2.model.OperatingDay;
import org.entur.netex2.model.OperatingPeriod_VersionStructure;
import org.entur.netex2.model.Operator;
import org.entur.netex2.model.Parking;
import org.entur.netex2.model.Quay;
import org.entur.netex2.model.Route;
import org.entur.netex2.model.ServiceJourney;
import org.entur.netex2.model.ServiceJourneyInterchange;
import org.entur.netex2.model.ServiceLink;
import org.entur.netex2.model.StopPlace;
import org.entur.netex2.model.TariffZone_VersionStructure;

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
