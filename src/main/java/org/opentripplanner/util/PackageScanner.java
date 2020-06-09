package org.opentripplanner.util;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Scanner Util to get all classes under one package
 */
public class PackageScanner {

  public static Collection<Class> getClasses(String packageName) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = null;
    try {
      resources = classLoader.getResources(path);
    } catch (IOException e) {
      e.printStackTrace();
      return emptyList();
    }
    List<File> dirs = new ArrayList();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      dirs.add(new File(resource.getFile()));
    }
    ArrayList classes = new ArrayList();
    for (File directory : dirs) {
      classes.addAll(findClasses(directory, packageName));
    }
    return classes;
  }

  /**
   * Recursive method used to find all classes in a given directory and subdirs.
   *
   * @param directory   The base directory
   * @param packageName The package name for classes found inside the base directory
   * @return The classes
   * @throws ClassNotFoundException
   */

  private static List<Class> findClasses(File directory, String packageName) {
    if (!directory.isDirectory()) {
      if (isJarFile(directory)) {
        return loadFromJar(directory, packageName);
      }
      return emptyList();
    } else {
      return loadFromDirectory(directory, packageName);
    }
  }

  private static List<Class> loadFromJar(File file, String packageName) {
    Matcher matcher = jarPattern.matcher(file.getPath());
    if (matcher.matches()) {
      String pathToJar = matcher.group(1);
      String packageDirectory = packageName.replace(".", "/");
      try {
        URL[] urls = {new URL("jar:file:" + pathToJar + "!/")};
        URLClassLoader cl = URLClassLoader.newInstance(urls);
        JarFile jarFile = new JarFile(pathToJar);
        return enumerationAsStream(jarFile.entries())
            .filter(entry -> entry.getName().startsWith(packageDirectory))
            .filter(entry -> entry.getName().endsWith(".class"))
            .map(JarEntry::getName)
            .map(name -> name.substring(0, name.length() - 6).replace("/", "."))
            .map(name -> {
              try {
                return cl.loadClass(name);
              } catch (ClassNotFoundException e) {
                return null;
              }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
      } catch (IOException e) {

      }
    }

    return emptyList();
  }

  private final static Pattern jarPattern = Pattern.compile("file:(.*?\\.jar)(?:!.*)?");

  private static boolean isJarFile(File directory) {
    return jarPattern.matcher(directory.getPath()).matches();
  }

  private static List<Class> loadFromDirectory(File directory, String packageName) {
    List<Class> classes = new ArrayList();
    File[] files = directory.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        assert !file.getName().contains(".");
        classes.addAll(loadFromDirectory(file, packageName + "." + file.getName()));
      } else if (file.getName().endsWith(".class")) {
        try {
          classes.add(Class
              .forName(
                  packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
        } catch (ClassNotFoundException e) {
          continue;
        }
      }
    }
    return classes;
  }

  private static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
            new Iterator<T>() {
              public T next() {
                return e.nextElement();
              }

              public boolean hasNext() {
                return e.hasMoreElements();
              }
            },
            Spliterator.ORDERED), false);
  }
}
