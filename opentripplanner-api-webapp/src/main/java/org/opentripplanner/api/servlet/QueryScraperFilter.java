package org.opentripplanner.api.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class QueryScraperFilter implements Filter {
    
    private ReflectiveQueryScraper scraper;

    @Override
    public void init(FilterConfig fc) throws ServletException {        
    }

    public QueryScraperFilter(Class<?> targetClass) {
        this.scraper = new ReflectiveQueryScraper(targetClass);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        Object obj = scraper.scrape(request);
        if (obj != null)
            request.setAttribute(scraper.targetClass.getSimpleName(), obj);
        // pass request and response through to the next filter/servlet in the chain
        chain.doFilter(request, response); 
    }

    @Override
    public void destroy() {
    }

}
