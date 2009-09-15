package org.opentripplanner.jags.gui;

import java.io.File;
import java.util.ArrayList;

import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Edge;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.DrawHandler;
import org.opentripplanner.jags.edgetype.Drawable;
import org.opentripplanner.jags.edgetype.Point;
import org.opentripplanner.jags.edgetype.Walkable;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.gtfs.Feed;

import processing.core.*;

public class HelloP5 extends PApplet{

	private static final long serialVersionUID = -8450606812010850595L;
	
	Graph gg = null;
	
	float start=Integer.MAX_VALUE;
	float end=Integer.MIN_VALUE;
	float left=Integer.MAX_VALUE;
	float bottom=Integer.MAX_VALUE;
	float right=Integer.MIN_VALUE;
	float top=Integer.MIN_VALUE;
	
	boolean timeMode=true;
	
	ArrayList<ArrayList<Point>> geoms = new ArrayList<ArrayList<Point>>();
	
	public class LoadDrawHandler implements DrawHandler{

		public void handle(Drawable todraw) throws Exception {
			// get geometry
			ArrayList<Point> geom = todraw.getGeometry();
			
			// extend drawing window bounds if necessary
			for( Point pp : geom ) {	
				left = min(pp.x,left);
				bottom = min(pp.y,bottom);
				right = max(pp.x,right);
				top = max(pp.y,top);
				start = min(pp.z,start);
				end = max(pp.z,end);
			}
			
			// add geom to list of geoms to be drawn
			geoms.add(geom);
		}
	}
	
	float xscale;
	float yscale;
	float xtrans;
	float ytrans;
	
	void frame(float left,float bottom,float right,float top) {
		  xscale = width/(right-left);
		  yscale = height/(bottom-top);
		  xtrans = -left;
		  ytrans = -top;
		  scale( xscale, yscale );
		  translate( xtrans, ytrans );
	}
	
	public void drawGeoms() {
		//this.pushMatrix();
		//frame(left,start,right,end);
		for( ArrayList<Point> geom : geoms ) {
			for(int i=0; i<geom.size()-1; i++) {
				Point p1 = geom.get(i);
				Point p2 = geom.get(i+1);
				if(timeMode) {
				    line(p1.x, p1.z, p2.x, p2.z);
				} else {
					line(p1.x, p1.y, p2.x, p2.y);
				}
			}
		}
		//this.popMatrix();
	}

	public void setup(){
		size(700, 700);
		stroke(155,0,0);

		smooth();
		background(255);
		
		try {
			Feed feed = new Feed( "/home/brandon/workspace/jags/bart-archiver_20090826_0242.zip" );
			gg = new Graph();
			GTFSHopLoader hl = new GTFSHopLoader(gg,feed);
			hl.load(new LoadDrawHandler());
			
			//frame(left,start,right,end);
			
			System.out.println( left );
			System.out.println( start );
			System.out.println( right );
			System.out.println( end );
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void draw(){
		if(timeMode) {
		    frame(left,start,right,end);
		} else {
			frame(left,bottom,right,top);
		}
		background(255);
		strokeWeight(0.001f);
		drawGeoms();
		strokeWeight(1);
		float x1 = 0*xscale+xtrans;
		float y1 = ytrans-mouseY/yscale;
		
		//System.out.println( x1 );
		//System.out.println( y1 );
		
		line(x1,y1,width*xscale+xtrans,mouseY*yscale+ytrans);
	}
	
	public void keyPressed() {
		if(this.keyCode==84) {
			this.timeMode = !this.timeMode;
		}
	}

} 