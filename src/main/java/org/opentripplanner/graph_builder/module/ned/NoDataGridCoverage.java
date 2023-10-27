package org.opentripplanner.graph_builder.module.ned;

import it.geosolutions.jaiext.range.NoDataContainer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.geotools.api.coverage.CannotEvaluateException;
import org.geotools.api.coverage.PointOutsideCoverageException;
import org.geotools.api.coverage.SampleDimension;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.geometry.Position;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.Record;
import org.geotools.api.util.RecordType;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;

public class NoDataGridCoverage implements GridCoverage {

  private final GridCoverage2D gridCoverage;
  private final NoDataContainer noData;

  private NoDataGridCoverage(GridCoverage2D gridCoverage) {
    this.gridCoverage = gridCoverage;
    this.noData = CoverageUtilities.getNoDataProperty(gridCoverage);
  }

  @Override
  public boolean isDataEditable() {
    return gridCoverage.isDataEditable();
  }

  @Override
  public GridGeometry getGridGeometry() {
    return gridCoverage.getGridGeometry();
  }

  @Override
  public int[] getOptimalDataBlockSizes() {
    return gridCoverage.getOptimalDataBlockSizes();
  }

  @Override
  public int getNumOverviews() {
    return gridCoverage.getNumOverviews();
  }

  @Override
  public GridGeometry getOverviewGridGeometry(int index) throws IndexOutOfBoundsException {
    return gridCoverage.getOverviewGridGeometry(index);
  }

  @Override
  public GridCoverage getOverview(int index) throws IndexOutOfBoundsException {
    return gridCoverage.getOverview(index);
  }

  @Override
  public List<GridCoverage> getSources() {
    return gridCoverage.getSources();
  }

  @Override
  public RenderedImage getRenderedImage() {
    return gridCoverage.getRenderedImage();
  }

  @Override
  public CoordinateReferenceSystem getCoordinateReferenceSystem() {
    return gridCoverage.getCoordinateReferenceSystem();
  }

  @Override
  public Bounds getEnvelope() {
    return gridCoverage.getEnvelope();
  }

  @Override
  public RecordType getRangeType() {
    return gridCoverage.getRangeType();
  }

  // Override the evaluate methods, so that a PointOutsideCoverageException is thrown for NO_DATE values
  @Override
  public Set<Record> evaluate(Position directPosition, Collection<String> collection)
    throws CannotEvaluateException {
    throw new UnsupportedOperationException("This methods is unsupported");
  }

  @Override
  public Object evaluate(Position directPosition) throws CannotEvaluateException {
    throw new UnsupportedOperationException("This methods is unsupported");
  }

  @Override
  public boolean[] evaluate(Position directPosition, boolean[] booleans)
    throws CannotEvaluateException, ArrayIndexOutOfBoundsException {
    throw new UnsupportedOperationException("This methods is unsupported");
  }

  @Override
  public byte[] evaluate(Position directPosition, byte[] bytes)
    throws CannotEvaluateException, ArrayIndexOutOfBoundsException {
    throw new UnsupportedOperationException("This methods is unsupported");
  }

  @Override
  public int[] evaluate(Position directPosition, int[] ints)
    throws CannotEvaluateException, ArrayIndexOutOfBoundsException {
    throw new UnsupportedOperationException("This methods is unsupported");
  }

  @Override
  public float[] evaluate(Position directPosition, float[] floats)
    throws CannotEvaluateException, ArrayIndexOutOfBoundsException {
    throw new UnsupportedOperationException("This methods is unsupported");
  }

  @Override
  public double[] evaluate(Position directPosition, double[] dest)
    throws CannotEvaluateException, ArrayIndexOutOfBoundsException {
    gridCoverage.evaluate(directPosition, dest);
    if (this.noData != null && this.noData.getAsSingleValue() == dest[0]) {
      throw new PointOutsideCoverageException("Value is NO_DATA.");
    }
    return dest;
  }

  @Override
  public int getNumSampleDimensions() {
    return gridCoverage.getNumSampleDimensions();
  }

  @Override
  public SampleDimension getSampleDimension(int index) throws IndexOutOfBoundsException {
    return gridCoverage.getSampleDimension(index);
  }

  @Override
  public RenderableImage getRenderableImage(int xAxis, int yAxis)
    throws UnsupportedOperationException, IndexOutOfBoundsException {
    return gridCoverage.getRenderableImage(xAxis, yAxis);
  }

  static GridCoverage create(GridCoverage2D gridCoverage2D) {
    return new NoDataGridCoverage(gridCoverage2D);
  }
}
