package org.opentripplanner.modularization.harness;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatasetTest {

  private static Dataset write(Path tmp, String datasetJson) throws IOException {
    Path dir = Files.createDirectories(tmp.resolve("datasets/d"));
    Files.writeString(dir.resolve("dataset.json"), datasetJson);
    return Dataset.load(dir);
  }

  @Test
  void inputsAreCopiedRenamedAndZippedAndConfigIsWritten(@TempDir Path tmp) throws IOException {
    Path repo = Files.createDirectories(tmp.resolve("repo"));
    Files.writeString(repo.resolve("a.osm.pbf"), "osm");
    Files.writeString(repo.resolve("b.txt"), "renamed");
    Path netex = Files.createDirectories(repo.resolve("netex"));
    Files.writeString(netex.resolve("one.xml"), "<x/>");
    Files.writeString(netex.resolve("two.xml"), "<y/>");

    var dataset = write(
      tmp,
      """
      {"inputs":[{"from":"a.osm.pbf"},{"from":"b.txt","as":"c.txt"},{"from":"netex","zipAs":"netex_x.zip"}],
       "buildConfig":{"k":1}}
      """
    );
    Path work = tmp.resolve("work");
    dataset.materialize(repo, work);

    assertTrue(Files.exists(work.resolve("a.osm.pbf")));
    assertEquals("renamed", Files.readString(work.resolve("c.txt")));
    try (var zip = new ZipFile(work.resolve("netex_x.zip").toFile())) {
      assertEquals(2, zip.size());
      assertTrue(zip.getEntry("one.xml") != null);
    }
    assertEquals("{\"k\":1}", Files.readString(work.resolve("build-config.json")).replaceAll("\\s", ""));
    assertEquals("{}", Files.readString(work.resolve("router-config.json")));
  }

  @Test
  void zipsOfTheSameDirectoryAreIdentical(@TempDir Path tmp) throws IOException {
    Path dir = Files.createDirectories(tmp.resolve("in"));
    Files.writeString(dir.resolve("b.xml"), "b");
    Files.writeString(dir.resolve("a.xml"), "a");
    Dataset.zipDirectory(dir, tmp.resolve("1.zip"));
    Dataset.zipDirectory(dir, tmp.resolve("2.zip"));
    assertArrayEquals(Files.readAllBytes(tmp.resolve("1.zip")), Files.readAllBytes(tmp.resolve("2.zip")));
  }

  @Test
  void missingInputIsAnError(@TempDir Path tmp) throws IOException {
    var dataset = write(tmp, "{\"inputs\":[{\"from\":\"nope.pbf\"}]}");
    assertThrows(IllegalStateException.class, () -> dataset.materialize(tmp, tmp.resolve("w")));
  }

  @Test
  void onlyDirectoriesWithADatasetFileAreDatasets(@TempDir Path tmp) throws IOException {
    write(tmp, "{}");
    Files.createDirectories(tmp.resolve("datasets/not-a-dataset"));
    assertEquals(List.of("d"), Dataset.loadAll(tmp.resolve("datasets")).stream().map(Dataset::name).toList());
  }

  @Test
  void workDirIsSubstitutedInConfigAndInMarkedFilesAndDropsGoToNestedDirectories(@TempDir Path tmp)
    throws IOException {
    Path repo = Files.createDirectories(tmp.resolve("repo"));
    Files.writeString(repo.resolve("feed.json"), "{\"url\":\"file://${workDir}/gbfs/a/x.json\"}");
    Files.writeString(repo.resolve("plain.json"), "{\"keep\":\"${workDir}\"}");
    Files.writeString(repo.resolve("update.json"), "{\"u\":\"${workDir}\"}");

    var dataset = write(
      tmp,
      """
      {"inputs":[
         {"from":"feed.json","as":"gbfs/a/gbfs.json","substitute":true},
         {"from":"plain.json","as":"gbfs/a/plain.json"}],
       "routerConfig":{"updaters":[{"url":"file://${workDir}/siri"}]},
       "steps":[{"name":"s","awaitDir":"siri","settleMillis":1,
                 "drops":[{"from":"update.json","toDir":"gbfs/a","substitute":true}]}]}
      """
    );
    Path work = tmp.resolve("work");
    dataset.materialize(repo, work);
    String abs = work.toAbsolutePath().toString();

    assertEquals("{\"url\":\"file://" + abs + "/gbfs/a/x.json\"}", Files.readString(work.resolve("gbfs/a/gbfs.json")));
    assertEquals("{\"keep\":\"${workDir}\"}", Files.readString(work.resolve("gbfs/a/plain.json")));
    assertTrue(Files.readString(work.resolve("router-config.json")).contains("file://" + abs + "/siri"));
    assertTrue(Files.isDirectory(work.resolve("siri")), "the watched directory exists at startup");

    dataset.applyDrops(dataset.steps().get(0), repo, work);
    assertEquals("{\"u\":\"" + abs + "\"}", Files.readString(work.resolve("gbfs/a/update.json")));
  }

  @Test
  void stepsAreReadInOrder(@TempDir Path tmp) throws IOException {
    var dataset = write(tmp, "{\"steps\":[{\"name\":\"one\"},{\"name\":\"two\",\"settleMillis\":5}]}");
    assertEquals(List.of("one", "two"), dataset.steps().stream().map(Dataset.Step::name).toList());
    assertEquals(2000, dataset.steps().get(0).settleMillis());
    assertEquals(5, dataset.steps().get(1).settleMillis());
  }
}
