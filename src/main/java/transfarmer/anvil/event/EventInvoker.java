package transfarmer.anvil.event;

import java.util.function.Consumer;

import static net.minecraft.util.ActionResult.FAIL;
import static net.minecraft.util.ActionResult.SUCCESS;

public class EventInvoker<E extends Event> {
    protected final EventList<E> listeners;
    protected final Class<E> listenerClass;

    public EventInvoker(final Class<E> listenerClass) {
        this.listeners = new EventList<>();
        this.listenerClass = listenerClass;
    }

    protected EventList<E> getListeners() {
        return this.listeners;
    }

    public void register(final Consumer<E> consumer) {
        this.register(consumer, EventPriority.FIVE);
    }

    public void register(final Consumer<E> consumer, final int priority) {
        this.register(consumer, priority, false);
    }

    public void register(final Consumer<E> consumer, final boolean persistence) {
        this.register(consumer, EventPriority.FIVE, persistence);
    }

    public void register(final Consumer<E> consumer, final int priority, final boolean persistence) {
        this.listeners.add(this.listenerClass, consumer, priority, persistence);
    }

    public E fire(final E event) {
        for (final EventListener<E> listener : this.getListeners()) {
            if (event.getResult() != FAIL && event.getResult() != SUCCESS || listener.isPersistent()) {
                listener.accept(event);
            }
        }

        return event;
    }
}
