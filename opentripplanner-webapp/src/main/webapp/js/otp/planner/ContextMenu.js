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

otp.namespace("otp.planner");

/**
 * @class
 */
otp.planner.ContextMenu = {
    map      : null,
    forms    : null,
    elements : null,
    renderTo : null,
    clickX   : null, // Store the pixel *on the map* the user clicked on
    clickY   : null,

    /** */
    constructor : function(config) {
        otp.configure(this, config);

        if (this.renderTo == null) {
            this.renderTo = Ext.get(otp.util.OpenLayersUtils.MAP_PANEL);
        }

        var opts = {
            renderTo : this.renderTo,
            items : this.getElements(),
            enableScrolling: false
        };

        this.renderTo.on('contextmenu', function(event) {
            var x = event.xy[0];
            var y = event.xy[1];
            this.clickX = x - this.container.getX();
            this.clickY = y - this.container.getY();
                            
            // Place the context menu above the cursor if placing it below the
            // cursor would cause it to go out of view.
            if (y + this.getHeight() > this.container.getHeight() + this.container.getY()) {
                y = y - this.getHeight();
            }
            if (x + this.getWidth() > this.container.getWidth() + this.container.getX()) {
                x = x - this.getWidth();
            }
            this.showAt([x, y]);
            event.stopEvent();
        }, this);
        this.renderTo.on('click', function(event) {
            this.hide();
        }, this);
        this.renderTo.on('blur', function(event) {
            this.hide();
        }, this);

        Ext.menu.Menu.call(this, opts);
    },

    /** */
    getElements : function()
    {
        // if we configured this CM object with elements, don't create
        if(this.elements)
            return this.elements;
 
        // OK, we don't have any CM elements, so let's try to get some from the other objects
        this.elements = new Array();
        var fcm = false;
        if(this.forms)
        {
            this.elements.push(this.forms.getContextMenu(this));
            fcm = true;
        }
        if(this.map)
        {
            var mcm = this.map.getContextMenu(this);
            if(mcm != null && mcm.length > 0)
            {
                // add a separator between formCM and mapCM
                if(fcm)
                    this.elements.push('-');

                this.elements.push(mcm);
            }
        }

        return this.elements;
    },
    

    /**
     * get map coordinate lat/lon given the position of the context menu click 
     */
    getMapCoordinate : function () {

        // Use actual click location rather than CM loc because the two aren't
        // neceesarily the same (e.g., when click is near the window border)
        return otp.util.OpenLayersUtils.getLatLonOfPixel(this.map.getMap(), this.clickX, this.clickY);
    },

    /**
     * get map coordinate lat/lon given the position of the context menu click 
     */
    getYOffsetMapCoordinate : function () {
        // Use actual click location rather than CM loc because the two aren't
        // neceesarily the same (e.g., when click is near the window border)
        return otp.util.OpenLayersUtils.getLatLonOfPixel(this.map.getMap(), this.clickX, this.clickY - otp.util.OpenLayersUtils.yOffsetToTip);
    },

    /**
     * center the map on the point of the context menu click
     */
    centerMapFromContextMenuXY : function () 
    {
        this.map.centerMapAtPixel(this.clickX, this.clickY);
    },

    CLASS_NAME : "otp.planner.ContextMenu"
};

otp.planner.ContextMenu = Ext.extend(Ext.menu.Menu, otp.planner.ContextMenu);