package org.opentripplanner.analyst.batch;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;

public class SyntheticRasterPopulation extends RasterPopulation {

    @Setter String name = "synthetic grid coverage";
    @Setter double resolutionMeters = 250; // deprecated
    @Setter String crsCode = "EPSG:4326";

    @PostConstruct
    public void loadIndividuals() {
        CoordinateReferenceSystem crs;
        try {
            crs = CRS.decode(crsCode, true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, width, height);
        Envelope refEnv = new ReferencedEnvelope(left, right, bottom, top, crs);
        //GridGeometry2D gg = new GridGeometry2D(gridEnv, refEnv);
        RenderedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        GridCoverage2D coverage = new GridCoverageFactory().create(name, image, refEnv);
        this.cov = coverage;
        super.createIndividuals();
    }
    
    public void deprecated() {
        double midLat = (top - bottom) / 2;
        DistanceLibrary dl = SphericalDistanceLibrary.getInstance();
        double xDistanceMeters = dl.distance(midLat, right, midLat, left);
        double yDistanceMeters = dl.distance(bottom, left, top, left);
        double xDistanceDegrees = (right - left);
        double yDistanceDegrees = (top - bottom);
        double xScale = xDistanceDegrees / xDistanceMeters;
        double yScale = yDistanceDegrees / yDistanceMeters;
        double xStep = resolutionMeters * xScale;
        double yStep = resolutionMeters * yScale;
        int row, col, i;
        row = i = 0;
        for (double lat = top; lat > bottom; lat -= yStep) {
            col = 0;
            for (double lon = left; lon < right; lon += xStep) {
                Individual individual = individualFactory.build(String.format("r%02d,c%02d", row, col), lon, lat, i);
                this.add(individual);
                col += 1;
                i +=1;
            }
            row += 1;
        }
    }

}
