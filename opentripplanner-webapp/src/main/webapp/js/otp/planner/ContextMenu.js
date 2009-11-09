otp.namespace("otp.planner");

/**
 * @class
 */
otp.planner.ContextMenu = {
    map      : null,
    forms    : null,
    elements : null,
    renderTo : null,

    /** */
    constructor: function(config)
    {
        console.log("enter ui.ContextMenu constructor");
        otp.configure(this, config);

        try
        {
            if(this.renderTo == null)
                 this.renderTo = Ext.get(otp.util.OpenLayersUtils.MAP_PANEL);

            var opts = {
                renderTo: this.renderTo,
                items:    this.getElements()
            };
    
            this.renderTo.on('contextmenu', function (event){ 
                this.showAt(event.xy);
                event.stopEvent();
            }, this);
            this.renderTo.on('click', function (event) {
                this.hide();
            }, this);
            this.renderTo.on('blur', function (event) {
                this.hide();
            }, this);

            Ext.menu.Menu.call(this, opts);
        }
        catch(e)
        {
            console.log("otp.planner.ContextMenu: consturctor error " + e);
        }

        console.log("exit ui.ContextMenu constructor");
    },

    /** */
    getElements : function()
    {
        // if we configured this CM object with elements, don't create
        if(this.elements)
            return this.elements;
 
        // OK, we don't have any CM elements, so let's try to get some from the other objects
        this.elements = new Array();
        if(this.forms)
        {
            this.elements.push(this.forms.getContextMenu(this));
            this.elements.push('-');
        }
        if(this.map)
        {
            this.elements.push(this.map.getContextMenu(this));
        }

        return this.elements;
    },


    /**
     * get map coordinate lon/lat given the position of the context menu click 
     */
    getMapCoordinate : function ()
    {
        var c  = otp.util.ExtUtils.getPixelXY(this.el, this.map.el);
        var ll = otp.util.OpenLayersUtils.getLatLonOfPixel(this.map.getMap(), c.x, c.y);
        return ll;
    },

    /**
     * center the map on the point of the context menu click
     */
    centerMapFromContextMenuXY : function () 
    {
        var c = otp.util.ExtUtils.getPixelXY(this.el, this.map.el);
        this.map.centerMapAtPixel(c.x, c.y);
    },

    CLASS_NAME : "otp.planner.ContextMenu"
};

try
{
    otp.planner.ContextMenu = Ext.extend(Ext.menu.Menu, otp.planner.ContextMenu);
}
catch(e)
{
    console.log("otp.planner.ContextMenu: error creating this type...please ignore this error if you are not using Ext");
}
