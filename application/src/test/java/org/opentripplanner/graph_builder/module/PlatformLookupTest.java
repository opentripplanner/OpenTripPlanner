package org.opentripplanner.graph_builder.module;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildService;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;

class PlatformLookupTest {

  private static final Platform PLATFORM = new Platform(
    I18NString.of("platform"),
    GeometryUtils.getGeometryFactory().createPoint(),
    Set.of("a")
  );

  private final DefaultOsmInfoGraphBuildRepository repository =
    new DefaultOsmInfoGraphBuildRepository();
  private final PlatformLookup subject = new PlatformLookup(
    new DefaultOsmInfoGraphBuildService(repository)
  );

  /**
   * Splitting a platform edge replaces it with two halves the repository knows nothing about, so a
   * later stop on the same platform could no longer find it.
   */
  @Test
  void theHalvesOfASplitEdgeAreFound() {
    var split = new SplitterVertex("split", 10, 53.55, I18NString.of("split"));
    var halves = edgesAround(split);

    subject.addSplitEdges(split, PLATFORM);

    for (var half : halves) {
      assertThat(subject.findPlatform(half)).hasValue(PLATFORM);
    }
  }

  /**
   * A boarding location can also snap to an existing endpoint of the way, which splits nothing. The
   * edges meeting there are unrelated streets, and recording them would let a later stop match a
   * platform it is not on.
   */
  @Test
  void theEdgesMeetingAnEndpointAreNotFound() {
    var endpoint = intersectionVertex(53.55, 10);
    var unrelated = edgesAround(endpoint);

    subject.addSplitEdges(endpoint, PLATFORM);

    for (var edge : unrelated) {
      assertThat(subject.findPlatform(edge)).isEmpty();
    }
  }

  /** What OSM processing registered is still found, the lookup only adds to it. */
  @Test
  void aPlatformFromOsmProcessingIsFound() {
    var edge = streetEdge(intersectionVertex(53.55, 10), intersectionVertex(53.55, 10.001));
    repository.addPlatform(edge, PLATFORM);

    assertThat(subject.findPlatform(edge)).hasValue(PLATFORM);
  }

  /** One edge in and one out, as a vertex in the middle of a way has. */
  private static StreetEdge[] edgesAround(StreetVertex vertex) {
    return new StreetEdge[] {
      streetEdge(intersectionVertex(53.55, 9.999), vertex),
      streetEdge(vertex, intersectionVertex(53.55, 10.001)),
    };
  }
}
