package org.nevertouchgrass.springfx.configuration;

import javafx.scene.Node;
import javafx.stage.Stage;
import org.nevertouchgrass.springfx.annotation.AnchorPaneController;
import org.nevertouchgrass.springfx.annotation.Constraints;
import org.nevertouchgrass.springfx.event.JavaFxStartEvent;
import org.nevertouchgrass.springfx.service.AnchorPaneConstraintsService;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ConstraintsAnnotationProcessor implements ApplicationListener<JavaFxStartEvent> {
    private final ApplicationContext applicationContext;
    private final ObjectFactory<AnchorPaneConstraintsService> anchorPaneConstraintsServiceProvider;

    public ConstraintsAnnotationProcessor(ApplicationContext applicationContext, ObjectFactory<AnchorPaneConstraintsService> anchorPaneConstraintsService) {
        this.applicationContext = applicationContext;
        this.anchorPaneConstraintsServiceProvider = anchorPaneConstraintsService;
    }

    @Override
    public void onApplicationEvent(JavaFxStartEvent event) {
        applicationContext.getBeansWithAnnotation(AnchorPaneController.class).forEach((name, bean) -> {
            var fields = bean.getClass().getDeclaredFields();
            var anchorPaneConstraintsService = anchorPaneConstraintsServiceProvider.getObject();
            for (var field : fields) {
                if (field.getType() == Stage.class) {
                    field.setAccessible(true);
                    try {
                        anchorPaneConstraintsService.setScene(((Stage) field.get(bean)).getScene());
                        break;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
            for (var field : fields) {
                if (field.isAnnotationPresent(Constraints.class)) {
                    field.setAccessible(true);
                    try {
                        processConstraints((Node) field.get(bean), field.getAnnotation(Constraints.class), anchorPaneConstraintsService);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void processConstraints(Node node, Constraints constraints, AnchorPaneConstraintsService anchorPaneConstraintsService) {
        if (constraints != null) {
            if (constraints.top() != -1) {
                anchorPaneConstraintsService.setAnchorConstraintsTop(node, constraints.top());
            }
            if (constraints.right() != -1) {
                anchorPaneConstraintsService.setAnchorConstraintsRight(node, constraints.right());
            }
            if (constraints.bottom() != -1) {
                anchorPaneConstraintsService.setAnchorConstraintsBottom(node, constraints.bottom());
            }
            if (constraints.left() != -1) {
                anchorPaneConstraintsService.setAnchorConstraintsLeft(node, constraints.left());
            }
        }

    }
}
