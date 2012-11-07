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

otp.widgets.TripSummaryWidget = 
    otp.Class(otp.widgets.Widget, {
    
    bikeTriangle    : null,
        
    initialize : function(id, planTripCallback) {
        
        otp.configure(this, id);
        otp.widgets.Widget.prototype.initialize.apply(this, arguments);
        
        var content = '';
        content += '<h3 class="your-trip">Your Trip:</h3>';
        /*content += '<ul class="otp-stats">';
        content += '<li><strong>Distance Traveled:</strong> <span id="otp-tsw-distance"></span></li>';
        content += '<li><strong>Estimated Time:</strong> <span id="otp-tsw-duration"></span></li>';
        // content += '<li><strong>Calories Burned:</strong> N/A</li>';
        // content += '<li><strong>Cost:</strong> N/A</li>';
        content += '</ul>';
        content += '<span id="otp-tsw-timeSummary"></span>';*/

        content += '<div id="otp-tsw-distance"></div>';
        content += '<div id="otp-tsw-duration"></div>';
        content += '<div id="otp-tsw-timeSummary"></div>';


        content += '<hr />';
        content += '<h6 class="drag-to-change">Drag to Change Trip:</h6>';
        content += '<div id="otp-tsw-bikeTriangle"></div>';

        content += '<div id="otp-tsw-bikeTypeRow">Use: ';
        content += '<input id="myOwnBikeRBtn" type="radio" name="bikeType" value="my_bike" checked> My Own Bike&nbsp;&nbsp;';
        content += '<input id="sharedBikeRBtn" type="radio" name="bikeType" value="shared_bike"> A Shared Bike';
        content += '</div>';
        

        if(otp.config.showAddThis) {
            content += '<hr />';
            content += '<h6 id="share-route-header">Share this Trip:</h6>';
            content += '<div id="share-route"></div>';

            this.setContent(content);
                
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
        else {
            this.setContent(content);        
        }
        

        this.bikeTriangle = new otp.widgets.BikeTrianglePanel('otp-tsw-bikeTriangle');
        this.bikeTriangle.onChanged = planTripCallback;
        
        document.getElementById('myOwnBikeRBtn').onclick = planTripCallback;
        document.getElementById('sharedBikeRBtn').onclick = planTripCallback;
    },

    restorePlan : function(data) {
	
	// looks like we're receiving errant restorPlan calls with no data -- catching and discarding
	if(data == null)
		return;

        if(data.mode === "BICYCLE") {
            $('#myOwnBikeRBtn').attr('checked', 'checked');
        }
        if(data.mode === "WALK,BICYCLE") {
            $('#sharedBikeRBtn').attr('checked', 'checked');
        }
        
        if(data.optimize === 'TRIANGLE') {
            this.bikeTriangle.setValues(data.triangleTimeFactor, data.triangleSlopeFactor, data.triangleSafetyFactor);
        }
    },
    
    updateMetrics : function(itin) {
    	
    	
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
    },
    
    CLASS_NAME : "otp.widgets.TripSummaryWidget"
});

