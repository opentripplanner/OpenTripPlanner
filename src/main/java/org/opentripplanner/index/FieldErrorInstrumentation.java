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

import org.opentripplanner.standalone.Router;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.collect.Maps;


/**
 * Log field errors
 */
public final class FieldErrorInstrumentation implements Instrumentation {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FieldErrorInstrumentation.class);

   
    
    public static FieldErrorInstrumentation get(String query, Router router, Map<String, Object> variables, MultivaluedMap<String, String> headers) {
        return new FieldErrorInstrumentation(query, router, variables, headers);
    }
    
    private static class InstrumentationContextBase<T>  implements InstrumentationContext<T> {
        public void onEnd(T result) {     
        }
        public void onEnd(Exception e) {    
        }  
    }
    
    private static final InstrumentationContextBase<Document> dic = new InstrumentationContextBase<Document>();
    private static final InstrumentationContextBase<List<ValidationError>> veic = new InstrumentationContextBase<List<ValidationError>>();
    private final MultivaluedMap<String, String> headers;
    private final Map<String, Object> variables;
    private final Router router;
    private final String query;
    private final long time = System.currentTimeMillis();



    public FieldErrorInstrumentation(String query, Router router, Map<String, Object> variables,
            MultivaluedMap<String, String> headers) {
        this.query = query;
        this.router = router;
        this.variables = variables;
        this.headers = headers;
    }

    private void setMetadata() {
        MDC.put("router", router.id);
        MDC.put("time", Long.toString(System.currentTimeMillis() - time));
        MDC.put("query", query);
        MDC.put("arguments", variables!=null ? variables.toString() : "");   
        if(headers!=null) {
            MDC.put("userAgent", headers.getFirst(HttpHeaders.USER_AGENT));
            MDC.put("referer", headers.getFirst("referer"));
            Sentry.getContext().setUser(new UserBuilder().setId(headers.getFirst("id")).build());
        }
    }
    
    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(ExecutionParameters parameters) {
  
        return new InstrumentationContextBase<ExecutionResult>(){
            @Override
            public void onEnd(Exception e) {
                setMetadata();
                LOG.warn("Error executing query", e);
                MDC.clear();
                super.onEnd(e);
            }
            
            @Override
            public void onEnd(ExecutionResult result) {
                if(result.getErrors().size() > 0) {
                    StringBuilder errors = new StringBuilder();
                    for(GraphQLError e: result.getErrors()){
                        if(e != null) {
                            errors.append(e.getLocations()).append(e.getErrorType()).append(e.getMessage()).append("\n");
                        }
                    }
                    MDC.put("errors", errors.toString());
                }

                Map<String,Object> data = result.getData();
                      
                Map<String, Object> plan=null;
                if(data.get("viewer")!=null) {
                    if(data.get("viewer") instanceof Map) {
                        Map<String, Object> viewer = (Map<String, Object>) data.get("viewer");
                        if(viewer.values().iterator().next() instanceof Map) {
                            plan = (Map<String, Object>) viewer.values().iterator().next();
                        }
                    }
                }
                
                if(data.get("plan")!=null && data.get("plan") instanceof Map) {                    
                    plan = (Map<String, Object>) data.get("plan");
                }
                
                boolean logged=false;
                if(plan != null) {
                    @SuppressWarnings("rawtypes")
                    List itineraries = (List) plan.get("itineraries");
                    if(itineraries !=null && itineraries.isEmpty()) {           
                        setMetadata();
                        logged=true;
                    }
                } 
                if(!logged && result.getErrors().size() > 0) {
                    setMetadata();
                    LOG.warn("Errors executing query");
                } 
                
                MDC.clear();
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
                MDC.put("parent", parameters.getEnvironment().getParentType().getName());
                MDC.put("field", parameters.getField().getName());
                setMetadata();
                LOG.warn("Exception while fetching field", e);      
                MDC.clear();
            }
        };
    }
}
