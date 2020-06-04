package user11681.anvil.event;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class EventInvocationHandler implements InvocationHandler {
    protected final Method eventListener;

    protected EventInvocationHandler(final Method eventListener) {
        this.eventListener = eventListener;
    }

    public static <T> T proxy(final Class<T> clazz, final Method eventListener) {
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new EventInvocationHandler(eventListener)));
    }

    @Override
    public Object invoke(final Object proxy, final Method method, Object[] args) throws Throwable {
        if (args != null) {
            final int length = args.length;

            args = Arrays.copyOf(args, length + 1);
            System.arraycopy(args, 0, args, 1, length);
            args[0] = proxy;

            return this.eventListener.invoke(null, args);
        }

        return this.eventListener.invoke(null, proxy);
    }
}
