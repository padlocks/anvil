package transfarmer.anvil.event;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.Map;
import java.util.function.Consumer;

import static net.minecraft.util.ActionResult.FAIL;
import static net.minecraft.util.ActionResult.SUCCESS;

public class EventInvoker {
    protected static final Map<Class<? extends Event>, EventList<? extends Event>> LISTENERS = new Reference2ReferenceOpenHashMap<>();

    public static <E extends Event> void register(final Class<E> clazz, final Consumer<E> consumer, final int priority, final boolean persistence) {
        final EventList<E> eventList = new EventList<>();

        eventList.add(clazz, consumer, priority, persistence);
        LISTENERS.put(clazz, eventList);
    }

    public static <E extends Event> E fire(final E event) {
        for (final EventListener<? extends Event> listener : LISTENERS.get(event.getClass())) {
            if (event.getResult() != FAIL && event.getResult() != SUCCESS || listener.isPersistent()) {
                listener.accept(event);
            }
        }

        return event;
    }
}
