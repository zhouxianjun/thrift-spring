package com.gary.thriftext.spring;

import com.gary.thriftext.register.InvokerFilter;
import com.gary.thriftext.register.LoadBalance;
import com.gary.thriftext.register.ThriftServerProviderFactory;
import com.gary.thriftext.register.invoker.Invoker;
import com.gary.thriftext.spring.annotation.ThriftFilter;
import com.gary.thriftext.spring.annotation.ThriftReference;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 16-4-23 上午12:29
 */
public class ReferenceBean implements FactoryBean, InitializingBean {
    @Setter
    private ThriftServerProviderFactory providerFactory;
    @Setter
    private Class<?> referenceClass;
    @Setter
    private ThriftReference reference;
    @Setter
    private ApplicationContext applicationContext;
    @Setter
    private LoadBalance loadBalance;

    private Object object;
    private Class<?> objectType;
    private List<InvokerFilter> filters;
    @Override
    public Object getObject() throws Exception {
        return object;
    }

    @Override
    public Class<?> getObjectType() {
        return objectType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // 加载Iface接口
        final String name = referenceClass.getName();
        objectType = ClassUtils.forName(name, classLoader);
        object = Proxy.newProxyInstance(classLoader, new Class[] { objectType }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Invoker invoker = loadBalance.selector(providerFactory.allServerAddressList(name, reference.version()), method);
                long start = System.currentTimeMillis();
                try {
                    List<InvokerFilter> filters = getFilters();
                    if (filters != null) {
                        for (InvokerFilter filter : filters) {
                            if (!filter.before(proxy, method, args)) {
                                invokeAfter(proxy, System.currentTimeMillis() - start, false);
                                return null;
                            }
                        }
                    }
                    Object invoke = invoker.invoker(method, args);
                    invokeAfter(proxy, System.currentTimeMillis() - start, true);
                    return invoke;
                } catch (Exception e) {
                    invokeAfter(proxy, System.currentTimeMillis() - start, false);
                    throw e;
                }
            }
        });
    }

    private void invokeAfter(Object proxy, long time, boolean success) {
        List<InvokerFilter> filters = getFilters();
        if (filters != null) {
            for (InvokerFilter filter : filters) {
                filter.after(proxy, time, success);
            }
        }
    }

    protected List<InvokerFilter> getFilters() {
        if (filters != null) return filters;
        Map<String, Object> map = applicationContext.getBeansWithAnnotation(ThriftFilter.class);
        if (map != null) {
            filters = new ArrayList<>();
            for (Object object : map.values()) {
                if (object instanceof InvokerFilter)
                    filters.add((InvokerFilter) object);
            }
        }
        return filters;
    }
}
