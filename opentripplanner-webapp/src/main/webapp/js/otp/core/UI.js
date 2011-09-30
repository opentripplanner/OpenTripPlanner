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

/**
 * sets up a Extjs UI
 * @class 
 */
otp.core.UI = {

    locale            : otp.locale.English,

    // panels & viewport
    north         : null,
    south         : null,
    east          : null,
    west          : null,
    center        : null,
    accordion     : null,

    // these inner panels are part of the inner border layout (within the center layout)
    // see getSubPanels() 
    innerCenter   : null,
    innerSouth    : null,
    innerEast     : null,

    viewport      : null,
    map           : null,

    // default config options
    alwaysUseDefaultNorthWestCenter : false,
    centerTitle   : '',

    /**
     * @constructor 
     */
    initialize : function(config)
    {
        otp.configure(this, config);

        this.viewport = new Ext.Viewport({
          layout:'border',
          deferredRender:false, 
          items:  this._getSubPanels()
        });

        // if we don't have specific south & east, assign the inners to these vars 
        // NOTE: do this here, vs within _getSubPanels()
        if(this.south == null) this.south = this.innerSouth;
        if(this.east  == null) this.east  = this.innerEast;
    },


    /** 
     * get the panels as defined in the constructor config
     * if no panels supplied by the constructor, we'll create a north / west / center ui
     * @private
     */
    _getSubPanels : function()
    {
        var retVal = [];
        
        // no panels defined yet, so create default 3 panel
        if(this.alwaysUseDefaultNorthWestCenter || (this.north == null && this.south == null && this.east == null && this.west == null && this.center == null))
        {
            // default inner - center panel connfig
            var innerCtr = {
                    id:     'center-inner',
                    region: 'center',
                    layout: 'fit',
                    html:     'this is the (inner) center panel'
            };

            // if we have a map attached here, we'll use the GeoExt panel
            if(this.map)
            {
                innerCtr = new GeoExt.MapPanel({
                    id        : otp.util.OpenLayersUtils.MAP_PANEL,
                    region    : 'center',
                    layout    : 'fit',
                    stateful  : false,
                    map       : this.map.getMap(),
                    zoom      : this.map.getMap().getZoom(),
                    bodyStyle : 'background-color:#F7F7F2'
                });
            }


            // this config creates an 'inner' boarder layout, with south and east panels into the main panel
            var centerConfig = {
                title:         this.centerTitle,
                region:        'center',
                id:            'center',
                layout:        'border',
                margins:        '1 0 0 0',
                hideMode:       'offsets',
                items:[
                  innerCtr,
                  {
                    hidden:  true,
                    id:      'south',
                    region:  'south',
                    html:    'this is the (inner) south panel',
                    layout:  'fit',
                    style: {
                      overflow: 'auto'      //otherwise IE won't scroll the elevation plot
                    },
                    height:  140,
                    border:  false,
                    split:   true,
                    useSplitTips:  true,
                    collapseMode: 'mini'
                  }
                  ,
                  {
                    hidden:   true,
                    id:       'east',
                    region:   'east',
                    html:     'this is the (inner) east panel',
                    layout:   'fit',
                    border:   false,
                    width:    250,
                    split:    true,
                    useSplitTips: true,
                    collapseMode: 'mini'
                  }
                ]
            }

            this.center = new Ext.Panel(centerConfig);
            this.innerCenter = this.center.getComponent(0);
            this.innerSouth  = this.center.getComponent(1);
            this.innerEast   = this.center.getComponent(2);


            this.west   = new Ext.Panel({
                layout:       'accordion',
                region:       'west',
                id:           'west-panel',
                header:       false,
                width:        360,
                minSize:      150,
                maxSize:      450,
                margins:      '30 0 1 1',
                split:        true,
                useSplitTips: true,
                collapsible:  true,
                collapseMode: 'mini',
                collapsible:   true,
                layoutConfig:{
                    animate:true,
                    collapseFirst: true
                }
            });
            this.accordion = this.west;
        }

        if(this.south)  retVal.push(this.south);
        if(this.east)   retVal.push(this.east);
        if(this.west)   retVal.push(this.west);
        if(this.center) retVal.push(this.center);
        if(this.north)  retVal.push(this.north);

        return retVal;
    },

    /** */
    doLayout : function()
    {
        this.viewport.doLayout();
    },

    /**
     * close any/all panels except for the main panel
     * @param {Object} doFull
     */
    fullScreen : function(doFull)
    {
        if(doFull)
        {
            if(this.south)                                       this.south.collapse();
            if(this.innerSouth && this.south != this.innerSouth) this.innerSouth.collapse();
            if(this.east)                                        this.east.collapse();
            if(this.innerEast && this.innerEast != this.east)    this.innerEast.collapse();
            if(this.west)                                        this.west.collapse();
            this.isFullScreen = true;
        }
        else
        {
            if(this.south)                                       this.south.expand();
            if(this.innerSouth && this.south != this.innerSouth) this.innerSouth.expand();
            if(this.east)                                        this.east.expand();
            if(this.innerEast && this.innerEast != this.east)    this.innerEast.expand();
            if(this.west)                                        this.west.expand();
            this.isFullScreen = false;
        }
        this.doLayout();
    },


    /**
     * UI clear method
     */
    clear : function()
    {
        this.doLayout();
    },

    CLASS_NAME : "otp.core.UI"
}

otp.core.UI = OpenLayers.Class(otp.core.UI);
