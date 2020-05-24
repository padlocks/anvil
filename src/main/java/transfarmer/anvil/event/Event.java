package transfarmer.anvil.event;

import net.minecraft.util.ActionResult;

import static net.minecraft.util.ActionResult.PASS;

/**
 * the base class used for events.
 *
 * A {@link ActionResult#FAIL} {@link Event#result} should cancel further processing and return early.
 */
public abstract class Event {
    protected ActionResult result;

    public Event() {
        this.result = PASS;
    }

    public ActionResult getResult() {
        return this.result;
    }

    public void setResult(final ActionResult result) {
        this.result = result;
    }
}
