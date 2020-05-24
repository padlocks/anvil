package transfarmer.anvil;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import transfarmer.anvil.event.Event;
import transfarmer.anvil.event.EventInvoker;
import transfarmer.anvil.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Main implements ModInitializer {
    public static final String MOD_ID = "anvil";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        final long start = System.nanoTime();

        LOGGER.info("Registering event listeners.");

        for (final Method method : new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader()).setScanners(new MethodAnnotationsScanner())).getMethodsAnnotatedWith(Listener.class)) {
            final int methodModifiers = method.getModifiers();

            if (Modifier.isStatic(methodModifiers) && Modifier.isPublic(methodModifiers) && method.getReturnType() == void.class && method.getParameterCount() == 1) {
                final Class<?> clazz = method.getParameterTypes()[0];

                if (Event.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                    final Listener annotation = method.getAnnotation(Listener.class);

                    //noinspection unchecked
                    EventInvoker.register((Class<? extends Event>) clazz, event -> {
                        try {
                            method.invoke(null, event);
                        } catch (final IllegalAccessException | InvocationTargetException exception) {
                            Main.LOGGER.error(exception);
                        }
                    }, annotation.priority(), annotation.persist());
                }
            }
        }

        LOGGER.info("Done after {} milliseconds.", (System.nanoTime() - start) / 1000000);
    }
}
