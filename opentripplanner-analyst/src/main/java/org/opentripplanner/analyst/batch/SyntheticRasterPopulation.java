package org.opentripplanner.analyst.batch;

import lombok.Setter;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyntheticRasterPopulation extends RasterPopulation {

    private static Logger LOG = LoggerFactory.getLogger(SyntheticRasterPopulation.class); 

    @Setter String name = "synthetic grid coverage";
    @Setter double resolutionMeters = 250; // deprecated
    @Setter String crsCode = "EPSG:4326";
    @Setter boolean boundsFromGraph = false; // use graph envelope, overriding any specified bounds
    
    @Override
    public void createIndividuals() {
        try {
            coverageCRS = CRS.decode(crsCode, true);
        } catch (Exception e) {
            LOG.error("error decoding coordinate reference system code.");
            e.printStackTrace();
            return;
        }
        if (boundsFromGraph) {
            // autowire graph service or pass in
        }
        gridEnvelope = new GridEnvelope2D(0, 0, cols, rows);
        refEnvelope = new ReferencedEnvelope(left, right, bottom, top, coverageCRS);
        gridGeometry = new GridGeometry2D(gridEnvelope, refEnvelope);
        super.createIndividuals0();
    }

}
