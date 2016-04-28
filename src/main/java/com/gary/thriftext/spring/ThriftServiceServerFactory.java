package com.gary.thriftext.spring;

import com.gary.thriftext.register.ThriftServerRegister;
import com.gary.thriftext.register.Utils;
import com.gary.thriftext.spring.annotation.ThriftService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: Thrift 服务初始化工厂
 * @date 2016/4/22 9:54
 */
@Slf4j
public class ThriftServiceServerFactory implements ApplicationContextAware, InitializingBean, Closeable {
    private ServerThread serverThread;
    @Setter
    private Class<TServer> serverClass;
    @Setter
    private Class<TServerTransport> transportClass;
    @Setter
    private TTransportFactory transportFactory;
    @Setter
    private TProtocolFactory protocolFactory;
    @Setter
    private ThriftServerRegister serverRegister;
    @Setter
    private String ip;
    @Setter
    private int port = 9090;
    @Setter
    private long warmup = 10 * 60 * 10000;
    private final String INTERFACE_NAME = "Iface";
    private final String PROCESSOR = "$Processor";

    private ApplicationContext applicationContext;

    public ThriftServiceServerFactory(Class<TServer> serverClass, Class<TServerTransport> transportClass, String ip) {
        this.serverClass = serverClass;
        this.transportClass = transportClass;
        this.ip = ip;
    }

    public void afterPropertiesSet() throws Exception {
        Map<String, Object> services = applicationContext.getBeansWithAnnotation(ThriftService.class);
        if (services != null) {
            TMultiplexedProcessor multiplexedProcessor = new TMultiplexedProcessor();
            for (Map.Entry<String, Object> entry : services.entrySet()) {
                Class serviceClass = entry.getValue().getClass();
                Class[] interfaces = serviceClass.getInterfaces();
                if (interfaces.length == 0)
                    throw new IllegalClassFormatException("service-class should implements Iface");

                Class<?> thriftInterface = getThriftInterfaceClass(interfaces);
                if (thriftInterface == null)
                    throw new IllegalClassFormatException("service-class should implements Iface");
                TProcessor processor = getProcessor(thriftInterface, entry.getValue());
                if (processor == null)
                    throw new IllegalClassFormatException("service-class should implements Iface");

                if (onRegisterProcessor(entry.getValue(), processor)) {
                    ThriftService service = AnnotationUtils.findAnnotation(serviceClass, ThriftService.class);
                    String serviceName = thriftInterface.getEnclosingClass().getSimpleName();
                    multiplexedProcessor.registerProcessor(serviceName, processor);
                    if (serverRegister != null) serverRegister.register(thriftInterface.getName(), service.version(), ip + ":" + port + ":" + service.weight() + ":" + System.currentTimeMillis() + ":" + warmup);
                    log.info("thrift service [{}-{}] register", serviceName, serviceClass);
                }
            }

            serverThread = new ServerThread(multiplexedProcessor);
            start();
        }
    }

    /**
     * 可做扩展
     * @param service
     * @param processor
     * @return
     */
    protected boolean onRegisterProcessor(Object service, TProcessor processor) {return true;}

    /**
     * 获取service 处理类
     * @param thriftInterface 接口
     * @param service
     * @return
     */
    protected TProcessor getProcessor(Class<?> thriftInterface, Object service) {
        String serviceName = thriftInterface.getEnclosingClass().getName();
        String pName = serviceName + PROCESSOR;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> pClass = ClassUtils.forName(pName, classLoader);
            if (!TProcessor.class.isAssignableFrom(pClass)) {
                return null;
            }
            Constructor<?> constructor = Utils.getConstructorByParent(pClass, thriftInterface);
            return (TProcessor) constructor.newInstance(service);
        } catch (Exception e) {
            log.warn("service: {} new instance processor error", serviceName, e);
        }
        return null;
    }

    private Class<?> getThriftInterfaceClass(Class[] interfaces) {
        for (Class anInterface : interfaces) {
            if (anInterface.getSimpleName().equals(INTERFACE_NAME)) {
                return anInterface;
            }
        }
        return null;
    }

    protected void start() {
        serverThread.start();
    }

    @Override
    public void close() throws IOException {
        if (serverThread != null) serverThread.stopServer();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private class ServerThread extends Thread {
        private TServer server;
        ServerThread(TMultiplexedProcessor multiplexedProcessor) throws Exception {
            if (!TServer.class.isAssignableFrom(serverClass))
                throw new IllegalClassFormatException("serverClass should setter for TServer");

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            TServerTransport transport = transportClass.getConstructor(int.class).newInstance(port);

            Class<?> argsClass = ClassUtils.forName(serverClass.getName() + ".Args", classLoader);
            TServer.AbstractServerArgs args = (TServer.AbstractServerArgs) Utils.getConstructorByParent(argsClass, TServerTransport.class).newInstance(transport);
            args.processor(multiplexedProcessor);
            if (transportFactory != null)
                args.transportFactory(transportFactory);
            if (protocolFactory != null)
            args.protocolFactory(protocolFactory);

            Constructor<TServer> serverConstructor = serverClass.getConstructor(args.getClass());
            server = serverConstructor.newInstance(args);
        }

        @Override
        public void run(){
            //启动服务
            server.serve();
        }

        public void stopServer(){
            server.stop();
        }
    }
}
