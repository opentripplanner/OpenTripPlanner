package org.opentripplanner.index;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.parameters.DataFetchParameters;
import graphql.execution.instrumentation.parameters.ExecutionParameters;
import graphql.execution.instrumentation.parameters.FieldFetchParameters;
import graphql.execution.instrumentation.parameters.FieldParameters;
import graphql.execution.instrumentation.parameters.ValidationParameters;
import graphql.language.Document;
import graphql.validation.ValidationError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 * Log field errors
 */
public final class FieldErrorInstrumentation implements Instrumentation {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FieldErrorInstrumentation.class);

    public static FieldErrorInstrumentation INSTANCE = new FieldErrorInstrumentation();
   
    
    private static class InstrumentationContextBase<T>  implements InstrumentationContext<T> {
        public void onEnd(T result) {     
        }
        public void onEnd(Exception e) {    
        }  
    }
    
    private static final InstrumentationContextBase<ExecutionResult> eric = new InstrumentationContextBase<ExecutionResult>();
    private static final InstrumentationContextBase<Document> dic = new InstrumentationContextBase<Document>();
    private static final InstrumentationContextBase<List<ValidationError>> veic = new InstrumentationContextBase<List<ValidationError>>();
    private static final InstrumentationContextBase<Object> oic = new InstrumentationContextBase<Object>();


    private FieldErrorInstrumentation() {
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(ExecutionParameters parameters) {
        MDC.put("query", parameters.getQuery());
        MDC.put("arguments", parameters.getArguments() == null ? null: parameters.getArguments().toString());
        MDC.put("operation",  parameters.getOperation());
        return eric;
    }

    @Override
    public InstrumentationContext<Document> beginParse(ExecutionParameters parameters) {
        return dic;
    }

    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(ValidationParameters parameters) {
        return veic;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginDataFetch(DataFetchParameters parameters) {
        return eric;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(FieldParameters parameters) {
        return eric;
    }

    @Override
    public InstrumentationContext<Object> beginFieldFetch(FieldFetchParameters parameters) {
        return new InstrumentationContextBase<Object>(){
            @Override
            public void onEnd(Exception e) {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                MDC.put("trace",  sw.toString());
                LOG.error("Exception while fetching field {}#{}, {}:{}",parameters.getEnvironment().getParentType().getName(), parameters.getField().getName(), e.getClass(), e.getMessage());                
            }
        };
    }
}
