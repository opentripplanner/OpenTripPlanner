/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A Servlet filter that constructs new objects of a given class, using reflection to pull their
 * field values directly from the query parameters in an HttpRequest. It then "seeds the request
 * scope" by storing a reference to the constructed object as an attribute of the HttpRequest itself.
 */
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
        Object obj = scraper.scrape(request.getParameterMap());
        if (obj != null)
            request.setAttribute(scraper.targetClass.getSimpleName(), obj);
        // pass request and response through to the next filter/servlet in the chain
        chain.doFilter(request, response); 
    }

    @Override
    public void destroy() {
    }

}
