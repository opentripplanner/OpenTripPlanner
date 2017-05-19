package org.opentripplanner.index;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLException;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategy;
import graphql.execution.SimpleExecutionStrategy;
import graphql.language.Field;
import graphql.schema.GraphQLObjectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TODO: write JavaDoc
 *
 */
public class ResourceConstrainedExecutorServiceExecutionStrategy extends ExecutionStrategy {

    ExecutorService executorService;

    private final long timeout;
    private final TimeUnit timeUnit;

    private final long maxResolves;
    private AtomicLong resolveCount = new AtomicLong();

    public ResourceConstrainedExecutorServiceExecutionStrategy(ExecutorService executorService, long timeout, TimeUnit timeUnit, long maxResolves) {
        this.executorService = executorService;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.maxResolves = maxResolves;
    }

    // @Override
    protected void countResolve() {
        long count = resolveCount.incrementAndGet();
        if (maxResolves > 0 && count > maxResolves) {
            throw new RuntimeException("Maximum limit of resolves (" + maxResolves + ") met while executing, reduce the result size of your query.");
        }
    }

    @Override
    public ExecutionResult execute(
        final ExecutionContext executionContext,
        final GraphQLObjectType parentType,
        final Object source,
        final Map<String, List<Field>> fields
    ) {
        if (executorService == null)
            return new SimpleExecutionStrategy().execute(executionContext, parentType, source, fields);

        List<Callable<ExecutionResult>> futures = new ArrayList();
        List<String> fieldNames = new ArrayList();

        for (String fieldName : fields.keySet()) {
            final List<Field> fieldList = fields.get(fieldName);
            futures.add(() -> resolveField(executionContext, parentType, source, fieldList));
            fieldNames.add(fieldName);
        }

        Map<String, Object> results = new LinkedHashMap<>();

        try {
            List<Future<ExecutionResult>> executionResults = executorService.invokeAll(futures, timeout, timeUnit);

            for (int i = 0; i < executionResults.size(); i++) {
                // TODO: Is there some kind of zip stream which could take this?
                Future<ExecutionResult> executionResultFuture = executionResults.get(i);
                ExecutionResult executionResult = executionResultFuture.get();
                results.put(fieldNames.get(i), executionResult != null ? executionResult.getData() : null);
            }
        } catch (CancellationException e) {
            executionContext.addError(new ExceptionWhileDataFetching(e));
        } catch (ExecutionException|InterruptedException e) {
            throw new GraphQLException(e);
        }

        return new ExecutionResultImpl(results, executionContext.getErrors());

    }
}