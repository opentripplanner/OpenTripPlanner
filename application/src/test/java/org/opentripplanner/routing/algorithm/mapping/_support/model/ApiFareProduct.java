package org.opentripplanner.routing.algorithm.mapping._support.model;

/**
 * A Fares V2 product. This is a type of ticket or monthly pass that customers can buy.
 *
 * @param id
 * @param name Name of the product, like "One-way single","Monthly pass"
 * @param amount The money amount
 * @param container The fare containers, ie. a smart card or an app.
 * @param category The rider category like senior, youth, veteran.
 */
@Deprecated
public record ApiFareProduct(
  String id,
  String name,
  ApiMoney amount,
  ApiFareQualifier container,
  ApiFareQualifier category
) {}
