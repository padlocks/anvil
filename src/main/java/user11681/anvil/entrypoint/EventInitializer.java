package user11681.anvil.entrypoint;

import java.util.Collection;
import user11681.anvil.event.Event;

public interface EventInitializer {
    Collection<Class<? extends Event>> get();
}
