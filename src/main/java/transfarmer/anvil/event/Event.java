package transfarmer.anvil.event;

import net.minecraft.util.ActionResult;

import static net.minecraft.util.ActionResult.PASS;

/**
 * the base class used for events.
 */
public abstract class Event {
    /**
     * <p>
     * {@link ActionResult#SUCCESS} should successfully adopt new behavior and cancel further processing.
     * </p>
     * <p>
     * {@link ActionResult#FAIL} should cancel further processing and return early.
     * </p>
     */
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
