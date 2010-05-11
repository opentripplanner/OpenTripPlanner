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

otp.namespace('otp.application');

otp.application.Attribution = {

    /** the text that gets displayed in the panel */
    attributionHtml: null,

    /** the panel itself */
    panel: null,

    /** the title of the panel, which gets displayed */
    panelTitle: null,

    initialize: function(config) {
        otp.configure(this, config);
        this.panel = new Ext.Panel({
                html: this.attributionHtml,
                title: this.panelTitle,
                layout: 'fit'
            });
    },

    getPanel: function() {
        return this.panel;
    },

    CLASS_NAME : "otp.application.Attribution"
};

otp.application.Attribution = new otp.Class(otp.application.Attribution);
