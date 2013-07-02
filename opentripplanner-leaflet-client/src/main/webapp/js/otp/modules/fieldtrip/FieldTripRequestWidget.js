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
        
        this.contentDiv  = $('<div class="otp-fieldTrip-requestWidget-content notDraggable" />').appendTo(this.mainDiv);
        this.render();
        this.center();
    },
    
    render : function() {
        var this_ = this;
        var context = _.clone(this.request);
        context.widgetId = this.id;
        this.contentDiv.empty().append(ich['otp-fieldtrip-request'](context));
        
        $('#' + this.id + '-outboundPlanButton').click(function(evt) {
            this_.module.planOutbound(this_.request);
        });

        $('#' + this.id + '-outboundSaveButton').click(function(evt) {
            this_.module.saveRequestTrip(this_.request, "outbound");
        });
        
        $('#' + this.id + '-inboundPlanButton').click(function(evt) {
            this_.module.planInbound(this_.request);
        });

        $('#' + this.id + '-inboundSaveButton').click(function(evt) {
            this_.module.saveRequestTrip(this_.request, "outbound");
        });
    },
    
    onClose : function() {
        delete this.module.requestWidgets[this.request.id];
    }
    
});
