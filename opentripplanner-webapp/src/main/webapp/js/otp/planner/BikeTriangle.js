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

otp.planner.BikeTriangle = {

    container:    null,
    panel:        null,
    cursor_size:  20,

    triangleTimeFactor:    null,
    triangleSlopeFactor:   null,
    triangleSafetyFactor:  null,

    // default is 100% safety 
    timeFactor:    0.0,
    slopeFactor:   0.0,
    safetyFactor:  1.0,


    initialize : function(config) {
        otp.configure(this, config);

        this.panel = new Ext.Panel({  
            id:             'trip-bike-triangle',
            title:          'Custom Bike Options',
            name:           'bikeTriangle',  
            style:          {
                                border: '1px solid gray'
                            }
        });
        
        var thisBT = this;
        this.panel.on({
              render: function(obj) {
                  thisBT.render(thisBT.container.getWidth(), 120);
              }
        });
    },
    
    render : function(width, height) {
        var tri_side = 2 * (height - this.cursor_size) * 1/Math.sqrt(3);
        var margin = this.cursor_size/2;	

        var canvas = Raphael(this.panel.body.id, width, height);

        var bg = canvas.rect(0,0,width,height).attr({
              stroke: 'none',
              fill: '#eee'
          });

        var triangle = canvas.path(["M",margin+tri_side/2,margin,"L",margin+tri_side,height-margin,"L",margin,height-margin,"z"]);


        triangle.attr({fill:"#ddd", stroke:"none"});
        
        var labelSize = "18px";

        var labelT = canvas.text(margin + tri_side/2, margin+24, "T");
        labelT.attr({fill:"red", "font-size":labelSize, "font-weight":"bold"});	

        var labelH = canvas.text(margin + 22, height-margin-14, "H");
        labelH.attr({fill:"green", "font-size":labelSize, "font-weight":"bold"});	

        var labelS = canvas.text(margin + tri_side - 22, height-margin-14, "S");
        labelS.attr({fill:"blue", "font-size":labelSize, "font-weight":"bold"});	
		
        var barLeft = margin*2 + tri_side; 
        var barWidth = width - margin*3 - tri_side;
        var barHeight = (height-margin*4)/3;
	
        var timeBar = canvas.rect(barLeft, margin, barWidth*.333, barHeight);
        timeBar.attr({fill:"red", stroke:"none"});

        var topoBar = canvas.rect(barLeft, margin*2+barHeight, barWidth*.333, barHeight);
        topoBar.attr({fill:"green", stroke:"none"});

        var safetyBar = canvas.rect(barLeft, margin*3 + barHeight*2, barWidth*.333, barHeight);
        safetyBar.attr({fill:"blue", stroke:"none"});

        var timeLabel = canvas.text(barLeft + barWidth/2, margin+barHeight/2, "Time: 33%");
        timeLabel.attr({"font-size":"16px", opacity:0});

        var topoLabel = canvas.text(barLeft + barWidth/2, margin*2+barHeight+barHeight/2, "Hills: 33%");
        topoLabel.attr({"font-size":"16px", opacity:0});

        var safetyLabel = canvas.text(barLeft + barWidth/2, margin*3+barHeight*2+barHeight/2, "Safety: 33%");
        safetyLabel.attr({"font-size":"16px", opacity:0});

        var cursor = canvas.circle(margin+tri_side/2, height-margin-(1/Math.sqrt(3))*(tri_side/2), this.cursor_size/2).attr({
            fill: "rgb(0,0,128)",
            stroke: "none",
        });

        var time, topo, safety;

        var thisBT = this;
        var animTime = 250;
        var start = function () {
            // storing original coordinates
            this.ox = this.attr("cx");
            this.oy = this.attr("cy");
            this.animate({opacity: .5}, animTime);
            timeBar.animate({opacity: .25}, animTime);    	
            topoBar.animateWith(timeBar, {opacity: .25}, animTime);    	
            safetyBar.animateWith(timeBar, {opacity: .25}, animTime);    
            timeLabel.animate({opacity: 1}, animTime);    	
            topoLabel.animate({opacity: 1}, animTime);
            safetyLabel.animate({opacity: 1}, animTime);
        },
        move = function (dx, dy) {
            // move will be called with dx and dy
            var nx = this.ox + dx, ny = this.oy + dy;
            if(ny >  height-margin) ny = height-margin;
            if(ny < margin) ny = margin;
            var offset =  (ny-margin)/(height-margin*2) * tri_side/2; 	
            if(nx < margin + (tri_side/2) - offset) nx = margin + (tri_side/2) - offset; 
            if(nx > margin + (tri_side/2) + offset) nx = margin + (tri_side/2) + offset;
            
            time = ((height-2*margin)-(ny-margin))/(height-2*margin);
            topo = thisBT.distToSegment(nx, ny, margin+tri_side/2, margin, margin+tri_side, height-margin)/(height-2*margin);
            safety = 1- time - topo;
    
            timeBar.attr({width: barWidth*time});
            topoBar.attr({width: barWidth*topo});
            safetyBar.attr({width: barWidth*safety});
            timeLabel.attr("text", "Time: "+Math.round(time*100)+"%");
            topoLabel.attr("text", "Hills: "+Math.round(topo*100)+"%");
            safetyLabel.attr("text", "Safety: "+Math.round(safety*100)+"%");    	
    
            this.attr({cx: nx, cy: ny});
        },
        up = function () {
            // restoring state
            this.animate({opacity: 1}, animTime);
            timeBar.animate({opacity: 1}, animTime);
            topoBar.animateWith(timeBar, {opacity: 1}, animTime);
            safetyBar.animateWith(timeBar, {opacity: 1}, animTime);
            timeLabel.animate({opacity: 0}, animTime);
            topoLabel.animate({opacity: 0}, animTime);
            safetyLabel.animate({opacity: 0}, animTime);

            // was seeing really odd small numbers in scientific notation when topo neared zero so added this
            if(topo < 0.005) {
                topo = 0.0;
            }
            
            thisBT.timeFactor = time;
            thisBT.slopeFactor = topo;
            thisBT.safetyFactor = safety;
        };

        cursor.drag(move, start, up);
    },

    enable : function() {
          if(this.container.findById('trip-bike-triangle') == null) {
               this.container.add(this.panel);
          }
          this.panel.show();
          this.container.doLayout();
    },

    disable : function() {
        if(!this.panel.hidden) {
            this.panel.hide();
        }
    },

    distance : function(x1, y1, x2, y2) {
	     return Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    },

    distToSegment : function(px, py, x1, y1, x2, y2) {
        var r, dx ,dy;
        dx = x2 - x1;
        dy = y2 - y1;
        r = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        return this.distance(px, py, (1 - r) * x1 + r * x2, (1 - r) * y1 + r * y2);
    },

    /** */
    setSHT : function(safe, hills, time) {
        this.safetyFactor = otp.util.ObjUtils.toFloat(safe,  1.0);
        this.slopeFactor  = otp.util.ObjUtils.toFloat(hills, 0.0);
        this.timeFactor   = otp.util.ObjUtils.toFloat(time,  0.0);
    },

    /** NOTE: don't rename this stuff, as OTP api depends on these values */
    getFormData : function() {
        return {
                triangleTimeFactor     : this.timeFactor,
                triangleSlopeFactor    : this.slopeFactor,
                triangleSafetyFactor   : this.safetyFactor
        }
    },

    CLASS_NAME: "otp.planner.BikeTriangle"

};

otp.planner.BikeTriangle = new otp.Class(otp.planner.BikeTriangle);
