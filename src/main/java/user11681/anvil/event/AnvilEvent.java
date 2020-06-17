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
public abstract class AnvilEvent {
    /**
     * {@link ActionResult#SUCCESS} indicates that the given values should be accepted and that further processing
     * should be canceled.
     * <br>
     * {@link ActionResult#PASS} indicates that the event should be passed to other listeners for further processing.
     * <br>
     * {@link ActionResult#FAIL} indicates that further processing should be canceled and that fallback behavior should
     * be used.
     */
    public ActionResult result;

    public AnvilEvent() {
        this.result = PASS;
    }

    /**
     * This method is a shortcut for {@link Anvil#fire(AnvilEvent)}.
     * Care should be taken to not assume an invalid return type.
     *
     * @param <T> the desired type to which this event should be cast.
     * @return this event.
     */
    @SuppressWarnings("unchecked")
    public <T extends AnvilEvent> T fire() {
        return (T) Anvil.fire(this);
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
}
