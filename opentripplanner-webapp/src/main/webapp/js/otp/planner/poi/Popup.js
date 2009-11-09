otp.namespace("otp.planner.poi");

/**
 * Class: otp.planner.poi.Popup
 * Represents popup for stop information.  The only thing that makes this
 *     specific to stop information is the contentFromFeature method.  This
 *     would better be handled by a generic popup class that accepted a
 *     template.  The template would be processed with the feature as context
 *     to get popup content.
 * 
 * Inherits from: 
 *  - OpenLayers.Popup.FramedCloud
 */
otp.planner.poi.Popup = {
    
    /**
     * Property: autoSize
     * {Boolean}  Default is true.
     */
    autoSize: true,

    minSize:  null,
    size   :  null,
    maxSize:  null,
    anchor:  {},
    
    /**
     * Property: closeBox
     * {Boolean} Include an option to close popup.  Default is true.
     */
    closeBox: true,
    
    /**
     * Property: overflow
     * {String} Overflow style property to be set on popup content element.
     *     Default is 'auto'.
     */
    overflow: "auto",
    
    /**
     * Constructor: otp.planner.poi.Popup
     * Create a popup with stop information.
     *
     * Parameters:
     * feature - {OpenLayers.Feature.Vector} A feature representing a stop.
     * options - {Object} Additional popup options.
     */
    initialize: function(feature, options, content)
    {
        this.minSize  = new OpenLayers.Size(120, 70);
        this.maxSize  = new OpenLayers.Size(350, 140);
        this.anchor   =  {
                size  : new OpenLayers.Size(8, 8),
                offset: new OpenLayers.Pixel(-4, -4)
        }

        var id = feature.id + "_popup";
        var center = feature.geometry.getBounds().getCenterLonLat();
        OpenLayers.Popup.FramedCloud.prototype.initialize.apply(
            this, [id, center, this.size, content, this.anchor, this.closeBox]
        );
        this.contentDiv.style.overflow = this.overflow;
    },

    /**
     *  this doesn't work -- can't edit text 
     */
    edit: function(text, x, y, show)
    {
        if(text)
        {
            //text = otp.utils.StringUtils.clean(text);
            if(text && text.length > 0)
                this.contentHTML = text;
        }

        if(x && y) 
        {
            this.lonlat.lon = x;
            this.lonlat.lat = y;
        }

        if(show)
            this.show();
    },

    CLASS_NAME: "otp.planner.poi.Popup"
};

try
{
    otp.planner.poi.Popup = new otp.Class(OpenLayers.Popup.FramedCloud, otp.planner.poi.Popup);
}
catch(e)
{
    console.log("otp.planner.poi.Layer: error creating this type...please ignore this error if you are not using OpenLayers");
}
