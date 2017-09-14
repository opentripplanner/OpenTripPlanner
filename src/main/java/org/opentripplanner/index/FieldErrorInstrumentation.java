package org.opentripplanner.index;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.parameters.DataFetchParameters;
import graphql.execution.instrumentation.parameters.ExecutionParameters;
import graphql.execution.instrumentation.parameters.FieldFetchParameters;
import graphql.execution.instrumentation.parameters.FieldParameters;
import graphql.execution.instrumentation.parameters.ValidationParameters;
import graphql.language.Document;
import graphql.validation.ValidationError;
import io.sentry.Sentry;
import io.sentry.event.UserBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

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
    
    private static final InstrumentationContextBase<Document> dic = new InstrumentationContextBase<Document>();
    private static final InstrumentationContextBase<List<ValidationError>> veic = new InstrumentationContextBase<List<ValidationError>>();

    private FieldErrorInstrumentation() {
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(ExecutionParameters parameters) {
        return new InstrumentationContextBase<ExecutionResult>(){
            @Override
            public void onEnd(Exception e) {
                MDC.put("query", parameters.getQuery());
                MDC.put("arguments", parameters.getArguments().toString());   
                @SuppressWarnings("unchecked")
                final MultivaluedMap<String, String> headers = (MultivaluedMap<String, String>) parameters.getArguments().get("headers");
                if(headers!=null) {
                    MDC.put("userAgent", headers.getFirst(HttpHeaders.USER_AGENT));
                    MDC.put("referer", headers.getFirst("referer"));
                    Sentry.getContext().setUser(new UserBuilder().setId(headers.getFirst("id")).build());
                }
                LOG.warn("Error executing query", e);
                MDC.clear();
                super.onEnd(e);
            }
            
            @Override
            public void onEnd(ExecutionResult result) {
                StringBuilder errors = new StringBuilder();
                if(result.getErrors().size() > 0) {
                    for(GraphQLError e: result.getErrors()){
                        errors.append(e.getMessage()).append("\n");
                    }
                }

                Map<String,Object> data = result.getData();
                      
                Map<String, Object> plan=null;
                if(data.get("viewer")!=null) {
                    Map<String, Object> viewer = (Map<String, Object>) data.get("viewer");
                    plan = (Map<String, Object>) viewer.values().iterator().next();    
                }
                
                if(data.get("plan")!=null) {                    
                    plan = (Map<String, Object>) data.get("plan");
                }
                
                if(plan != null) {
                    @SuppressWarnings("rawtypes")
                    List itineraries = (List) plan.get("itineraries");
                    if(itineraries.isEmpty()) {
                        if(result.getErrors().size() > 0) {
                            MDC.put("errors", errors.toString());
                        }
                        
                        MDC.put("query", parameters.getQuery());
                        MDC.put("arguments", parameters.getArguments().toString());   
                        @SuppressWarnings("unchecked")
                        final MultivaluedMap<String, String> headers = (MultivaluedMap<String, String>) parameters.getArguments().get("headers");
                        if(headers!=null) {
                            MDC.put("userAgent", headers.getFirst(HttpHeaders.USER_AGENT));
                            MDC.put("referer", headers.getFirst("referer"));
                            Sentry.getContext().setUser(new UserBuilder().setId(headers.getFirst("id")).build());
                        }
                        LOG.warn("Zero routes found");
                        MDC.clear();
                    }
                } else if(result.getErrors().size() > 0) {
                    MDC.put("errors", errors.toString());
                    MDC.put("query", parameters.getQuery());
                    MDC.put("arguments", parameters.getArguments().toString()); 
                    final MultivaluedMap<String, String> headers = (MultivaluedMap<String, String>) parameters.getArguments().get("headers");
                    if(headers!=null) {
                        MDC.put("userAgent", headers.getFirst(HttpHeaders.USER_AGENT));
                        MDC.put("referer", headers.getFirst("referer"));
                        Sentry.getContext().setUser(new UserBuilder().setId(headers.getFirst("id")).build());
                    }
                    LOG.warn("Errors executing query");
                    MDC.clear();
                }
             
                super.onEnd(result);
            }
            
        };
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
        return new InstrumentationContextBase<ExecutionResult>();
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(FieldParameters parameters) {
        return new InstrumentationContextBase<ExecutionResult>();
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
