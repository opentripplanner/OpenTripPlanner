package org.opentripplanner.profile;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import com.google.common.collect.Maps;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

import org.joda.time.LocalDate;
import org.mapdb.Fun.Tuple2;
import org.opengis.referencing.FactoryException;
import org.opentripplanner.analyst.EmptyPolygonException;
import org.opentripplanner.analyst.PointFeature;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.UnsupportedGeometryException;
import org.opentripplanner.api.parameter.QualifiedMode;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.DominanceFunction.EarliestArrival;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.CommandLineParameters;

/** Compute travel time distributions from a ProfileRouter */
public class ProfileDistributionComputer {
    private ProfileRouter rtr;
    
    public ProfileDistributionComputer(ProfileRouter rtr) {
        if (!rtr.completed) {
            throw new IllegalArgumentException("This profile router has not yet finished routing! Cannot compute distributions from it.");
        }
            
        
        this.rtr = rtr;
    }
    
    /**
     * Compute travel time distributions to every point in a point set.
     */
    public DistributionResultSet computeResults (PointSet ps) {
        DistributionResultSet ret = new DistributionResultSet(ps.capacity);
        
        for (int i = 0; i < ps.capacity; i++) {
            PointFeature pf = ps.getFeature(i);
            ret.distributions[i] = getDistribution(pf.getLat(), pf.getLon());
        }
        
        return ret;
    }
    
    /** compute travel time distribution to a particular point */
    public Distribution getDistribution(double lat, double lon) {        
        Collection<StopAtDistance> viableStops = findNearbyStops(lat, lon, rtr.request.maxWalkTime);
        
        if (viableStops == null || viableStops.isEmpty())
            return null;

        // TODO: shouldn't we be looking at distances from stops rather than distances from stop clusters?
        final TObjectIntMap<StopCluster> egressTimes = new TObjectIntHashMap<StopCluster>();
        for (StopAtDistance stop : viableStops) {
            egressTimes.put(stop.stop, stop.etime);
        }
        
        // first, prune options 
        // we don't want to modify or duplicate rides here, so we use a Tuple2<Lower bound at destination, Ride>
        // and accumulate an upper bound
        List<Ride> propagatedRides = new ArrayList<Ride>();
        
        int minUpperBound = Integer.MAX_VALUE;
        
        for (StopCluster clust : rtr.retainedRides.keySet()) {
            if (!egressTimes.containsKey(clust))
                continue;
            
            for (Ride ride : rtr.retainedRides.get(clust)) {
                int egressTime = egressTimes.get(clust);
                propagatedRides.add(ride);
                
                // TODO: subomptimalMinutes?
                int upper = ride.durationUpperBound() + egressTime;
                
                if (upper < minUpperBound)
                    minUpperBound = upper;
            }
        }
        
        final int finalMinUpperBound = minUpperBound;
        
        // find all of the rides with lower bound less than minimum upper bound.
        // we use less than not less than or equal to; there's no reason to take a
        // ride whose lower bound is exactly the same as the minimum upper bound; that ride
        // can at best be indifferent.        
        Collection<Ride> viableRides = Collections2.filter(propagatedRides, new Predicate<Ride> () {
            @Override
            public boolean apply(Ride input) {
                return input.durationLowerBound() + egressTimes.get(input.to) < finalMinUpperBound;
            }
        });
        
        // TODO run this iteratively (important for timetable case)
        switch (rtr.request.choiceStrategy) {
        case PERFECT_INFORMATION:
            return computePerfectInformation(viableRides, egressTimes);
        default:
            throw new UnsupportedOperationException("Unimplemented choice strategy");
        }
    }
    
    /**
     * Compute the distribution of arrival times at the destination under the assumption of perfect information.
     * Currently only works for the frequency case.
     */
    public Distribution computePerfectInformation (Collection<Ride> viableRides, TObjectIntMap<StopCluster> egressTimes) {
        // compute individual distributions for each ride
        // TODO: nested common trunks
        // TODO: timetables
        
        // we use half-second bins in the distributions to minimize sampling error
        Map<Ride, Distribution> distributions = Maps.newHashMap();
        
        for (Ride ride : viableRides) {
            // accumulate rides starting at the origins
            Ride[] seq = new Ride[ride.pathLength()];
            
            int i = seq.length - 1;
          
            Ride loopRide = ride;
            
            while (loopRide != null) {
                seq[i--] = loopRide;
                loopRide = loopRide.previous;
            }
            
            // the distribution of when you leave your origin is an infinitely high spike
            // (or, in our case, due to sampling, a half-second segment with probability 1)
            Distribution d = Distribution.uniform(1);
        
            for (Ride seqRide : seq) {
                // make a distribution for the wait
                // (half-seconds)
                Distribution w = Distribution.uniform((seqRide.waitStats.max - seqRide.waitStats.min) * 2);
                w.offset = (seqRide.waitStats.min + seqRide.accessTime) * 2;
                // convolve with ride time to get arrival time, unless ride time has no variability, in which case propagate
                
                if (seqRide.rideStats.max != seqRide.rideStats.min) {
                    // TODO: this should not be a uniform distribution
                    Distribution r = Distribution.uniform((seqRide.rideStats.max - seqRide.rideStats.min) * 2);
                    r.offset = seqRide.rideStats.min * 2;
                    w = w.convolve(r);
                }
                else {
                    w.offset += seqRide.rideStats.min;
                }
                
                // make a distribution for cumulative wait time plus ride time
                d = d.convolve(w);
            }
            
            // offset the distribution to the destination
            d.offset += egressTimes.get(ride.to);
            
            distributions.put(ride, d);
        }
        
        // at this point we no longer care about the rides
        return combinePerfectInformation(distributions.values());
    }
    
