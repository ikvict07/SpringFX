package org.nevertouchgrass.springfx.configuration;

import javafx.stage.Stage;
import org.nevertouchgrass.springfx.navigation.NavigationController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringFXConfiguration {
    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public NavigationController navigationController(Stage stage) {
        return new NavigationController(stage);
    }
}
