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

otp.modules.fieldtrip.FieldTripRequestWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,
        
    initialize : function(id, module, request) {
        var this_ = this;  
        
        this.module = module;
        this.request = request;    
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            cssClass : 'otp-fieldTrip-requestWidget',
            title : "Field Trip Request #"+request.id,
            resizable : true,
            closeable : true,
        });
        
        //this.contentDiv  = $('<div class="otp-fieldTrip-requestWidget-content notDraggable" />').appendTo(this.mainDiv);
        this.render();
        this.center();
    },
    
    render : function() {
        var this_ = this;
        var context = _.clone(this.request);
        context.widgetId = this.id;
        context.dsUrl = otp.config.datastoreUrl;
        if(this.request.outboundTrip) context.outboundPlanInfo = this.module.constructPlanInfo(this.request.outboundTrip);
        if(this.request.inboundTrip) context.inboundPlanInfo = this.module.constructPlanInfo(this.request.inboundTrip);
        //this.contentDiv.empty().append(ich['otp-fieldtrip-request'](context));
        if(this.content) this.content.remove();
        this.content = ich['otp-fieldtrip-request'](context).appendTo(this.mainDiv);
        
        if(this.request.outboundTrip) {
            this.content.find('.outboundPlanInfo').css('cursor', 'pointer').click(function() {
                this_.module.renderTrip(this_.request.outboundTrip);
            });
        }
        
        if(this.request.inboundTrip) {
            this.content.find('.inboundPlanInfo').css('cursor', 'pointer').click(function() {
                this_.module.renderTrip(this_.request.inboundTrip);
            });
        }
                 
        //$('#' + this.id + '-outboundPlanButton').click(function(evt) {
        this.content.find('.outboundPlanButton').click(function(evt) {
            this_.module.planOutbound(this_.request);
        });
        
        this.content.find('.outboundSaveButton').click(function(evt) {
            this_.module.saveRequestTrip(this_.request, "outbound");
        });
        
        this.content.find('.inboundPlanButton').click(function(evt) {
            
            this_.module.planInbound(this_.request);
        });

        this.content.find('.inboundSaveButton').click(function(evt) {
            this_.module.saveRequestTrip(this_.request, "inbound");
        });
        
        this.content.find('.printablePlanLink').click(function(evt) {
            evt.preventDefault();
            var req = this_.request;
            var printWindow = window.open('','Group Plan','toolbar=yes, scrollbars=yes, height=500, width=800');
            var context = _.clone(req);
            var outboundItinIndex = 0; inboundItinIndex = 0;
            context["outboundItinIndex"] = function() {
                return outboundItinIndex++;
            };
            context["inboundItinIndex"] = function() {
                return inboundItinIndex++;
            };
            
            var content = ich['otp-fieldtrip-printablePlan'](context);
            
            // populate itin details
            if(req.outboundTrip) {
                var itins = req.outboundTrip.groupItineraries;
                for(var i = 0; i < itins.length; i++) {
                    var itinData = JSON.parse(otp.util.Text.lzwDecode(itins[i].itinData));
                    var itin = new otp.modules.planner.Itinerary(itinData, null);
                    content.find('.outbound-itinBody-'+i).html(itin.getHtmlNarrative());
                }
            }
            if(req.inboundTrip) {
                var itins = req.inboundTrip.groupItineraries;
                for(var i = 0; i < itins.length; i++) {
                    var itinData = JSON.parse(otp.util.Text.lzwDecode(itins[i].itinData));
                    var itin = new otp.modules.planner.Itinerary(itinData, null);
                    content.find('.inbound-itinBody-'+i).html(itin.getHtmlNarrative());
                }
            }

            var html = "";
            html += '<link rel="stylesheet" href="js/otp/modules/planner/planner-style.css" />';
            html += '<link rel="stylesheet" href="js/otp/modules/fieldtrip/fieldtrip-style.css" />';
            
            html += content.html();
            printWindow.document.write(html);
        });
    },
    
    onClose : function() {
        delete this.module.requestWidgets[this.request.id];
    },
    
    tripPlanned : function() {
        $('#' + this.id + '-outboundSaveButton').removeAttr("disabled");
        $('#' + this.id + '-inboundSaveButton').removeAttr("disabled");
    }
    
});
