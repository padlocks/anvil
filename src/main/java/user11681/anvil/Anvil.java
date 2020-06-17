package user11681.anvil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.Event;
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
import user11681.anvil.event.AnvilEvent;
import user11681.anvil.event.EventInvocationHandler;
import user11681.anvil.event.EventListener;
import user11681.anvil.event.Listener;
import user11681.anvil.event.ListenerList;
import user11681.anvil.mixin.duck.ArrayBackedEventDuck;

public final class Anvil implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LogManager.getLogger("anvil");

    private static final Map<Class<?>, ListenerList<? extends AnvilEvent>> EVENTS = new HashMap<>();
    private static final Map<Class<?>, Set<Class<?>>> SUBEVENTS = new HashMap<>();
    private static final Set<Event<?>> FABRIC_EVENTS = new HashSet<>();

    private static boolean fabricSupport = true;

    private static int abstractEvents;
    private static int implementations;
    private static int anvilEvents;
    private static int listenedAnvilEvents;
    private static int fabricEvents;
    private static int totalEvents;
    private static int anvilListeners;
    private static int fabricListeners;
    private static int totalListeners;

    @Override
    public final void onPreLaunch() {
        final long eventDuration = time(Anvil::registerEvents);
        final long listenerDuration = time(Anvil::registerListeners);

        final String implementationString = implementations == 1 ? "implementation" : "implementations";
        final String anvilClassString = anvilEvents == 1 ? "class" : "classes";
        final String listenerString = totalListeners == 1 ? "listener" : "listeners";
        final String anvilEventString = listenedAnvilEvents == 1 ? "event" : "events";
        final String fabricEventString = fabricEvents == 1 ? "event" : "events";
        final String totalEventString = totalEvents == 1 ? "event was": "events were";

        LOGGER.info("Registered {} anvil event {} ({} abstract and {} {}) in {} μs.", anvilEvents, anvilClassString, abstractEvents, implementations, implementationString, eventDuration);
        LOGGER.info("Registered {} event {} ({} to {} anvil {} and {} to {} Fabric {}) in in {} μs.",
                totalListeners, listenerString, anvilListeners, listenedAnvilEvents, anvilEventString, fabricListeners, fabricEvents, fabricEventString, listenerDuration);
        LOGGER.info("Registration finished after {} μs. {} {} registered.", eventDuration + listenerDuration, totalEvents, totalEventString);
    }

    private long time(final Runnable runnable) {
        final long start = System.nanoTime();

        runnable.run();

        return (System.nanoTime() - start) / 1000;
    }

    private static void registerEvents() {
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
            for (final Class<? extends AnvilEvent> clazz : entrypoint.get()) {
                registerBranch(clazz, clazz);
            }
        }
    }

    private static <T extends AnvilEvent, U extends AnvilEvent> void registerBranch(final Class<U> child, final Class<T> parent) {
        if (AnvilEvent.class.isAssignableFrom(parent.getSuperclass())) {
            //noinspection unchecked
            final Class<T> superclass = (Class<T>) parent.getSuperclass();

            registerBranch(child, superclass);
        }

        final Set<Class<?>> subevents;

        if (!SUBEVENTS.containsKey(parent)) {
            subevents = new HashSet<>();

            subevents.add(parent);
            SUBEVENTS.put(parent, subevents);

            if (Modifier.isAbstract(parent.getModifiers())) {
                ++abstractEvents;
            } else {
                ++implementations;
            }

            ++anvilEvents;
            ++totalEvents;
        } else {
            subevents = SUBEVENTS.get(parent);
        }

        subevents.add(child);

        if (!EVENTS.containsKey(parent)) {
            EVENTS.put(parent, new ListenerList<>());
        }
    }

    private static void registerListeners() {
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
                    final Listener annotation = method.getAnnotation(Listener.class);

                    if (annotation != null) {
                        final int modifiers = method.getModifiers();

                        if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && method.getParameterCount() > 0) {
                            registerListener(method.getParameterTypes()[0], method, annotation);
                            ++totalListeners;
                        }
                    }
                }
            }
        }
    }

    private static <E, T> void registerListener(final Class<?> eventClass, final Method method, final Listener annotation) {
        if (AnvilEvent.class.isAssignableFrom(eventClass)) {
            if (method.getReturnType() == void.class && method.getParameterCount() == 1) {
                for (final Class<?> eventSubclass : SUBEVENTS.get(eventClass)) {
                    final int priority = annotation.priority();

                    if (priority < 0) {
                        throw new IllegalArgumentException(String.format("%s priority < 0", method));
                    } else if (priority > 100) {
                        throw new IllegalArgumentException(String.format("%s priority > 100", method));
                    }

                    //noinspection unchecked
                    final ListenerList<E> listeners = (ListenerList<E>) EVENTS.get(eventSubclass);

                    //noinspection unchecked
                    listeners.add((Class<E>) eventSubclass, (final E event) -> {
                        try {
                            method.invoke(null, event);
                        } catch (final InvocationTargetException | IllegalAccessException exception) {
                            LOGGER.error(String.format("An error occurred while attempting to fire %s.", eventSubclass.getName()), exception.getCause());
                        }
                    }, priority, annotation.persist());

                    if (listeners.size() == 1 && !Modifier.isAbstract(eventSubclass.getModifiers())) {
                        ++listenedAnvilEvents;
                    }
                }

                ++anvilListeners;
            }
        } else if (fabricSupport) {
            if (annotation.priority() != Listener.DEFAULT_PRIORITY) {
                throw new IllegalArgumentException("anvil does not support non-anvil event priorities.");
            } else if (annotation.persist()) {
                throw new IllegalArgumentException("anvil cannot send canceled events to non-anvil events.");
            }

            for (final Field field : eventClass.getDeclaredFields()) {
                final String className = "net.fabricmc.fabric.impl.base.event.ArrayBackedEvent";

                try {
                    field.setAccessible(true);
                    //noinspection unchecked
                    final Event<T> event = (Event<T>) field.get(null);

                    if (Class.forName(className).isInstance(event)) {
                        //noinspection unchecked
                        event.register(EventInvocationHandler.proxy(((ArrayBackedEventDuck<T>) event).getType(), method));
                    }

                    if (FABRIC_EVENTS.add(event)) {
                        ++fabricEvents;
                        ++totalEvents;
                    }

                    ++fabricListeners;
                } catch (final IllegalAccessException exception) {
                    LOGGER.error("illegal access? Impossible.", exception);
                } catch (final ClassNotFoundException exception) {
                    LOGGER.error(String.format("unable to class find %s. Abort support for Fabric API events.", className), exception);
                    fabricSupport = false;
                }
            }
        }
    }

    public static int getAbstractEvents() {
        return abstractEvents;
    }

    public static int getEventImplementations() {
        return implementations;
    }

    public static int getAnvilEvents() {
        return anvilEvents;
    }

    public static int getFabricEvents() {
        return fabricEvents;
    }

    public static int getTotalEvents() {
        return totalEvents;
    }

    public static int getAnvilListeners() {
        return anvilListeners;
    }

    public static int getFabricListeners() {
        return fabricListeners;
    }

    public static int getTotalListeners() {
        return totalListeners;
    }

    public static <E extends AnvilEvent> E fire(final E event) {
        final ListenerList<? extends AnvilEvent> listeners = EVENTS.get(event.getClass());

        if (listeners == null) {
            throw new IllegalStateException(String.format("attempted to call an unregistered event %s.", event.getClass().getName()));
        }

        for (final EventListener<? extends AnvilEvent> listener : listeners) {
            if (event.shouldContinue() || listener.isPersistent()) {
                listener.accept(event);
            }
        }

        return event;
    }
}
