package org.opentripplanner.apis.support.graphql.injectdoc;

import static graphql.util.TraversalControl.CONTINUE;

import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitor;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.GraphQLUnionType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * This is GraphQL visitor which injects custom documentation on types and fields.
 */
public class InjectCustomDocumentation
  extends GraphQLTypeVisitorStub
  implements GraphQLTypeVisitor {

  private final CustomDocumentation customDocumentation;

  public InjectCustomDocumentation(CustomDocumentation customDocumentation) {
    this.customDocumentation = customDocumentation;
  }

  @Override
  public TraversalControl visitGraphQLScalarType(
    GraphQLScalarType scalar,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return typeDoc(context, scalar, (s, doc) -> s.transform(b -> b.description(doc)));
  }

  @Override
  public TraversalControl visitGraphQLInterfaceType(
    GraphQLInterfaceType interface_,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return typeDoc(context, interface_, (f, doc) -> f.transform(b -> b.description(doc)));
  }

  @Override
  public TraversalControl visitGraphQLEnumType(
    GraphQLEnumType enumType,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return typeDoc(context, enumType, (f, doc) -> f.transform(b -> b.description(doc)));
  }

  @Override
  public TraversalControl visitGraphQLEnumValueDefinition(
    GraphQLEnumValueDefinition enumValue,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return fieldDoc(
      context,
      enumValue,
      enumValue.getDeprecationReason(),
      (f, doc) -> f.transform(b -> b.description(doc)),
      (f, reason) -> f.transform(b -> b.deprecationReason(reason))
    );
  }

  @Override
  public TraversalControl visitGraphQLFieldDefinition(
    GraphQLFieldDefinition field,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return fieldDoc(
      context,
      field,
      field.getDeprecationReason(),
      (f, doc) -> f.transform(b -> b.description(doc)),
      (f, reason) -> f.transform(b -> b.deprecate(reason))
    );
  }

  @Override
  public TraversalControl visitGraphQLInputObjectField(
    GraphQLInputObjectField inputField,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return fieldDoc(
      context,
      inputField,
      inputField.getDeprecationReason(),
      (f, doc) -> f.transform(b -> b.description(doc)),
      (f, reason) -> f.transform(b -> b.deprecate(reason))
    );
  }

  @Override
  public TraversalControl visitGraphQLInputObjectType(
    GraphQLInputObjectType inputType,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return typeDoc(context, inputType, (f, doc) -> f.transform(b -> b.description(doc)));
  }

  @Override
  public TraversalControl visitGraphQLObjectType(
    GraphQLObjectType object,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return typeDoc(context, object, (f, doc) -> f.transform(b -> b.description(doc)));
  }

  @Override
  public TraversalControl visitGraphQLUnionType(
    GraphQLUnionType union,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    return typeDoc(context, union, (f, doc) -> f.transform(b -> b.description(doc)));
  }

  /* private methods */

  /**
   * Set or append description on a Scalar, Object, InputType, Union, Interface or Enum.
   */
  private <T extends GraphQLNamedSchemaElement> TraversalControl typeDoc(
    TraverserContext<GraphQLSchemaElement> context,
    T element,
    BiFunction<T, String, T> setDescription
  ) {
    customDocumentation
      .typeDescription(element.getName(), element.getDescription())
      .map(doc -> setDescription.apply(element, doc))
      .ifPresent(f -> changeNode(context, f));
    return CONTINUE;
  }

  /**
   * Set or append description and deprecated reason on a field [Object, InputType, Interface,
   * Union or Enum].
   */
  private <T extends GraphQLNamedSchemaElement> TraversalControl fieldDoc(
    TraverserContext<GraphQLSchemaElement> context,
    T field,
    String originalDeprecatedReason,
    BiFunction<T, String, T> setDescription,
    BiFunction<T, String, T> setDeprecatedReason
  ) {
    // All fields need to be defined in a named element
    if (!(context.getParentNode() instanceof GraphQLNamedSchemaElement parent)) {
      throw new IllegalArgumentException("The field does not have a named parent: " + field);
    }
    var fieldName = field.getName();
    var typeName = parent.getName();

    Optional<T> withDescription = customDocumentation
      .fieldDescription(typeName, fieldName, field.getDescription())
      .map(doc -> setDescription.apply(field, doc));

    Optional<T> withDeprecated = customDocumentation
      .fieldDeprecatedReason(typeName, fieldName, originalDeprecatedReason)
      .map(doc -> setDeprecatedReason.apply(withDescription.orElse(field), doc));

    withDeprecated.or(() -> withDescription).ifPresent(f -> changeNode(context, f));

    return CONTINUE;
  }
}
