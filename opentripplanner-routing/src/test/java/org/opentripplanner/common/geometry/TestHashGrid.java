package org.opentripplanner.common.geometry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import junit.framework.TestCase;

/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>. */

@SuppressWarnings("unused")
public class TestHashGrid extends TestCase{

   private Graph g;
   private HashGrid<Vertex> hashGrid;
   private GtfsContext context;

   @Override
   protected void setUp() throws Exception {
//       context = GtfsLibrary.readGtfs(new File(ConstantsForTests.PORTLAND_GTFS));
//       g = new Graph();
//       GTFSPatternHopFactory hl = new GTFSPatternHopFactory(context);
//       hl.run(g);
       g = ConstantsForTests.getInstance().getPortlandGraph();
       // g = Graph.load(graphFile, Graph.LoadLevel.FULL);
       hashGrid = new HashGrid<Vertex>(100, 400, 400);
       for (Vertex v : g.getVertices()) {
           hashGrid.put(v);
       }
       System.out.println(hashGrid.toStringVerbose());
   }
           
   @Test
   public void testQueryTime() {
       final int ITERATIONS = 100000;
       List<Vertex> cv = new ArrayList<Vertex>(g.getVertices());
       Collections.shuffle(cv);
       int count = 0;
       long t0 = System.currentTimeMillis();
       long dummy = 0;
       for (Vertex v : cv) {
           Vertex closest = hashGrid.closest(v, 100);
           if (closest != null)
               // prevent JVM from optimizing out calls
               dummy += closest.getIndex(); 
           if (count >= ITERATIONS) 
               break;
           assertNotSame(closest, v);
           count += 1;
       }
       long t1 = System.currentTimeMillis();
       double queryTimeMsec = (t1 - t0)/((double)count);
       System.out.printf("%d queries, average query time: %f msec \n", count, queryTimeMsec);
       assertTrue(queryTimeMsec < 1);
       System.out.printf("Meaningless output: %d \n", dummy);
   }
   
   @Test
   public void testFalsePositives() {
       final int ITERATIONS = 10000;
       final double RADIUS = 50.5;
       List<Vertex> cv = new ArrayList<Vertex>(g.getVertices());
       Collections.shuffle(cv);
       int count = 0;
       int found = 0;
       for (Vertex v : cv) {
           Vertex closest = hashGrid.closest(v, RADIUS);
           if (closest != null) {
               assertTrue(v.distance(closest) < RADIUS);
               found += 1;
           }
           count += 1;
           if (count >= ITERATIONS) 
               break;
       }
       assertTrue(found > count * 0.98);
   }
   
}
