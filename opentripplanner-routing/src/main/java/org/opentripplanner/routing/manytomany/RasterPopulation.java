package org.opentripplanner.routing.manytomany;

import java.io.File;
import java.io.PrintWriter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Vertex;

public class RasterPopulation extends Population {

	public Individual[][] grid;

	//public int xCells, yCells; 
	
	public RasterPopulation(String filename) {
		super();
		System.out.printf("Loading targets from raster file %s\n", filename);

		PrintWriter csvWriter = null;
        try {
			File rasterFile = new File(filename);

			// csv output for checking load region in GIS 
			File csvFile = new File("/home/syncopate/Desktop/coverage.csv");
			csvWriter = new PrintWriter(csvFile);
			
			// determine file format and CRS, then load raster
			AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
			AbstractGridCoverage2DReader reader = format.getReader(rasterFile);
			CoordinateReferenceSystem sourceCRS = reader.getCrs();
			GridCoverage2D cov = reader.read(null);

			// for reprojection to/from WGS84
			CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326", true);
			MathTransform tr = CRS.findMathTransform(sourceCRS, WGS84);

			// envelope around our area of interest (petite couronne)
			ReferencedEnvelope wgsEnv = new ReferencedEnvelope(1.86, 2.76, 48.52, 49.1, WGS84);
			// reproject the envelope to the raster's CRS, longitude first
			ReferencedEnvelope sourceEnv = wgsEnv.transform(sourceCRS, true);
			// crop raster to reprojected envelope
			cov = (GridCoverage2D) Operations.DEFAULT.crop(cov, sourceEnv);
			// fetch grid information from the new cropped raster
			GridGeometry2D gg = cov.getGridGeometry();
			GridEnvelope2D gridEnv = gg.getGridRange2D();
			
			// grid coordinate object to be reused for reading each cell in the cropped raster
			GridCoordinates2D coord = new GridCoordinates2D();
			// evaluating a raster returns an array of results, in this case 1D
			int[] val = new int[1];
			for (int gy = gridEnv.y, ny = 0; ny < gridEnv.height; ny++, gy++) {
				for (int gx = gridEnv.x, nx = 0; nx < gridEnv.width; gx++, nx++) {
					coord.x = gx;
					coord.y = gy;
					// find coordinates for current raster cell in raster CRS
					DirectPosition sourcePos = gg.gridToWorld(coord);
					cov.evaluate(sourcePos, val);
					if (val[0] <= 0)
						continue;
					// convert coordinates in raster CRS to WGS84
					DirectPosition targetPos = tr.transform(sourcePos, null);
					// write to csv file for later verification in GIS
					csvWriter.printf("%.6f;%.6f;%f\n",
							targetPos.getOrdinate(0), targetPos.getOrdinate(1),
							val[0] / 100.0);
					// add this grid cell to the population
					Vertex vertex = new GenericVertex("raster",
							targetPos.getOrdinate(0), targetPos.getOrdinate(1));
					Individual individual = new Individual(vertex, val[0]/100.0);
					elements.add(individual);
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Error loading population from raster file", ex);
		} finally {
		    if (csvWriter != null) {
		        csvWriter.close();
		    }
		}
		System.out.printf("Done loading raster from file\n");
	}
}
