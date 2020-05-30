package user11681.anvil.event;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import user11681.anvil.Main;
import user11681.anvil.entrypoint.EventInitializer;
import user11681.anvil.entrypoint.ListenerInitializer;

import static net.minecraft.util.ActionResult.FAIL;
import static net.minecraft.util.ActionResult.SUCCESS;

public class EventInvoker implements PreLaunchEntrypoint {
    protected static final Map<Class<? extends Event>, EventList<? extends Event>> LISTENERS = new Reference2ReferenceOpenHashMap<>();
    protected static final Map<Class<? extends Event>, List<Class<? extends Event>>> SUBEVENTS = new Reference2ReferenceOpenHashMap<>();

    @Override
    public void onPreLaunch() {
        long time = System.nanoTime();

        Main.LOGGER.info("Registered {} event classes in {} μs.", registerEvents(), (System.nanoTime() - time) / 1000);

        time = System.nanoTime();

        Main.LOGGER.info("Registered {} event listeners in {} μs.", registerListeners(), (System.nanoTime() - time) / 1000);
    }

    protected static int registerEvents() {
        final List<EventInitializer> entrypoints = FabricLoader.getInstance().getEntrypoints("anvilEvents", EventInitializer.class);

        for (final EventInitializer entrypoint : entrypoints) {
            for (final Class<? extends Event> clazz : entrypoint.get()) {
                registerBranch(clazz);
            }
        }

        return LISTENERS.size();
    }

    protected static void registerBranch(final Class<? extends Event> clazz) {
        final Class<?> superclass = clazz.getSuperclass();

        if (Event.class.isAssignableFrom(superclass)) {
            //noinspection unchecked
            registerBranch((Class<? extends Event>) superclass);

            final List<Class<? extends Event>> subevents;

            if (SUBEVENTS.containsKey(superclass)) {
                subevents = SUBEVENTS.get(superclass);
            } else {
                subevents = new ReferenceArrayList<>();
                SUBEVENTS.put(clazz, subevents);
            }

            if (!subevents.contains(clazz)) {
                subevents.add(clazz);
            }
        }

        if (!LISTENERS.containsKey(clazz)) {
            LISTENERS.put(clazz, new EventList<>());
        }
    }

    protected static int registerListeners() {
        final List<ListenerInitializer> entrypoints = FabricLoader.getInstance().getEntrypoints("anvilListeners", ListenerInitializer.class);

        for (final ListenerInitializer entrypoint : entrypoints) {
            for (final Class<?> clazz : entrypoint.get()) {
                for (final Method method : clazz.getDeclaredMethods()) {
                    final int modifiers = method.getModifiers();

                    if (Modifier.isPublic(modifiers) && method.getReturnType() == void.class && Modifier.isStatic(modifiers) && method.getParameterCount() == 1) {
                        final Listener annotation = method.getAnnotation(Listener.class);

                        if (annotation != null) {
                            final Class<?> parameterType = method.getParameterTypes()[0];

                            if (Event.class.isAssignableFrom(parameterType)) {
                                registerListener(parameterType, method, annotation);
                            }
                        }
                    }
                }
            }
        }

        int registered = 0;

        for (final EventList<?> listeners : LISTENERS.values()) {
            registered += listeners.size();
        }

        return registered;
    }

    protected static <E extends Event> void registerListener(final Class<?> parameterType, final Method method, final Listener annotation) {
        for (final Class<? extends Event> eventClass : SUBEVENTS.get(parameterType)) {
            final int priority = annotation.priority();

            if (priority < 0) {
                throw new IllegalArgumentException("Event priority may not be less than 0.");
            } else if (priority > 100) {
                throw new IllegalArgumentException("Event priority may not be greater than 100.");
            }

            //noinspection unchecked
            ((EventList<E>) LISTENERS.get(eventClass)).add((Class<E>) eventClass, event -> {
                try {
                    method.invoke(null, event);
                } catch (final IllegalAccessException | InvocationTargetException exception) {
                    Main.LOGGER.error(exception);
                }
            }, priority, annotation.persist());
        }
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
