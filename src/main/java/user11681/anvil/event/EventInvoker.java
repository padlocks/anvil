package user11681.anvil.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import user11681.anvil.Main;
import user11681.anvil.entrypoint.ClientEventInitializer;
import user11681.anvil.entrypoint.ClientListenerInitializer;
import user11681.anvil.entrypoint.CommonEventInitializer;
import user11681.anvil.entrypoint.CommonListenerInitializer;
import user11681.anvil.entrypoint.EventInitializer;
import user11681.anvil.entrypoint.ListenerInitializer;
import user11681.anvil.entrypoint.ServerEventInitializer;
import user11681.anvil.entrypoint.ServerListenerInitializer;

import static net.minecraft.util.ActionResult.FAIL;
import static net.minecraft.util.ActionResult.SUCCESS;

public class EventInvoker implements PreLaunchEntrypoint {
    protected static final Map<Class<? extends Event>, EventList<? extends Event>> LISTENERS = new HashMap<>();
    protected static final Map<Class<? extends Event>, List<Class<? extends Event>>> SUBEVENTS = new HashMap<>();

    @Override
    public void onPreLaunch() {
        long time = System.nanoTime();

        Main.LOGGER.info("Registered {} event classes in {} μs.", registerEvents(), (System.nanoTime() - time) / 1000);

        time = System.nanoTime();

        Main.LOGGER.info("Registered {} event listeners in {} μs.", registerListeners(), (System.nanoTime() - time) / 1000);
    }

    protected static int registerEvents() {
        final FabricLoader loader = FabricLoader.getInstance();
        final List<EventInitializer> entrypoints = new ArrayList<>(loader.getEntrypoints("anvilCommonEvents", CommonEventInitializer.class));

        switch (loader.getEnvironmentType()) {
            case CLIENT:
                entrypoints.addAll(loader.getEntrypoints("anvilClientEvents", ClientEventInitializer.class));
                break;
            case SERVER:
                entrypoints.addAll(loader.getEntrypoints("anvilServerEvents", ServerEventInitializer.class));
        }

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
                subevents = new ArrayList<>();
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
        final FabricLoader loader = FabricLoader.getInstance();
        final List<ListenerInitializer> entrypoints = new ArrayList<>(loader.getEntrypoints("anvilCommonListeners", CommonListenerInitializer.class));

        switch (loader.getEnvironmentType()) {
            case CLIENT:
                entrypoints.addAll(loader.getEntrypoints("anvilClientListeners", ClientListenerInitializer.class));
            case SERVER:
                entrypoints.addAll(loader.getEntrypoints("anvilServerListeners", ServerListenerInitializer.class));
        }

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
