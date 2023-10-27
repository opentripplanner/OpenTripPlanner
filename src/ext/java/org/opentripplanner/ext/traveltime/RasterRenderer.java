package org.opentripplanner.ext.traveltime;

import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;

import jakarta.ws.rs.core.StreamingOutput;
import java.awt.image.DataBuffer;
import javax.media.jai.RasterFactory;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ext.traveltime.geometry.ZSampleGrid;

public class RasterRenderer {

  static StreamingOutput createGeoTiffRaster(ZSampleGrid<WTWD> sampleGrid) {
    int minX = sampleGrid.getXMin();
    int minY = sampleGrid.getYMin();
    int maxY = sampleGrid.getYMax();

    int width = sampleGrid.getXMax() - minX + 1;
    int height = maxY - minY + 1;

    Coordinate center = sampleGrid.getCenter();

    double resX = sampleGrid.getCellSize().x;
    double resY = sampleGrid.getCellSize().y;

    var raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_INT, width, height, 1, null);
    var dataBuffer = raster.getDataBuffer();

    // Initialize with NO DATA value
    for (int i = 0; i < dataBuffer.getSize(); i++) {
      dataBuffer.setElem(i, Integer.MIN_VALUE);
    }

    for (var s : sampleGrid) {
      final WTWD z = s.getZ();
      raster.setSample(s.getX() - minX, maxY - s.getY(), 0, z.wTime / z.w);
    }

    ReferencedEnvelope geom = new GridGeometry2D(
      new GridEnvelope2D(0, 0, width, height),
      new AffineTransform2D(resX, 0, 0, resY, center.x + resX * minX, center.y + resY * minY),
      DefaultGeographicCRS.WGS84
    )
      .getEnvelope2D();

    GridCoverage2D gridCoverage = new GridCoverageFactory().create("traveltime", raster, geom);

    GeoTiffWriteParams wp = new GeoTiffWriteParams();
    wp.setCompressionMode(MODE_EXPLICIT);
    wp.setCompressionType("LZW");
    ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
    params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
    return outputStream -> {
      GeoTiffWriter writer = new GeoTiffWriter(outputStream);
      writer.write(gridCoverage, params.values().toArray(new GeneralParameterValue[1]));
      writer.dispose();
      outputStream.close();
    };
  }
}