    /**
     * Combine distributions at the destination to get an overall distribution, assuming the user always
     * takes the optimal route (the same assumption as in Owen and Levinson 2014 (http://ao.umn.edu).
     * 
     * This formula is not strictly correct when multiple options contain the same ride, as we are using the
     * formula for probabilities of independent events here.
     */
    private Distribution combinePerfectInformation(Collection<Distribution> options) {
        if (options.size() == 0)
            return null;
        
        if (options.size() == 1)
            return options.iterator().next();
        
        // the minimum elapsed time is the optimal one
        return Distribution.min(options);
    }
    
    /**
     * find nearby transit stops and access times 
     * note that we are not using samplesets at all here but rather linking the destination
     * into the graph and directly computing access times to stop vertices.
     */
    public Collection<StopAtDistance> findNearbyStops (double lat, double lon, int timeCutoffMinutes) {
        // TODO: cache access times
        // TODO: don't hard wire egress modes
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.dominanceFunction = new DominanceFunction.EarliestArrival();
        rr.batch = true;
        rr.from = new GenericLocation(lat, lon);
        rr.walkSpeed = rtr.request.walkSpeed;
        rr.to = rr.from;
        
        try {
            rr.setRoutingContext(rtr.graph);
        } catch (VertexNotFoundException e) {
            return null;
        }        
        
        // RoutingRequest dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        rr.worstTime = (rr.dateTime + timeCutoffMinutes * 60);
        AStar astar = new AStar();
        rr.longDistance = true;
        rr.setNumItineraries(1);
        ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds
        
        ArrayList<StopAtDistance> stops = Lists.newArrayList();
        
        for (TransitStop tstop : rtr.graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                stops.add(new StopAtDistance(s, new QualifiedMode("WALK")));
            }
        }
        
        return stops;
    }
    
    /**
     * test hook. usage: path/to/graph/input/directory path/to/polygon/shapefile.shp origin_lat origin_lon 
     * generates a bunch of pngs showing the cumulative distribution of arriving at a location at a given time.
     */
    public static void main (String... args) {
        // build a graph from the input directory
        File inputDir = new File(args[0]);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            System.out.println("Input directory does not exist or is not a directory.");
            return;
        }
        
        CommandLineParameters params = new CommandLineParameters();
        params.build = inputDir;
        params.inMemory = true;
        
        GraphBuilder graphBuilder = GraphBuilder.forDirectory(params, inputDir);
        
        graphBuilder.run();
        
        Graph graph = graphBuilder.getGraph();
        graph.index(new DefaultStreetVertexIndexFactory());
        
        PointSet ps;
        try {
            ps = PointSet.fromShapefile(new File(args[1]));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        
        ProfileRequest req = new ProfileRequest();
        req.analyst = true;
        req.accessModes = req.egressModes = req.directModes = new QualifiedModeSet("WALK");
        req.transitModes = new TraverseModeSet("TRANSIT");
        req.fromTime = 7 * 60 * 60;
        req.toTime = 9 * 60 * 60;
        req.fromLat = req.toLat = Double.parseDouble(args[2]);
        req.fromLon = req.toLon = Double.parseDouble(args[3]);
        req.walkSpeed = 1.33f;
        req.maxWalkTime = 20;
        req.limit = 3;
        req.streetTime = 20;
        req.date = new LocalDate(2015, 3, 25);
        req.suboptimalMinutes = 5;
        req.choiceStrategy = ProfileChoiceStrategy.PERFECT_INFORMATION;
        
        ProfileRouter rtr = new ProfileRouter(graph, req);
        rtr.route();
        ProfileDistributionComputer pdc = new ProfileDistributionComputer(rtr);
        
        long now = System.currentTimeMillis();
        DistributionResultSet drs = pdc.computeResults(ps);
        System.out.println("Computing distributions took " + (System.currentTimeMillis() - now) / 1000 + " seconds");
        
        // calculate pointset bounds
        Envelope env = new Envelope();
        
        for (int i = 0; i < ps.featureCount(); i++) {
            PointFeature pf = ps.getFeature(i);
            env.expandToInclude(pf.getLon(), pf.getLat());
        }
        
        // render the images
        for (int minute = 0; minute < 120; minute++) {
            BufferedImage frame = new BufferedImage(1920, 1080, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics gr = frame.getGraphics();
            
            // draw each polygon on the frame with its probability
            for (int i = 0; i < ps.featureCount(); i++) {
                if (drs.distributions[i] == null)
                    continue;
                
                PointFeature pf = ps.getFeature(i);
                
                Polygon geom = (Polygon) pf.getGeom();
                
                // compute the color
                float blue = (float) (drs.distributions[i].get(minute * 120));
                Color c = new Color(1 - blue, 1 - blue, 1f, 1f);
                gr.setColor(c);
                
                java.awt.Polygon p = new java.awt.Polygon();
                
                for (Coordinate coord : geom.getCoordinates()) {
                    int x = (int) ((coord.x - env.getMinX()) / env.getWidth() * 1920);
                    int y = (int) ((coord.y - env.getMinY()) / env.getHeight() * 1080);
                    p.addPoint(x, y);
                }
                
                gr.fillPolygon(p);
            }
            
            gr.dispose();
            
            File out = new File("out" + String.format(Locale.US, "%04d", minute) + ".png");
            try {
                FileOutputStream fos = new FileOutputStream(out);
                ImageIO.write(frame, "png", fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
