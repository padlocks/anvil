package user11681.anvil.event;

import net.minecraft.util.ActionResult;
import user11681.anvil.Anvil;

import static net.minecraft.util.ActionResult.CONSUME;
import static net.minecraft.util.ActionResult.FAIL;
import static net.minecraft.util.ActionResult.PASS;
import static net.minecraft.util.ActionResult.SUCCESS;

/**
 * the base class used for events.
 */
public abstract class Event {
    /**
     * {@link ActionResult#SUCCESS} should successfully adopt new behavior and cancel further processing.
     * {@link ActionResult#FAIL} should cancel further processing and return early.
     */
    protected ActionResult result;

    public Event() {
        this.result = PASS;
    }

    /**
     * This method is a shortcut for {@link Anvil#fire(Event)}.
     * Care should be taken to not assume an invalid return type.
     *
     * @param <T> the desired type to which this event should be cast.
     * @return this event.
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> T fire() {
        return (T) Anvil.fire(this);
    }

    public ActionResult getResult() {
        return this.result;
    }

    public void setAccepted() {
        if (!this.isAccepted()) {
            this.setConsume();
        }
    }

    public boolean isAccepted() {
        return this.result.isAccepted();
    }

    public boolean shouldContinue() {
        return !this.isFail() && !this.isSuccess();
    }

    public boolean isFail() {
        return this.result == FAIL;
    }

    public boolean isPass() {
        return this.result == PASS;
    }

    public boolean isConsume() {
        return this.result == CONSUME;
    }

    public boolean isSuccess() {
        return this.result == SUCCESS;
    }

    public void setFail() {
        this.result = FAIL;
    }

    public void setPass() {
        this.result = PASS;
    }

    public void setConsume() {
        this.result = CONSUME;
    }

    public void setSuccess() {
        this.result = SUCCESS;
    }

    public void setResult(final ActionResult result) {
        this.result = result;
    }
}
