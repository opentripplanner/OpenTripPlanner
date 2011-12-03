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

otp.namespace("otp.util");

/**
 * Utility routines for Analytics
 */
otp.util.HtmlUtils = {

    defaultLogo : 'images/ui/logoSmall.png',
    defaultAlt  : 'OpenTripPlanner',

    /** routines to fixup the HTML shell (e.g., localize strings, add logos, etc...) */
    fixHtml : function(config)
    {
        try
        {
            if(!this.hasLogoLinkImg())
                this.drawCustomLogo(config.logo);
        }
        catch(e)
        {
            console.log("GA EXCEPTION: AnalyticsUtils.importGoogleAnalytics threw exception " + e);
        }
    },


    /** create logo image, using a custom logo if specified */
    drawCustomLogo : function(logo, alt)
    {
        try
        {
            var logoPath = (typeof logo === 'string') ? logo : this.defaultLogo;
            var altStr   = (typeof logo === 'string') ? alt  : this.defaultAlt;

            // TODO: refactor w/out Extjs ???
            var logoAnchorWrapper = this.getLogoLink(); 
            Ext.DomHelper.append(logoAnchorWrapper, {tag: 'img',
                                                     alt: altStr,
                                                     src: logoPath
            });
        }
        catch(e)
        {
            console.log("GA EXCEPTION: AnalyticsUtils.importGoogleAnalytics threw exception " + e);
        }
    },

    getElement : function(el, doc)
    {
        var retVal = null;
        try
        {
            if(doc == null)
                doc = document;
            retVal = doc.getElementById(el);
        }
        catch(e)
        {
            console.log('HtmlUtils.getElement: no element ' + el + ' in doc ' + doc);
        }

        if(retVal == null)
        {
            try
            {
                retVal = document.getElementById(el);
            }
            catch(e)
            {
                console.log('HtmlUtils.getElement: no element ' + el + ' in doc ' + doc);
            }
        }

        return retVal;
    },

    /** */
    hideShowElement : function(el, doc, disp)
    {
        try
        {
            var e = this.getElement(el, doc);
            var s = e.style.display;

            // default display is 'block', could also be 'inline', etc...
            if(disp == null)
                disp = 'block';

            if(s && s == 'none')
                e.style.display = disp;
            else 
                e.style.display = 'none';
        }
        catch(e)
        {
        }
    },

    /** */
    getLogoLink : function(path)
    {
        if(!path)
            path='a';
        return Ext.get('logo').query(path)[0];
    },

    /** check whether the logo link has an img tag */
    hasLogoLinkImg : function()
    {
        var retVal = false;
        try 
        {
            var x = this.getLogoLink('a/img');
            if(x)
                retVal = true;
        }
        catch(e)
        {}
        return retVal;
    },

    CLASS_NAME : "otp.util.HtmlUtils"
};