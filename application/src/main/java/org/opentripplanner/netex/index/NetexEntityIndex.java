package org.opentripplanner.netex.index;

import java.util.Collection;
import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalVersionMapById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalElement;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMultimap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalVersionMapById;
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

/**
 * This class holds indexes of Netex objects for lookup during the NeTEx import using the {@link
 * NetexEntityIndexReadOnlyView}.
 * <p>
 * A NeTEx import is grouped into several levels: <em>shard data</em>, <em>group of shared
 * data</em>, and <em>single files</em>. We create a hierarchy of {@code NetexImportDataIndex} to
 * avoid keeping everything in memory and to be able to override values in a more specific(lower)
 * level.
 * <p>
 * There is one instance of this class for <em>shard data</em> - the ROOT. For each <em>group of
 * shared data</em> a new {@code NetexImportDataIndex} is created with the ROOT as a parent. When
 * such <em>group of shared data</em> is not needed any more it is discard and become ready for
 * garbage collection. For each <em>single files</em> a new {@code NetexImportDataIndex} is created
 * with the corresponding
 * <em>group of shared data</em> as parent. The <em>single files</em> object is thrown away when
 * the file is loaded.
 * <p>
 * This hierarchy make it possible to override values in child instances of the {@code
 * NetexImportDataIndex} and save memory during the load operation, because data not needed any more
 * can be thrown away.
 * <p>
 * The hierarchy implementation is delegated to the {@link org.opentripplanner.netex.index.hierarchy.AbstractHierarchicalMap}
 * and the {@link HierarchicalElement} classes.
 * <p/>
 * The mapping code should not insert entities, so an instance of this class implements the {@link
 * NetexEntityIndexReadOnlyView} which is passed to the mapping code for translation into OTP domain
 * model objects.
 */
public class NetexEntityIndex {

  private final NetexEntityIndex parent;

  // Indexes to entities
  public final HierarchicalMapById<Authority> authoritiesById;
  public final HierarchicalMapById<DatedServiceJourney> datedServiceJourneys;
  public final HierarchicalMapById<DayType> dayTypeById;
  public final HierarchicalMultimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId;
  public final HierarchicalMapById<DestinationDisplay> destinationDisplayById;
  public final HierarchicalMapById<FlexibleStopPlace> flexibleStopPlaceById;
  public final HierarchicalMapById<GroupOfLines> groupOfLinesById;
  public final HierarchicalMapById<GroupOfStopPlaces> groupOfStopPlacesById;
  public final HierarchicalMapById<JourneyPattern_VersionStructure> journeyPatternsById;
  public final HierarchicalMapById<FlexibleLine> flexibleLineByid;
  public final HierarchicalMapById<Line> lineById;
  public final HierarchicalMapById<StopPlace> multiModalStopPlaceById;
  public final HierarchicalMapById<Network> networkById;
  public final HierarchicalMapById<Notice> noticeById;
  public final HierarchicalMapById<NoticeAssignment> noticeAssignmentById;
  public final HierarchicalMapById<OperatingDay> operatingDayById;
  public final HierarchicalMapById<OperatingPeriod_VersionStructure> operatingPeriodById;
  public final HierarchicalMapById<Operator> operatorsById;
  public final HierarchicalVersionMapById<Quay> quayById;
  public final HierarchicalMap<String, String> flexibleStopPlaceByStopPointRef;
  public final HierarchicalMap<String, String> quayIdByStopPointRef;
  public final HierarchicalMapById<Route> routeById;
  public final HierarchicalMapById<ServiceJourney> serviceJourneyById;
  public final HierarchicalMapById<ServiceJourneyInterchange> serviceJourneyInterchangeById;
  public final HierarchicalMapById<ServiceLink> serviceLinkById;
  public final HierarchicalVersionMapById<StopPlace> stopPlaceById;
  public final HierarchicalVersionMapById<TariffZone_VersionStructure> tariffZonesById;
  public final HierarchicalMapById<Branding> brandingById;
  public final HierarchicalMapById<Parking> parkings;

  // Relations between entities - The Netex XML sometimes relies on the
  // nested structure of the XML document, rater than explicit references.
  // Since we throw away the document we need to keep track of these.

  public final HierarchicalMap<String, String> networkIdByGroupOfLineId;

  // Shared data
  public final HierarchicalElement<String> timeZone;

