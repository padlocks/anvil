package transfarmer.anvil.event;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class EventListener<E> implements Comparable<EventListener<? extends Event>> {
    protected final Class<E> eventClass;
    protected final Consumer<E> consumer;
    protected final int priority;
    protected final boolean persistence;

    public EventListener(final Class<E> eventClass, final Consumer<E> consumer, final int priority,
                            final boolean persistence) {
        this.eventClass = eventClass;
        this.consumer = consumer;
        this.priority = priority;
        this.persistence = persistence;
    }

    public Class<E> getEventClass() {
        return this.eventClass;
    }

    public Consumer<E> getConsumer() {
        return this.consumer;
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean isPersistent() {
        return this.persistence;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof EventListener && ((EventListener<?>) other).getEventClass() == this.getEventClass();
    }

    @Override
    public int compareTo(@Nonnull final EventListener<? extends Event> other) {
        return this.priority - other.priority;
    }

    public <F extends Event> void accept(final F event) {
        if (this.eventClass.isInstance(event)) {
            this.consumer.accept((E) event);
        }
    }
}
