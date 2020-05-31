package user11681.anvil.entrypoint;

import java.util.Collection;
import user11681.anvil.event.Event;

public interface EventInitializer {
    /**
     * @return the implementations of {@link Event} to be registered.
     * Superclasses are registered automatically.
     */
    Collection<Class<? extends Event>> get();
}
