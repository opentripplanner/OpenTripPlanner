package org.opentripplanner.graph_builder.module.ned;

import it.geosolutions.jaiext.range.NoDataContainer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.Record;
import org.opengis.util.RecordType;

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
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return gridCoverage.getCoordinateReferenceSystem();
    }

    @Override
    public Envelope getEnvelope() {
        return gridCoverage.getEnvelope();
    }

    @Override
    public RecordType getRangeType() {
        return gridCoverage.getRangeType();
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
    public List<GridCoverage> getSources() {
        return gridCoverage.getSources();
    }

    @Override
    public RenderableImage getRenderableImage(int xAxis, int yAxis)
    throws UnsupportedOperationException, IndexOutOfBoundsException {
        return gridCoverage.getRenderableImage(xAxis, yAxis);
    }

    @Override
    public RenderedImage getRenderedImage() {
        return gridCoverage.getRenderedImage();
    }

    // Override the evaluate methods, so that a PointOutsideCoverageException is thrown for NO_DATE values
    @Override
    public Set<Record> evaluate(
            DirectPosition directPosition, Collection<String> collection
    ) throws PointOutsideCoverageException, CannotEvaluateException {
        throw new UnsupportedOperationException("This methods is unsupported");
    }

    @Override
    public Object evaluate(DirectPosition directPosition)
    throws PointOutsideCoverageException, CannotEvaluateException {
        throw new UnsupportedOperationException("This methods is unsupported");
    }

    @Override
    public boolean[] evaluate(DirectPosition directPosition, boolean[] booleans)
    throws PointOutsideCoverageException, CannotEvaluateException, ArrayIndexOutOfBoundsException {
        throw new UnsupportedOperationException("This methods is unsupported");
    }

    @Override
    public byte[] evaluate(DirectPosition directPosition, byte[] bytes)
    throws PointOutsideCoverageException, CannotEvaluateException, ArrayIndexOutOfBoundsException {
        throw new UnsupportedOperationException("This methods is unsupported");
    }

    @Override
    public int[] evaluate(DirectPosition directPosition, int[] ints)
    throws PointOutsideCoverageException, CannotEvaluateException, ArrayIndexOutOfBoundsException {
        throw new UnsupportedOperationException("This methods is unsupported");
    }

    @Override
    public float[] evaluate(DirectPosition directPosition, float[] floats)
    throws PointOutsideCoverageException, CannotEvaluateException, ArrayIndexOutOfBoundsException {
        throw new UnsupportedOperationException("This methods is unsupported");
    }

    @Override
    public double[] evaluate(DirectPosition directPosition, double[] dest)
    throws PointOutsideCoverageException, CannotEvaluateException, ArrayIndexOutOfBoundsException {
        gridCoverage.evaluate(directPosition, dest);
        if (this.noData != null && this.noData.getAsSingleValue() == dest[0]) {
            throw new PointOutsideCoverageException("Value is NO_DATA.");
        }
        return dest;
    }

    static GridCoverage create(GridCoverage2D gridCoverage2D) {
        return new NoDataGridCoverage(gridCoverage2D);
    }
}
