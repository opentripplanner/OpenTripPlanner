package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

import jakarta.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkSequenceProjection;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.LinksInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.Projections_RelStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkInJourneyPattern_VersionedChildStructure;
import org.rutebanken.netex.model.ServiceLinkRefStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

class ServiceLinkMapperTest {

  private static final Double[] SERVICE_LINKS_COORDINATES = {
    59.90929,
    10.74527,
    59.90893,
    10.74493,
    59.90870,
    10.74585,
  };

  private static final Double[] QUAY1_COORDINATES = { 59.9093, 10.7453 };
  private static final Double[] QUAY2_COORDINATES = { 59.9089, 10.7449 };
  private static final Double[] QUAY3_COORDINATES = { 59.9087, 10.7459 };
  public static final double FLOATING_POINT_COMPARISON_PRECISION = 0.000001;
  private StopPattern.StopPatternBuilder stopPatternBuilder;
  private ServiceLinkMapper serviceLinkMapper;
  private DataImportIssueStore issueStore;
  private HierarchicalMap<String, String> quayIdByStopPointRef;
  private EntityById<RegularStop> stopsById;

  @BeforeEach
  void setUpTestData() {
    ServiceLink serviceLink1 = createServiceLink(
      "RUT:ServiceLink:1",
      "RUT:StopPoint:1",
      "RUT:StopPoint:2",
      new Double[] {
        SERVICE_LINKS_COORDINATES[0],
        SERVICE_LINKS_COORDINATES[1],
        SERVICE_LINKS_COORDINATES[2],
        SERVICE_LINKS_COORDINATES[3],
      }
    );
    ServiceLink serviceLink2 = createAlternativeServiceLink(
      "RUT:ServiceLink:2",
      "RUT:StopPoint:2",
      "RUT:StopPoint:3",
      new Double[] {
        SERVICE_LINKS_COORDINATES[2],
        SERVICE_LINKS_COORDINATES[3],
        SERVICE_LINKS_COORDINATES[4],
        SERVICE_LINKS_COORDINATES[5],
      }
    );
    ServiceLink serviceLink3 = createServiceLinkWithDirectLineString(
      "RUT:ServiceLink:3",
      "RUT:StopPoint:1",
      "RUT:StopPoint:2",
      new Double[] {
        SERVICE_LINKS_COORDINATES[0],
        SERVICE_LINKS_COORDINATES[1],
        SERVICE_LINKS_COORDINATES[2],
        SERVICE_LINKS_COORDINATES[3],
      }
    );

    // links to a quay that is not part of the stops
    ServiceLink serviceLinkToUnknownQuay = createServiceLink(
      "RUT:ServiceLink:4",
      "RUT:StopPoint:1",
      "RUT:StopPoint:4",
      new Double[] {
        SERVICE_LINKS_COORDINATES[0],
        SERVICE_LINKS_COORDINATES[1],
        SERVICE_LINKS_COORDINATES[2],
        SERVICE_LINKS_COORDINATES[3],
      }
    );
    // skips the second stop, so its to-point does not match the journey pattern when used first
    ServiceLink serviceLinkSkippingStop = createServiceLink(
      "RUT:ServiceLink:5",
      "RUT:StopPoint:1",
      "RUT:StopPoint:3",
      new Double[] {
        SERVICE_LINKS_COORDINATES[0],
        SERVICE_LINKS_COORDINATES[1],
        SERVICE_LINKS_COORDINATES[4],
        SERVICE_LINKS_COORDINATES[5],
      }
    );

    HierarchicalMapById<ServiceLink> serviceLinksById = new HierarchicalMapById<>();
    serviceLinksById.add(serviceLinkToUnknownQuay);
    serviceLinksById.add(serviceLinkSkippingStop);
    serviceLinksById.add(serviceLink1);
    serviceLinksById.add(serviceLink2);
    serviceLinksById.add(serviceLink3);

    Quay quay1 = new Quay()
      .withId("NSR:Quay:1")
      .withCentroid(getLocation(QUAY1_COORDINATES[0], QUAY1_COORDINATES[1]));
    Quay quay2 = new Quay()
      .withId("NSR:Quay:2")
      .withCentroid(getLocation(QUAY2_COORDINATES[0], QUAY2_COORDINATES[1]));
    Quay quay3 = new Quay()
      .withId("NSR:Quay:3")
      .withCentroid(getLocation(QUAY3_COORDINATES[0], QUAY3_COORDINATES[1]));

    List<Quay> quaysById = List.of(quay1, quay2, quay3);

    quayIdByStopPointRef = new HierarchicalMap<>();
    quayIdByStopPointRef.add("RUT:StopPoint:1", "NSR:Quay:1");
    quayIdByStopPointRef.add("RUT:StopPoint:2", "NSR:Quay:2");
    quayIdByStopPointRef.add("RUT:StopPoint:3", "NSR:Quay:3");
    quayIdByStopPointRef.add("RUT:StopPoint:4", "NSR:Quay:4");

    stopsById = new DefaultEntityById<>();
    issueStore = new DefaultDataImportIssueStore();

    QuayMapper quayMapper = new QuayMapper(
      ID_FACTORY,
      issueStore,
      new SiteRepository().withContext()
    );
    stopPatternBuilder = StopPattern.create(3);

    Station parentStation = Station.of(ID_FACTORY.createId("NSR:StopArea:1"))
      .withName(NonLocalizedString.ofNullable("Parent Station"))
      .withCoordinate(59.908, 10.745)
      .build();

    for (int i = 0; i < quaysById.size(); i++) {
      RegularStop stop = quayMapper.mapQuayToStop(
        quaysById.get(i),
        parentStation,
        List.of(),
        new NetexMainAndSubMode(TransitMode.BUS, "UNKNOWN"),
        Accessibility.NO_INFORMATION
      );
      stopPatternBuilder.stops.with(i, stop);
      stopsById.add(stop);
    }

    serviceLinkMapper = new ServiceLinkMapper(
      ID_FACTORY,
      serviceLinksById,
      quayIdByStopPointRef,
      stopsById,
      issueStore,
      150
    );
  }

