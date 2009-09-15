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
	
	void setTransformation(float left,float bottom,float right,float top) {
		  xscale = width/(right-left);
		  yscale = height/(bottom-top);
		  xtrans = -left;
		  ytrans = -top;
	}
	
	public void drawGeoms() {
		//this.pushMatrix();
		//frame(left,start,right,end);
		for( ArrayList<Point> geom : geoms ) {
			for(int i=0; i<geom.size()-1; i++) {
				Point p1 = geom.get(i);
				Point p2 = geom.get(i+1);
				if(timeMode) {
				    line((p1.x+xtrans)*xscale, (p1.z+ytrans)*yscale, (p2.x+xtrans)*xscale, (p2.z+ytrans)*yscale);
				} else {
					line((p1.x+xtrans)*xscale, (p1.y+ytrans)*yscale, (p2.x+xtrans)*xscale, (p2.y+ytrans)*yscale);
				}
			}
		}
		//this.popMatrix();
	}

	public void setup(){
		size(700, 700, JAVA2D);
		stroke(155,0,0);

		smooth();
		background(255);
		
		try {			
			Feed feed = new Feed( "../../caltrain_gtfs.zip" );
			gg = new Graph();
			GTFSHopLoader hl = new GTFSHopLoader(gg,feed);
			System.out.println( "Loading feed to graph" );
			hl.load(new LoadDrawHandler(),true);
			System.out.println( "Done" );
			
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
		    setTransformation(left,start,right,end);
		} else {
			setTransformation(left,bottom,right,top);
		}
		background(255);
		strokeWeight(0.1f);
		drawGeoms();
		strokeWeight(0.1f);
		
//		if(timeMode) {
//			//draw a line that represents the time to select
//			float x1 = -(xtrans-mouseX/xscale);
//			float y1 = -(ytrans-mouseY/yscale);
//			
//			System.out.println( left );
//			System.out.println( right );
//			System.out.println( start );
//			System.out.println( end );
//			System.out.println( x1 );
//			System.out.println( y1 );
//			
//			line(left,start,right,y1);
//			
//			line(left,y1,right,y1);
//		} else {
//			//draw a line that represents the time to select
//			float x1 = -(xtrans-mouseX/xscale);
//			float y1 = -(ytrans-mouseY/yscale);
//			
//			System.out.println( left );
//			System.out.println( right );
//			System.out.println( start );
//			System.out.println( end );
//			System.out.println( x1 );
//			System.out.println( y1 );
//			
//			line(left,start,right,y1);
//			
//			line(left,y1,right,y1);
//		}
	}
	
	public void keyPressed() {
		if(this.keyCode==84) {
			this.timeMode = !this.timeMode;
		}
	}
	
	public void mousePressed() {
		System.out.println( "("+mouseX+","+mouseY+")" );
	}

} 