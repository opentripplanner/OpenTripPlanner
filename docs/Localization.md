# Localization

This page contains instructions for both developers and translators on how to make the OTP interface usable by people who speak different languages. Developers will need to take certain steps to mark translatable strings within the source code. Translators will need to edit specific files within the project to create or revise the translation for their language.

In OTP we use gettext for localization, for the following reasons:

- [Plural suport](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poplurals)
- [Context support](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poautocmnt)
- Automatic extraction of translatable strings from source code
- [Translator comments](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poautocmnt) support
- Source references (we can see where each translated string is used in the source code)

In the Javascript UI the [i18next](http://i18next.com) library is used.

Three types of files are used in the OTP localization process:

- The `.pot` file is the message template. It is a starting point for creating new `.po` files.
- `.po` files are created and edited by translators based on the `.pot` file.
- `.json` files are generated from the `.po` files for each language.
- `.js` files are localization configuration files which specify units and time/date formats.

Only the `.po` and `.js` files are directly edited. The `.pot` file is created from an automated analysis of annotated source code. The `.json` files are also automatically generated as an easy way for the Javascript UI to consume the contents of the `.po` files.

All translation files are in the directory `/src/client/i18n`.

## For Software Developers: Adding New Strings

When you add a string to Javascript source that will be seen by the end user, wherever that string is referenced you should surround it with a call to a special function. The name of the function depends on what kind of string it is:

- basic string: `_tr('string', parameters)`
- basic string with context: `ngettext('context', 'string')`
- string with plural: `ngettext('singular', 'plural', quantity)`
- string with plural and context: `npgettext('context', 'singular', 'plural', quantity)`

For more detail, see [Sprintf parameters](http://www.diveintojavascript.com/projects/javascript-sprintf).

A "context" is any string (preferably short and without whitespace) that is used to disambiguate the translation of the main string. It is used when developers get input from translators that some string should be translated in different ways in different parts of the program. Each of those distinct places will be assigned a different context string.

When you add strings to the source code, if you think that translators might not understand how the string is used or what parameters it requires, add translator comments like this:

```javascript
//TRANSLATORS: Start: location at [time date] (Used in print itinerary  
//when do you start your trip)                                          
html += '<h3>' + _tr('Start: %s at %s', this.getStartLocationStr(), this.getStartTimeStr()) + '</h3>';
```

Translator comments must always start with `TRANSLATORS:` and must be in the line immediately before translated string. Otherwise they won't be extracted together with the string.

### Examples:

#### Basic translated string
```javascript
//TRANSLATORS: Board Public transit route name (agency name     
//Stop ID ) start time                                          
html += '<li><b>' + _tr('Board') + '</b>: ' + leg.from.name + ' (' + leg.from.stopId.agencyId + ' Stop ID #' +

//With named sprintf parameters (our preferred option)

//TRANSLATORS: Start: location at [time date] (Used in print itinerary  
//when do you start your trip)                                          
html += '<h3>' + _tr('Start: %(location)s at %(time_date)s', { 'location': this.getStartLocationStr(), 'time_date': this.getStartTimeStr()}) + '</h3>';

//With positional sprintf parameters (to be avoided because word order changes between languages)
html += '<h3>' + _tr('End: %1$s at %2$s', this.getEndLocationStr(), this.getEndTimeStr())+'</h3>';
```

#### Normal string with context
```javascript
 if(leg.headsign) html +=  pgettext("bus_direction", " to ") + leg.headsign;
 
//same string could be different translation
//TRANSLATORS: [distance] to [name of destination]              
html += " "+otp.util.Itin.distanceString(leg.distance)+ pgettext("direction", " to ")+leg.to.name;

```

#### Plural strings

```javascript
//TRANSLATORS: widget title                                             
this.setTitle(ngettext("%d Itinerary Returned", "%d Itineraries Returned", this.itineraries.length));
```

If you add new strings to the source code, it is good practice to also update the translation template and the translations but it is not mandatory (these can be updated later). It is also recommended to include "i18n string change" in the commit message.

Updating translations
---------------------
Translations are updated with the help of [Babel](http://babel.pocoo.org/) and [i18next-conv](https://github.com/jamuhl/i18next-gettext-converter) (xgettext doesn't yet have great Javascript support).

Babel is used to extract strings from the Javascript source code into the shared `.POT` translation template, and also for updating the existing `.PO` language translations when new strings are introduced in the template. i18next-conv is used to convert the `.PO` translation files for the individual languages to `.json` files which are used by the Javascript translation library.

### Installing Babel

You can install it from your operating system's package repository (if available) or you can use [virtualenv](http://simononsoftware.com/virtualenv-tutorial/).

1. Install virtualenv (This depends on your operating system)
2. Create virtualenv with name .venv in directory where src and other files resides (Root OpenTripPlanner directory). `virtualenv2 .venv`
3. Use virtualenv `source .venv/bin/activate`
4. Install babel `pip install babel`

If you didn't install babel from virtualenv in root OpenTripPlanner directory you have to add path to babel in Makefile. change `PYBABEL` variable to path to pybabel.

### Installing i18next-conv

i18next-conv requires [nodejs](http://nodejs.org/).

Once you have NodeJS installed, use `npm install i18next-conv` to install i18next-conv in the same directory where you created virtualenv.

### Updating the `.pot` Template

In the root of the OTP repo, run `make`. The commands in the `Makefile` will extract the translatable strings from the Javascript files and update the translation template `messages.pot`, as well as the `.po` translation files for all the different languages.

Once this is done, you can translate the new strings in the `.po` files. After saving the updated `.po` file, run
`make update_js` to transform to PO files into `.json`, which is used at runtime by the Javascript translation library. After you rebuild OTP, all new strings should be visible in the UI.

## For Translators: Creating New Translations

The following can get a bit technical. If you want to do a translation but don't want to / know how to install all this software, post to the `opentripplanner-dev` mailing list stating what language you want to translate, and someone will make you  a corresponding `.po` file.

### Creating a New Translation File

 New `.po` files are created from the `.pot` template with the help of `msginit`, which is run like this:
 `msginit init -l <LAN> -i messages.pot -o <LAN>.po`, where `<LAN>` is a culture code. New `.po` files can also be created with the help of `Poedit`. All translation files should be placed in the directory `/src/client/i18n`.

 Please use the ISO language code as the culture code (e.g. `fr.po` for French). We will append country codes in the following limited circumstances:

 - British versus US English (`en_GB.po` and `en_US.po`)
 - Brazilian Portuguese `pt_BR.po`, as opposed to `pt.po` for European Portuguese
 - Chinese: `zh_TW.po` for traditional characters as used in e.g. Taiwan and Hong Kong, and `zh_CN.po` for simplified characters as used in mainland China, Singapore, etc.

 These conventions are based on the [Launchpad Translation](https://help.launchpad.net/Translations/YourProject/ImportingTranslations) page.

 In Linux you can see the culture codes for all the locales you have installed with the command `locale -a`. A list of culture codes is also availible [here](http://download1.parallels.com/SiteBuilder/Windows/docs/3.2/en_US/sitebulder-3.2-win-sdk-localization-pack-creation-guide/30801.htm).



### Performing the Translation

#### Configuration
Copy the locale configuration script `English.js` from `/src/client/js/otp/locale` to `YourLanguage.js` and customize it to your language. Change the name, units, locale_short and datepicker_locale_short values. Translate infoWidgets and localize the time/date formats.

Then take the following steps:

- Add the culture code to the `LANGS` variable in the Makefile`
- Add the new `YourLanguage.js` to the locales variable in `/src/client/js/otp/config.js`
- Add a new datepicker translation to `/src/client/js/lib/jquery-ui/i18n`
- Load the new datepicker translation and `YourLanguage.js` in `/src/client/index.html`

#### Translating Strings

For translating the strings themselves, you can use any program that supports gettext files. You can in theory use any text editor, but programs or plugins purpose-built for translating are recommended. Most of them support checking parameter correctness, translation memory, web translating services etc. to make the task easier.

Here are some such programs (all free and open source):

- [Poedit](http://poedit.net/) For Linux, Windows, and Mac. Use a version newer then 1.5. This is the recommended choice for getting started with localization. It supports translation memory and file context.
- [Web Poedit](https://localise.biz/free/poedit) Usable from within a web browser, you don't have to install or register
- [Gted](http://www.gted.org/) A plugin for the Eclipse IDE.
- [Lokalize](http://userbase.kde.org/Lokalize) Runs under KDE on Linux, has some Windows support. Supports translation memory and file context.
- [Virtaal](http://virtaal.translatehouse.org/index.html) For Linux, Windows, and beta for Mac. Supports Google and Microsoft web translation and other translation memory services.

All these programs support setting a string to "fuzzy", marking that it needs review etc. in case you translate something but aren't sure of it's correctness. Sometimes those flags are set automatically if the original string was changed and translators must check if the translation is still correct.

#### Caveats

Be careful when translating that the translated strings have the same format as the original. If spaces appear at the start or end of the strings, they must also appear in the translation. The order of unnamed (positional) parameters may change depending on the target language. You can also leave parameter out of the translation if it is irrelevant in the target language.
