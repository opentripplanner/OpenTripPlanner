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

otp.namespace("otp.modules.fieldtrip");


otp.modules.fieldtrip.FieldTripModule = 
    otp.Class(otp.modules.planner.PlannerModule, {

    moduleName  : "Field Trip Planner",
    moduleId    : "fieldtrip",
    
    itinWidget  : null,
    
    groupSize   : 100,
    bannedSegments : null,
    itineraries : null,
    
    showIntermediateStops : true,
    
    stopsWidget: false,
    
    initialize : function(webapp) {
        otp.modules.planner.PlannerModule.prototype.initialize.apply(this, arguments);
    },

    activate : function() {
        if(this.activated) return;
        otp.modules.planner.PlannerModule.prototype.activate.apply(this);

        this.optionsWidget = new otp.widgets.TripWidget('otp-'+this.moduleId+'-optionsWidget', this);
        this.optionsWidget.$().resizable();
        this.addWidget(this.optionsWidget);
        
        this.optionsWidget.minimizable = true;
        this.optionsWidget.addHeader("Trip Options");
        
        this.optionsWidget.addControl("time", new otp.widgets.TW_TimeSelector(this.optionsWidget), true);
        this.optionsWidget.addVerticalSpace(12, true);
        
        
        var modeSelector = new otp.widgets.TW_ModeSelector(this.optionsWidget);
        this.optionsWidget.addControl("mode", modeSelector, true);

        modeSelector.addModeControl(new otp.widgets.TW_MaxWalkSelector(this.optionsWidget));
        modeSelector.addModeControl(new otp.widgets.TW_GroupTripOptions(this.optionsWidget, "Number of Students: "));
        //modeSelector.addModeControl(new otp.widgets.TW_BikeTriangle(this.optionsWidget));
        //modeSelector.addModeControl(new otp.widgets.TW_PreferredRoutes(this.optionsWidget));

        modeSelector.refreshModeControls();

        this.optionsWidget.addSeparator();
        this.optionsWidget.addControl("submit", new otp.widgets.TW_Submit(this.optionsWidget));

    },
    
    getExtendedQueryParams : function() {
        return { showIntermediateStops : this.showIntermediateStops };
    },
    
    userPlanTripStart : function() {
        console.log("uPTS");
        this.currentGroupSize = this.groupSize;
        this.bannedSegments = [];
        this.itineraries = [];
    },
    
    processPlan : function(tripPlan, restoring) {
        if(this.itinWidget == null) {
            this.itinWidget = new otp.widgets.ItinerariesWidget(this.moduleId+"-itinWidget", this);
            this.widgets.push(this.itinWidget);
        }
        /*if(restoring && this.restoredItinIndex) {
            this.itinWidget.updateItineraries(tripPlan, this.restoredItinIndex);
            this.restoredItinIndex = null;
        } else  {
            this.itinWidget.updateItineraries(tripPlan);
        }*/
        //this.itinWidget.updateItineraries(tripPlan);
        //this.itinWidget.show();
        
        /*if(restoring) {
            this.optionsWidget.restorePlan(tripPlan);
        }*/
        //this.drawItinerary(tripPlan.itineraries[0]);
        
        //var itin = new otp.modules.planner.Itinerary(tripPlan.itineraries[0], tripPlan);
        var itin = tripPlan.itineraries[0];
        var capacity = itin.getGroupTripCapacity();
        console.log("cur grp size:"+this.currentGroupSize+", cap="+capacity);
        
        this.itineraries.push(itin);
        
        var segments = itin.getTripSegments();
        this.bannedSegments = this.bannedSegments.concat(segments);
        console.log(this.bannedSegments.join(','));
        this.bannedTrips = this.bannedSegments.join(',');     
        
        
        if(this.currentGroupSize > capacity) {
            this.currentGroupSize -= capacity;
            itin.groupSize = capacity;
            console.log("remaining: "+this.currentGroupSize);
            this.planTrip();
        }
        else {
            console.log("done!");
            itin.groupSize = this.currentGroupSize;
            this.drawItinerary(this.itineraries[0]);
            this.itinWidget.updateItineraries(this.itineraries, tripPlan.queryParams);
            this.itinWidget.show();
            this.itinWidget.bringToFront();
        }
        
    },
    
});
