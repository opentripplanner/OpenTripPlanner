package org.opentripplanner.common;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.csvreader.CsvReader;
import com.google.common.collect.Collections2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Namer {

    public static final String consonants = "bdfgjklmnprstvz";
    public static final String vowels = "aeiou";
    public static final List<String> syllables = Lists.newArrayList();
    public static final List<String> words = Lists.newArrayList();
    static {
        for (int c = 0; c < consonants.length(); c++) {
            for (int v = 0; v < vowels.length(); v++) {
                syllables.add("" + consonants.charAt(c) + vowels.charAt(v));
            }
        }
    }
    static {
        try {
            CsvReader reader = new CsvReader("/usr/share/dict/british-english", '\'', Charset.forName("UTF8"));
            while (reader.readRecord()) {
                if (reader.getColumnCount() > 1) continue;
                words.add(reader.get(0));
            }
            Collections.shuffle(words);
        } catch (Exception ex) {
            words.clear();
        }
    }

    Set<String> usedNames = Sets.newHashSet();
    int n = 0;
    Random random = new Random();

    public String generateUniqueName() {
        String name;
        do {
            name = "";
            int nSyllables = random.nextInt(2) + 2;
            for (int i = 0; i < nSyllables; i++) {
                name += syllables.get(random.nextInt(syllables.size()));
            }
        } while (usedNames.contains(name));
        usedNames.add(name);
        return name;
    }

    public String getUniqueWord() {
        return words.get(n++);
    }

    public static void main (String[] args) {
        Namer namer = new Namer();
        for (int i = 0; i < 1000; i++) {
            System.out.println(namer.generateUniqueName());
            System.out.println(namer.getUniqueWord());
        }
    }
}
