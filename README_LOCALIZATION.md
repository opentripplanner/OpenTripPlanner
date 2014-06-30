<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Localization HOWTO](#localization-howto)
  - [Adding new strings](#adding-new-strings)
    - [Examples:](#examples)
      - [Normal string](#normal-string)
      - [Normal string with context](#normal-string-with-context)
      - [Plural strings](#plural-strings)
  - [Updating translations](#updating-translations)
    - [Babel install](#babel-install)
    - [i18next-conv install](#i18next-conv-install)
    - [Updating](#updating)
  - [Creating new translations](#creating-new-translations)
      - [Short version](#short-version)
      - [Long version](#long-version)
      - [Both versions](#both-versions)
  - [Translating](#translating)
    - [Gotchas](#gotchas)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Localization HOWTO


This is HOWTO for developers how to format strings in OTP and for translators how to translate OTP.

In OTP gettext is used for localization. Why?

- [Plural suport](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poplurals)
- [Context support](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poautocmnt)
- Automatic string extraction
- [Translator comments](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poautocmnt) support
- Source reference (It is saved where each string is used)

On frontend [i18next](http://i18next.com) library is used.

3 types of files are used in OTP localization:
- **POT file** this is message template. It is used for creating new PO files.
- **PO file** this is file which translators translate.
- **json file** this is PO translated file converted to json
- **js file** this is language file which specifies units, and time/date formats

You must edit only PO file and js file. POT file is created and updated automatically and json also.
All translation files are in folder **src/client/i18n** .

Adding new strings
------------------

You add string in javascript at the place you want to use it and you surround it with a call to a special function. Name of the function depends on what string it is:

- normal string `_tr('string', parameters)`
- normal string with **context** `ngettext('context', 'string')`
- string with a plural `ngettext('singular', 'plural', value)`
- string with a plural and context `npgettext('context', 'singular', 'plural', value)`

More about [Sprintf parameters](http://www.diveintojavascript.com/projects/javascript-sprintf).

Context is any string (preferably without spaces and short) that is used to disambiguate translation of key. It is used when developers get input from translators that some string has to be translated to different translation in different parts of a program. Those parts get different context.

When you add strings if you think that translator wouldn't now how string is used or in what are parameters add translator comments:
```javascript
//TRANSLATORS: Start: location at [time date] (Used in print itinerary  
//when do you start your trip)                                          
html += '<h3>' + _tr('Start: %s at %s', this.getStartLocationStr(), this.getStartTimeStr()) + '</h3>';
```
Translator comments **must** always start with "**TRANSLATORS:**" and must be in the line immediately before translated string otherwise they wouldn't be extracted together with a string.

### Examples:

#### Normal string
```javascript
//TRANSLATORS: Board Public transit route name (agency name     
//Stop ID ) start time                                          
html += '<li><b>' + _tr('Board') + '</b>: ' + leg.from.name + ' (' + leg.from.stopId.agencyId + ' Stop ID #' +

//with named sprintf parameters ()preferred option)

//TRANSLATORS: Start: location at [time date] (Used in print itinerary  
//when do you start your trip)                                          
html += '<h3>' + _tr('Start: %(location)s at %(time_date)s', { 'location': this.getStartLocationStr(), 'time_date': this.getStartTimeStr()}) + '</h3>';

// with positional sprintf parameters
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

If you add new strings it is nice to update translation template and translations but it is not mandatory. It is also nice to write "string change" to commit message.

Updating translations
---------------------
Translations are updated with a help of [Babel](http://babel.pocoo.org/) and [i18next-conv](https://github.com/jamuhl/i18next-gettext-converter) (xgettext doesn't support all things in Javascript yet).
Babel is used to extract strings from javascript file to translation template and for updating translation. i18next-conv is used to convert translation files to json which is used in Javascript translation library.

### Babel install
You can install it from a package repository if it is in it or you can use [virtualenv](http://simononsoftware.com/virtualenv-tutorial/).

1. Install virtualenv (This depends on your operating system)
2. Create virtualenv with name .venv in directory where src and other files resides (Root OpenTripPlanner directory). `virtualenv2 .venv`
3. Use virtualenv `source .venv/bin/activate`
4. Install babel `pip install babel`

If you didn't install babel from virtualenv in root OpenTripPlanner directory you have to add path to babel in Makefile. change `PYBABEL` variable to path to pybabel.

### i18next-conv install
You need [nodejs](http://nodejs.org/)

Do `npm install i18next-conv` in same directory where you created virtualenv.

### Updating

Then run `make`. This extracts translations from javascript files and updates translation template messages.pot and all translation files.
After that you can translate some strings and after you save PO file run
`make update_js`. This transforms PO files to json which is used by Javascript translation library. After you rebuild (`mvn project`) All new strings should be visible.

## Creating new translations

#### Short version

Post to opentripplanner-dev mailinglist what language/country you want to translate and you will get a PO file. Or you can use Poedit to create new translation from message template (POT file).

Copy English.js from src/client/js/otp/locale to YourLanguage.js and customize it to your language.
Change name, metric, locale_short and datepicker_locale_short.
Translate infoWidgets and localize time/date formats.


#### Long version

PO files are created from a template with a help of msginit program which is run like this:
`msginit init -l LAN -i messages.pot -o LAN.po` or with the help of Poedit.
Where LAN is culture code.
All translation files are in folder **src/client/i18n** .

Use the ISO language code as culture code, such fr.po for French. Only append the country code in the following circumstances:

- variants of international English: for example, en_GB.po
- Brazillian Portuguese: pt_BR.po (Brazillian Portuguese), as opposed to pt.po for European Portuguese
- Chinese: traditional zh_TW.po and simplified zh_CN.po

Based on [Launchpad Translation](https://help.launchpad.net/Translations/YourProject/ImportingTranslations)

In Linux you can see all culture codes you have installed with a command `locale -a`. They are also availible [here](http://download1.parallels.com/SiteBuilder/Windows/docs/3.2/en_US/sitebulder-3.2-win-sdk-localization-pack-creation-guide/30801.htm)

#### Both versions

Add new culture (PO file) to Makefile in LANGS variable.
Add new Language.js to locales variable in src/client/js/otp/config.js
Add new datepicker translation to src/client/js/lib/jquery-ui/i18n
Load new datepicker translation and Language.js in src/client/index.html

## Translating
For translating you can use any program that supports gettext files. You can also use any text editor but program specific for translating is recommended. Most of them support checking parameter correctness, translation memory, web translating services etc.. and makes your life easier.

Programs (All are free and open source):

- [Poedit](http://poedit.net/) (Linux, Windows, Mac) version newer then 1.5 (suggested option for starting in localization) supports translation memory and file context
- [web poedit](https://localise.biz/free/poedit) (freely usable from a browser, you don't even have to register)
- [Gted](http://www.gted.org/) (Plugin for Eclipse)
- [Lokalize](http://userbase.kde.org/Lokalize) (KDE on Linux, and Windows support kinda) supports translation memory and file context
- [Virtaal](http://virtaal.translatehouse.org/index.html) (Linux, Windows, Mac beta) Supports Google and Microsoft web translation and other translation memories

All the programs support setting string to Fuzzy/needs review etc. this is used if you translate something but aren't sure of it's correctness. Sometimes it is set automatically if original string changed and it is up to a translator to see if translation is still corect.

### Gotchas
Be careful when translating that translated strings have same format. If spaces are at start and end of strings they must also be in translation. Order of unnamed parameters of course depends on a translation. But you can change the order of named parameters if this is better in your language. You can also leave parameter out of a translation.
    
