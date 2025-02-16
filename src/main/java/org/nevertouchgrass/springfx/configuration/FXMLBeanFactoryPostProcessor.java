package org.nevertouchgrass.springfx.configuration;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
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

        getFxmlNames().forEach(fxmlName -> {
            try {
                String className = capitalize(fxmlName) + "Parent";
                String beanName = fxmlName + "Parent";

                Class<? extends Parent> dynamicClass = createNamedDynamicClass(className);

                BeanDefinition beanDefinition = BeanDefinitionBuilder
                        .genericBeanDefinition(dynamicClass)
                        .setScope(BeanDefinition.SCOPE_SINGLETON)
                        .setInitMethodName("initialize")
                        .addPropertyValue("fxmlName", fxmlName)
                        .getBeanDefinition();

                registry.registerBeanDefinition(beanName, beanDefinition);
            } catch (Exception e) {
                throw new IllegalStateException("Error creating dynamic subclass for " + fxmlName, e);
            }
        });
    }


    @SuppressWarnings("unchecked")
    private Class<? extends Parent> createNamedDynamicClass(String className) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Parent.class);
        enhancer.setUseCache(false);
        enhancer.setInterfaces(new Class[]{FXMLAware.class});
        enhancer.setNamingPolicy((prefix, source, key, names) -> className);

        return (Class<? extends Parent>) enhancer.createClass();
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

    private void copyProperties(Parent target, Parent source) {
        target.getChildrenUnmodifiable().addAll(source.getChildrenUnmodifiable());
        target.setStyle(source.getStyle());
        target.setId(source.getId());
        target.getStyleClass().addAll(source.getStyleClass());
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

interface FXMLAware {
    String getFxmlName();

    void setFxmlName(String fxmlName);
}