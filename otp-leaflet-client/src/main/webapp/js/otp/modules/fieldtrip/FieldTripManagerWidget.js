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

otp.modules.fieldtrip.FieldTripManagerWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,
    
    initialize : function(id, module) {
        var this_ = this;  
        
        this.module = module;

        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            cssClass : 'otp-fieldTripManager',
            title : 'Field Trip Requests',
            resizable: true,
        });
    
        ich['otp-fieldtrip-manager']({ widgetId : this.id }).appendTo(this.mainDiv);
        $('#'+this.id+'-tabs').tabs({
            heightStyle : "fill",
        });

        this.newRequestsList = this.mainDiv.find('.newRequestsList');
        this.plannedRequestsList = this.mainDiv.find('.plannedRequestsList');
        this.cancelledRequestsList = this.mainDiv.find('.cancelledRequestsList');
        this.pastTripsList = this.mainDiv.find('.pastTripsList');
        
        this.mainDiv.find('.refreshButton').button().click(function() {
            this_.module.loadRequests();
        });

        var date = new Date();
        date.setDate(date.getDate() + 1);
        this.mainDiv.find('.reportDateInput').datepicker().datepicker("setDate", date);

        this.mainDiv.find('.viewReportButton').button().click(function() {
            var m = moment(this_.mainDiv.find('.reportDateInput').val());
            window.open(otp.config.datastoreUrl + "/fieldtrip/opsReport?month=" + (m.month()+1) + "&day=" + m.date() + "&year=" + m.year());
        });
        
        this.mainDiv.resizable({
            alsoResize: '#' + this.id + ' .ui-tabs-panel',
            minWidth: 400,
            minHeight: 200,
        });

        this.module.loadRequests();

        //this.buildTripManager($('#'+this.id+'-tripsTab'));
    },

    /** requests viewer **/
     
    buildRequestsViewer : function(container) {
        this.requestsList = $('<div class="otp-fieldTripRequests-list notDraggable" />').appendTo(container);
        
        this.module.loadRequests();
    },
    
    updateRequests : function(requests) {
        var this_ = this;
        
        this.newRequestsList.empty();
        this.plannedRequestsList.empty();
        this.cancelledRequestsList.empty();
        this.pastTripsList.empty();
        
        for(var i = 0; i < requests.length; i++) {
            var req = requests[i];
            //console.log(req);
            //$('<div class="otp-fieldTripRequests-listRow">'+req.teacherName+", "+req.schoolName+'</div>').appendTo(this.requestsList);
            
            var row = ich['otp-fieldtrip-requestRow'](otp.util.FieldTrip.getRequestContext(req));
            
            if(req.status === "cancelled") {
                row.appendTo(this.cancelledRequestsList);
            }

            else { // not a cancelled request
                if(req.travelDate && moment(req.travelDate).diff(moment(), 'days')  < 0) { // past trip
                    row.appendTo(this.pastTripsList);                        
                }
                else { // 'new' or 'planned' active request
                    if(otp.util.FieldTrip.getOutboundTrip(req) && 
                       otp.util.FieldTrip.getInboundTrip(req)) row.appendTo(this.plannedRequestsList);
                    else row.appendTo(this.newRequestsList);
                }

                // make the the request clickable
                row.data('req', req).click(function() {
                    var req = $(this).data('req');
                    this_.module.showRequest(req);
                });
            }            
        }
    },

     
    /** trip manager **/

    trips : null,
    selectedTrip : null,
    
    selectedDate : null,
    
    buildTripManager : function(container) {    
        // TODO : refactor
        var tripListContainer = $('<div class="otp-fieldTripManager-callListContainer"></div>').appendTo(container);
        
        var selectRow = $('<div class="notDraggable otp-fieldTripManager-callListHeader"></div>').appendTo(tripListContainer);
        $('<input type="radio" name="'+this.id+'+-selectGroup" checked />').appendTo(selectRow)
        .click(function() {
            console.log("all");
            this_.selectedDate = null;
            this_.module.refreshTrips();
        });
        $('<span>&nbsp;Show all trips&nbsp;&nbsp;<br></span>').appendTo(selectRow);
        this.dateRadio = $('<input type="radio" name="'+this.id+'+-selectGroup" />').appendTo(selectRow)
        .click(function() {
            console.log("date");
            this_.selectedDate = this_.datePicker.val();
            this_.module.refreshTrips();
        });
        $('<span>&nbsp;Show trips on:&nbsp;</span>').appendTo(selectRow);
        this.datePicker = $('<input type="text" style="font-size:11px; width: 60px;" />').datepicker({
            onSelect: function(date) {
                console.log(date);
                this_.selectedDate = date;
                this_.dateRadio.prop('checked',true);
                this_.module.refreshTrips();
            }
        }).appendTo(selectRow);
        this.datePicker.datepicker("setDate", new Date());
        
        this.tripList = $('<div class="otp-fieldTripManager-callList"></div>').appendTo(tripListContainer);

        var tripInfoContainer = $('<div class="notDraggable" style="height:210px;"></div>').appendTo(container);
        this.tripInfo = $('<div class="otp-fieldTripManager-callInfo notDraggable"></div>').appendTo(tripInfoContainer);
        var tripButtonRow = $('<div style="margin-top: 4px; text-align: center;" />').appendTo(tripInfoContainer);
        
        var mainButtonRow = $('<div class="otp-fieldTrip-buttonRow" />').appendTo(container);
        
        
        $('<button id="'+this.id+'-saveButton">View Requests</button>').button()
        .appendTo(mainButtonRow).click(function() {
            this_.module.showRequests();
        });        
        $('<button id="'+this.id+'-saveButton">Save Current Planned Trip</button>').button()
        .appendTo(mainButtonRow).click(function() {
            this_.module.showSaveWidget();
        });
        
        $('<button id="'+this.id+'-deleteButton">Delete</button>').button()
        .appendTo(tripButtonRow).click(function() {
            this_.deleteSelectedTrip();
        });
        $('<button id="'+this.id+'-renderButton">Render</button>').button()
        .appendTo(tripButtonRow).click(function() {
            if(this_.selectedTrip !== null) this_.module.renderTrip(this_.selectedTrip);
        });
    },
    
    updateTrips : function(trips) {
        var this_ = this;
        this.trips = trips;
        this.tripList.empty();
        for(var i=0; i < trips.length; i++) {
            var trip = trips[i];
            $('<div class="otp-fieldTripManager-tripRow">'+trip.description+', '+trip.serviceDay+'</div>')
            .prependTo(this.tripList)
            .data('trip', trip)
            .click(function() {
                this_.selectedTrip =  $(this).data('trip');
                this_.showTripDetails(this_.selectedTrip);
            });
        }
    },
    
    showTripDetails : function(trip) {
        var html = "<h3>Trip Details</h3>"
        html += '<div class="otp-fieldTripManager-tripDetail"><b>Description</b>: '+trip.description+"</div>";
        html += '<div class="otp-fieldTripManager-tripDetail"><b>Origin</b>: '+trip.origin+"</div>";
        html += '<div class="otp-fieldTripManager-tripDetail"><b>Destination</b>: '+trip.destination+"</div>";
        html += '<div class="otp-fieldTripManager-tripDetail"><b>Day of Travel</b>: '+trip.serviceDay+"</div>";
        html += '<div class="otp-fieldTripManager-tripDetail"><b>Time of Travel</b>: '+trip.departure+"</div>";
        html += '<div class="otp-fieldTripManager-tripDetail"><b>Group Size</b>: '+trip.passengers+"</div>";
        html += '<div class="otp-fieldTripManager-tripDetail"><b>Created by</b>: '+trip.createdBy+"</div>";
        html += '<div class="otp-fieldTripManager-tripDetail"><b>Created</b>: '+trip.timeStamp+"</div>";
        this.tripInfo.html(html);
    },
    
    deleteSelectedTrip : function() {
        if(this.selectedTrip == null) {
            return;
        }
        
        this.module.deleteTrip(this.selectedTrip);        
    }
});
