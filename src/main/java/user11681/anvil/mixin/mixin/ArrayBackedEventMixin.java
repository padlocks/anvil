package user11681.anvil.mixin.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import user11681.anvil.mixin.duck.ArrayBackedEventDuck;

@Mixin(targets = "net.fabricmc.fabric.api.event.ArrayBackedEvent", priority = 500)
public abstract class ArrayBackedEventMixin<T> implements ArrayBackedEventDuck<T> {
    @Override
    @Accessor
    public abstract Class<T> getType();
}
