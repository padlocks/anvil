package user11681.anvil.event;

import net.minecraft.util.ActionResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Listener {
    /**
     * @return an integer in the interval [0, 10] indicating the priority of this event listener.
     * Events with the greatest priorities are called first.
     */
    int priority() default 50;

    /**
     * @return a Boolean value indicating whether this event listener should be called even if
     * the {@link Event#result} is {@link ActionResult#FAIL} or {@link ActionResult#SUCCESS}.
     */
    boolean persist() default false;
}
