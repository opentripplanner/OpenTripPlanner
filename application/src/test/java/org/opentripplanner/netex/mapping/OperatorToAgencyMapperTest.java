package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Operator;

public class OperatorToAgencyMapperTest {

  private static final String ID = "ID";
  private static final String NAME = "Olsen";
  private static final String URL = "http://olsen.no/help";
  private static final String PHONE = "+47 88882222";

  @Test
  public void mapOperatorWithEverything() {
    // Given
    Operator operator = new Operator()
      .withId(ID)
      .withName(new MultilingualString().withValue(NAME))
      .withContactDetails(new ContactStructure().withUrl(URL).withPhone(PHONE));

    // When mapped
    org.opentripplanner.transit.model.organization.Operator o;
    o = new OperatorToAgencyMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY
    ).mapOperator(operator);

    // Then expect
    assertEquals(ID, o.getId().getId());
    assertEquals(NAME, o.getName());
    assertEquals(URL, o.getUrl());
    assertEquals(PHONE, o.getPhone());
  }

  @Test
  public void mapOperatorWithMinimumDataSet() {
    // Given
    Operator operator = new Operator()
      .withId(ID)
      .withName(new MultilingualString().withValue(NAME));

    // When mapped
    org.opentripplanner.transit.model.organization.Operator o;
    o = new OperatorToAgencyMapper(
      DataImportIssueStore.NOOP,
      MappingSupport.ID_FACTORY
    ).mapOperator(operator);

    // Then expect
    assertEquals(ID, o.getId().getId());
    assertEquals(NAME, o.getName());
    assertNull(o.getUrl());
    assertNull(o.getPhone());
  }

  @Test
  public void mapOperatorWithMissingName() {
    // Given
    Operator operator = new Operator().withId(ID);

    // When mapped
    org.opentripplanner.transit.model.organization.Operator o;
    DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();
    o = new OperatorToAgencyMapper(issueStore, MappingSupport.ID_FACTORY).mapOperator(operator);

    // Then expect
    assertEquals(ID, o.getId().getId());
    assertEquals(ID, o.getName());
    assertNull(o.getUrl());
    assertNull(o.getPhone());
    assertEquals(1, issueStore.listIssues().size());
    DataImportIssue dataImportIssue = issueStore.listIssues().get(0);
    assertEquals("MissingOperatorName", dataImportIssue.getType());
  }
}
