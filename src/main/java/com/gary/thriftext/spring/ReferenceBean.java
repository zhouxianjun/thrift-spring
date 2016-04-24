package com.gary.thriftext.spring;

import com.gary.thriftext.register.ThriftServerProviderFactory;
import com.gary.thriftext.spring.annotation.ThriftReference;
import lombok.Setter;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 16-4-23 上午12:29
 */
public class ReferenceBean implements FactoryBean, InitializingBean {
    @Setter
    private int maxActive = 32;// 最大活跃连接数
    @Setter
    private int idleTime = 180000; // -1,关闭空闲检测
    @Setter
    private ThriftServerProviderFactory providerFactory;
    @Setter
    private Class<TTransport> transportClass;
    @Setter
    private Class<TProtocol> protocolClass;
    @Setter
    private Class<?> referenceClass;
    @Setter
    private ThriftReference reference;

    private Object object;
    private Class<?> objectType;
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
        String name = referenceClass.getName();
        objectType = ClassUtils.forName(name, classLoader);
        // 加载Client.Factory类
        Class<TServiceClientFactory<TServiceClient>> fi = (Class<TServiceClientFactory<TServiceClient>>) ClassUtils.forName(name.replace("$Iface", "") + "$Client$Factory", classLoader);
        TServiceClientFactory<TServiceClient> clientFactory = fi.newInstance();
        ThriftClientPoolFactory clientPool = new ThriftClientPoolFactory(providerFactory, clientFactory,
                reference, referenceClass, transportClass, protocolClass);
        GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
        poolConfig.maxActive = maxActive;
        poolConfig.minIdle = 0;
        poolConfig.minEvictableIdleTimeMillis = idleTime;
        poolConfig.timeBetweenEvictionRunsMillis = idleTime / 2L;
        final GenericObjectPool<TServiceClient> pool = new GenericObjectPool<>(clientPool, poolConfig);
        object = Proxy.newProxyInstance(classLoader, new Class[] { objectType }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                TServiceClient client = pool.borrowObject();
                try {
                    return method.invoke(client, args);
                } catch (Exception e) {
                    throw e;
                } finally {
                    pool.returnObject(client);
                }
            }
        });
    }
}
