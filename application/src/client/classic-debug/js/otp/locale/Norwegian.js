otp.namespace("otp.locale");

/**
 * @class
 */
otp.locale.Norwegian = {

    config: {
        //Name of a language written in a language itself (Used in Frontend to
        //choose a language)
        name: 'Norsk',
        //FALSE-imperial units are used
        //TRUE-Metric units are used
        metric : true, 
        //Name of localization file (*.po file) in src/client/i18n
        locale_short : "no",
        //Name of datepicker localization in
        //src/client/js/lib/jquery-ui/i18n (usually
        //same as locale_short)
        //this is index in $.datepicker.regional array
        //If file for your language doesn't exist download it from here
        //https://github.com/jquery/jquery-ui/tree/1-9-stable/ui/i18n
        //into src/client/js/lib/jquery-ui/i18n
        //and add it in index.html after other localizations
        //It will be used automatically when UI is switched to this locale
        datepicker_locale_short: "no"
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
            title: 'Om',
            content: '<p>OTP Debug web klient</p>',
            //cssClass: 'otp-contactWidget',
        },
        {
            title: 'Kontakt',
            content: '<p>Comments? Contact us at...</p>'
        },
    ],


    time: {
        format         : "D. MM. YYYY H:mm", //momentjs
        date_format    : "DD.MM.YYYY", //momentjs
        time_format    : "H:mm", //prej je blo H:i momentjs
        time_format_picker : "HH:mm", //http://trentrichardson.com/examples/timepicker/#tp-formatting
    },


    CLASS_NAME: "otp.locale.Norwegian"
};
