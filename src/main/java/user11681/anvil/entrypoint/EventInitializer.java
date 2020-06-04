package user11681.anvil.entrypoint;

import java.util.Collection;
import user11681.anvil.event.AnvilEvent;

public interface EventInitializer {
    /**
     * @return the implementations of {@link AnvilEvent} to be registered.
     * Superclasses are registered automatically.
     */
    Collection<Class<? extends AnvilEvent>> get();
}
