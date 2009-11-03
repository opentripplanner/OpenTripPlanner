package org.opentripplanner.jags.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.DrawHandler;
import org.opentripplanner.jags.edgetype.Drawable;
import org.opentripplanner.jags.edgetype.DrawablePoint;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;
import org.opentripplanner.jags.spt.ShortestPathTree;

import processing.core.*;

public class ScheduleViz extends PApplet {

    private static final long serialVersionUID = -8450606812010850595L;

    Graph gg = null;

    ShortestPathTree spt = null;

    float start = Integer.MAX_VALUE;

    float end = Integer.MIN_VALUE;

    float left = Integer.MAX_VALUE;

    float bottom = Integer.MAX_VALUE;

    float right = Integer.MIN_VALUE;

    float top = Integer.MIN_VALUE;

    boolean timeMode = false;

    float time = 0;

    Vertex startVertex = null;

    // ArrayList<ArrayList<Point>> geoms = new ArrayList<ArrayList<Point>>();

    public class LoadDrawHandler implements DrawHandler {

        public void handle(Drawable todraw) throws Exception {
            // get geometry
            ArrayList<DrawablePoint> geom = todraw.getDrawableGeometry();

            // extend drawing window bounds if necessary
            for (DrawablePoint pp : geom) {
                left = min(pp.x, left);
                bottom = min(pp.y, bottom);
                right = max(pp.x, right);
                top = max(pp.y, top);
                start = min(pp.z, start);
                end = max(pp.z, end);
            }
        }
    }

    float xscale;

    float yscale;

    float xtrans;

    float ytrans;

    void setTransformation(float left, float bottom, float right, float top) {
        xscale = width / (right - left);
        yscale = height / (bottom - top);
        xtrans = -left;
        ytrans = -top;
    }

    public void drawGeom(ArrayList<DrawablePoint> geom) {
        for (int i = 0; i < geom.size() - 1; i++) {
            DrawablePoint p1 = geom.get(i);
            DrawablePoint p2 = geom.get(i + 1);
            if (timeMode) {
                line((p1.x + xtrans) * xscale, (p1.z + ytrans) * yscale, (p2.x + xtrans) * xscale,
                        (p2.z + ytrans) * yscale);
            } else {
                line((p1.x + xtrans) * xscale, (p1.y + ytrans) * yscale, (p2.x + xtrans) * xscale,
                        (p2.y + ytrans) * yscale);
            }
        }
    }

    public void setup() {
        size(700, 700, JAVA2D);
        stroke(155, 0, 0);

        smooth();
        background(255);

        try {
            GtfsContext context = GtfsLibrary.readGtfs(new File("../../caltrain_gtfs.zip"));
            gg = new Graph();
            GTFSHopLoader hl = new GTFSHopLoader(gg, context);
            System.out.println("Loading feed to graph");
            hl.load(new LoadDrawHandler(), true);
            System.out.println("Done");

            setTransformation(left, bottom, right, top);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class Drawer implements DrawHandler {
        public void handle(Drawable payload) {
            ArrayList<DrawablePoint> geom = payload.getDrawableGeometry();
            drawGeom(geom);
        }
    }

    public void draw() {
        // Set scaling factors dependant on the current mode

        stroke(155, 0, 0);
        background(255);
        strokeWeight(0.1f);
        try {
            gg.draw(new Drawer());
        } catch (Exception e) {
            e.printStackTrace();
        }
        stroke(0, 0, 155);
        strokeWeight(2);

        try {
            if (spt != null) {
                spt.draw(new Drawer());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        stroke(155, 155, 155);
        strokeWeight(1);
        if (timeMode) {
            // draw a line that represents the time to select
            line(0, mouseY, width, mouseY);
        }
    }

    public void keyPressed() {
        if (this.keyCode == 84) {
            this.timeMode = !this.timeMode;
        }

        if (timeMode) {
            setTransformation(left, start, right, end);
        } else {
            setTransformation(left, bottom, right, top);
        }
    }

    public void mousePressed() {
        if (timeMode) {
            time = mouseY / yscale - ytrans;
            System.out.println("time:" + time);
        } else {
            float lat = mouseY / yscale - ytrans;
            float lon = mouseX / xscale - xtrans;

            startVertex = gg.nearestVertex(lat, lon);
        }

        if (startVertex != null) {
            System.out.println("find SPT from " + startVertex + " at " + time);
            GregorianCalendar now = new GregorianCalendar();
            now.set(GregorianCalendar.HOUR_OF_DAY, 0);
            now.set(GregorianCalendar.MINUTE, 0);
            now.set(GregorianCalendar.SECOND, 0);
            now.add(GregorianCalendar.SECOND, (int) time);

            State s0 = new State(now.getTimeInMillis());
            spt = Dijkstra.getShortestPathTree(gg, startVertex.label, null, s0, new WalkOptions());

        }

    }

}