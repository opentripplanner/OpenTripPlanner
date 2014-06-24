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
        name: 'le français',
        //FALSE-imperial units are used
        //TRUE-Metric units are used
        metric : true, 
        //Name of localization file (*.po file) in otp-leaflet-client/src/main/webapp/i18n
        locale_short : "fr",
        //Name of datepicker localization in
        //otp-leaflet-client/src/main/webapp/js/lib/jquery-ui/i18n (usually
        //same as locale_short)
        //this is index in $.datepicker.regional array
        //If file for your language doesn't exist download it from here
        //https://github.com/jquery/jquery-ui/tree/1-9-stable/ui/i18n
        //into otp-leaflet-client/src/main/webapp/js/lib/jquery-ui/i18n
        //and add it in index.html after other localizations
        //It will be used automatically when UI is switched to this locale
        datepicker_locale_short: "fr"
    },


    time : {
        // TODO
        hour_abbrev    : "hour",
        hours_abbrev   : "hours",
        hour           : "hour",
        hours          : "hours",

        format : "d.m.Y \\à H:i",
        date_format : "d-m-Y",
        time_format : "H:i",
        minute : "minute",
        minutes : "minutes",
        minute_abbrev : "min",
        minutes_abbrev : "min",
        second_abbrev : "sec",
        seconds_abbrev : "sec",
        months : [ 'Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc' ]
    },


    CLASS_NAME : "otp.locale.French"
};
