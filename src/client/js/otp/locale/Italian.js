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
otp.locale.Italian = {

    config :
    {
        //Name of a language written in a language itself (Used in Frontend to
        //choose a language)
        name: 'Italiano',
        //FALSE-imperial units are used
        //TRUE-Metric units are used
        metric : true,
        //Name of localization file (*.po file) in src/client/i18n
        locale_short : "it",
        //Name of datepicker localization in
        //src/client/js/lib/jquery-ui/i18n (usually
        //same as locale_short)
        //this is index in $.datepicker.regional array
        //If file for your language doesn't exist download it from here
        //https://github.com/jquery/jquery-ui/tree/1-9-stable/ui/i18n
        //into src/client/js/lib/jquery-ui/i18n
        //and add it in index.html after other localizations
        //It will be used automatically when UI is switched to this locale
        datepicker_locale_short: "it"
    },

    /**
     * Info Widgets: a list of the non-module-specific "information widgets"
     * that can be accessed from the top bar of the client display. Expressed as
     * an array of objects, where each object has the following fields:
     * - content: <string> the HTML content of the widget
     * - [title]: <string> the title of the widget
     * - [cssClass]: <string> the name of a CSS class to apply to the widget.
     * If not specified, the default styling is used.
     */
    infoWidgets : [
            {
                title: 'Il Servizio',
                content: '<p>Il servizio di Calcolo Percorsi Regionale si basa sui dati del servizio programmato delle aziende aderenti al sistema di Bigliettazione Elettronica piemontese BIP.</p>\
                <p>Il cuore del sistema è costituito dal modulo di calcolo percorsi, basato sul consolidato motore OpenTripPlanner (OTP), progetto open source ormai giunto al suo quinto anno di sviluppo continuativo.</p>\
                <p>OTP permette di ricercare percorsi multimodali utilizzando il trasporto pubblico (in tutte le sue declinazioni: bus e tram urbani, servizi extraurbani, treni, ecc); permette inoltre di calcolare percorsi pedonali con funzionalità uniche quali l’attraversamento diagonale di piazze o aree libere.</p>\
                <p>OTP è interamente basato su standard aperti, largamente diffusi e consolidati, quali:</p>\
                <p>- OpenStreetMap (OSM) per il grafo di navigazione stradale</p>\
                <p>- il GTFS per la descrizione dei servizi di trasporto pubblico</p>\
                ',
                //cssClass: 'otp-contactWidget',
            },
            {
                title: 'Contatti',
                content: '<p>Per informazioni su percorsi e orari contattare il numero verde 800333444.</p>\
                          <p>Per suggerimenti contattare:<br>Regione Piemonte<br>Settore Trasporto Pubblico Locale<br>via Belfiore 23<br>e-mail: tpl@regione.piemonte.it</p>'
            },
    ],



    time:
    {
        format: "DD.MM.YYYY, HH:mm", //momentjs
        date_format: "DD/MM/YYYY", //momentjs
        time_format: "HH:mm", //momentjs
        time_format_picker : "hh:mmtt", //http://trentrichardson.com/examples/timepicker/#tp-formatting
    },


    CLASS_NAME : "otp.locale.Italian"
};
