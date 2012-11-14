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
    
    initialize : function(webapp) {
        otp.modules.planner.PlannerModule.prototype.initialize.apply(this, arguments);
    },
    
    processPlan : function(tripPlan, queryParams, restoring) {
        if(this.itinWidget == null) {
            this.itinWidget = new otp.widgets.ItinerariesWidget(this.moduleId+"itinWidget");
            this.widgets.push(this.itinWidget);
        }
        this.itinWidget.updateItineraries(tripPlan.itineraries);
        this.itinWidget.show();
    },
        
    CLASS_NAME : "otp.modules.multimodal.MultimodalPlannerModule"
});
