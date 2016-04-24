package com.gary.thriftext.spring;

import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TServerTransport;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 16-4-23 上午11:31
 */
public class Utils {
    public static <T> Constructor<T> getConstructorByParent(Class<T> classes, Class<?> ...parentParams) throws NoSuchMethodException {
        for (Constructor<?> constructor : classes.getDeclaredConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (parentParams == null && types == null) return (Constructor<T>) constructor;
            if (parentParams != null && parentParams.length == types.length) {
                boolean ok = true;
                for (int i = 0; i < parentParams.length; i++) {
                    if (!parentParams[i].isAssignableFrom(types[i]) && !types[i].isAssignableFrom(parentParams[i])) {
                        ok = false;
                        break;
                    }
                }

                if (ok) return (Constructor<T>) constructor;
            }
        }
        return classes.getConstructor(parentParams);
    }
}
