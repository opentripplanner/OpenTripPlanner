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

otp.namespace("otp.core");


otp.core.MapContextMenu =
    otp.Class(otp.core.ContextMenu, {

    map : null,
    moduleItems : null,
    contextMenuLatLng : null,
    
    initialize : function(map, menuClicked) {
        var this_ = this;

        otp.core.ContextMenu.prototype.initialize.call(this, map.lmap, function(event) {
            this_.contextMenuLatLng = event.latlng;
        });

        this.map = map;
        this.moduleItems = $("<div />").appendTo(this.menu);

        //TRANSLATORS: Context menu
        this.addItem(_tr("Recenter Map Here"), function() {
            this_.map.lmap.panTo(this_.contextMenuLatLng);
        //TRANSLATORS: Context menu
        }).addItem(_tr("Zoom In"), function() {
            this_.map.lmap.zoomIn();
        //TRANSLATORS: Context menu
        }).addItem(_tr("Zoom Out"), function() {
            this_.map.lmap.zoomOut();
        });
        
        
    },

    getOffset : function(event) {
        return { 
            top: event.containerPoint.y + this.map.$().offset().top,
            left: event.containerPoint.x
        };
    },
        
    addModuleItem : function(text, clickHandler) {
        var this_ = this;
        $('<div class="otp-popupMenu-item">'+text+'</div>')
        .appendTo($(this.moduleItems))
        .click(function() {
            clickHandler.call(this, this_.contextMenuLatLng);
        });
        return this; // for chaining
    },
    
    clearModuleItems : function() {
        this.moduleItems.empty();
    }
    
});
