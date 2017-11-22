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
                    if(req.inboundTripStatus && req.outboundTripStatus) {
                       row.appendTo(this.plannedRequestsList);
                    }
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
});
