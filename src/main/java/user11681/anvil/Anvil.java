package user11681.anvil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import user11681.anvil.entrypoint.ClientEventInitializer;
import user11681.anvil.entrypoint.ClientListenerInitializer;
import user11681.anvil.entrypoint.CommonEventInitializer;
import user11681.anvil.entrypoint.CommonListenerInitializer;
import user11681.anvil.entrypoint.EventInitializer;
import user11681.anvil.entrypoint.ListenerInitializer;
import user11681.anvil.entrypoint.ServerEventInitializer;
import user11681.anvil.entrypoint.ServerListenerInitializer;
import user11681.anvil.event.Event;
import user11681.anvil.event.EventListener;
import user11681.anvil.event.Listener;
import user11681.anvil.event.ListenerList;

public class Anvil implements PreLaunchEntrypoint {
    public static final String MOD_ID = "anvil";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    protected static final Map<Class<? extends Event>, ListenerList<? extends Event>> EVENTS = new HashMap<>();
    protected static final Map<Class<? extends Event>, Set<Class<? extends Event>>> SUBEVENTS = new HashMap<>();
    protected static int implementations;
    protected static int listeners;

    @Override
    public void onPreLaunch() {
        long time = System.nanoTime();
        registerEvents();
        long duration = (System.nanoTime() - time) / 1000;

        final int totalEvents = getTotalEvents();
        final String implementationString = implementations == 1 ? "implementation": "implementations";
        final String classString = totalEvents == 1 ? "class" : "classes";

        LOGGER.info("Registered {} event {} ({} abstract and {} {}) in {} μs.", totalEvents, classString, getAbstractEvents(), implementations, implementationString, duration);

        time = System.nanoTime();
        registerListeners();
        duration = (System.nanoTime() - time) / 1000;

        final String listenerString = listeners == 1 ? "listener" : "listeners";

        LOGGER.info("Registered {} event {} in {} μs.", listeners, listenerString, duration);
    }

    protected static void registerEvents() {
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
    }

    protected static <T extends Event> void registerBranch(final Class<T> clazz) {
        if (Event.class.isAssignableFrom(clazz.getSuperclass())) {
            //noinspection unchecked
            final Class<T> superclass = (Class<T>) clazz.getSuperclass();

            registerBranch(superclass);
            registerSuperevents(clazz, superclass);
        }

        if (!SUBEVENTS.containsKey(clazz)) {
            final Set<Class<? extends Event>> subevents = new HashSet<>();

            subevents.add(clazz);

            SUBEVENTS.put(clazz, subevents);

            if (!Modifier.isAbstract(clazz.getModifiers())) {
                ++implementations;
            }
        }

        if (!EVENTS.containsKey(clazz)) {
            EVENTS.put(clazz, new ListenerList<>());
        }
    }

    protected static <T extends Event> void registerSuperevents(final Class<T> clazz, final Class<? super T> superclass) {
        if (Event.class.isAssignableFrom(superclass)) {
            SUBEVENTS.get(superclass).add(clazz);

            registerSuperevents(clazz, superclass.getSuperclass());
        }
    }

    protected static void registerListeners() {
        final FabricLoader loader = FabricLoader.getInstance();
        final List<ListenerInitializer> entrypoints = new ArrayList<>(loader.getEntrypoints("anvilCommonListeners", CommonListenerInitializer.class));

        switch (loader.getEnvironmentType()) {
            case CLIENT:
                entrypoints.addAll(loader.getEntrypoints("anvilClientListeners", ClientListenerInitializer.class));
                break;
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
                                ++listeners;
                            }
                        }
                    }
                }
            }
        }
    }

    protected static <E extends Event> void registerListener(final Class<?> parameterType, final Method method, final Listener annotation) {
        for (final Class<? extends Event> eventClass : SUBEVENTS.get(parameterType)) {
            final int priority = annotation.priority();

            if (priority < 0) {
                throw new IllegalArgumentException(String.format("%s priority < 0", method));
            } else if (priority > 100) {
                throw new IllegalArgumentException(String.format("%s priority > 100", method));
            }

            //noinspection unchecked
            ((ListenerList<E>) EVENTS.get(eventClass)).add((Class<E>) eventClass, event -> {
                try {
                    method.invoke(null, event);
                } catch (final InvocationTargetException | IllegalAccessException exception) {
                    LOGGER.error("An error occurred while attempting to fire an event: ", exception.getCause());
                }
            }, priority, annotation.persist());
        }
    }

    public static int getAbstractEvents() {
        return getTotalEvents() - getEventImplementations();
    }

    public static int getEventImplementations() {
        return implementations;
    }

    public static int getTotalEvents() {
        return EVENTS.size();
    }

    public static int getTotalListeners() {
        return listeners;
    }

    public static <E extends Event> E fire(final E event) {
        for (final EventListener<? extends Event> listener : EVENTS.get(event.getClass())) {
            if (event.shouldContinue() || listener.isPersistent()) {
                listener.accept(event);
            }
        }

        return event;
    }
}
