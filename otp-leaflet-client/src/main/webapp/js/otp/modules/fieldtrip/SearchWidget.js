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


otp.modules.fieldtrip.SearchWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,
    
    initialize : function(id, module) {
        this.module = module;

        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            cssClass : 'otp-fieldtrip-searchWidget',
            title : "Search Field Trips",
            resizable: true,
            closeable : true,
            persistOnClose : true,
            openInitially : false,            
        });

        ich['otp-fieldtrip-searchWidget']({
            widgetId : this.id
        }).appendTo(this.mainDiv);
        this.center();

        this.mainDiv.find(".searchTabs").tabs({
            heightStyle : "fill",
        });
        
        $('#' + this.id + '-searchButton').button().click($.proxy(this.runSearch, this));        

        this.mainDiv.find(".dateInput1").datepicker().datepicker("setDate", new Date());
        this.mainDiv.find(".dateInput2").datepicker().datepicker("setDate", new Date());
        this.mainDiv.find(".extendedDateInput").hide();

        this.mainDiv.find(".dateOperatorSelect").change(_.bind(function() {
            if(this.mainDiv.find(".dateOperatorSelect").val() === 'between') {
                this.mainDiv.find(".extendedDateInput").show();
            }
            else {
                this.mainDiv.find(".extendedDateInput").hide();
            }

        }, this));
    },
    
    runSearch : function() {
        
        var queryStr = "";

        queryStr += "teacherName " + this.mainDiv.find(".teacherOperatorSelect").val() + " ?";
        queryStr += " " + this.mainDiv.find('input:radio[name=andor1]:checked').val() + " ";
        queryStr += "schoolName " + this.mainDiv.find(".schoolOperatorSelect").val() + " ?";
        queryStr += " " + this.mainDiv.find('input:radio[name=andor2]:checked').val() + " ";
        queryStr += "travelDate " + this.mainDiv.find(".dateOperatorSelect").val() + " ?";
        if(this.mainDiv.find(".dateOperatorSelect").val() === 'between') {
            queryStr += " and ?";
        }

        var params = {
            sessionId : this.module.sessionManager.sessionId,
            query : queryStr,
            teacherValue : this.mainDiv.find(".teacherInput").val(),
            schoolValue : this.mainDiv.find(".schoolInput").val(),
            date1 : this.mainDiv.find(".dateInput1").val()
        };
        if(this.mainDiv.find(".dateOperatorSelect").val() === 'between') {
            params.date2 = this.mainDiv.find(".dateInput2").val();
        }

        this.mainDiv.find(".searchTabs").tabs("select", 1);
        this.mainDiv.find(".resultsTab").html("Searching...")

        var this_ = this;

        $.ajax(this.module.datastoreUrl+'/fieldtrip/searchRequests', {
            type: 'GET',
            
            data: params,
            
            success: function(data) {
                this_.showResults(data);
            },
            
            error: function(data) {
                this.mainDiv.find(".resultsTab").html("Error Completing Search");
                console.log("error searching");
                console.log(data);
            }
        });

    },

    showResults : function(requests) {

        var filteredRequests = [];

        if(this.mainDiv.find('input:radio[name=plannedOnly]:checked').val() === "true") {
            _.each(requests, function(req) {
                if(otp.util.FieldTrip.getOutboundTrip(req) && otp.util.FieldTrip.getInboundTrip(req)) {
                    filteredRequests.push(req);
                }
            });
        }
        else filteredRequests = requests;

        var tabDiv = this.mainDiv.find(".resultsArea");
        if(filteredRequests.length > 0) {
            tabDiv.html("Found " + filteredRequests.length + " results:");
        }
        else {
            tabDiv.html("<i>No results found matching the search criteria</i>");
            return;            
        }

        _.each(filteredRequests, function(req) {

            var row = ich['otp-fieldtrip-requestRow'](otp.util.FieldTrip.getRequestContext(req));
            row.appendTo(tabDiv);
            var this_ = this;
            row.data('req', req).click(function() {
                var req = $(this).data('req');
                this_.module.showRequest(req);
            });

        }, this);
    }
});