  /**
   * Create a root node.
   */
  public NetexEntityIndex() {
    this.parent = null;
    this.authoritiesById = new HierarchicalMapById<>();
    this.dayTypeById = new HierarchicalMapById<>();
    this.dayTypeAssignmentByDayTypeId = new HierarchicalMultimap<>();
    this.datedServiceJourneys = new HierarchicalMapById<>();
    this.destinationDisplayById = new HierarchicalMapById<>();
    this.flexibleStopPlaceById = new HierarchicalMapById<>();
    this.groupOfLinesById = new HierarchicalMapById<>();
    this.groupOfStopPlacesById = new HierarchicalMapById<>();
    this.journeyPatternsById = new HierarchicalMapById<>();
    this.flexibleLineByid = new HierarchicalMapById<>();
    this.lineById = new HierarchicalMapById<>();
    this.multiModalStopPlaceById = new HierarchicalMapById<>();
    this.networkById = new HierarchicalMapById<>();
    this.networkIdByGroupOfLineId = new HierarchicalMap<>();
    this.noticeById = new HierarchicalMapById<>();
    this.noticeAssignmentById = new HierarchicalMapById<>();
    this.operatingDayById = new HierarchicalMapById<>();
    this.operatingPeriodById = new HierarchicalMapById<>();
    this.operatorsById = new HierarchicalMapById<>();
    this.quayById = new HierarchicalVersionMapById<>();
    this.flexibleStopPlaceByStopPointRef = new HierarchicalMap<>();
    this.quayIdByStopPointRef = new HierarchicalMap<>();
    this.routeById = new HierarchicalMapById<>();
    this.serviceJourneyById = new HierarchicalMapById<>();
    this.serviceLinkById = new HierarchicalMapById<>();
    this.serviceJourneyInterchangeById = new HierarchicalMapById<>();
    this.stopPlaceById = new HierarchicalVersionMapById<>();
    this.tariffZonesById = new HierarchicalVersionMapById<>();
    this.brandingById = new HierarchicalMapById<>();
    this.timeZone = new HierarchicalElement<>();
    this.parkings = new HierarchicalMapById<>();
  }

  /**
   * Create a child node.
   *
   * @param parent can not be <code>null</code>.
   */
  public NetexEntityIndex(NetexEntityIndex parent) {
    this.parent = parent;
    this.authoritiesById = new HierarchicalMapById<>(parent.authoritiesById);
    this.dayTypeById = new HierarchicalMapById<>(parent.dayTypeById);
    this.dayTypeAssignmentByDayTypeId = new HierarchicalMultimap<>(
      parent.dayTypeAssignmentByDayTypeId
    );
    this.datedServiceJourneys = new HierarchicalMapById<>(parent.datedServiceJourneys);
    this.destinationDisplayById = new HierarchicalMapById<>(parent.destinationDisplayById);
    this.flexibleStopPlaceById = new HierarchicalMapById<>(parent.flexibleStopPlaceById);
    this.groupOfLinesById = new HierarchicalMapById<>(parent.groupOfLinesById);
    this.groupOfStopPlacesById = new HierarchicalMapById<>(parent.groupOfStopPlacesById);
    this.journeyPatternsById = new HierarchicalMapById<>(parent.journeyPatternsById);
    this.flexibleLineByid = new HierarchicalMapById<>(parent.flexibleLineByid);
    this.lineById = new HierarchicalMapById<>(parent.lineById);
    this.multiModalStopPlaceById = new HierarchicalMapById<>(parent.multiModalStopPlaceById);
    this.networkById = new HierarchicalMapById<>(parent.networkById);
    this.networkIdByGroupOfLineId = new HierarchicalMap<>(parent.networkIdByGroupOfLineId);
    this.noticeById = new HierarchicalMapById<>(parent.noticeById);
    this.noticeAssignmentById = new HierarchicalMapById<>(parent.noticeAssignmentById);
    this.operatingDayById = new HierarchicalMapById<>(parent.operatingDayById);
    this.operatingPeriodById = new HierarchicalMapById<>(parent.operatingPeriodById);
    this.operatorsById = new HierarchicalMapById<>(parent.operatorsById);
    this.quayById = new HierarchicalVersionMapById<>(parent.quayById);
    this.flexibleStopPlaceByStopPointRef = new HierarchicalMap<>(
      parent.flexibleStopPlaceByStopPointRef
    );
    this.quayIdByStopPointRef = new HierarchicalMap<>(parent.quayIdByStopPointRef);
    this.routeById = new HierarchicalMapById<>(parent.routeById);
    this.serviceJourneyById = new HierarchicalMapById<>(parent.serviceJourneyById);
    this.serviceLinkById = new HierarchicalMapById<>(parent.serviceLinkById);
    this.serviceJourneyInterchangeById = new HierarchicalMapById<>(
      parent.serviceJourneyInterchangeById
    );
    this.stopPlaceById = new HierarchicalVersionMapById<>(parent.stopPlaceById);
    this.tariffZonesById = new HierarchicalVersionMapById<>(parent.tariffZonesById);
    this.brandingById = new HierarchicalMapById<>(parent.brandingById);
    this.timeZone = new HierarchicalElement<>(parent.timeZone);
    this.parkings = new HierarchicalMapById<>(parent.parkings);
  }

