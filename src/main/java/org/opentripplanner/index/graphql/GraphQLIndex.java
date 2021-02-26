package org.opentripplanner.index.graphql;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import org.apache.commons.io.Charsets;
import org.opentripplanner.index.graphql.datafetchers.GraphQLStopImpl;
import org.opentripplanner.index.graphql.datafetchers.GraphQLAgencyImpl;
import org.opentripplanner.index.graphql.datafetchers.GraphQLAlertImpl;
import org.opentripplanner.index.graphql.datafetchers.GraphQLFeedImpl;
import org.opentripplanner.index.graphql.datafetchers.GraphQLNodeTypeResolver;
import org.opentripplanner.index.graphql.datafetchers.GraphQLPlaceInterfaceTypeResolver;
import org.opentripplanner.index.graphql.datafetchers.GraphQLQueryTypeImpl;
import org.opentripplanner.index.graphql.datafetchers.GraphQLRouteImpl;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class GraphQLIndex {

  static final Logger LOG = LoggerFactory.getLogger(GraphQLIndex.class);

  static private final GraphQLSchema indexSchema = buildSchema();

  static final ExecutorService threadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
      .setNameFormat("GraphQLExecutor-%d")
      .build());

  static private GraphQLSchema buildSchema() {
    try {
      URL url = Resources.getResource("schema.graphqls");
      String sdl = Resources.toString(url, Charsets.UTF_8);
      TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
      RuntimeWiring runtimeWiring = RuntimeWiring
          .newRuntimeWiring()
          .type("Node", type -> type.typeResolver(new GraphQLNodeTypeResolver()))
          .type("PlaceInterface", type -> type.typeResolver(new GraphQLPlaceInterfaceTypeResolver()))
          .type(IntrospectionTypeWiring.build(GraphQLStopImpl.class))
          .type(IntrospectionTypeWiring.build(GraphQLAgencyImpl.class))
//          .type(IntrospectionTypeWiring.build(GraphQLAlertImpl.class))
          .type(IntrospectionTypeWiring.build(GraphQLFeedImpl.class))
          .type(IntrospectionTypeWiring.build(GraphQLQueryTypeImpl.class))
          .type(IntrospectionTypeWiring.build(GraphQLRouteImpl.class))
          .build();
      SchemaGenerator schemaGenerator = new SchemaGenerator();
      return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }
    catch (Exception e) {
      LOG.error("Unable to build Legacy GraphQL Schema", e);
    }
    return null;
  }

  static HashMap<String, Object> getGraphQLExecutionResult(
      String query, Router router, GraphIndex index, Map<String, Object> variables, String operationName,
      int maxResolves, int timeoutMs, Locale locale
  ) {
    MaxQueryComplexityInstrumentation instrumentation = new MaxQueryComplexityInstrumentation(
        maxResolves);
    GraphQL graphQL = GraphQL.newGraphQL(indexSchema).instrumentation(instrumentation).build();

    if (variables == null) {
      variables = new HashMap<>();
    }

    GraphQLRequestContext requestContext = new GraphQLRequestContext(
        router, index
    );

    ExecutionInput executionInput = ExecutionInput
        .newExecutionInput()
        .query(query)
        .operationName(operationName)
        .context(requestContext)
        .root(router)
        .variables(variables)
        .locale(locale)
        .build();
    HashMap<String, Object> content = new HashMap<>();
    ExecutionResult executionResult;
    try {
      executionResult = graphQL.executeAsync(executionInput).get(timeoutMs, TimeUnit.MILLISECONDS);
      if (!executionResult.getErrors().isEmpty()) {
        content.put("errors", mapErrors(executionResult.getErrors()));
      }
      if (executionResult.getData() != null) {
        content.put("data", executionResult.getData());
      }
    }
    catch (Exception e) {
      Throwable reason = e;
      if (e.getCause() != null) { reason = e.getCause(); }
      LOG.warn("Exception during graphQL.execute: " + reason.getMessage(), reason);
      content.put("errors", mapErrors(List.of(reason)));
    }
    return content;
  }

  static Response getGraphQLResponse(
      String query, Router router, GraphIndex index, Map<String, Object> variables, String operationName,
      int maxResolves, int timeoutMs, Locale locale
  ) {
    Response.ResponseBuilder res = Response.status(Response.Status.OK);
    HashMap<String, Object> content = getGraphQLExecutionResult(
        query,
        router, index,
        variables,
        operationName,
        maxResolves,
        timeoutMs,
        locale
    );
    return res.entity(content).build();
  }

  static private List<Map<String, Object>> mapErrors(Collection<?> errors) {
    return errors.stream().map(e -> {
      HashMap<String, Object> response = new HashMap<>();

      if (e instanceof GraphQLError) {
        GraphQLError graphQLError = (GraphQLError) e;
        response.put("message", graphQLError.getMessage());
        response.put("errorType", graphQLError.getErrorType());
        response.put("locations", graphQLError.getLocations());
        response.put("path", graphQLError.getPath());
      }
      else {
        if (e instanceof Exception) {
          response.put("message", ((Exception) e).getMessage());
        }
        response.put("errorType", e.getClass().getSimpleName());
      }

      return response;
    }).collect(Collectors.toList());
  }

}
