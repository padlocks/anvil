package transfarmer.anvil.event;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.Map;
import java.util.function.Consumer;

import static net.minecraft.util.ActionResult.FAIL;
import static net.minecraft.util.ActionResult.SUCCESS;

public class EventInvoker {
    protected static final Map<Class<? extends Event>, EventList<? extends Event>> LISTENERS = new Reference2ReferenceOpenHashMap<>();

    public static <E extends Event> void register(final Class<E> clazz, final Consumer<E> consumer, final int priority,
                                                  final boolean persistence) {
        if (priority < 0) {
            throw new IllegalArgumentException("Event priority may not be less than 0.");
        } else if (priority > 10) {
            throw new IllegalArgumentException("Event priority may not be greater than 10.");
        }

        final EventList<E> list;

        if (!LISTENERS.containsKey(clazz)) {
            list = new EventList<>();
        } else {
            //noinspection unchecked
            list = (EventList<E>) LISTENERS.get(clazz);
        }

        list.add(clazz, consumer, priority, persistence);
        LISTENERS.put(clazz, list);
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
