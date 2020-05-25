package transfarmer.anvil.event;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import transfarmer.anvil.Main;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Consumer;

import static net.minecraft.util.ActionResult.FAIL;
import static net.minecraft.util.ActionResult.SUCCESS;

public class EventInvoker {
    protected static final Map<Class<? extends Event>, EventList<? extends Event>> LISTENERS = new Reference2ReferenceOpenHashMap<>();

    public static void load() {}

    static {
        final long start = System.nanoTime();

        Main.LOGGER.info("Registering event listeners.");

        for (final Method method : new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader()).setScanners(new MethodAnnotationsScanner())).getMethodsAnnotatedWith(Listener.class)) {
            final int methodModifiers = method.getModifiers();

            if (Modifier.isStatic(methodModifiers) && Modifier.isPublic(methodModifiers) && method.getReturnType() == void.class && method.getParameterCount() == 1) {
                final Class<?> clazz = method.getParameterTypes()[0];

                if (Event.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                    final Listener annotation = method.getAnnotation(Listener.class);

                    //noinspection unchecked
                    register((Class<? extends Event>) clazz, event -> {
                        try {
                            method.invoke(null, event);
                        } catch (final IllegalAccessException | InvocationTargetException exception) {
                            Main.LOGGER.error(exception);
                        }
                    }, annotation.priority(), annotation.persist());
                }
            }
        }

        Main.LOGGER.info("Done after {} milliseconds.", (System.nanoTime() - start) / 1000000);
    }

    protected static <E extends Event> void register(final Class<E> clazz, final Consumer<E> consumer, final int priority,
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
