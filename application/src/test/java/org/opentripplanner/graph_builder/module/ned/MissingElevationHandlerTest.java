package org.opentripplanner.graph_builder.module.ned;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.framework.i18n.LocalizedStringFormat;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.StreetElevationExtensionBuilder;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class MissingElevationHandlerTest {

  private static final DataImportIssueStore issueStore = DefaultDataImportIssueStore.NOOP;

  private StreetEdge AB, BC, CA, AB2, AD, ED, AF, FG, GB, FH, CJ, JI, AI, BJ;

  private Map<Vertex, Double> elevations;

  @BeforeEach
  void setUp() {
    IntersectionVertex A, B, C, D, E, F, G, H, I, J;

    A = vertex("A");
    B = vertex("B");
    C = vertex("C");

    AB = edge(A, B, 50);
    BC = edge(B, C, 50);
    CA = edge(C, A, 50);

    //
    AB2 = edge(A, B, 100);

    //  A - D - E leaf
    D = vertex("D");
    E = vertex("E");

    AD = edge(A, D, 100);
    ED = edge(E, D, 200);

    // loop from A - F - G - B
    F = vertex("F");
    G = vertex("G");

    AF = edge(A, F, 100);
    FG = edge(F, G, 100);
    GB = edge(G, B, 150);

    // leaf from F, which should use the interpolated elevation from F
    H = vertex("H");
    FH = edge(F, H, 100);

    // complex path with multiple elevation points (A, B, C) dependent on edge lengths
    I = vertex("I");
    J = vertex("J");

    CJ = edge(C, J, 10);
    JI = edge(J, I, 10);
    AI = edge(A, I, 25);
    BJ = edge(B, J, 30);

    elevations = Map.of(A, 100d, B, 200d, C, 300d);

    assignElevation(AB, elevations);
    assignElevation(BC, elevations);
    assignElevation(CA, elevations);
  }

  @Test
  void zeroPropagationDistance() {
    var subject = new MissingElevationHandler(issueStore, elevations, 0);

    subject.run();

    assertElevation(AB, 0, 100, 50, 200);
    assertElevation(BC, 0, 200, 50, 300);
    assertElevation(CA, 0, 300, 50, 100);
    // The elevation for both A (original) and B (original) is known, so AB2 is assigned an elevation
    assertElevation(AB2, 0, 100, 100, 200);
    assertNullElevation(AD);
    assertNullElevation(ED);
    assertNullElevation(AF);
    assertNullElevation(FG);
    assertNullElevation(GB);
    assertNullElevation(FH);
    assertNullElevation(CJ);
    assertNullElevation(JI);
    assertNullElevation(AI);
    assertNullElevation(BJ);
  }

  @Test
  void smallPropagationDistance() {
    var subject = new MissingElevationHandler(issueStore, elevations, 20);

    subject.run();

    assertElevation(AB, 0, 100, 50, 200);
    assertElevation(BC, 0, 200, 50, 300);
    assertElevation(CA, 0, 300, 50, 100);
    // The elevation for both A (original) and B (original) is known, so AB2 is assigned an elevation
    assertElevation(AB2, 0, 100, 100, 200);
    assertNullElevation(AD);
    assertNullElevation(ED);
    assertNullElevation(AF);
    assertNullElevation(FG);
    assertNullElevation(GB);
    assertNullElevation(FH);
    assertElevation(CJ, 0, 300, 10, 300);
    assertElevation(JI, 0, 300, 10, 300);
    // The elevation for both A (original) and I (pending) is known, so AI is assigned an elevation
    assertElevation(AI, 0, 100, 25, 300);
    // The elevation for both B (original) and J (pending) is known, so BJ is assigned an elevation
    assertElevation(BJ, 0, 200, 30, 300);
  }

  @Test
  void partialPropagationDistance() {
    var subject = new MissingElevationHandler(issueStore, elevations, 150);

    subject.run();

    assertElevation(AB, 0, 100, 50, 200);
    assertElevation(BC, 0, 200, 50, 300);
    assertElevation(CA, 0, 300, 50, 100);
    // The elevation for both A (original) and B (original) is known, so AB2 is assigned an elevation
    assertElevation(AB2, 0, 100, 100, 200);
    // E has a pending elevation of 100
    assertElevation(AD, 0, 100, 100, 100);
    assertNullElevation(ED);
    // F has a pending elevation of 100
    assertElevation(AF, 0, 100, 100, 100);
    assertNullElevation(FG);
    assertNullElevation(GB);
    assertNullElevation(FH);
    // J is interpolated as 25/45 of A - C
    assertElevation(CJ, 0, 300, 10, 255.6);
    // I is interpolated as 35/45 of A - C
    assertElevation(JI, 0, 255.6, 10, 211.1);
    assertElevation(AI, 0, 100, 25, 211.1);
    assertElevation(BJ, 0, 200, 30, 255.6);
  }

  @Test
  void fullPropagationDistance() {
    var subject = new MissingElevationHandler(issueStore, elevations, 300);

    subject.run();

    assertElevation(AB, 0, 100, 50, 200);
    assertElevation(BC, 0, 200, 50, 300);
    assertElevation(CA, 0, 300, 50, 100);
    // The elevation for both A (original) and B (original) is known, so AB2 is assigned an elevation
    assertElevation(AB2, 0, 100, 100, 200);
    // E has a pending elevation of 100
    assertElevation(AD, 0, 100, 100, 100);
    // E has a pending elevation of 100
    assertElevation(ED, 0, 100, 200, 100);
    // F is interpolated as 100/350 of  A - B
    assertElevation(AF, 0, 100, 100, 128.6);
    // G is interpolated as 200/350 of  A - B
    assertElevation(FG, 0, 128.6, 100, 157.1);
    assertElevation(GB, 0, 157.1, 150, 200);
    // H has a pending elevation of F
    assertElevation(FH, 0, 128.6, 100, 128.6);
    // J is interpolated as 25/45 of A - C
    assertElevation(CJ, 0, 300, 10, 255.6);
    // I is interpolated as 35/45 of A - C
    assertElevation(JI, 0, 255.6, 10, 211.1);
    assertElevation(AI, 0, 100, 25, 211.1);
    assertElevation(BJ, 0, 200, 30, 255.6);
  }

  private IntersectionVertex vertex(String A) {
    return StreetModelForTest.intersectionVertex(A, 0, 0);
  }

  private StreetEdge edge(IntersectionVertex from, IntersectionVertex to, double length) {
    return new StreetEdgeBuilder<>()
      .withFromVertex(from)
      .withToVertex(to)
      .withName(new LocalizedStringFormat("%s%s", from.getName(), to.getName()))
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();
  }

  private void assignElevation(StreetEdge edge, Map<Vertex, Double> elevations) {
    Double fromElevation = elevations.get(edge.getFromVertex());
    Double toElevation = elevations.get(edge.getToVertex());

    Coordinate[] coords = new Coordinate[] {
      new Coordinate(0, fromElevation),
      new Coordinate(edge.getDistanceMeters(), toElevation),
    };

    PackedCoordinateSequence profile = new PackedCoordinateSequence.Double(coords);

    StreetElevationExtensionBuilder.of(edge)
      .withElevationProfile(profile)
      .withComputed(true)
      .build()
      .ifPresent(edge::setElevationExtension);
  }

  private void assertNullElevation(StreetEdge edge) {
    assertNull(edge.getElevationProfile());
  }

  private void assertElevation(StreetEdge edge, double... points) {
    var expectedCoordinates = new ArrayList<>();
    for (int i = 0; i + 1 < points.length; i += 2) {
      expectedCoordinates.add(new Coordinate(points[i], points[i + 1]));
    }

    assertArrayEquals(
      expectedCoordinates.toArray(),
      edge.getElevationProfile().toCoordinateArray()
    );
  }
}
