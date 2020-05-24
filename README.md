<img src="https://raw.githubusercontent.com/transfarmer/anvil/1.15.2/src/main/resources/assets/anvil/logo.png" width="20%"></img>

# anvil

[![](https://jitpack.io/v/transfarmer/anvil.svg)](https://jitpack.io/#transfarmer/anvil)

a Fabric API that implements a Forge-like priority-based event system. It features simple creation of events and registration of event listeners and modification of event behavior via `ActionResult`s.

Also see [anvil events](https://github.com/transfarmer/anvilevents).

## usage
### including anvil in Gradle
Click the JitPack banner above; replace `implementation` with `modImplementation`.<br>
If you want to include this mod as a jar-in-jar depdendency, then also add this below `modImplementation`:
```groovy
include "com.github.transfarmer:anvil:${VERSION}"
```
, where `${VERSION}` is your chosen version from above. Use `1.15.2-SNAPSHOT` for the latest commit.

### event definition and registration
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
which can optionally receive arguments for priority (between and including 0 and 10) and persistence. 
The method must have exactly one parameter: the event that is being listened to:
```java
import transfarmer.anvil.event.EventPriority;

public class Listeners {
    @Listener(priority = 7, persist = true)
    public static void onTest(TestEvent event) {
        if (event.getFlag()) {
            event.setFlag(false);
            event.setResult(ActionResult.FAIL);
        }
    }
}
