package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public class TestEdge extends Edge {
  private final double weight;
  private final double distance;
  private final int seconds;
  private final I18NString name;
  private final boolean includeGeometryInPath;
  private final LineString geometry;

  private TestEdge(TestEdgeBuilder builder) {
    super(builder.from, builder.to);
    this.weight = builder.weight;
    this.seconds = builder.seconds;
    this.name = builder.name;
    this.distance = builder.distance;
    this.includeGeometryInPath = builder.includeGeometryInPath;

    Coordinate[] coords = new Coordinate[2];
    coords[0] = builder.from.getCoordinate();
    coords[1] = builder.to.getCoordinate();
    this.geometry = GeometryUtils.getGeometryFactory().createLineString(coords);
  }

  public static TestEdgeBuilder of(Vertex from, Vertex to) {
    return new TestEdgeBuilder(from, to);
  }

  @Override
  public I18NString getName() {
    return name;
  }

  @Override
  public State[] traverse(State s0) {
    var editor = s0.edit(this);
    editor.incrementWeight(weight);
    editor.incrementTimeInSeconds(seconds);
    editor.incrementWalkDistance(distance);
    return editor.makeStateArray();
  }

  @Override
  public double getDistanceMeters() {
    return distance;
  }

  @Override
  public LineString getGeometry() {
    return geometry;
  }

  @Override
  public boolean includeGeometryInPath() {
    return includeGeometryInPath;
  }

  public static class TestEdgeBuilder {
    Vertex from;
    Vertex to;
    double weight = 100;
    double distance = 100;
    int seconds = 100;
    I18NString name = I18NString.of("TestEdge");
    public boolean includeGeometryInPath = true;

    public TestEdgeBuilder(Vertex from, Vertex to) {
      this.from = from;
      this.to = to;
    }

    public TestEdgeBuilder withWeight(double weight) {
      this.weight = weight;
      return this;
    }

    public TestEdgeBuilder withSeconds(int seconds) {
      this.seconds = seconds;
      return this;
    }

    public TestEdgeBuilder withDistance(double distance) {
      this.distance = distance;
      return this;
    }

    public TestEdgeBuilder withIncludeGeometryInPath(boolean include) {
      this.includeGeometryInPath = include;
      return this;
    }

    public TestEdge build() {
      return new TestEdge(this);
    }
  }
}
