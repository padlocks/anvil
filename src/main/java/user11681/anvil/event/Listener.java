package user11681.anvil.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.util.ActionResult;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Listener {
    int DEFAULT_PRIORITY = 50;

    /**
     * @return an integer in the interval [0, 100] indicating the priority of this event listener.
     * Events with the highest priorities are called first.
     *
     * <b>Must be</b> {@link Listener#DEFAULT_PRIORITY} <b>when listening to Fabric API events.</b>
     */
    int priority() default DEFAULT_PRIORITY;

    /**
     * @return a Boolean value indicating whether this event listener should be called even if
     * {@link AnvilEvent#result} is {@link ActionResult#FAIL} or {@link ActionResult#SUCCESS}.
     *
     * <b>Must be {@code false} when listening to Fabric API events</b>.
     */
    boolean persist() default false;
}
