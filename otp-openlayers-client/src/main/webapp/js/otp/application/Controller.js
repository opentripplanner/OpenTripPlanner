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
        this.config = otp.util.ObjUtils.getConfig(config);

        // TODO more work needed to make train, bikeshare, etc... modes a 'switchable' feature in the UI
        // TODO see otp.config_defaults.planner.options and the related code as to how to turn stuff on & off
        if(this.config.planner.options.showBikeshareMode)
        {
            otp.locale.English.tripPlanner.mode = otp.locale.English.tripPlanner.with_bikeshare_mode;
        }

        // set defaults on the config.map if things don't already exist
        otp.inherit(this.config.map, {
            cm               : this.cm, 
            locale           : this.config.locale,
            routerId         : this.config.routerId,
            attribution      : otp.util.ExtUtils.MAP_ATTRIBUTION,
            plannerOptions   : this.config.planner.options,
            options          : {
                controls: []
            }
        });

        this.params  = new otp.util.ParseUrlParams();
        this.map  = new otp.core.Map(this.config.map);
        this.ui   = new otp.core.UI({map:this.map, locale:this.config.locale});

        // do things like localize HTML strings, and custom icons, etc...
        otp.util.HtmlUtils.fixHtml(this.config);

        // initialize utilities
        otp.util.imagePathManager.addCustomAgencies(this.config.useCustomIconsForAgencies);

        ////////// trip planner ///////////
        this.poi  = new otp.planner.poi.Control({map:this.map.getMap()});
        var  purl = this.params.getPlannerUrl(this.config.planner.url);
        var  pconfig = this.config.planner;
        pconfig.url = purl;
        pconfig.map = this.map;
        pconfig.poi = this.poi;
        pconfig.ui  = this.ui;
        pconfig.locale = this.config.locale;
        pconfig.routerId = this.config.routerId;
        pconfig.geocoder_cfg = pconfig.geocoder;
        this.planner = new otp.planner.Planner(pconfig);
        this.makeContextMenu();
        this.ui.accordion.add(this.planner.getPanel());

        if(this.config.systemMap && this.config.systemMap.enabled) {
            this.sm = new otp.systemmap.Systemmap(Ext.apply({}, {map: this.map}, config.systemMap));
        }

        if(this.config.attributionPanel && this.config.attributionPanel.enabled) {
            this.attributionPanel = new otp.application.Attribution(this.config.attributionPanel);
            this.ui.accordion.add(this.attributionPanel.getPanel());
        }

        this.ui.doLayout();
        this.load();

        if(this.config.splashScreen && this.config.splashScreen.enabled) {
            otp.util.ExtUtils.makePopup(this.config.splashScreen, this.config.splashScreen.title, true, 600, 300, true);
        }
    },

   /**
    * will call map & trip planner form routines in order to populate based on URL params
    */
    load : function()
    {
        // do the POI and the openTool stuff (and if a POI exists, suspend the pan on the tool)
        var p = this.params.getPoi(this.poi, this.map);

        // full screen
        if(this.params.isFullScreen())
            this.ui.fullScreen(true);

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
        var cmConfig = {locale: this.config.locale};
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
