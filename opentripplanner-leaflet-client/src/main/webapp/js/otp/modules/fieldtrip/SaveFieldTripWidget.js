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

otp.modules.fieldtrip.SaveFieldTripWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,

    initialize : function(id, module) {
        otp.widgets.Widget.prototype.initialize.call(this, id, module.webapp.widgetManager);
        var this_ = this;

        this.$().addClass('otp-saveFieldTripWidget');
        this.addHeader("Save Field Trip");
        
        $('<div class="notDraggable">Description:<br><textarea id="'+this.id+'-desc" style="width: 250px; height: 100px;"/></div>').appendTo(this.$());
        
        var buttonRow = $('<div class="otp-fieldTrip-buttonRow" />').appendTo(this.$());
        
        $('<button id="'+this.id+'-saveButton">Save</button>').button()
        .appendTo(buttonRow).click(function() {
            //console.log("val: " +$('textarea#'+this_.id+'-desc').val());
            module.saveTrip($('textarea#'+this_.id+'-desc').val());
            this_.hide();
        });
        $('<button id="'+this.id+'-cancelButton">Cancel</button>').button()
        .appendTo(buttonRow).click(function() {
            this_.hide();
        });
                
        this.center();
    }
});
