package org.nevertouchgrass.springfx.configuration;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Empty;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.springframework.util.StringUtils.capitalize;

@Component
@EnableConfigurationProperties(SpringFXConfigurationProperties.class)
public class FXMLBeanFactoryPostProcessor implements BeanFactoryPostProcessor, ApplicationContextAware, EnvironmentAware {

    private SpringFXConfigurationProperties configurationProperties;
    private ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
            throw new IllegalStateException("BeanFactory is not a BeanDefinitionRegistry!");
        }

        List<String> fxmlNames = getFxmlNames();
        for (String fxmlName : fxmlNames) {
            try {
                String className = capitalize(fxmlName) + "Parent";
                String beanName = fxmlName + "Parent";

                Class<? extends Parent> dynamicClass = createNamedDynamicClass(className, fxmlName);
                BeanDefinition beanDefinition = createBeanDefinition(dynamicClass, fxmlName);

                registry.registerBeanDefinition(beanName, beanDefinition);
            } catch (Exception e) {
                throw new IllegalStateException("Error creating dynamic subclass for " + fxmlName, e);
            }
        }
    }

    private BeanDefinition createBeanDefinition(Class<? extends Parent> dynamicClass, String fxmlName) {
        return BeanDefinitionBuilder
                .genericBeanDefinition(dynamicClass)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .getBeanDefinition();
    }


    private Class<? extends Parent> createNamedDynamicClass(String className, String fxmlName) {
        System.out.println("Creating dynamic class for " + fxmlName);
        try {
            Parent root = loadFxml(fxmlName);
            var rootClass = root.getClass();
            System.out.println("Root class: " + rootClass);
            DynamicType.Builder<? extends Parent> dynamicType = new ByteBuddy()
                    .subclass(rootClass)
                    .method(any())
                    .intercept(MethodDelegation.to(new FxmlInterceptor(root)))
                    .name(className);

            return dynamicType.make().load(getClass().getClassLoader()).getLoaded();
        } catch (Exception e) {
            throw new RuntimeException("Error creating dynamic class", e);
        }
    }

    public class FxmlInterceptor {
        private final Object target;

        public FxmlInterceptor(Object target) {
            this.target = target;
        }

        @RuntimeType
        public Object intercept(@Origin Method method,
                                @AllArguments Object[] args,
                                @SuperMethod Method superMethod,
                                @Empty Object defaultValue
        ) throws Throwable {
            method.setAccessible(true);
            return method.invoke(target, args);
        }
    }


    @SneakyThrows
    private Parent loadFxml(String fxmlName) {
        String fxmlLocation = configurationProperties.getFxmlLocation();
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/" + fxmlLocation + "/" + fxmlName + ".fxml")
        ));
        loader.setControllerFactory(applicationContext::getBean);
        return loader.load();
    }


    private List<String> getFxmlNames() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:" + configurationProperties.getFxmlLocation() + "/*.fxml");

            return Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .filter(Objects::nonNull)
                    .map(name -> name.substring(0, name.length() - 5))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan FXML files", e);
        }
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        BindResult<SpringFXConfigurationProperties> result = Binder.get(environment)
                .bind("project", SpringFXConfigurationProperties.class);
        this.configurationProperties = result.get();
    }
}