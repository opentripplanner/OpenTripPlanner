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

otp.namespace("otp.locale");

/**
 * @class
 */
otp.locale.French = {

    config : {
        //Name of a language written in a language itself (Used in Frontend to
        //choose a language)
        name: 'Français',
        //FALSE-imperial units are used
        //TRUE-Metric units are used
        metric : true, 
        //Name of localization file (*.po file) in src/client/i18n
        locale_short : "fr",
        //Name of datepicker localization in
        //src/client/js/lib/jquery-ui/i18n (usually
        //same as locale_short)
        //this is index in $.datepicker.regional array
        //If file for your language doesn't exist download it from here
        //https://github.com/jquery/jquery-ui/tree/1-9-stable/ui/i18n
        //into src/client/js/lib/jquery-ui/i18n
        //and add it in index.html after other localizations
        //It will be used automatically when UI is switched to this locale
        datepicker_locale_short: "fr"
    },


    time : {
        format         : "DD.MM.YY [à] H:mm", //moment.js
        date_format    : "DD/MM/YYYY", //momentjs must be same as date_picker format which is by default: mm/dd/yy
        time_format    : "HH:mm", //momentjs
        time_format_picker : "HH:mm", //http://trentrichardson.com/examples/timepicker/#tp-formatting
    },


    CLASS_NAME : "otp.locale.French"
};
