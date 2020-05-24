package transfarmer.anvil.event;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class EventListener<E> implements Comparable<EventListener<E>> {
    protected final Class<E> clazz;
    protected final Consumer<E> consumer;
    protected final int priority;
    protected final boolean persistence;

    public EventListener(final Class<E> eventClass, final Consumer<E> consumer, final int priority,
                            final boolean persistence) {
        this.clazz = eventClass;
        this.consumer = consumer;
        this.priority = priority;
        this.persistence = persistence;
    }

    public Class<E> getEventClass() {
        return this.clazz;
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
    public int compareTo(@Nonnull final EventListener<E> other) {
        return this.priority - other.priority;
    }

    public void accept(final E event) {
        this.consumer.accept(event);
    }
}
