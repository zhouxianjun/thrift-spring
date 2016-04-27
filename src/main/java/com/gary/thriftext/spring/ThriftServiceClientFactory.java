package com.gary.thriftext.spring;

import com.gary.thriftext.register.ThriftServerProviderFactory;
import com.gary.thriftext.spring.annotation.ThriftReference;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: Thrift 客户端初始化工厂
 * @date 2016/4/22 15:09
 */
@Slf4j
public class ThriftServiceClientFactory extends InstantiationAwareBeanPostProcessorAdapter implements ApplicationContextAware {
    @Setter
    private ThriftServerProviderFactory providerFactory;
    @Setter
    private Class<TTransport> transportClass;
    @Setter
    private Class<TProtocol> protocolClass;

    @Setter
    private int maxActive = 32;// 最大活跃连接数
    @Setter
    private int idleTime = 180000; // -1,关闭空闲检测
    @Setter
    private ApplicationContext applicationContext;

    private ConcurrentHashMap<String, ReferenceBean> referenceBeans = new ConcurrentHashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Method[] methods = bean.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && ! Modifier.isStatic(method.getModifiers())) {
                try {
                    ThriftReference reference = method.getAnnotation(ThriftReference.class);
                    if (reference != null) {
                        Object value = refer(reference, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean);
                        }
                    }
                } catch (Throwable e) {
                    log.error("Failed to init remote service reference at method {} in class {}, cause: {}", new Object[]{name, bean.getClass().getName(), e.getMessage(), e});
                }
            }
        }

        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (! field.isAccessible()) {
                    field.setAccessible(true);
                }
                ThriftReference reference = field.getAnnotation(ThriftReference.class);
                if (reference != null) {
                    Object value = refer(reference, field.getType());
                    if (value != null) {
                        field.set(bean, value);
                    }
                }
            } catch (Throwable e) {
                log.error("Failed to init remote service reference at filed {} in class {}, cause: {}", new Object[]{e.getMessage(), field.getName(), bean.getClass().getName(), e});
            }
        }
        return bean;
    }

    private Object refer(ThriftReference reference, Class<?> referenceClass) throws Exception {
        String key = referenceClass.getName() + ":" + reference.version();
        ReferenceBean referenceBean = referenceBeans.get(key);
        if (referenceBean != null)
            return referenceBean.getObject();
        referenceBean = new ReferenceBean();
        referenceBean.setIdleTime(idleTime);
        referenceBean.setMaxActive(maxActive);
        referenceBean.setProtocolClass(protocolClass);
        referenceBean.setTransportClass(transportClass);
        referenceBean.setReference(reference);
        referenceBean.setReferenceClass(referenceClass);
        referenceBean.setProviderFactory(providerFactory);
        referenceBean.setApplicationContext(applicationContext);
        referenceBean.afterPropertiesSet();
        referenceBeans.putIfAbsent(key, referenceBean);
        referenceBean = referenceBeans.get(key);
        return referenceBean.getObject();
    }
}
