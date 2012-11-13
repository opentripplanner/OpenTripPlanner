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

otp.namespace("otp.widgets");

otp.widgets.TripWidget = 
    otp.Class(otp.widgets.Widget, {
    
    planTripCallback : null,
    panels : null,
        
    initialize : function(id, planTripCallback) {
    
        otp.widgets.Widget.prototype.initialize.apply(this, arguments);
        this.planTripCallback = planTripCallback;
        
        this.panels = { };       
    },

    addPanel : function(id, panel) {
        panel.$().appendTo(this.$());
        //$("<hr />").appendTo(this.$());
        panel.doAfterLayout();
        this.panels[id] = panel;
    },
    
    addSeparator : function() {
        $("<hr />").appendTo(this.$());
    },
    
    restorePlan : function(data) {
	    if(data == null) return;

        for(var id in this.panels) {
            this.panels[id].restorePlan(data);
        }
    },
    
    newItinerary : function(itin) {
        for(var id in this.panels) {
            this.panels[id].newItinerary(itin);
        }
    },
    
    
    CLASS_NAME : "otp.widgets.TripWidget"
});


/** PANEL CLASSES */

otp.widgets.TripWidgetPanel = otp.Class({
    
    div :   null,
    tripWidget : null,
    
    initialize : function(tripWidget) {
        this.tripWidget = tripWidget;
        this.div = document.createElement('div');
        //this.div.className()
    },
    
    setContent : function(content) {
        this.div.innerHTML = content;
        console.log("twp seting:"+content)
    },
        
    doAfterLayout : function() {
    },
    
    restorePlan : function(data) {
    },
    
    newItinerary : function(itin) {
    },

    $ : function() {
        return $(this.div);
    }
});


otp.widgets.TW_TripSummary = 
    otp.Class(otp.widgets.TripWidgetPanel, {
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
        
        var content = '';
        content += '<div id="otp-tsw-distance"></div>';
        content += '<div id="otp-tsw-duration"></div>';
        content += '<div id="otp-tsw-timeSummary"></div>';    
        this.setContent(content);
        console.log("tsp set content");
    },

    newItinerary : function(itin) {
    	console.log("TS newItin");
    	
    	var dist = 0;
    	
    	for(var i=0; i < itin.legs.length; i++) {
    		dist += itin.legs[i].distance;
        }
    	
        $("#otp-tsw-distance").html(Math.round(100*(dist/1609.344))/100+" mi.");
        $("#otp-tsw-duration").html(otp.util.Time.msToHrMin(itin.duration));	
        
        var timeByMode = { };
        for(var i=0; i < itin.legs.length; i++) {
            if(itin.legs[i].mode in timeByMode) {
                timeByMode[itin.legs[i].mode] = timeByMode[itin.legs[i].mode] + itin.legs[i].duration;
            }
            else {
                timeByMode[itin.legs[i].mode] = itin.legs[i].duration;
            }
        }
        
        var summaryStr = "";
        for(mode in timeByMode) {
            summaryStr += otp.util.Time.msToHrMin(timeByMode[mode]) + " " + this.getModeName(mode) + " / ";
        }
        summaryStr = summaryStr.slice(0, -3);
        $("#otp-tsw-timeSummary").html(summaryStr);	
    },    

    getModeName : function(mode) {
        switch(mode) {
            case 'WALK':
                return "walking";
            case 'BICYCLE':
                return "biking";
        }
        return "n/a";
    }
});


otp.widgets.TW_BikeTriangle = 
    otp.Class(otp.widgets.TripWidgetPanel, {
    
    bikeTriangle :  null,
       
    initialize : function(tripWidget) {
        console.log(this);
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
        
        var content = '';
        content += '<h6 class="drag-to-change">Drag to Change Trip:</h6>';
        content += '<div id="otp-tsw-bikeTriangle"></div>';
        
        this.setContent(content);
    },

    doAfterLayout : function() {
        this.bikeTriangle = new otp.widgets.BikeTrianglePanel('otp-tsw-bikeTriangle');
        this.bikeTriangle.onChanged = this.tripWidget.planTripCallback;
        this.tripWidget.$().draggable({ cancel: "#otp-tsw-bikeTriangle" });
    },

    restorePlan : function(data) {
        if(data.optimize === 'TRIANGLE') {
            this.bikeTriangle.setValues(data.triangleTimeFactor, data.triangleSlopeFactor, data.triangleSafetyFactor);
        }
    }
        
});


otp.widgets.TW_BikeType = 
    otp.Class(otp.widgets.TripWidgetPanel, {
       
    initialize : function(tripWidget) {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);

        var content = '';        
        content += 'Use: ';
        content += '<input id="myOwnBikeRBtn" type="radio" name="bikeType" value="my_bike" checked> My Own Bike&nbsp;&nbsp;';
        content += '<input id="sharedBikeRBtn" type="radio" name="bikeType" value="shared_bike"> A Shared Bike';
        
        this.setContent(content);
    },

    doAfterLayout : function() {
        document.getElementById('myOwnBikeRBtn').onclick = this.tripWidget.planTripCallback;
        document.getElementById('sharedBikeRBtn').onclick = this.tripWidget.planTripCallback;
    },
    
    restorePlan : function(data) {
        if(data.mode === "BICYCLE") {
            $('#myOwnBikeRBtn').attr('checked', 'checked');
        }
        if(data.mode === "WALK,BICYCLE") {
            $('#sharedBikeRBtn').attr('checked', 'checked');
        }
    }
        
});



otp.widgets.TW_AddThis = 
    otp.Class(otp.widgets.TripWidgetPanel, {
       
    initialize : function() {
        otp.widgets.TripWidgetPanel.prototype.initialize.apply(this, arguments);
        
        var content = '';
        content += '<h6 id="share-route-header">Share this Trip:</h6>';
        content += '<div id="share-route"></div>';

        this.setContent(content);
    },
    
    doAfterLayout : function() {
        // Copy our existing share widget from the header and customize it for route sharing.
        // The url to share is set in PlannerModule.js in the newTrip() callback that is called
        // once a new route is loaded from the server.
        var addthisElement = $(".addthis_toolbox").clone();
        addthisElement.find(".addthis_counter").remove();
        
        // give this addthis toolbox a unique class so we can activate it alone in Webapp.js
        addthisElement.addClass("addthis_toolbox_route");
        addthisElement.appendTo("#share-route");
        addthisElement.attr("addthis:title", "Check out my trip planned on "+otp.config.siteName);
        addthisElement.attr("addthis:description", otp.config.siteDescription);    
    }
});
