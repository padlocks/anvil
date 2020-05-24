# anvil
a Fabric API that implements a Forge-like priority-based event system. It features simple creation of events and registration of event listeners and modification of event behavior via `ActionResult`s.

Also see [Anvil events](https://github.com/transfarmer/anvilevents).

[![](https://jitpack.io/v/transfarmer/anvil.svg)](https://jitpack.io/#transfarmer/anvil)

## usage
### event definition
In order to define an event, your event class must extend Event and contain a `public static final` EventInvoker field. Its name does not matter.
```java
import transfarmer.anvil.event.Event;
import transfarmer.anvil.event.EventInvoker;

public class TestEvent extends Event {
    public static final EventInvoker INVOKER = new EventInvoker<>(TestEvent.class)
    
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
In order to fire an event, simply invoke the `fire(Event)` method in your EventInvoker instance:
```java
public class Callers {
    public static void fireTestEvent() {
        TestEvent.INVOKER.fire(new TestEvent(true));
    }
}
```

### listening to events
In order to listen to an event, annotate a `public static final` method with the `@Listener` annotation. The method must have exactly one parameter: the event that is being listened to:
```java
public class Listeners {
    @Listener
    public static void onTest(TestEvent event) {
        if (event.getFlag()) {
            event.setFlag(false);
            event.setResult(ActionResult.FAIL);
        }
    }
}
