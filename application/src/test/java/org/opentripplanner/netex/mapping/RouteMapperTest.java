package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.mapping.MappingSupport.createJaxbElement;

import com.google.common.collect.ArrayListMultimap;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Branding;
import org.opentripplanner.transit.service.SiteRepository;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.BrandingRefStructure;
import org.rutebanken.netex.model.GroupOfLinesRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.PresentationStructure;
import org.rutebanken.netex.model.TransportOrganisationRefStructure;

class RouteMapperTest {

  private static final String NETWORK_ID = "RUT:Network:1";
  private static final String GOL_ID_1 = "RUT:GroupOfLines:1";
  private static final String GOL_ID_2 = "RUT:GroupOfLines:2";
  private static final String GOL_NAME_1 = "G1";
  private static final String GOL_NAME_2 = "G2";
  private static final String AUTHORITY_ID = "RUT:Authority:1";
  private static final String BRANDING_ID = "RUT:Branding:1";
  private static final String LINE_ID = "RUT:Line:1";
  private static final String FERRY_WITHOUT_BICYCLES_ID = "RUT:Line:2:NoBicycles";

  private static final Set<String> EMPTY_FERRY_WITHOUT_BICYCLE_IDS = Collections.emptySet();

  @Test
  void mapRouteWithDefaultAgency() {
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    Line line = createExampleLine();

    RouteMapper routeMapper = new RouteMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      ArrayListMultimap.create(),
      new DefaultEntityById<>(),
      netexEntityIndex.readOnlyView(),
      ZoneId.systemDefault().getId(),
      EMPTY_FERRY_WITHOUT_BICYCLE_IDS
    );

    Route route = routeMapper.mapRoute(line);

