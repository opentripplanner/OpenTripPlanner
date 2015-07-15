package org.opentripplanner.graph_builder.linking;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.IntersectionTransitLink;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.Collection;

/**
 * Link transit stops to the street network in a non-destructive manner; i.e. don't
 * modify the street network. Being non-destructive is important in Analyst; new transit
 * models should not affect the length of existing streets, even slightly (due to float
 * roundoff from splits, etc.) An error of even a few seconds can be significant if it causes
 * the router to miss a transit vehicle or transfer, or causes the stop to move just beyond a
 * hard boundary. 
 * 
 * We do this by using the same linking code that is used
 * for Analyst (the SampleFactory code) which additionally means that this code
 * path is used and tested in multiple applications.
 *
 * This is not currently used. But it could be very useful for patching temporary transit lines in
 * interactively generated scenarios.
 */
public class SampleStopLinker {
    private Graph graph;

    /** keep track of stops that are linked to the same vertices */
    private Multimap<VertexPair, TransitStop> links;

    public SampleStopLinker (Graph graph) {
        this.graph = graph;
    }

    /**
     * Link all transit stops. If makeTransfers is true, create direct transfer
     * edges between stops linked to the same pair of vertices. This is important
     * e.g. for transit centers where there are many stops on the same street segment;
     * we don't want to force the user to walk to the end of the street and back.
     * 
     * If you're not generating transfers via the street network there is no need to make
     * transfers at this stage. But if you're not generating transfers via the street network,
     * why are you using this module at all?
     */
    public void link (boolean makeTransfers) {
        if (makeTransfers)
            links = HashMultimap.create();

        SampleFactory sf = graph.getSampleFactory();

        for (TransitStop tstop : Iterables.filter(graph.getVertices(), TransitStop.class)) {
            Sample s = sf.getSample(tstop.getLon(), tstop.getLat());

            // TODO: stop unlinked annotation
            if (s == null)
                continue;

            new IntersectionTransitLink(tstop, (OsmVertex) s.v0, s.d0);
            new IntersectionTransitLink((OsmVertex) s.v0, tstop, s.d0);
            new IntersectionTransitLink(tstop, (OsmVertex) s.v1, s.d1);
            new IntersectionTransitLink((OsmVertex) s.v1, tstop, s.d1);

            if (makeTransfers) {
                // save the sample so we can make direct transfers between stops
                VertexPair vp = new VertexPair(s.v0, s.v1);
                links.put(vp, tstop);
            }
        }

        if (makeTransfers) {
            // make direct transfers between stops
            for (Collection<TransitStop> tss : links.asMap().values()) {
                for (TransitStop ts0 : tss) {
                    for (TransitStop ts1 : tss) {
                        // make a geometry
                        GeometryFactory gf = GeometryUtils.getGeometryFactory();
                        LineString geom =
                                gf.createLineString(new Coordinate[] { ts0.getCoordinate(), ts1.getCoordinate() });

                        double dist =
                                SphericalDistanceLibrary.distance(ts0.getLat(), ts0.getLon(), ts1.getLat(), ts1.getLon());

                        // building unidirectional edge, we'll hit this again in the opposite direction
                        new SimpleTransfer(ts1, ts1, dist, geom);
                    }
                }
            }
        }
    }

    /** represents an unordered pair of vertices from a sample */
    private static class VertexPair {
        private final int v1, v2;

        public VertexPair(Vertex v1, Vertex v2) {
            this.v1 = v1.getIndex();
            this.v2 = v2.getIndex();
        }

        public int hashCode() {
            // bidirectional hash code
            return v1 + v2;
        }

        public boolean equals (Object other) {
            if (other instanceof VertexPair) {
                VertexPair vpo = (VertexPair) other;
                // bidirectional comparison
                return vpo.v1 == v1 && vpo.v2 == v2 || vpo.v2 == v1 && vpo.v1 == v2; 
            }

            return false;
        }
    }
}
