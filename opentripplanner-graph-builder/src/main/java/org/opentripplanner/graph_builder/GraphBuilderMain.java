package org.opentripplanner.graph_builder;

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

public class GraphBuilderMain {

    private static final String CLASSPATH_PREFIX = "classpath:";

    private static final String FILE_PREFIX = "file:";

    public static void main(String[] args) throws IOException {

        if( args.length == 0) {
            System.err.println("usage: config.xml");
            System.exit(-1);
        }
        
        List<String> paths = new ArrayList<String>();
        paths.add("classpath:org/opentripplanner/graph_builder/application-context.xml");
        for (String arg : args)
            paths.add(arg);

        ApplicationContext context = createContext(paths, new HashMap<String, BeanDefinition>());
        GraphBuilderTask task = (GraphBuilderTask) context.getBean("graphBuilderTask");
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

        for (Map.Entry<String, BeanDefinition> entry : additionalBeans.entrySet())
            ctx.registerBeanDefinition(entry.getKey(), entry.getValue());

        ctx.refresh();
        ctx.registerShutdownHook();
        return ctx;
    }
}
