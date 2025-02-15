package org.nevertouchgrass.springfx.navigation;

import javafx.scene.Parent;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Deque;

public class NavigationController {

    private final Stage stage;

    private final Deque<Parent> stack = new ArrayDeque<>();

    public NavigationController(Stage stage) {
        this.stage = stage;
    }

    public void navigateTo(Parent parent) {
        stack.push(parent);
        stage.getScene().setRoot(parent);
    }

    public void navigateBack() {
        stack.pop();
        stage.getScene().setRoot(stack.peek());
    }
}
