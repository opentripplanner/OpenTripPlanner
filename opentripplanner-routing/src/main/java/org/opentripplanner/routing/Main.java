/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

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
package org.opentripplanner.routing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.Vector;
import java.util.zip.ZipFile;

import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import au.com.bytecode.opencsv.CSVReader;

// a trivial change

class Main {
    public static Graph load_graph(String filename) {
        Graph gg = new Graph();
        try {
            FileReader fr = new FileReader(new File(filename));
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitline = line.split(",");
                String fromvName = splitline[1];
                String tovName = splitline[2];
                String name = splitline[0];
                double length = Double.parseDouble(splitline[3]);

                double fromx = Double.parseDouble(fromvName.substring(0, 4));
                double fromy = Double.parseDouble(fromvName.substring(4));
                double tox = Double.parseDouble(tovName.substring(0, 4));
                double toy = Double.parseDouble(tovName.substring(4));
                Vertex fromv = gg.addVertex(fromvName, fromx, fromy);
                Vertex tov = gg.addVertex(tovName, tox, toy);
                gg.addEdge(new Street(fromv, tov, name, name, length));
                gg.addEdge(new Street(tov, fromv, name, name, length));
            }
        } catch (IOException x) {

        }
        return gg;
    }

    public static Vector<String[]> read_pairs(String filename) {
        Vector<String[]> retbuffer = new Vector<String[]>();

        try {
            FileReader fr = new FileReader(new File(filename));
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitline = line.split(",");
                retbuffer.add(splitline);
            }
        } catch (IOException x) {
            System.out.println(x.toString());
        }

        return retbuffer;
    }

    private static void load_and_benchmark() {
        Graph gg = load_graph("map.csv");
        Vector<String[]> pairs = read_pairs("nodepairs.csv");

        int TESTSIZE = 100;
        if (pairs.size() < TESTSIZE)
            TESTSIZE = pairs.size();

        int total = 0;

        for (int i = 0; i < TESTSIZE; i++) {
            String[] pair = (String[]) pairs.elementAt(i);
            System.out.print((i + 1) + "\t" + pair[0] + "\t" + pair[1]);

            long t0 = System.currentTimeMillis();
            ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, pair[0], pair[1], new State(),
                    new TraverseOptions());
            @SuppressWarnings("unused")
            GraphPath gp = spt.getPath(gg.getVertex(pair[1]));
            long t1 = System.currentTimeMillis();
            total += (t1 - t0);
            System.out.println("\t" + (t1 - t0) + "ms");
        }
        System.out.println("total: " + total);
    }

    @SuppressWarnings("unused")
    private static void load_and_save() {
        Graph gg = load_graph("map.csv");

        try {
            FileOutputStream fos = new FileOutputStream("t.tmp");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(gg);
            oos.close();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    @SuppressWarnings("unused")
    private static void play_with_zipfile() {
        File ff = new File("src/test/resources/google_transit.zip");
        try {
            ZipFile zf = new ZipFile(ff);
            InputStream stops = zf.getInputStream(zf.getEntry("stops.txt"));
            CSVReader reader = new CSVReader(new InputStreamReader(stops));
            String[] header = reader.readNext();
            System.out.println(header.length);

        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public static void main(String[] args) {
        load_and_benchmark();
    }
}