  /**
   * Prepare to for indexing of a new sub-level of entities(shared-files, shared-group-files and
   * group-files). This is a life-cycle method used to notify this class that a new dataset is about
   * to be processed. Any existing intermediate state must be saved(pushed down the stack).
   */
  public NetexEntityIndex push() {
    return new NetexEntityIndex(this);
  }

  /**
   * It is now safe to discard any intermediate state added since last call to the {@link #push()}
   * method.
   */
  public NetexEntityIndex pop() {
    return this.parent;
  }

  public NetexEntityIndexReadOnlyView readOnlyView() {
    return new NetexEntityIndexReadOnlyView() {
      /**
       * Lookup a Network given a GroupOfLine id or an Network id. If the given
       * {@code groupOfLineOrNetworkId} is a GroupOfLine ID, we lookup the GroupOfLine, and then
       * lookup its Network. If the given {@code groupOfLineOrNetworkId} is a Network ID then we
       * can lookup the Network directly.
       * <p/>
       * If no Network is found {@code null} is returned.
       */
      @Override
      public Network lookupNetworkForLine(String groupOfLineOrNetworkId) {
        GroupOfLines groupOfLines = groupOfLinesById.lookup(groupOfLineOrNetworkId);

        String networkId = groupOfLines == null
          ? groupOfLineOrNetworkId
          : networkIdByGroupOfLineId.lookup(groupOfLines.getId());

        return networkById.lookup(networkId);
      }

      @Override
      public ReadOnlyHierarchicalMapById<GroupOfLines> getGroupsOfLinesById() {
        return groupOfLinesById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<Authority> getAuthoritiesById() {
        return authoritiesById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<DayType> getDayTypeById() {
        return dayTypeById;
      }

      /**
       * @deprecated This should be replaced with a collection of DayTypeAssignment. The
       *             mapper is responsible for indexing its data, except for entities by id.
       */
      @Deprecated
      public ReadOnlyHierarchicalMap<
        String,
        Collection<DayTypeAssignment>
      > getDayTypeAssignmentByDayTypeId() {
        return dayTypeAssignmentByDayTypeId;
      }

      @Override
      public ReadOnlyHierarchicalMapById<DatedServiceJourney> getDatedServiceJourneys() {
        return datedServiceJourneys;
      }

      @Override
      public ReadOnlyHierarchicalMapById<DestinationDisplay> getDestinationDisplayById() {
        return destinationDisplayById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<FlexibleStopPlace> getFlexibleStopPlacesById() {
        return flexibleStopPlaceById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<GroupOfStopPlaces> getGroupOfStopPlacesById() {
        return groupOfStopPlacesById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<JourneyPattern_VersionStructure> getJourneyPatternsById() {
        return journeyPatternsById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<FlexibleLine> getFlexibleLineById() {
        return flexibleLineByid;
      }

      @Override
      public ReadOnlyHierarchicalMapById<Line> getLineById() {
        return lineById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<StopPlace> getMultiModalStopPlaceById() {
        return multiModalStopPlaceById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<Notice> getNoticeById() {
        return noticeById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<NoticeAssignment> getNoticeAssignmentById() {
        return noticeAssignmentById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<OperatingDay> getOperatingDayById() {
        return operatingDayById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<
        OperatingPeriod_VersionStructure
      > getOperatingPeriodById() {
        return operatingPeriodById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<Operator> getOperatorsById() {
        return operatorsById;
      }

      @Override
      public ReadOnlyHierarchicalVersionMapById<Quay> getQuayById() {
        return quayById;
      }

      @Override
      public ReadOnlyHierarchicalMap<String, String> getQuayIdByStopPointRef() {
        return quayIdByStopPointRef;
      }

      @Override
      public ReadOnlyHierarchicalMap<String, String> getFlexibleStopPlaceByStopPointRef() {
        return flexibleStopPlaceByStopPointRef;
      }

      @Override
      public ReadOnlyHierarchicalMapById<Route> getRouteById() {
        return routeById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<ServiceJourney> getServiceJourneyById() {
        return serviceJourneyById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<
        ServiceJourneyInterchange
      > getServiceJourneyInterchangeById() {
        return serviceJourneyInterchangeById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<ServiceLink> getServiceLinkById() {
        return serviceLinkById;
      }

      @Override
      public ReadOnlyHierarchicalVersionMapById<StopPlace> getStopPlaceById() {
        return stopPlaceById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<Parking> getParkingsById() {
        return parkings;
      }

      @Override
      public ReadOnlyHierarchicalVersionMapById<TariffZone_VersionStructure> getTariffZonesById() {
        return tariffZonesById;
      }

      @Override
      public ReadOnlyHierarchicalMapById<Branding> getBrandingById() {
        return brandingById;
      }

      @Override
      public String getTimeZone() {
        return timeZone.get();
      }
    };
  }
}
