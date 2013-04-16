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

otp.namespace("otp.planner");

/**
  * TopoRenderer
  * 
  * Renders a 2-D topographic map of a trip in a specified panel using the 
  * RaphaelJS library.
  * 
  * TopoRenderer is created by Planner.
  * 
  * Adapted by Timothy Weyrer (04/15/2013)
  */

otp.planner.TopoRendererStatic = {

    map :       null,
    panel :     null,

    extWrapper :            null,
    mainContainerDiv :      null,
    axisDiv :               null,
    terrainContainerDiv :   null,
    terrainDiv :            null,
    previewDiv :            null,

    terrainPct :        0.8,
    axisWidth :         50, 
    nonBikeLegWidth:    150,

    terrainCursor :     null,

    currentLeft      :  0,
    currentMouseRect :  null,
    markerLayer    :    null,
    locationPoint  :    null,
    locationMarker :    null,

    legInfoArr :        null,
    nonBikeWalkLegCount :   null,
    minElev :           null,
    maxElev :           null,
    totalDistance :     null,
    metricsSystem :   	null, 
    unitRepresentation :	null, 
    elevInterval	:	null, 
    THIS          :     null,

    /** */
    initialize : function(config)
    {
        otp.configure(this, config);
        otp.planner.TopoRendererStatic.THIS = this;
        this.THIS = this;
        
        this.metricsSystem = otp.config.metricsSystem;

        if(this.metricsSystem == 'international')
        {
        	this.unitRepresentation = ' m';
        	this.elevInterval = 15; 
        }else
        {
        	this.unitRepresentation = " '";
        	this.elevInterval = 50;
        }
    },
    
    processItinerary : function(itin) {
        this.legInfoArr = new Array();
        this.nonBikeWalkLegCount = 0;
        this.minElev = 100000;
        this.maxElev = -1000;
        
        this.totalDistance = 0;
        
        for (var li = 0; li < itin.m_legStore.getTotalCount(); li++) {
            
            var leg = itin.m_legStore.getAt(li);
        
            var legInfo = new Array();
            this.legInfoArr.push(legInfo);
            
            legInfo.leg = leg;
            
            if(leg.get('mode') != "BICYCLE" && leg.get('mode') != "WALK") {
                this.nonBikeWalkLegCount++;
                continue;
            }
            
            var steps = leg.data.steps;
            var firstElev = 0, lastElev = 0;
            for (var si = 0; si < steps.length; si++) {
            	//sum up distance in case mode is either walk or bicycle
            	if(leg.get('mode') == "BICYCLE" || leg.get('mode') == "WALK") 
            		this.totalDistance += steps[si].distance; // total distance in meter
                if (typeof steps[si].elevation == 'undefined') {
                    continue;
                }
                var elevArr = steps[si].elevation.split(","); 
                for (var ei = 1; ei < elevArr.length; ei+=2) {
                    var elev;
                    if(this.metricsSystem == 'international')
                    	elev = elevArr[ei]; //convert to feet 
                    else // english - convert to feet
                    	elev = elevArr[ei] * 3.2808399;
                    if (elev < this.minElev) {
                        this.minElev = elev;
                    }
                    if (elev > this.maxElev) {
                        this.maxElev = elev;
                    }
                    if(firstElev == 0 && elev > 0) firstElev = elev;
                    if(elev > 0) lastElev = elev;
                }       
            }
            legInfo.firstElev = firstElev;
            legInfo.lastElev = lastElev;
        }
        
    },
    
    draw : function(itin, tree) {
        
        this.processItinerary(itin);
        
        var width = this.panel.getWidth();
        var height = this.panel.getHeight();
        
        if(height == 0) height = 180;
        if(width == 0) width = 1074;
        
        this.render(width, height);
        
        this.extWrapper = new Ext.Panel({
            contentEl : this.mainContainerDiv,
            layout: 'fit',
        });
                
    },
    
    redraw : function() {
        this.panel.remove(this.extWrapper);
        
        var width = this.panel.getWidth();
        var height = this.panel.getHeight();
        
        if(height == 0) height = 180;
        if(width == 0) width = 1074;
        
        this.render(width, height);
        this.extWrapper = new Ext.Panel({
            contentEl : this.mainContainerDiv,
            layout: 'fit',
        });
        this.panel.add(this.extWrapper);
        this.panel.doLayout();
        this.postLayout();
    },

    postLayout : function()
    {
        var this_ = this;
        this.extWrapper.on('resize', function(el) {
            this_.redraw();
        });
    },
    
    
    render : function(width, height) {
        var this_ = this;

        this.currentLeft = 0;
        this.currentMouseRect = null;
        this.markerLayer = null;
        this.locationPoint = null;
        this.locationMarker = null;
        
        // compute the width of the main elevation graphic in pixels
        // compute the resolution of the main terrain
        var terrainWidth, res;
        if(this.metricsSystem == 'international')
        {
        	res = this.totalDistance / (width - this.axisWidth - 10 - this.nonBikeLegWidth*this.nonBikeWalkLegCount);
        	terrainWidth = (this.totalDistance)/res;
        }	
        else
        {
        	res = (this.totalDistance*3.2808399) / (width - this.axisWidth - 10 - this.nonBikeLegWidth*this.nonBikeWalkLegCount);
        	terrainWidth = (this.totalDistance*3.2808399)/res;
        }

        // if the graph is wider than what can be displayed without scrolling, 
        // split the panel between the main graph and a "preview" strip 
        var showPreview = (terrainWidth > (width - this.axisWidth));
        var terrainHeight = showPreview ? height * this.terrainPct : height;
        var previewHeight = height - terrainHeight;
        
        // expand the min/max elevation range to align with interval multiples 
        this.minElev = this.elevInterval*Math.floor(this.minElev/this.elevInterval);
        this.maxElev = this.elevInterval*Math.ceil(this.maxElev/this.elevInterval);

        // create the container div elements and associated Raphael canvases                     
        this.createContainerDivs(width, height, width-this.axisWidth); 

        var axisCanvas = Raphael(this.axisDiv);
        var terrainCanvas = Raphael(this.terrainDiv, width-this.axisWidth, terrainHeight, showPreview);
               
        // set up the terrain "cursor" w/ initial x = -10; not visible until
        // mouse first hovers over a terrain segment
        this.terrainCursor = terrainCanvas.rect(-10, 0, 1, terrainHeight).attr({
            fill : '#000',
            stroke : 'none',
            opacity : .75
        });
        
        // draw the "blue sky" background on both terrain and axis canvases
        terrainCanvas.rect(0, 0, width-this.axisWidth, terrainHeight).attr({
            fill : '90-rgb(135,206,255)-rgb(204,245,255)', 
            stroke : 'none'
        });        
        axisCanvas.rect(0, 0, this.axisWidth, terrainHeight).attr({
            fill : '90-rgb(135,206,255)-rgb(204,245,255)', 
            stroke : 'none'
        });
        
        var d, rect;
        
        // draw the axis elevation labels
        // in case the elevation difference btw this.maxElev and this.minElev is small (one interval), the visualization looks kind of bad ... depends on personal preference
        if((this.maxElev - this.minElev) == this.elevInterval)
            this.minElev -=this.elevInterval;

        var subDivisions = (this.maxElev-this.minElev)/this.elevInterval;
        var subDivHeight = terrainHeight / subDivisions;
        for (d = 0; d <= subDivisions; d++) {
            var textY = subDivHeight*d;
            axisCanvas.rect(0, textY, this.axisWidth, 1).attr({
                fill: 'white',
                stroke: 'none'
            });
            terrainCanvas.rect(0, textY, width-this.axisWidth, 1).attr({ //CHANGE 04/12/13 terrainWidth to width ... so that the whole div has the blue background
                fill: 'white',
                stroke: 'none'
            });
            //Labels for y axis
            if(d == 0) textY += 12; //CHANGE 04/12/13 ... before 6 ... a higher value in this position 
            if(d == subDivisions) textY -= 6;
            
            axisCanvas.text(this.axisWidth-3, textY, (this.maxElev-d*this.elevInterval)+this.unitRepresentation).attr({ 
                fill: 'black',
                'font-size' : '12px',
                'font-weight' : 'bold',
                'text-anchor' : 'end'
            });
        }

        // MAIN LOOP

        var bgLabels = new Array();
        var fgLabels = new Array();
        var mouseRects = new Array();
        var previewXCoords = new Array();
        var previewYCoords = new Array();
        var currentX = 0, lastTerrainHeight = terrainHeight/2;

        // iterate through legs
        for (var li = 0; li < this.legInfoArr.length; li++) {
            var legInfo = this.legInfoArr[li];
            var leg = legInfo.leg; //itin.m_legStore.getAt(li);
            leg.topoGraphSpan = 0;
            var legStartX = currentX;

            // for non-bike/walk legs, insert fixed-width arrow graphic into
            // topo graph indicating a "jump"
            if(leg.get('mode') != "BICYCLE" && leg.get('mode') != "WALK")
            {
                var prevElevY = (li == 0) ? terrainHeight/2 : terrainHeight-terrainHeight*(this.legInfoArr[li-1].lastElev-this.minElev)/(this.maxElev-this.minElev);
                var nextElevY = (li >= this.legInfoArr.length-1) ? terrainHeight/2 : terrainHeight-terrainHeight*(this.legInfoArr[li+1].firstElev-this.minElev)/(this.maxElev-this.minElev);

                if(isNaN(prevElevY) || prevElevY < 0 || prevElevY >= terrainHeight) prevElevY = terrainHeight/2;
                if(isNaN(nextElevY) || nextElevY < 0 || nextElevY >= terrainHeight) nextElevY = terrainHeight/2;

                var midX = currentX + this.nonBikeLegWidth/2;
                var midY = (prevElevY + nextElevY)/2;

                var curve = [["M",currentX+4, prevElevY],["C", midX, prevElevY, midX, prevElevY, midX, midY],["C", midX, nextElevY, midX, nextElevY, currentX+this.nonBikeLegWidth-16, nextElevY]];
                terrainCanvas.path(curve).attr({
                    stroke : 'black', 
                    'stroke-width' : '6',
                    fill : 'none'
                });

                // mode strings (localized to otp.locale by default) 
                var mode    = leg.get('mode').toLowerCase();
                var modeStr = otp.util.Modes.translate(mode, this.locale);
                var imgPath = "images/ui/trip/mode/" + mode + ".png";

                terrainCanvas.image(imgPath, midX-10, midY-10, 20, 20);

                // draw the arrowhead
                terrainCanvas.path(["M",currentX+this.nonBikeLegWidth-16, nextElevY-12, "L", currentX+this.nonBikeLegWidth-4, nextElevY, "L", currentX+this.nonBikeLegWidth-16, nextElevY+12,"z"]).attr({
                    fill: 'black',
                    stroke: 'none'
                });
                terrainCanvas.text(currentX + this.nonBikeLegWidth/2, terrainHeight - 10, modeStr + " " + leg.get('routeShortName')).attr({
                    fill: 'black',
                    'font-size' : '14px',
                    'font-weight' : 'bold'
                });
                
                previewXCoords.push(legStartX);
                previewYCoords.push(previewHeight);
                
                previewXCoords.push(legStartX+this.nonBikeLegWidth); //CHANGE 04/12/13 width instead of terrainWidth at last parameter within brackets                

                currentX += this.nonBikeLegWidth;
                continue;
            }
                        
            // for bike/walk legs, iterate through each step of the leg geometry
            var steps = leg.data.steps;
            var legXCoords = new Array(), legYCoords = new Array();
            for (si = 0; si < steps.length; si++) {
                var step = steps[si];

                var segWidth;
                if(this.metricsSystem == 'international')
                	segWidth = (step.distance)/res;
                else
                	segWidth = (step.distance*3.2808399)/res;
               
                leg.topoGraphSpan += segWidth;
                var xCoords = new Array(), yCoords = new Array();
                var terrainPoly = null;

                if (step.elevation != undefined) {
                    var elevArr = step.elevation.split(",");
                    if(elevArr.length > 2) {
                        var stepLenM = elevArr[elevArr.length-2]; 
                        for (var j = 0; j < elevArr.length-1; j+=2) {
                            var posM = elevArr[j];
                            
                            var elev; //CHANGE 04/12/13
                            if(this.metricsSystem == 'international')
                            	elev = elevArr[j+1];
                            else
                            	elev = elevArr[j+1] * 3.2808399;
                            
                            var x = currentX + (posM/stepLenM)*segWidth;
                            var y = terrainHeight-terrainHeight*(elev-this.minElev)/(this.maxElev-this.minElev);
                            if(j >= elevArr.length-2) x += 1;
                            xCoords.push(x);
                            yCoords.push(y);
                            legXCoords.push(x);
                            legYCoords.push(y);
                            previewXCoords.push((width - this.axisWidth)*x/(width - this.axisWidth));//CHANGE 04/12/13 change form terrainwidth to width
                            previewYCoords.push(previewHeight * (0.8 - 0.6*(elev-this.minElev)/(this.maxElev-this.minElev)));
                        }

                        var pathStr = "M "+ xCoords[0] + " " + yCoords[0]+ " ";
                        for(var p = 1; p < xCoords.length; p++) {
                            pathStr += "L "+ xCoords[p] + " " + yCoords[p]+ " ";
                        }

                        terrainCanvas.path(pathStr).attr({
                            stroke : 'rgb(34, 139, 34)', 
                            'stroke-width' : '3',
                            fill : 'none'
                        });                

                    }
                    
                    lastTerrainHeight = yCoords[yCoords.length-1];
                }
                
                var streetLabelBG = terrainCanvas.text(currentX + segWidth/2, terrainHeight/2, step.streetName).attr({
                    stroke: 'white',                    
                    'stroke-width' : 3,
                    opacity: 0,
                    'font-size' : '16px'
                });

                var streetLabelFG = terrainCanvas.text(currentX + segWidth/2, terrainHeight/2, step.streetName).attr({
                    fill: 'black',
                    opacity: 0,
                    'font-size' : '16px'
                });
                
                bgLabels.push(streetLabelBG);
                fgLabels.push(streetLabelFG);

                // create a rectangular area in front of this elevation segment
                // to handle mouse envets
                
                var mouseRect = terrainCanvas.rect(currentX, 0, segWidth, terrainHeight).attr({
                    fill: 'white',
                    'fill-opacity': 0,
                    stroke: 'none'
                });

                mouseRect.poly = terrainPoly;
                mouseRect.labelBG = streetLabelBG;
                mouseRect.labelFG = streetLabelFG;
                mouseRect.leg = leg;
                mouseRect.legStartX = legStartX;

                mouseRect.mouseover(function(event) {
                    // highlight the polygon and show the street name label
                    // for the segment we're entering
                    //if(this.poly != null) this.poly.animate({fill: "rgb(80, 200, 120)"}, 300);
                    this.animate({'fill-opacity': .25}, 300);
                    this.labelBG.animate({opacity: 1}, 300);
                    this.labelFG.animate({opacity: 1}, 300);
                    this_.currentMouseRect = this;
                });
                
                mouseRect.mouseout(function(event) {
                    // hide the terrain cursor
                    this_.terrainCursor.attr({x : -10});
                    
                    // de-highlight the polygon and hide the street name label
                    // for the segment we're leaving
                    //if(this.poly != null) this.poly.animate({fill: "rgb(34, 139, 34)"}, 300);
                    this.animate({'fill-opacity': 0}, 300);

                    this.labelBG.animate({opacity: 0}, 300);
                    this.labelFG.animate({opacity: 0}, 300);
                    
                    // hide the locator marker on the main map
                    if(this_.locationMarker != null) {
                        this_.locationMarker.style = { display : 'none' };
                        this_.markerLayer.redraw();
                    }
                });

                mouseRect.mousemove(function (event) {
                    // shift terrain cursor to follow mouse movement
                    var nx = Math.round(event.clientX - this_.panel.getEl().getLeft() - this_.axisWidth - this_.currentLeft);
                    this_.terrainCursor.attr({x : nx});

                    // also, show / move the locator marker on the main map
                    var distAlongLS = this.leg.get('legGeometry').getLength() * (nx-this.legStartX)/this.leg.topoGraphSpan;
                    this_.locationPoint = this_.pointAlongLineString(this.leg.get('legGeometry'), distAlongLS);
                    if(this_.markerLayer == null) {
                        this_.markerLayer = this_.map.getMap().getLayersByName('trip-marker-layer')[0];
                    }

                    if(this_.locationMarker == null || this_.locationMarker.attributes.mode != this.leg.get('mode')) {
                        var topoMarkerID = this.leg.get('mode').toLowerCase()+'-topo-marker';
                        this_.locationMarker = this_.markerLayer.getFeatureById(topoMarkerID);
                    }
                    this_.locationMarker.style = null;
                    this_.locationMarker.move(new OpenLayers.LonLat(this_.locationPoint.x, this_.locationPoint.y));
                });
                
                mouseRect.click(function (event) {
                    // respond to clicks by recentering map
                    if(this_.locationPoint != null) {
                        this_.map.getMap().setCenter(new OpenLayers.LonLat(this_.locationPoint.x, this_.locationPoint.y));
                    }
                });
                
                mouseRects.push(mouseRect);
                
                currentX += segWidth;
            } // end of step loop
            
            var polyStr = "M "+ legXCoords[0] + " " + legYCoords[0]+ " ";
            for(var p = 1; p < legXCoords.length; p++) {
                polyStr += "L "+ legXCoords[p] + " " + legYCoords[p]+ " ";
            }

            polyStr += "L "+ legXCoords[legXCoords.length-1] + " " + terrainHeight + " ";
            polyStr += "L "+ legXCoords[0] + " " + terrainHeight + " z";

            terrainPoly = terrainCanvas.path(polyStr).attr({
                fill : "url(images/ui/topo/bg_"+leg.get("mode").toLowerCase()+".png)", //'rgb(34, 139, 34)', 
                opacity : .5,
                stroke : 'none'
            });
        } // end of leg loop
        
        // bring terrain cursor and street labels (currently hidden) to foreground
        this_.terrainCursor.toFront();        
        for(var b=0; b < bgLabels.length; b++) {
            bgLabels[b].toFront();
        }
        for(var f=0; f < fgLabels.length; f++) {
            fgLabels[f].toFront();
        }
        for(var m=0; m < mouseRects.length; m++) {
            mouseRects[m].toFront();
        }
    },
    
    createContainerDivs : function(width, height, terrainWidth, showPreview) {
        var upperHeight = showPreview ? height * this.terrainPct : height;
        var lowerHeight = height - upperHeight;
                
        this.axisDiv = document.createElement('div');
        this.axisDiv.style.position = 'absolute';
        this.axisDiv.style.top = '0px';
        this.axisDiv.style.left = '0px';
        this.axisDiv.style.height = upperHeight + 'px';
        this.axisDiv.style.width = this.axisWidth + 'px';        
        
        this.terrainContainerDiv = document.createElement('div');
        this.terrainContainerDiv.style.overflow = 'hidden';
        this.terrainContainerDiv.style.position = 'absolute';
        this.terrainContainerDiv.style.top = '0px';
        this.terrainContainerDiv.style.left = this.axisWidth + 'px';
        this.terrainContainerDiv.style.height = upperHeight + 'px';
        this.terrainContainerDiv.style.width = (width - this.axisWidth) + 'px';        
        
        this.terrainDiv = document.createElement('div');
        this.terrainDiv.style.position = 'absolute';
        this.terrainDiv.style.top = '0px';
        this.terrainDiv.style.left = '0px';
        this.terrainDiv.style.paddingBottom = '50px'; 
        this.terrainDiv.style.height = upperHeight + 'px';
        this.terrainDiv.style.width = terrainWidth + 'px';        

        this.mainContainerDiv = document.createElement('div');
        this.mainContainerDiv.appendChild(this.axisDiv);
        this.mainContainerDiv.appendChild(this.terrainContainerDiv);
        this.terrainContainerDiv.appendChild(this.terrainDiv);
    },

    pointAlongLineString : function(ls, d) {
        var points = ls.components;
        if(d <= 0) return points[0];
        for(var i = 0; i < points.length - 1; i++) {
            var segLen = points[i].distanceTo(points[i+1]);
            if(d <= segLen) { // this segent contains the point at distance d
                var x = points[i].x + (d/segLen * (points[i+1].x - points[i].x));
                var y = points[i].y + (d/segLen * (points[i+1].y - points[i].y));
                return new OpenLayers.Geometry.Point(x, y);
            }
            d -= segLen;
        }

        return points[points.length-1];
    },
    
    CLASS_NAME: "otp.planner.TopoRenderer"
};

otp.planner.TopoRenderer = new otp.Class(otp.planner.TopoRendererStatic);
