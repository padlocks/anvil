package transfarmer.anvil.event;

import net.minecraft.util.ActionResult;

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
     * {@link ActionResult#FAIL} should cancel further processing and fall back to default behavior.
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

    public boolean isAccepted() {
        return this.result.isAccepted();
    }
}
