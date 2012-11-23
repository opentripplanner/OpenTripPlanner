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

package org.opentripplanner.api.thrift;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;


public class OTPServerMain {  
	
	private static final String CLASSPATH_PREFIX = "classpath:";

	private static final String FILE_PREFIX = "file:";

	public static void main(String[] args) throws IOException {

		if (args.length == 0) {
			System.err.println("usage: config.xml");
			System.exit(-1);
		}

		List<String> paths = new ArrayList<String>();
		paths.add("classpath:org/opentripplanner/api_thrift/application-context.xml");
		for (String arg : args)
			paths.add(arg);

		ApplicationContext context = createContext(paths,
				new HashMap<String, BeanDefinition>());
		OTPServerTask task = (OTPServerTask) context
				.getBean("otpServerTask");
		task.run();
	}

	public static ApplicationContext createContext(Iterable<String> paths,
			Map<String, BeanDefinition> additionalBeans) {

		GenericApplicationContext ctx = new GenericApplicationContext();
		XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);

		for (String path : paths) {
			if (path.startsWith(CLASSPATH_PREFIX)) {
				path = path.substring(CLASSPATH_PREFIX.length());
				xmlReader.loadBeanDefinitions(new ClassPathResource(path));
			} else if (path.startsWith(FILE_PREFIX)) {
				path = path.substring(FILE_PREFIX.length());
				xmlReader.loadBeanDefinitions(new FileSystemResource(path));
			} else {
				xmlReader.loadBeanDefinitions(new FileSystemResource(path));
			}
		}

		for (Map.Entry<String, BeanDefinition> entry : additionalBeans
				.entrySet())
			ctx.registerBeanDefinition(entry.getKey(), entry.getValue());

		ctx.refresh();
		ctx.registerShutdownHook();
		return ctx;
	}
}
