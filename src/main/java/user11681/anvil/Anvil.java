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

public class Anvil implements PreLaunchEntrypoint {
    public static final String MOD_ID = "anvil";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    protected static final Map<Class<?>, ListenerList<? extends AnvilEvent>> EVENTS = new HashMap<>();
    protected static final Map<Class<?>, Set<Class<?>>> SUBEVENTS = new HashMap<>();
    protected static final Set<Event<?>> FABRIC_EVENTS = new HashSet<>();

    protected static boolean fabricSupport = true;
    protected static int abstractEvents;
    protected static int implementations;
    protected static int anvilEvents;
    protected static int listenedAnvilEvents;
    protected static int fabricEvents;
    protected static int totalEvents;
    protected static int anvilListeners;
    protected static int fabricListeners;
    protected static int totalListeners;

    @Override
    public void onPreLaunch() {
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

    protected long time(final Runnable runnable) {
        final long start = System.nanoTime();
        runnable.run();

        return (System.nanoTime() - start) / 1000;
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
            for (final Class<? extends AnvilEvent> clazz : entrypoint.get()) {
                registerBranch(clazz);
            }
        }
    }

    protected static <T extends AnvilEvent> void registerBranch(final Class<T> clazz) {
        if (AnvilEvent.class.isAssignableFrom(clazz.getSuperclass())) {
            //noinspection unchecked
            final Class<T> superclass = (Class<T>) clazz.getSuperclass();

            registerBranch(superclass);
            SUBEVENTS.get(superclass).add(clazz);
        }

        if (!SUBEVENTS.containsKey(clazz)) {
            final Set<Class<?>> subevents = new HashSet<>();

            subevents.add(clazz);
            SUBEVENTS.put(clazz, subevents);

            if (Modifier.isAbstract(clazz.getModifiers())) {
                ++abstractEvents;
            } else {
                ++implementations;
            }

            ++anvilEvents;
            ++totalEvents;
        }

        if (!EVENTS.containsKey(clazz)) {
            EVENTS.put(clazz, new ListenerList<>());
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
                    final Listener annotation = method.getAnnotation(Listener.class);

                    if (annotation != null) {
                        final int modifiers = method.getModifiers();

                        if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
                            registerListener(method.getParameterTypes()[0], method, annotation);
                            ++totalListeners;
                        }
                    }
                }
            }
        }
    }

    protected static <E, T> void registerListener(final Class<?> eventClass, final Method method, final Listener annotation) {
        if (AnvilEvent.class.isAssignableFrom(eventClass)) {
            if (method.getReturnType() == void.class && method.getParameterCount() == 1) {
                for (final Class<?> subeventClass : SUBEVENTS.get(eventClass)) {
                    final int priority = annotation.priority();

                    if (priority < 0) {
                        throw new IllegalArgumentException(String.format("%s priority < 0", method));
                    } else if (priority > 100) {
                        throw new IllegalArgumentException(String.format("%s priority > 100", method));
                    }

                    //noinspection unchecked
                    final ListenerList<E> listeners = (ListenerList<E>) EVENTS.get(subeventClass);

                    //noinspection unchecked
                    listeners.add((Class<E>) subeventClass, (final E event) -> {
                        try {
                            method.invoke(null, event);
                        } catch (final InvocationTargetException | IllegalAccessException exception) {
                            LOGGER.error(String.format("An error occurred while attempting to fire %s: ", subeventClass.getName()), exception.getCause());
                        }
                    }, priority, annotation.persist());

                    ++anvilListeners;

                    if (listeners.size() == 1) {
                        ++listenedAnvilEvents;
                    }
                }
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
                    final boolean accessible = field.isAccessible();
                    field.setAccessible(true);
                    //noinspection unchecked
                    final Event<T> event = (Event<T>) field.get(null);
                    field.setAccessible(accessible);

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
                    LOGGER.error(String.format("unable to class find %s.", className), exception);
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

    public static int getFabricListenerS() {
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