    assertEquals(MappingSupport.ID_FACTORY.createId(LINE_ID), route.getId());
    assertEquals("Line 1", route.getLongName().toString());
    assertEquals("L1", route.getShortName());
  }

  @Test
  void mapRouteWithAgencySpecified() {
    NetexEntityIndex netexIndex = new NetexEntityIndex();
    OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP
    );

    Network network = new Network()
      .withId(NETWORK_ID)
      .withTransportOrganisationRef(
        createJaxbElement(new TransportOrganisationRefStructure().withRef(AUTHORITY_ID))
      );

    netexIndex.networkById.add(network);
    netexIndex.authoritiesById.add(new Authority().withId(AUTHORITY_ID));

    transitBuilder.getAgenciesById().add(createAgency());

    Line line = createExampleLine();

    RouteMapper routeMapper = new RouteMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      transitBuilder.getAgenciesById(),
      transitBuilder.getOperatorsById(),
      transitBuilder.getBrandingsById(),
      transitBuilder.getGroupsOfRoutesByRouteId(),
      transitBuilder.getGroupOfRouteById(),
      netexIndex.readOnlyView(),
      TimetableRepositoryForTest.TIME_ZONE_ID,
      EMPTY_FERRY_WITHOUT_BICYCLE_IDS
    );

    Route route = routeMapper.mapRoute(line);

    assertEquals(AUTHORITY_ID, route.getAgency().getId().getId());
  }

  @Test
  void mapRouteWithColor() {
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    Line line = createExampleLine();
    byte[] color = new byte[] { 127, 0, 0 };
    byte[] textColor = new byte[] { 0, 127, 0 };
    line.setPresentation(new PresentationStructure().withColour(color).withTextColour(textColor));

    RouteMapper routeMapper = new RouteMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      ArrayListMultimap.create(),
      new DefaultEntityById<>(),
      netexEntityIndex.readOnlyView(),
      ZoneId.systemDefault().getId(),
      EMPTY_FERRY_WITHOUT_BICYCLE_IDS
    );

    Route route = routeMapper.mapRoute(line);

    assertEquals("7F0000", route.getColor());
    assertEquals("007F00", route.getTextColor());
  }

  @Test
  void allowBicyclesOnFerries() {
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    Line lineWithBicycles = createExampleFerry(LINE_ID);
    Line lineWithOutBicycles = createExampleFerry(FERRY_WITHOUT_BICYCLES_ID);

    RouteMapper routeMapper = new RouteMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      ArrayListMultimap.create(),
      new DefaultEntityById<>(),
      netexEntityIndex.readOnlyView(),
      ZoneId.systemDefault().getId(),
      Set.of(FERRY_WITHOUT_BICYCLES_ID)
    );

    Route ferryWithBicycles = routeMapper.mapRoute(lineWithBicycles);
    assertEquals(BikeAccess.ALLOWED, ferryWithBicycles.getBikesAllowed());

    Route ferryWithOutBicycles = routeMapper.mapRoute(lineWithOutBicycles);
    assertEquals(BikeAccess.NOT_ALLOWED, ferryWithOutBicycles.getBikesAllowed());
  }

  @Test
  void mapRouteWithoutBranding() {
    NetexEntityIndex netexEntityIndex = new NetexEntityIndex();
    Line line = createExampleLine();

    RouteMapper routeMapper = new RouteMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      new DefaultEntityById<>(),
      ArrayListMultimap.create(),
      new DefaultEntityById<>(),
      netexEntityIndex.readOnlyView(),
      ZoneId.systemDefault().getId(),
      EMPTY_FERRY_WITHOUT_BICYCLE_IDS
    );

    Route route = routeMapper.mapRoute(line);

    assertNull(route.getBranding());
  }

  @Test
  void mapRouteWithBranding() {
    NetexEntityIndex netexIndex = new NetexEntityIndex();
    OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP
    );

    transitBuilder
      .getBrandingsById()
      .add(Branding.of(MappingSupport.ID_FACTORY.createId(BRANDING_ID)).build());

    Line line = createExampleLine();

    RouteMapper routeMapper = new RouteMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      transitBuilder.getAgenciesById(),
      transitBuilder.getOperatorsById(),
      transitBuilder.getBrandingsById(),
      ArrayListMultimap.create(),
      new DefaultEntityById<>(),
      netexIndex.readOnlyView(),
      TimetableRepositoryForTest.TIME_ZONE_ID,
      EMPTY_FERRY_WITHOUT_BICYCLE_IDS
    );

    Route route = routeMapper.mapRoute(line);

    Branding branding = route.getBranding();
    assertNotNull(branding);
    assertEquals(BRANDING_ID, branding.getId().getId());
  }

  @Test
  void mapRouteWithGroupOfRoutes() {
    NetexEntityIndex netexIndex = new NetexEntityIndex();
    OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP
    );

    Line line = createExampleLine();

    line.getRepresentedByGroupRef().setRef(GOL_ID_1);
    transitBuilder.getGroupOfRouteById().add(createGroupOfRoutes(GOL_ID_1, GOL_NAME_1));
    transitBuilder
      .getGroupsOfRoutesByRouteId()
      .put(MappingSupport.ID_FACTORY.createId(LINE_ID), createGroupOfRoutes(GOL_ID_2, GOL_NAME_2));

    RouteMapper routeMapper = new RouteMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY,
      transitBuilder.getAgenciesById(),
      transitBuilder.getOperatorsById(),
      transitBuilder.getBrandingsById(),
      transitBuilder.getGroupsOfRoutesByRouteId(),
      transitBuilder.getGroupOfRouteById(),
      netexIndex.readOnlyView(),
      TimetableRepositoryForTest.TIME_ZONE_ID,
      EMPTY_FERRY_WITHOUT_BICYCLE_IDS
    );

    Route route = routeMapper.mapRoute(line);

    List<GroupOfRoutes> groupsOfLines = route.getGroupsOfRoutes();

    assertEquals(2, groupsOfLines.size());
    assertTrue(groupsOfLines.stream().anyMatch(gol -> GOL_ID_1.equals(gol.getId().getId())));
    assertTrue(groupsOfLines.stream().anyMatch(gol -> GOL_ID_2.equals(gol.getId().getId())));
  }

  private Line createExampleLine() {
    Line line = new Line();
    line.setId(LINE_ID);
    line.setTransportMode(AllVehicleModesOfTransportEnumeration.METRO);
    line.setName(new MultilingualString().withValue("Line 1"));
    line.setPublicCode("L1");
    line.setRepresentedByGroupRef(new GroupOfLinesRefStructure().withRef(NETWORK_ID));
    line.setBrandingRef(new BrandingRefStructure().withRef(BRANDING_ID));
    return line;
  }

  private Line createExampleFerry(String id) {
    var ferry = createExampleLine();
    ferry.setId(id);
    ferry.setTransportMode(AllVehicleModesOfTransportEnumeration.WATER);
    return ferry;
  }

  private Agency createAgency() {
    return TimetableRepositoryForTest.agency("Ruter AS")
      .copy()
      .withId(MappingSupport.ID_FACTORY.createId(AUTHORITY_ID))
      .build();
  }

  private GroupOfRoutes createGroupOfRoutes(String id, String name) {
    return GroupOfRoutes.of(MappingSupport.ID_FACTORY.createId(id)).withName(name).build();
  }
}
