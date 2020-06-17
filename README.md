<img src="https://raw.githubusercontent.com/user11681/anvil/1.16/src/main/resources/assets/anvil/icon.png" width="20%"></img>

# anvil

[![](https://jitpack.io/v/user11681/anvil.svg)](https://jitpack.io/#user11681/anvil)

an event system for Fabric that features simple creation of events and registration of event listeners
and supports changing method context and modification of event behavior via `ActionResult`s.

Anvil supports Fabric API events.

Also see [anvil events](https://github.com/user11681/anvilevents).

##
### including anvil with Gradle
Click the JitPack banner above; replace `implementation` with `modImplementation`.

If you want to include this mod as a jar-in-jar dependency, then also add this below `modImplementation`:
```groovy
include "com.github.user11681:anvil:${VERSION}"
```
, where `${VERSION}` is your chosen version from above. Use `1.16-SNAPSHOT` for the latest commit.

## making events
### event definition
In order to define an event, extend the `AnvilEvent` class:
```java
public class TestEvent extends AnvilEvent {
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
To register an event, define an entrypoint class that implements one of 
`CommonEventInitializer`, `ClientEventInitializer` and `ServerEventInitializer`:
```java
public class ExampleModEventInitializer implements CommonEventInitializer {
    public Collection<Class<? extends AnvilEvent>> get() {
        return Arrays.asList(TestEvent.class);
    }   
}
```
and include it in your Fabric JSON file:
```json
{
    "entrypoints": {
        "main": [
            ". . ."
        ],
        "anvilCommonEvents": [
            "com.examplemod.ExampleModEventInitializer"
        ],
        "anvilClientEvents": [
            ". . ."
        ],
        "anvilServerEvents": [
            ". . ."
        ]
    }
}
```
Registering an event causes all of its superclasses up to `AnvilEvent` to be registered if not already registered.

### firing events
In order to fire an event, invoke `Anvil#fire(Event)` or `AnvilEvent#fire()`:
```java
public class EventHooks {
    public static void fireTestEvent() {
        Anvil.fire(new TestEvent(true));
        // or
        // new TestEvent(true).fire();
    }
}
```

## listening to events
### registering listener classes
Classes containing event listener methods should be specified in an entrypoint class that implements one of
`CommonListenerInitializer`, `ClientListenerInitializer` and `ServerListenerInitializer`:
```java
public class ExampleModListenerInitializer implements CommonListenerInitializer {
    public Collection<Class<?>> get() {
        return Arrays.asList(Listeners.class);
    }   
}
```
. Further, the entrypoint should be specified in your Fabric JSON file:
```json
{
    "entrypoints": {
        "main": [
            ". . ."
        ],
        "anvilCommonListeners": [
            "io.github.examplemod.ExampleModListenerInitializer"
        ],
        "anvilClientListeners": [
            ". . ."
        ],
        "anvilServerListeners": [
            ". . ."
        ]           
    }
}
```

### listening to anvil events
Event listener methods must be `public static void` and marked with the `@Listener` annotation,
which can optionally receive arguments for priority
(between and including 0 and 100; default: 50; listeners with the greatest `priority` are called first)
and persistence (default: `false`), which indicates that the event listener should receive events
even with `FAIL` or `SUCCESS` result. Listener classes are searched for event listeners automatically.

The method must have exactly one parameter: the event that is being listened to:
```java
public class Listeners {
    @Listener(priority = 40, persist = true)
    public static void onTest(TestEvent event) {
        if (event.getFlag()) {
            event.setResult(ActionResult.FAIL);
        } else {
            event.setSuccess();
        }
    }
}
```
. The event listener will receive all events of its parameter's type and its subclasses.

### listening to Fabric events
To listen to a Fabric event, make a `public static` method marked with the `@Listener` annotation
***without any parameters*** whose return type is the same as that of the callback's abstract method.
The annotation may not take parameters except the default values because anvil currently does not support
non-anvil event priorities and persistence.

Anvil registers Fabric events automatically.
```java
public class Listeners {
    @Listener
    public static ItemStack onPickBlock(ClientPickBlockApplyCallback callback,
                                        PlayerEntity player, HitResult result, ItemStack stack) {
        return new ItemStack(Items.OBSIDIAN);
    }
}
```

### some tips
- Although `@Environment` suffices for preventing a side from attempting to register an event meant for another side,
utilize the `ClientListenerInitializer` and `ServerListenerInitializer` entrypoints to separate them when appropriate.
- Make your listener classes implement `*EventInitializer` and make `get()` return `this.getClass()`.
- Use Fabric events whenever they are appropriate instead of anvil events as they are faster.
