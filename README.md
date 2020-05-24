# anvil
a Fabric API that implements a Forge-like priority-based event system. It features simple creation of events and registration of event listeners and modification of event behavior via `ActionResult`s.

Also see [Anvil events](https://github.com/transfarmer/anvilevents).

[![](https://jitpack.io/v/transfarmer/anvil.svg)](https://jitpack.io/#transfarmer/anvil)

## usage
### event definition
In order to define and register an event, simply extend Event:
```java
import transfarmer.anvil.event.Event;

public class TestEvent extends Event {
    protected boolean flag;
    
    public TestEvent(boolean flag) {
        this.flag = flag;
    }

    public boolean getFlag() {
        return this.flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
```

### firing events
In order to fire an event, invoke `EventInvoker#fire(Event)`:
```java
import transfarmer.anvil.event.EventInvoker;

public class Callers {
    public static void fireTestEvent() {
        EventInvoker.fire(new TestEvent(true));
    }
}
```

### listening to events
In order to listen to an event, annotate a `public static final` method with the `@Listener` annotation,
which can optionally receive arguments for priority and persistence.
The method must have exactly one parameter: the event that is being listened to:
```java
public class Listeners {
    @Listener(priority = EventPriority.SEVEN, persist = true)
    public static void onTest(TestEvent event) {
        if (event.getFlag()) {
            event.setFlag(false);
            event.setResult(ActionResult.FAIL);
        }
    }
}
