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

otp.namespace("otp.modules.multimodal");


otp.modules.multimodal.MultimodalPlannerModule = 
    otp.Class(otp.modules.planner.PlannerModule, {

    moduleName  : "Multimodal Trip Planner",
    moduleId    : "multimodal",
    
    itinWidget  : null,
    
    showIntermediateStops : false,
    
    stopsWidget: false,
    
    routeData : null,
    
    initialize : function(webapp) {
        otp.modules.planner.PlannerModule.prototype.initialize.apply(this, arguments);
    },

    activate : function() {
        if(this.activated) return;
        otp.modules.planner.PlannerModule.prototype.activate.apply(this);

        // setup options widget
        
        this.optionsWidget = new otp.widgets.TripWidget('otp-'+this.moduleId+'-optionsWidget', this);
        this.optionsWidget.$().resizable();
        this.addWidget(this.optionsWidget);
        
        this.optionsWidget.minimizable = true;
        this.optionsWidget.addHeader("Trip Options");
        
        if(this.webapp.geocoders && this.webapp.geocoders.length > 0) {
            this.optionsWidget.addControl("locations", new otp.widgets.TW_LocationsSelector(this.optionsWidget, this.webapp.geocoders), true);
            this.optionsWidget.addVerticalSpace(12, true);
        }
                
        this.optionsWidget.addControl("time", new otp.widgets.TW_TimeSelector(this.optionsWidget), true);
        this.optionsWidget.addVerticalSpace(12, true);
        
        
        var modeSelector = new otp.widgets.TW_ModeSelector(this.optionsWidget);
        this.optionsWidget.addControl("mode", modeSelector, true);

        modeSelector.addModeControl(new otp.widgets.TW_MaxWalkSelector(this.optionsWidget));
        modeSelector.addModeControl(new otp.widgets.TW_BikeTriangle(this.optionsWidget));
        modeSelector.addModeControl(new otp.widgets.TW_PreferredRoutes(this.optionsWidget));

        modeSelector.refreshModeControls();

        this.optionsWidget.addSeparator();
        this.optionsWidget.addControl("submit", new otp.widgets.TW_Submit(this.optionsWidget));
        
    },
    
    getExtendedQueryParams : function() {
        return { showIntermediateStops : this.showIntermediateStops };
    },
            
    processPlan : function(tripPlan, restoring) {
        if(this.itinWidget == null) {
            this.itinWidget = new otp.widgets.ItinerariesWidget(this.moduleId+"-itinWidget", this);
            this.widgets.push(this.itinWidget);
        }
        if(restoring && this.restoredItinIndex) {
            this.itinWidget.updateItineraries(tripPlan.itineraries, tripPlan.queryParams, this.restoredItinIndex);
            this.restoredItinIndex = null;
        } else  {
            this.itinWidget.updateItineraries(tripPlan.itineraries, tripPlan.queryParams);
        }
        this.itinWidget.show();
        
        if(restoring) {
            this.optionsWidget.restorePlan(tripPlan);
        }
        this.drawItinerary(tripPlan.itineraries[0]);
    },
    
    CLASS_NAME : "otp.modules.multimodal.MultimodalPlannerModule"
});
