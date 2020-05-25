package transfarmer.anvil.event;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import transfarmer.anvil.Main;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.util.ActionResult.FAIL;
import static net.minecraft.util.ActionResult.SUCCESS;

public class EventInvoker {
    protected static final Map<Class<? extends Event>, EventList<? extends Event>> LISTENERS = new Reference2ReferenceOpenHashMap<>();

    public static void load() {
    }

    static {
        final long start = System.nanoTime();

        Main.LOGGER.info("Registering event listeners.");

        final Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader()).setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner(), new MethodAnnotationsScanner()));

        for (final Class<? extends Event> clazz : getClassAndSubclasses(reflections, Event.class)) {
            LISTENERS.put(clazz, new EventList<>());
        }

        for (final Class<?> clazz : reflections.getTypesAnnotatedWith(Anvil.class)) {
            for (final Method method : clazz.getDeclaredMethods()) {
                final int methodModifiers = method.getModifiers();

                if (Modifier.isPublic(methodModifiers) && method.getReturnType() == void.class && Modifier.isStatic(methodModifiers) && method.getParameterCount() == 1) {
                    try {
                        final Listener annotation = method.getAnnotation(Listener.class);

                        //noinspection unchecked
                        for (final Class<? extends Event> subclass : getClassAndSubclasses(reflections, (Class<? extends Event>) method.getParameterTypes()[0])) {
                            register(subclass, event -> {
                                try {
                                    method.invoke(null, event);
                                } catch (final IllegalAccessException | InvocationTargetException exception) {
                                    Main.LOGGER.trace(exception);
                                }
                            }, annotation.priority(), annotation.persist());
                        }
                    } catch (final ClassCastException ignored) {
                    }
                }
            }
        }

        Main.LOGGER.info("Done after {} ms.", (System.nanoTime() - start) / 1000000);
    }

    protected static <T> Set<Class<? extends T>> getClassAndSubclasses(final Reflections reflections, final Class<T> clazz) {
        final Set<Class<? extends T>> classes = new HashSet<>();

        classes.add(clazz);
        classes.addAll(reflections.getSubTypesOf(clazz));

        return classes;
    }

    protected static <E extends Event> void register(final Class<E> clazz, final Consumer<E> consumer,
                                                     final int priority,
                                                     final boolean persistence) {
        if (priority < 0) {
            throw new IllegalArgumentException("Event priority may not be less than 0.");
        } else if (priority > 10) {
            throw new IllegalArgumentException("Event priority may not be greater than 10.");
        }

        //noinspection unchecked
        ((EventList<E>) LISTENERS.get(clazz)).add(clazz, consumer, priority, persistence);
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
