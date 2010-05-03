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

otp.namespace("otp.application");

/**
 * responsible for building and wiring together the trip planner appliction
 * will also parse any command line parameters, and execute any commands therein
 */
otp.application.Controller = {

    // creation
    map        : null,
    ui         : null,
    poi        : null,
    cm         : null,
    planner    : null,
    params     : null,

    /** */
    initialize : function(config)
    {
        this.config = config;
        if(this.config == null || this.config.map == null)
            this.config = otp.config;

        Ext.apply(this.config.map, {
            cm               : this.cm, 
            attribution      : otp.util.ExtUtils.MAP_ATTRIBUTION,
            options: {
                 controls: []
            }
        });

        this.params  = new otp.util.ParseUrlParams();
        this.map  = new otp.core.Map(this.config.map);
        this.ui   = new otp.core.UI({map:this.map});

        // create logo image, using a custom logo if specified
        var customLogo = this.config.logo;
        var logoPath = (typeof customLogo === 'string') ? customLogo : 'images/ui/logoSmall.png';
        var logoAnchorWrapper = Ext.get('logo').query('a')[0];
        Ext.DomHelper.append(logoAnchorWrapper, {tag: 'img',
                                                 alt: "OpenTripPlanner home",
                                                 src: logoPath
                                                 });

        // initialize utilities
        otp.util.imagePathManager.addCustomAgencies(this.config.useCustomIconsForAgencies);

        ////////// trip planner ///////////
        this.poi  = new otp.planner.poi.Control({map:this.map.getMap()});
        var  purl = this.params.getPlannerUrl(this.config.planner.url);
        var  pconfig = this.config.planner;
        pconfig.url = purl;
        pconfig.map = this.map;
        pconfig.poi = this.poi;
        pconfig.ui  = this.ui;;
        this.planner = new otp.planner.Planner(pconfig);
        this.makeContextMenu();
        this.ui.accordion.add(this.planner.getPanel());

        if (this.config.systemMap.enabled) {
            this.sm = new otp.systemmap.Systemmap(Ext.apply({}, {map: this.map}, config.systemMap));
        }

        this.ui.doLayout();

        this.load();

    },

   /**
    * will call map & trip planner form routines in order to populate based on URL params
    */
    load : function()
    {
        // do the POI and the openTool stuff (and if a POI exists, suspend the pan on the tool)
        var p = this.params.getPoi(this.poi, this.map);

        // trip planner forms
        var forms = this.planner.getForms();
        forms.populate(this.params.m_params);
        if (this.params.hasSubmit()) {
            forms.submit();
        }
    },

   /**
     * create a right-click context menu
     */
    makeContextMenu : function()
    {
        var  cmConfig = {};
        if(this.config.plannerContextMenu)
            cmConfig.forms = this.planner.getForms();
        if(this.config.mapContextMenu)
            cmConfig.map = this.map;

        this.cm = new otp.planner.ContextMenu(cmConfig);
        this.cm.map = this.map;  // NOTE: must add the map back to the cm (in case map no mapping params)
    },

    CLASS_NAME : "otp.application.Controller"
};

otp.application.Controller = new otp.Class(otp.application.Controller);
