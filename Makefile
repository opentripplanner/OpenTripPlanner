PYBABEL=../test_translations/.venv/bin/pybabel
LOCALE_FOLDER=./otp-leaflet-client/src/main/webapp/i18n
BABEL_CFG=$(LOCALE_FOLDER)/babel.cfg
TEMPLATE_FILE=$(LOCALE_FOLDER)/messages.pot
LANGS=sl en
JS_FILESPATH=./otp-leaflet-client/src/main/webapp/js/otp
JS_FILES = $(shell find $(JS_FILESPATH)/ -name '*.js')
LOCALE_FILES = $(shell find $(LOCALE_FOLDER)/ -name '*.po')
LAN=en

#Extracts new translation from JS files and creates PO template
$(TEMPLATE_FILE): $(JS_FILES)
	$(PYBABEL) extract --project=OpenTripPlanner -F $(BABEL_CFG) -s -k _tr -c TRANSLATORS: -o $(TEMPLATE_FILE) $(JS_FILESPATH)

#Updates translations with new unstraslated strings from template
.PHONY: update_po
update_po: $(TEMPLATE_FILE)
	for LAN in $(LANGS); do $(PYBABEL) update --domain "$$LAN" --locale "$$LAN" --input-file $(TEMPLATE_FILE) --output-file $(LOCALE_FOLDER)/"$$LAN.po"; done

#Updates js files from new translations in po files
.PHONY: update_js
update_js: $(LOCALE_FILES)
	for LAN in $(LANGS); do i18next-conv -l "$$LAN" -s "$(LOCALE_FOLDER)/$$LAN.po" -t "$(JS_FILESPATH)/locale/$$LAN.json"; done

#Creates new translation
.PHONY: init
init:
	$(PYBABEL) init --domain "$(LAN)" --locale "$(LAN)" --input-file $(TEMPLATE_FILE) --output-file $(LOCALE_FOLDER)/"$(LAN).po";

