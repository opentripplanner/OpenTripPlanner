package org.opentripplanner.test.support;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

/**
 * This annotation processor allows you to provide a file pattern like
 * "docs/examples/**\/build-config.json" as the input for a JUnit
 * {@link org.junit.jupiter.params.ParameterizedTest}.
 * <p>
 * Check the usages of {@link FilePatternSource} to see examples for how to use.
 */
class FilePatternArgumentsProvider
  implements ArgumentsProvider, AnnotationConsumer<FilePatternSource> {

  private List<String> patterns;

  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
    return patterns.stream().flatMap(FilePatternArgumentsProvider::resolvePaths).map(Arguments::of);
  }

  private static Stream<Path> resolvePaths(String pattern) {
    var pathMatcher = FileSystems.getDefault().getPathMatcher("glob:./" + pattern);

    var pathsFound = new ArrayList<Path>();
    try {
      Files.walkFileTree(
        Paths.get("."),
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            if (pathMatcher.matches(path)) {
              pathsFound.add(path);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
          }
        }
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return pathsFound.stream();
  }

  @Override
  public void accept(FilePatternSource variableSource) {
    patterns = Arrays.stream(variableSource.pattern()).toList();
  }
}
