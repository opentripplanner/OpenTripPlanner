# Localization HOWTO


This is HOWTO for developers how to format strings in OTP and for translators how to translate OTP.

In OTP gettext is used for localization. Why?

- [Plural suport](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poplurals)
- [Context support](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poautocmnt)
- Automatic string extraction
- [Translator comments](http://pology.nedohodnik.net/doc/user/en_US/ch-poformat.html#sec-poautocmnt) support

Adding new strings
------------------

You add string in Javascript file and you surround it with a call to a special function. Name of the function depends on what string it is:

- normal string `_tr('string', parameters)`
- normal string with **context** `ngettext('context', 'string')`
- string with a plural `ngettext('singular', 'plural', value)`
- string with a plural and context `npgettext('context', 'singular', 'plural', value)`

When you add strings if you think that translator wouldn't now where string is used or in what way add translator comments:
```javascript
//TRANSLATORS: Start: location at [time date] (Used in print itinerary  
//when do you start your trip)                                          
html += '<h3>' + _tr('Start: %s at %s', this.getStartLocationStr(), this.getStartTimeStr()) + '</h3>';
```
Translator comments **must** always start with "**TRANSLATORS:**" and must be in the line immediately before translated string otherwise they wouldn't be extracted together with a string.

**TODO: EXAMPLES**
translators comments, context etc...

If you add new strings it is nice to update translation template and translations but it is not mandatory. It is also nice to write string change to commit message.

Updating translations
---------------------
Translations are updated with a help of [Babel](http://babel.pocoo.org/) and [i18next-conv](https://github.com/jamuhl/i18next-gettext-converter) (xgettext doesn't support all things in Javascript yet)

### Babel install
You can install it from a package repository if it is in it or you can use [virtualenv](http://simononsoftware.com/virtualenv-tutorial/).
1. Install virtualenv (This depends on your operating system)
2. Create virtualenv with name .venv in directory where otp-leaflet and other files resides. `virtualenv2 .venv`
3. Use virtualenv `source .venv/bin/activate`
3. Install babel `pip install babel`

### i18next-conv install
You need [nodejs](http://nodejs.org/)

Do `npm install i18next-conv` in same directory where you created virtualenv.

### Updating

Then run `make`. This extracts translations from javascript files and updates translation template messages.pot and all translation files.
After that you can translate some strings and after you save po file run
`make update_js`. This transforms po files to json which is used by Javascript translation library. After you rebuild (`mvn project`) All new strings should be visible.

## Creating new translations

#### Short version

Post to opentripplanner-dev mailinglist what language/country you want to translate and you will get a PO file. Or you can use poedit to create new translation from message template (POT file).

#### Long version

PO files are created from a template with a help of msginit program which is run like this:
`msginit init -l LAN -i messages.pot -o LAN.po`
Where LAN is culture code.
All translation files are in folder **otp-leaflet-client/src/main/webapp/i18n** .
If you are translating for a country from where this language comes from use two letter country code (example de, fr, es). If you are translating for other country language use full culture (example de-AT, ca-ES).
In Linux you can see all culture codes you have installed with a command `locale -a`. They are also availible [here](http://download1.parallels.com/SiteBuilder/Windows/docs/3.2/en_US/sitebulder-3.2-win-sdk-localization-pack-creation-guide/30801.htm)

Add new culture to Makefile in LANGS variable.

## Translating
For translating you can use any program that supports gettext files. You can also use any text editor but program specific for translating is recommended. Most of them support checking parameter correctness, translation memory, web translating services etc..

Programs (All are free and open source):

- [Poedit](http://poedit.net/) (Linux, Windows, Mac) version newer then 1.5 (suggested option for starting in localization) supports translation memory and file context
- [web poedit](https://localise.biz/free/poedit) (freely usable from a browser, you don't even have to register)
- [Gted](http://www.gted.org/) (Plugin for Eclipse)
- [Lokalize](http://userbase.kde.org/Lokalize) (KDE on Linux, and Windows support kinda) supports translation memory and file context
- [Virtaal](http://virtaal.translatehouse.org/index.html) (Linux, Windows, Mac beta) Supports Google and Microsoft web translation and other translation memories

All the programs support setting string to Fuzzy/needs review etc. this is used if you translate something but aren't sure of it's correctness. Sometimes it is set automatically if original string changed and it is up to a translator to see if translation is still corect.

### Gotchas
Be careful when translating that translated strings have same format. If spaces are at start and end of strings they must also be in translation. Order of parameters of course depends on a translation
    
