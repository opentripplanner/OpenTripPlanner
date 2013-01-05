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

        otp.widgets.Widget.prototype.initialize.call(this, id, module.webapp.widgetManager);
        this.$().addClass('otp-fieldTripManager');
        //this.$().resizable();
        this.minimizable = true;
        this.addHeader("Field Trip Manager");
        
        var tripListContainer = $('<div class="otp-fieldTripManager-callListContainer"></div>').appendTo(this.$());
        
        var selectRow = $('<div class="notDraggable otp-fieldTripManager-callListHeader"></div>').appendTo(tripListContainer);
        $('<input type="radio" name="'+this.id+'+-selectGroup" checked />').appendTo(selectRow)
        .click(function() {
            console.log("all");
        });
        $('<span>&nbsp;Show all trips&nbsp;&nbsp;<br></span>').appendTo(selectRow);
        $('<input type="radio" name="'+this.id+'+-selectGroup" />').appendTo(selectRow)
        .click(function() {
            console.log("date");
        });
        $('<span>&nbsp;Show trips on:&nbsp;</span>').appendTo(selectRow);
        $('<input type="text" style="font-size:11px; width: 60px;" />').datepicker().appendTo(selectRow);
        
        this.tripList = $('<div class="otp-fieldTripManager-callList"></div>').appendTo(tripListContainer);

        this.tripInfo = $('<div class="otp-fieldTripManager-callInfo"></div>').appendTo(this.$());
        
        var buttonRow = $('<div class="otp-fieldTrip-buttonRow" />').appendTo(this.$());
        
        //$('#'+this.id+'-saveButton')
        $('<button id="'+this.id+'-saveButton">Save Current Planned Trip</button>').button()
        .appendTo(buttonRow).click(function() {
            this_.module.showSaveWidget();
        });
                
        
    }
});
