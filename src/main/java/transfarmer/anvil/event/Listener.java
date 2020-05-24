package transfarmer.anvil.event;

import net.minecraft.util.ActionResult;
import transfarmer.anvil.event.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Listener {
    /**
     * @return a value indicating the priority of this event listener.
     * Events with the greatest priorities are called first.
     */
    int priority() default EventPriority.FIVE;

    /**
     * @return a Boolean value indicating whether this event listener
     * should be called—even if the result is {@link ActionResult#FAIL}—or not.
     */
    boolean persist() default false;
}
