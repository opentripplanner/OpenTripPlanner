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
  */

otp.planner.TopoRenderer = {
    
    map :      null,
    panel :    null,
    
    axisDiv :              null,
    terrainContainerDiv :  null,
    terrainDiv :           null,
    previewDiv :           null,
    
    terrainPct :       0.8,
    axisWidth :        40,
    nonBikeLegWidth:   150,
    
    terrainCursor :    null,
    
    currentLeft      : 0,
    currentMouseRect : null,
    markerLayer    :   null,
    locationPoint  :   null,
    locationMarker :   null,
    
    /** */
    initialize : function(config)
    {
        otp.configure(this, config);
    },
    
    draw : function(itin, tree) {
        var thisTR = this;

        var nonBikeLegCount = 0;
        
        // calculate the distance and elevation range
        var distance = 0, minElev = 100000, maxElev = -1000;
        for (var li = 0; li < itin.m_legStore.getTotalCount(); li++) {
            
            var leg = itin.m_legStore.getAt(li);
            
            if(leg.get('mode') != "BICYCLE") {
                nonBikeLegCount++;
                continue;
            }
            
            var steps = leg.data.steps;
            for (var si = 0; si < steps.length; si++) {
                distance += steps[si].distance;
                if (typeof steps[si].elevation == 'undefined') {
                    continue;
                }
                var elevArr = steps[si].elevation.split(",");
                for (var ei = 1; ei < elevArr.length; ei+=2) {
                    var elevFt = elevArr[ei] * 3.2808399;
                    if (elevFt < minElev) {
                        minElev = elevFt;
                    }
                    if (elevFt > maxElev) {
                        maxElev = elevFt;
                    }
                }       
            }
        }
        minElev = 100*Math.floor(minElev/100);
        maxElev = 100*Math.ceil(maxElev/100);
        
        var width = this.panel.getEl().getWidth();
        var height = this.panel.getEl().getHeight();
        this.currentLeft = 0;
        this.currentMouseRect = null;
        this.markerLayer = null;
        this.locationPoint = null;
        this.locationMarker = null;
        var res = 5; // resolution of the main terrain graph in ft per pixel
        var terrainWidth = (distance*3.2808399)/res + nonBikeLegCount*this.nonBikeLegWidth;
        var showPreview = (terrainWidth > width);
        var terrainHeight = showPreview ? height * this.terrainPct : height;
        var previewHeight = height - terrainHeight;
        var subDivisions = (maxElev-minElev)/100;
        var subDivHeight = terrainHeight / subDivisions;
                
        this.createContainerDivs(width, height, terrainWidth, showPreview);
        
        var axisCanvas = Raphael(this.axisDiv);
        var terrainCanvas = Raphael(this.terrainDiv);
               
        // set up the terrain "cursor" w/ initial x = -10; not visible until
        // mouse first hovers over a terrain segment
        this.terrainCursor = terrainCanvas.rect(-10, 0, 1, terrainHeight).attr({
            fill : '#000',
            stroke : 'none',
            opacity : .75
        });
        
        // draw the "blue sky" background on both terrain and axis canvases
        terrainCanvas.rect(0, 0, terrainWidth, terrainHeight).attr({
            fill : '90-rgb(135,206,255)-rgb(204,245,255)', 
            stroke : 'none'
        });        
        axisCanvas.rect(0, 0, this.axisWidth, terrainHeight).attr({
            fill : '90-rgb(135,206,255)-rgb(204,245,255)', 
            stroke : 'none'
        });
        
        var d, rect;
        
        // draw the axis elevation labels
        for (d = 0; d <= subDivisions; d++) {
            var textY = subDivHeight*d;
            axisCanvas.rect(0, textY, this.axisWidth, 1).attr({
                fill: 'white',
                stroke: 'none'
            });
            terrainCanvas.rect(0, textY, terrainWidth, 1).attr({
                fill: 'white',
                stroke: 'none'
            });
            
            if(d == 0) textY += 6;
            if(d == subDivisions) textY -= 6;
            axisCanvas.text(this.axisWidth-3, textY, (maxElev-d*100)+"'").attr({
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
        var currentX = 0;
        
        // iterate through legs
        for (var li = 0; li < itin.m_legStore.getTotalCount(); li++) {
            
            var leg = itin.m_legStore.getAt(li);
            leg.topoGraphSpan = 0;
            var legStartX = currentX;
            
            // for non-bike legs, insert fixed-width arrow graphic into
            // topo graph indicating a "jump"
            if(leg.get('mode') != "BICYCLE") {
                var nonBikeRect = terrainCanvas.rect(currentX+2, 0, this.nonBikeLegWidth-2, terrainHeight).attr({
                    fill: 'white',
                    opacity: .75,
                    stroke: 'none'
                });                

                terrainCanvas.rect(currentX+4, terrainHeight/2-6, this.nonBikeLegWidth-15, 12).attr({
                    fill: 'black',
                    stroke: 'none'
                });
                terrainCanvas.path(["M",currentX+this.nonBikeLegWidth-16, terrainHeight/2-12, "L", currentX+this.nonBikeLegWidth-4, terrainHeight/2, "L", currentX+this.nonBikeLegWidth-16, terrainHeight/2+12,"z"]).attr({
                    fill: 'black',
                    stroke: 'none'
                });
                terrainCanvas.text(currentX + this.nonBikeLegWidth/2, terrainHeight/2 + 24, leg.get('mode')+" SEGMENT").attr({
                    fill: 'black',
                    'font-size' : '14px',
                    'font-weight' : 'bold'
                });
                
                previewXCoords.push(width*(legStartX)/terrainWidth);
                previewYCoords.push(previewHeight);
                
                previewXCoords.push(width*(legStartX+this.nonBikeLegWidth)/terrainWidth);
                previewYCoords.push(previewHeight);

                currentX += this.nonBikeLegWidth;
                continue;
            }
                        
            // for bike legs, iterate through each step of the leg geometry
            var steps = leg.data.steps;        
            for (si = 0; si < steps.length; si++) {
                var step = steps[si];
                var segWidth = (step.distance*3.2808399)/res;
                leg.topoGraphSpan += segWidth;

                var xCoords = new Array(), yCoords = new Array();
                var terrainPoly = null;

                if (step.elevation != undefined) {
                    //console.log("elev="+step.elevation);
                    var elevArr = step.elevation.split(",");
                    if(elevArr.length > 2) {
                        var stepLenM = elevArr[elevArr.length-2]; 
                        for (var j = 0; j < elevArr.length-1; j+=2) {
                            var posM = elevArr[j];
                            var elevFt = elevArr[j+1] * 3.2808399;
                            var x = currentX + (posM/stepLenM)*segWidth;
                            var y = terrainHeight-terrainHeight*(elevFt-minElev)/(maxElev-minElev);
                            if(j >= elevArr.length-2) x += 1;
                            xCoords.push(x);
                            yCoords.push(y);                        
                            previewXCoords.push(width*x/terrainWidth);
                            previewYCoords.push(previewHeight * (0.8 - 0.6*(elevFt-minElev)/(maxElev-minElev)));
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

                        pathStr += "L "+ (currentX + segWidth +1) + " " + terrainHeight + " ";
                        pathStr += "L "+ currentX + " " + terrainHeight + " z";


                        terrainPoly = terrainCanvas.path(pathStr).attr({
                            fill : 'rgb(34, 139, 34)', 
                            opacity : .5,
                            stroke : 'none'
                        });
                    }
                }
                
                var streetLabelBG = terrainCanvas.text(currentX + segWidth/2, 12, step.streetName).attr({
                    stroke: 'white',                    
                    'stroke-width' : 3,
                    opacity: 0,
                    'font-size' : '16px'
                });

                var streetLabelFG = terrainCanvas.text(currentX + segWidth/2, 12, step.streetName).attr({
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
                    if(this.poly != null) this.poly.animate({fill: "rgb(80, 200, 120)"}, 300);
                    this.labelBG.animate({opacity: 1}, 300);
                    this.labelFG.animate({opacity: 1}, 300);
                    thisTR.currentMouseRect = this;
                });
                
                mouseRect.mouseout(function(event) {
                    // hide the terrain cursor
                    thisTR.terrainCursor.attr({x : -10});
                    
                    // de-highlight the polygon and hide the street name label
                    // for the segment we're leaving
                    if(this.poly != null) this.poly.animate({fill: "rgb(34, 139, 34)"}, 300);
                    this.labelBG.animate({opacity: 0}, 300);
                    this.labelFG.animate({opacity: 0}, 300);
                    
                    // hide the locator marker on the main map
                    if(thisTR.locationMarker != null) {
                        thisTR.locationMarker.style = { display : 'none' };
                        thisTR.markerLayer.redraw();
                    }
                });
                
                mouseRect.mousemove(function (event) {
                    // shift terrain cursor to follow mouse movement
                    var nx = Math.round(event.clientX - thisTR.panel.getEl().getLeft() - thisTR.axisWidth - thisTR.currentLeft);
                    thisTR.terrainCursor.attr({x : nx});
                    
                    // also, show / move the locator marker on the main map
                    var distAlongLS = this.leg.get('legGeometry').getLength() * (nx-this.legStartX)/this.leg.topoGraphSpan;
                    thisTR.locationPoint = thisTR.pointAlongLineString(this.leg.get('legGeometry'), distAlongLS);                    
                    if(thisTR.markerLayer == null) {                        
                        thisTR.markerLayer = thisTR.map.getMap().getLayersByName('trip-marker-layer')[0];
                    }
                    if(thisTR.locationMarker == null) {
                        thisTR.locationMarker = thisTR.markerLayer.getFeatureById('bike-topo-marker');
                    }
                    thisTR.locationMarker.style = null;
                    thisTR.locationMarker.move(new OpenLayers.LonLat(thisTR.locationPoint.x, thisTR.locationPoint.y));
                });
                
                mouseRect.click(function (event) {
                    // respond to clicks by recentering map
                    if(thisTR.locationPoint != null) {
                        thisTR.map.getMap().setCenter(new OpenLayers.LonLat(thisTR.locationPoint.x, thisTR.locationPoint.y));
                    }
                });
                
                mouseRects.push(mouseRect);

                currentX += segWidth;
            }
        }
        
        // bring terrain cursor and street labels (currently hidden) to foreground
        thisTR.terrainCursor.toFront();        
        for(var b=0; b < bgLabels.length; b++) {
            bgLabels[b].toFront();
        }
        for(var f=0; f < fgLabels.length; f++) {
            fgLabels[f].toFront();
        }
        for(var m=0; m < mouseRects.length; m++) {
            mouseRects[m].toFront();
        }
        
        // draw preview panel (along bottom), if necessary

        if(showPreview) {
            var previewCanvas = Raphael(this.previewDiv);

            var previewPathStr = "M 0 " + previewHeight + " ";
            for(var p = 0; p < previewXCoords.length; p++) {
                previewPathStr += "L "+ previewXCoords[p] + " " + previewYCoords[p]+ " ";
            }
            previewPathStr += "L " + width + " " + previewHeight + " z";

            previewCanvas.path(previewPathStr).attr({
                stroke : 'none', 
                fill : 'gray'
            });  

            var pctVisible = (width - this.axisWidth) / terrainWidth;
            var sliderWidth = pctVisible*width;
            var slider = previewCanvas.rect(this.currentLeft, 3, sliderWidth, previewHeight-4).attr({
                fill : '#bbb', 
                'fill-opacity' : .5,
                stroke : '#000',
                'stroke-width' : 2
            });
            var rightArrow = previewCanvas.path([
                     "M",this.currentLeft+sliderWidth+6,previewHeight*.25,
                     "L",this.currentLeft+sliderWidth+18,previewHeight*.5,
                     "L",this.currentLeft+sliderWidth+6,previewHeight*.75,"z"]).attr({
                fill : '#000', 
                stroke : 'none',
                opacity : 0
            });
            var leftArrow = previewCanvas.path([
                     "M",this.currentLeft-6,previewHeight*.25,
                     "L",this.currentLeft-18,previewHeight*.5,
                     "L",this.currentLeft-6,previewHeight*.75,"z"]).attr({
                fill : '#000', 
                stroke : 'none',
                opacity : 0
            });
            
            slider.mouseover(function(event) {
                this.animate({fill: "#eee"}, 500);
                rightArrow.animateWith(this, {opacity: 1}, 500);
                leftArrow.animateWith(this, {opacity: 1}, 500);
            });
            slider.mouseout(function(event) {
                this.animate({fill: "#bbb"}, 500);
                rightArrow.animateWith(this, {opacity: 0}, 500);
                leftArrow.animateWith(this, {opacity: 0}, 500);
            });

            var start = function () {
                // storing original coordinates
                this.ox = this.attr("x");
            },
            move = function (dx, dy) {
                var nx = this.ox + dx;
                if(nx < 0) nx = 0;
                if(nx > width-sliderWidth) nx = width-sliderWidth;

                thisTR.currentLeft = Math.round(-(nx/width)*terrainWidth);
                thisTR.terrainDiv.style.left =  thisTR.currentLeft + 'px';

                var ndx = nx - this.attr("x");
                this.attr({x: nx});
                rightArrow.translate(ndx,0);
                leftArrow.translate(ndx,0);
            },
            up = function () {
            };

            slider.drag(move, start, up);
        }
    },
    
    createContainerDivs : function(width, height, terrainWidth, showPreview) {
        var upperHeight = showPreview ? height * this.terrainPct : height;;
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
        this.terrainDiv.style.height = upperHeight + 'px';
        this.terrainDiv.style.width = terrainWidth + 'px';        

        if(showPreview) {
            this.previewDiv = document.createElement('div');
            this.previewDiv.style.position = 'absolute';
            this.previewDiv.style.top = upperHeight + 'px';
            this.previewDiv.style.left = '0px';
            this.previewDiv.style.height = lowerHeight + 'px';
            this.previewDiv.style.width = width + 'px';        
        }
        
        // Remove all existing elements from the topo panel and add the new div
        var panelEl = this.panel.getEl();
        while (panelEl.first()) { 
            panelEl.first().remove();
        }
        panelEl.appendChild(this.axisDiv);
        panelEl.appendChild(this.terrainContainerDiv);
        this.terrainContainerDiv.appendChild(this.terrainDiv);
        if(showPreview) panelEl.appendChild(this.previewDiv);
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

otp.planner.TopoRenderer = new otp.Class(otp.planner.TopoRenderer);