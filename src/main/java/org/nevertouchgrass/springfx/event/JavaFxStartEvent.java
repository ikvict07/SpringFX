package org.nevertouchgrass.springfx.event;

import org.springframework.context.ApplicationEvent;

public class JavaFxStartEvent extends ApplicationEvent {
    public JavaFxStartEvent(Object source) {
        super(source);
    }
}
