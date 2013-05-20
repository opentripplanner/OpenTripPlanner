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

otp.modules.fieldtrip.FieldTripRequestsWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,

    trips : null,
    selectedTrip : null,
    
    selectedDate : null,
    
    initialize : function(id, module) {
        var this_ = this;  
        
        this.module = module;

        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            cssClass : 'otp-fieldTripRequestsWidget',
            title : "Field Trip Requests"
        });
        
        this.requestsList = $('<div class="otp-fieldTripRequests-list notDraggable" />').appendTo(this.mainDiv);
        
        var buttonRow = $('<div class="otp-fieldTripRequests-buttonRow" />').appendTo(this.mainDiv);
        
        $('<button id="'+this.id+'-planOutboundButton">Plan Outbound</button>').button()
        .appendTo(buttonRow).click(function() {
        });

        $('<button id="'+this.id+'-planInboundButton">Plan Inbound</button>').button()
        .appendTo(buttonRow).click(function() {
        });
        
        module.loadRequests();
    },
    
    updateRequests : function(requests) {
        this.requestsList.empty();
        for(var i = 0; i < requests.length; i++) {
            var req = requests[i];
            $('<div class="otp-fieldTripRequests-listRow">'+req.teacherName+", "+req.schoolName+'</div>').appendTo(this.requestsList);
        }
    }
    
});
