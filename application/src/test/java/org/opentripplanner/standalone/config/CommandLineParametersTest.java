package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beust.jcommander.ParameterException;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandLineParametersTest {

  private static final File BASE_DIR = new File(".");

  private CommandLineParameters subject;

  @BeforeEach
  public void setUp() {
    subject = CommandLineParameters.createCliForTest(BASE_DIR);
    subject.port = 13524;
  }

  @Test
  public void getBaseDirectory() {
    assertEquals(BASE_DIR, subject.getBaseDirectory());
  }

  @Test
  public void baseDirectoryIsRequired() {
    subject = new CommandLineParameters();
    assertThrows(ParameterException.class, () -> subject.inferAndValidate());
  }

  @Test
  public void everyThingOffByDefault() {
    assertFalse(subject.doBuildStreet());
    assertFalse(subject.doBuildTransit());
    assertFalse(subject.doLoadGraph());
    assertFalse(subject.doLoadStreetGraph());
    assertFalse(subject.doSaveGraph());
    assertFalse(subject.doServe());
  }

  @Test
  public void build() {
    subject.build = true;
    assertTrue(subject.doBuildStreet());
    assertTrue(subject.doBuildTransit());
    assertFalse(subject.doSaveGraph());
    assertFalse(subject.doServe());

    subject.save = true;
    subject.serve = false;
    assertTrue(subject.doBuildStreet());
    assertTrue(subject.doBuildTransit());
    assertTrue(subject.doSaveGraph());
    assertFalse(subject.doServe());
    subject.inferAndValidate();

    subject.save = false;
    subject.serve = true;
    assertTrue(subject.doBuildStreet());
    assertTrue(subject.doBuildTransit());
    assertFalse(subject.doSaveGraph());
    assertTrue(subject.doServe());
    subject.inferAndValidate();

    subject.save = true;
    subject.serve = true;
    assertTrue(subject.doBuildStreet());
    assertTrue(subject.doBuildTransit());
    assertTrue(subject.doSaveGraph());
    assertTrue(subject.doServe());
    subject.inferAndValidate();
  }

  @Test
  public void buildStreet() {
    subject.buildStreet = true;
    assertTrue(subject.doBuildStreet());
    assertFalse(subject.doBuildTransit());
    assertTrue(subject.doSaveStreetGraph());
    assertFalse(subject.doSaveGraph());
  }

  @Test
  public void doLoadGraph() {
    subject.load = true;
    assertTrue(subject.doLoadGraph());
    assertTrue(subject.doServe());
  }

  @Test
  public void doLoadStreetGraph() {
    subject.loadStreet = true;
    assertTrue(subject.doLoadStreetGraph());
    assertFalse(subject.doBuildStreet());
    assertFalse(subject.doSaveStreetGraph());
    assertFalse(subject.doSaveGraph());

    subject.save = true;
    subject.serve = true;
    assertTrue(subject.doLoadStreetGraph());
    assertFalse(subject.doBuildStreet());
    assertTrue(subject.doBuildTransit());
    assertTrue(subject.doSaveGraph());
    assertTrue(subject.doServe());
    // Is valid
    subject.inferAndValidate();
  }

  @Test
  public void validateLoad() {
    subject.load = true;
    // No exception thrown
    subject.inferAndValidate();

    // Implicit given, but should be ok to set
    subject.serve = true;

    // No exception thrown
    subject.inferAndValidate();
  }

  @Test
  public void validateAtLeastOnParametersSet() {
    assertThrows(ParameterException.class, () -> subject.inferAndValidate());
  }

  @Test
  public void validateNoMoreThanTwoParametersSet() {
    // --build, --load, --buildStreet and --loadStreet is not allowed together
    // test a few of the possible combinations:
    validateWith().build().load().expectNotValid();
    validateWith().build().buildStreet().expectNotValid();
    validateWith().buildStreet().loadStreet().expectNotValid();
    validateWith().load().loadStreet().expectNotValid();
  }

  @Test
  public void loadCanNotBeUsedWithSave() {
    // --load can not be used with --save
    validateWith().load().save().expectNotValid();
  }

  @Test
  public void buildStreetCanNotBeUsedWithServe() {
    // --buildStreet can not be used with --serve
    validateWith().buildStreet().server().expectNotValid();
  }

  @Test
  public void buildRequiresSaveOrServeOrBoth() {
    // --build requires --save and/or --serve
    validateWith().build().expectNotValid();
  }

  @Test
  public void loadStreetRequiresSaveOrServeOrBoth() {
    // --loadStreet requires --save and/or --serve
    validateWith().loadStreet().expectNotValid();
  }

  private TestValidation validateWith() {
    return new TestValidation();
  }

  private class TestValidation {

    TestValidation() {
      setUp();
    }

    TestValidation build() {
      subject.build = true;
      return this;
    }

    TestValidation load() {
      subject.load = true;
      return this;
    }

    TestValidation buildStreet() {
      subject.buildStreet = true;
      return this;
    }

    TestValidation loadStreet() {
      subject.loadStreet = true;
      return this;
    }

    TestValidation save() {
      subject.save = true;
      return this;
    }

    TestValidation server() {
      subject.serve = true;
      return this;
    }

    void expectNotValid() {
      assertThrows(ParameterException.class, () -> subject.inferAndValidate());
    }
  }
}
