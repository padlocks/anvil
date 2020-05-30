<img src="https://raw.githubusercontent.com/transfarmer/anvil/1.15.2/src/main/resources/assets/anvil/logo.png" width="20%"></img>

# anvil

[![](https://jitpack.io/v/transfarmer/anvil.svg)](https://jitpack.io/#transfarmer/anvil)

an event system for Fabric that features simple creation of events and registration of event listeners
and supports changing method context and modification of event behavior via `ActionResult`s.

Also see [anvil events](https://github.com/transfarmer/anvilevents).

##
### including anvil in Gradle
Click the JitPack banner above; replace `implementation` with `modImplementation`.<br>
If you want to include this mod as a jar-in-jar dependency, then also add this below `modImplementation`:
```groovy
include "com.github.transfarmer:anvil:${VERSION}"
```
, where `${VERSION}` is your chosen version from above. Use `1.15.2-SNAPSHOT` for the latest commit.

### event definition
In order to define an event, extend the `Event` class:
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

### event registration
To register an event, specify an entrypoint class that `implements EventInitializer`
and implement `get()`, which should return a `Collection` of the classes of the events to be registered:
```java
package com.examplemod;

import transfarmer.anvil.entrypoint.EventInitializer;

public class ExampleModEventInitializer implements EventInitializer {
    public Collection<Class<? extends Event>> get() {
        return Arrays.asList(TestEvent.class);
    }   
}
```
and include it in your Fabric JSON file:
```json
{
    "main": [
        // ...
    ],
    "anvilEvents": [
        "com.examplemod.ExampleModEventInitializer"
    ]
}
```

### firing events
In order to fire an event, invoke `EventInvoker#fire(Event)`:
```java
import transfarmer.anvil.event.EventInvoker;

public class EventHooks {
    public static void fireTestEvent() {
        EventInvoker.fire(new TestEvent(true));
    }
}
```

### registering listener classes
Classes containing event listeners should be specified in an entrypoint class that `implements ListenerInitializer`
and overrides the `get()` method:
```java
package com.examplemod;

import transfarmer.anvil.entrypoint.ListenerInitializer;

public class ExampleModListenerInitializer implements ListenerInitializer {
    public Collection<Class<?>> get() {
        return Arrays.asList(Listeners.class);
    }   
}
```
. Further, the entrypoint class should be specified in your mod JSON file:
```json
{
    "main": [
        // ...
    ],
    "anvilListeners": [
        "com.examplemod.ExampleModListenerInitializer"
    ]
}
```

### defining event listeners
Event listener methods must be `public static final` and marked with the `@Listener` annotation,
which can optionally receive arguments for priority (between and including 0 and 100; default: 50)
and persistence (default: `false`), which indicates that the event listener should receive events
even with `FAIL` or `SUCCESS` result. Listener classes are searched for event listeners automatically.

The method must have exactly one parameter: the event that is being listened to:
```java
import transfarmer.anvil.event.Listener;

public class Listeners {
    @Listener(priority = 40, persist = true)
    public static void onTest(TestEvent event) {
        if (event.getFlag()) {
            event.setResult(ActionResult.FAIL);
        }
    }
}
```
.