  @Test
  void testMapValidServiceLinks() {
    JourneyPattern journeyPattern = new JourneyPattern().withId("RUT:JourneyPattern:1300");
    journeyPattern.setLinksInSequence(
      new LinksInJourneyPattern_RelStructure()
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
            new ServiceLinkRefStructure().withRef("RUT:ServiceLink:1")
          )
        )
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
            new ServiceLinkRefStructure().withRef("RUT:ServiceLink:2")
          )
        )
    );

    List<LineString> shape = serviceLinkMapper.getGeometriesByJourneyPattern(
      journeyPattern,
      stopPatternBuilder.build()
    );

    assertEquals(0, issueStore.listIssues().size());

    Coordinate[] coordinates = shape.get(0).getCoordinates();

    assertEquals(
      SERVICE_LINKS_COORDINATES[0],
      coordinates[0].getY(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[1],
      coordinates[0].getX(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[2],
      coordinates[1].getY(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[3],
      coordinates[1].getX(),
      FLOATING_POINT_COMPARISON_PRECISION
    );

    coordinates = shape.get(1).getCoordinates();

    assertEquals(
      SERVICE_LINKS_COORDINATES[2],
      coordinates[0].getY(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[3],
      coordinates[0].getX(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[4],
      coordinates[1].getY(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[5],
      coordinates[1].getX(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
  }

  @Test
  void testMapWrongNumberOfServiceLinks() {
    JourneyPattern journeyPattern = new JourneyPattern().withId("RUT:JourneyPattern:1300");

    journeyPattern.setLinksInSequence(
      new LinksInJourneyPattern_RelStructure()
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
            new ServiceLinkRefStructure().withRef("RUT:ServiceLink:1")
          )
        )
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
            new ServiceLinkRefStructure().withRef("RUT:ServiceLink:2")
          )
        )
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
            new ServiceLinkRefStructure().withRef("RUT:ServiceLink:2")
          )
        )
    );

    List<LineString> shape = serviceLinkMapper.getGeometriesByJourneyPattern(
      journeyPattern,
      stopPatternBuilder.build()
    );

    assertEquals(1, issueStore.listIssues().size());

    Coordinate[] coordinates = shape.get(0).getCoordinates();

    // when the provided service links are invalid, the mapper falls back to
    // generating straight-line ServiceLinks between the stops.
    assertEquals(QUAY1_COORDINATES[0], coordinates[0].getY(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(QUAY1_COORDINATES[1], coordinates[0].getX(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(QUAY2_COORDINATES[0], coordinates[1].getY(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(QUAY2_COORDINATES[1], coordinates[1].getX(), FLOATING_POINT_COMPARISON_PRECISION);

    coordinates = shape.get(1).getCoordinates();

    assertEquals(QUAY2_COORDINATES[0], coordinates[0].getY(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(QUAY2_COORDINATES[1], coordinates[0].getX(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(QUAY3_COORDINATES[0], coordinates[1].getY(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(QUAY3_COORDINATES[1], coordinates[1].getX(), FLOATING_POINT_COMPARISON_PRECISION);
  }

  @Test
  void testMapServiceLinkWithDirectLineString() {
    JourneyPattern journeyPattern = new JourneyPattern().withId("RUT:JourneyPattern:1300");
    journeyPattern.setLinksInSequence(
      new LinksInJourneyPattern_RelStructure()
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
            new ServiceLinkRefStructure().withRef("RUT:ServiceLink:3")
          )
        )
        .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
          new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
            new ServiceLinkRefStructure().withRef("RUT:ServiceLink:2")
          )
        )
    );

    List<LineString> shape = serviceLinkMapper.getGeometriesByJourneyPattern(
      journeyPattern,
      stopPatternBuilder.build()
    );

    assertEquals(0, issueStore.listIssues().size());

    Coordinate[] coordinates = shape.get(0).getCoordinates();

    assertEquals(
      SERVICE_LINKS_COORDINATES[0],
      coordinates[0].getY(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[1],
      coordinates[0].getX(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[2],
      coordinates[1].getY(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
    assertEquals(
      SERVICE_LINKS_COORDINATES[3],
      coordinates[1].getX(),
      FLOATING_POINT_COMPARISON_PRECISION
    );
  }

  @Test
  void testServiceLinkWithUnknownQuayFallsBackToStraightLine() {
    var shape = serviceLinkMapper.getGeometriesByJourneyPattern(
      journeyPatternOf("RUT:ServiceLink:4", "RUT:ServiceLink:2"),
      stopPatternBuilder.build()
    );

    assertThat(issueTypes()).containsExactly("ServiceLinkWithoutQuay");
    assertStraightLine(shape.get(0), QUAY1_COORDINATES, QUAY2_COORDINATES);
  }

  @Test
  void testServiceLinkWithUnmappedStopPointFallsBackToStraightLine() {
    var unmapped = createServiceLink(
      "RUT:ServiceLink:6",
      "RUT:StopPoint:1",
      "RUT:StopPoint:UNMAPPED",
      SERVICE_LINKS_COORDINATES
    );
    var links = new HierarchicalMapById<ServiceLink>();
    links.add(unmapped);
    var mapper = new ServiceLinkMapper(
      ID_FACTORY,
      links,
      quayIdByStopPointRef,
      stopsById,
      issueStore,
      150
    );

    var shape = mapper.getGeometriesByJourneyPattern(
      journeyPatternOf("RUT:ServiceLink:6", "RUT:ServiceLink:6"),
      stopPatternBuilder.build()
    );

    assertThat(issueTypes()).contains("ServiceLinkWithoutQuay");
    assertStraightLine(shape.get(0), QUAY1_COORDINATES, QUAY2_COORDINATES);
  }

  @Test
  void testServiceLinkWithFromPointMismatchFallsBackToStraightLine() {
    // link 1 goes from quay 1 to quay 2, but at index 1 the journey pattern expects quay 2 -> 3
    var shape = serviceLinkMapper.getGeometriesByJourneyPattern(
      journeyPatternOf("RUT:ServiceLink:1", "RUT:ServiceLink:1"),
      stopPatternBuilder.build()
    );

    assertThat(issueTypes()).containsExactly("ServiceLinkQuayMismatch");
    assertStraightLine(shape.get(1), QUAY2_COORDINATES, QUAY3_COORDINATES);
  }

  @Test
  void testServiceLinkWithToPointMismatchFallsBackToStraightLine() {
    // link 5 goes from quay 1 to quay 3, but at index 0 the journey pattern expects quay 1 -> 2
    var shape = serviceLinkMapper.getGeometriesByJourneyPattern(
      journeyPatternOf("RUT:ServiceLink:5", "RUT:ServiceLink:2"),
      stopPatternBuilder.build()
    );

    assertThat(issueTypes()).containsExactly("ServiceLinkQuayMismatch");
    assertStraightLine(shape.get(0), QUAY1_COORDINATES, QUAY2_COORDINATES);
  }

  @Test
  void testDirectLineStringFromToValidation() {
    // point refs (quay 1 -> quay 3) do not match the journey pattern (quay 1 -> quay 2), but the
    // refs are not validated when the line string is directly on the service link
    var link = createServiceLinkWithDirectLineString(
      "RUT:ServiceLink:7",
      "RUT:StopPoint:1",
      "RUT:StopPoint:3",
      new Double[] {
        SERVICE_LINKS_COORDINATES[0],
        SERVICE_LINKS_COORDINATES[1],
        SERVICE_LINKS_COORDINATES[2],
        SERVICE_LINKS_COORDINATES[3],
      }
    );
    var links = new HierarchicalMapById<ServiceLink>();
    links.add(link);
    var mapper = new ServiceLinkMapper(
      ID_FACTORY,
      links,
      quayIdByStopPointRef,
      stopsById,
      issueStore,
      150
    );

    var shape = mapper.getGeometriesByJourneyPattern(
      journeyPatternOf("RUT:ServiceLink:7", "RUT:ServiceLink:7"),
      stopPatternBuilder.build()
    );

    assertThat(issueTypes()).contains("ServiceLinkQuayMismatch");

    assertStraightLine(shape.get(0), QUAY1_COORDINATES, QUAY2_COORDINATES);
  }

  private static JourneyPattern journeyPatternOf(String... serviceLinkRefs) {
    var links = new LinksInJourneyPattern_RelStructure();
    for (String ref : serviceLinkRefs) {
      links.withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
        new ServiceLinkInJourneyPattern_VersionedChildStructure().withServiceLinkRef(
          new ServiceLinkRefStructure().withRef(ref)
        )
      );
    }
    return new JourneyPattern().withId("RUT:JourneyPattern:1300").withLinksInSequence(links);
  }

  private List<String> issueTypes() {
    return issueStore.listIssues().stream().map(DataImportIssue::getType).toList();
  }

  private static void assertStraightLine(LineString line, Double[] from, Double[] to) {
    Coordinate[] c = line.getCoordinates();
    assertEquals(2, c.length);
    assertEquals(from[0], c[0].getY(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(from[1], c[0].getX(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(to[0], c[1].getY(), FLOATING_POINT_COMPARISON_PRECISION);
    assertEquals(to[1], c[1].getX(), FLOATING_POINT_COMPARISON_PRECISION);
  }

  private SimplePoint_VersionStructure getLocation(double latitude, double longitude) {
    return new SimplePoint_VersionStructure().withLocation(
      new LocationStructure()
        .withLongitude(BigDecimal.valueOf(longitude))
        .withLatitude(BigDecimal.valueOf(latitude))
    );
  }

  private ServiceLink createServiceLink(String id, String from, String to, Double[] coordinates) {
    DirectPositionListType directPositionListType = new DirectPositionListType().withValue(
      coordinates
    );
    LinkSequenceProjection linkSequenceProjection = new LinkSequenceProjection().withLineString(
      new LineStringType().withPosList(directPositionListType)
    );
    JAXBElement<LinkSequenceProjection_VersionStructure> linkSequenceProjection_versionStructure =
      MappingSupport.createJaxbElement(linkSequenceProjection);
    Projections_RelStructure projections_relStructure =
      new Projections_RelStructure().withProjectionRefOrProjection(
        linkSequenceProjection_versionStructure
      );

    return new ServiceLink()
      .withId(id)
      .withFromPointRef(new ScheduledStopPointRefStructure().withRef(from))
      .withToPointRef(new ScheduledStopPointRefStructure().withRef(to))
      .withProjections(projections_relStructure);
  }

  private ServiceLink createAlternativeServiceLink(
    String id,
    String from,
    String to,
    Double[] coordinates
  ) {
    var posOrPoints = new ArrayList<>();
    for (int i = 0; i < coordinates.length; i += 2) {
      posOrPoints.add(new DirectPositionType().withValue(coordinates[i], coordinates[i + 1]));
    }
    var lineString = new LineStringType().withPosOrPointProperty(posOrPoints);
    var linkSequenceProjection = new LinkSequenceProjection().withLineString(lineString);
    var linkSequenceProjection_versionStructure = MappingSupport.createJaxbElement(
      linkSequenceProjection
    );
    var projections_relStructure = new Projections_RelStructure().withProjectionRefOrProjection(
      linkSequenceProjection_versionStructure
    );

    return new ServiceLink()
      .withId(id)
      .withFromPointRef(new ScheduledStopPointRefStructure().withRef(from))
      .withToPointRef(new ScheduledStopPointRefStructure().withRef(to))
      .withProjections(projections_relStructure);
  }

  /**
   * Some NeTEx producers put the {@link LineStringType} directly on the {@link ServiceLink}
   * instead of wrapping it in a {@code Projections_RelStructure}.
   */
  private ServiceLink createServiceLinkWithDirectLineString(
    String id,
    String from,
    String to,
    Double[] coordinates
  ) {
    DirectPositionListType directPositionListType = new DirectPositionListType().withValue(
      coordinates
    );

    return new ServiceLink()
      .withId(id)
      .withFromPointRef(new ScheduledStopPointRefStructure().withRef(from))
      .withToPointRef(new ScheduledStopPointRefStructure().withRef(to))
      .withLineString(new LineStringType().withPosList(directPositionListType));
  }
}
