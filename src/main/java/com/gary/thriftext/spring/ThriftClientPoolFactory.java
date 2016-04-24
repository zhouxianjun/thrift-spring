package com.gary.thriftext.spring;

import com.gary.thriftext.register.ThriftServerProviderFactory;
import com.gary.thriftext.spring.annotation.ThriftReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: 连接池,thrift-client for spring
 * @date 2016/4/22 15:40
 */
@Slf4j
public class ThriftClientPoolFactory extends BasePoolableObjectFactory<TServiceClient> {
    private final TServiceClientFactory<TServiceClient> clientFactory;
    private ThriftServerProviderFactory providerFactory;
    private ThriftReference reference;
    private Class<?> referenceClass;
    private Class<TTransport> transportClass;
    private Class<TProtocol> protocolClass;
    private PoolOperationCallBack callback;

    private String service;
    private String version;
    private String serviceName;

    protected ThriftClientPoolFactory(ThriftServerProviderFactory providerFactory,
                                      TServiceClientFactory<TServiceClient> clientFactory,
                                      ThriftReference reference,
                                      Class<?> referenceClass,
                                      Class<TTransport> transportClass,
                                      Class<TProtocol> protocolClass) throws Exception {
        this.providerFactory = providerFactory;
        this.clientFactory = clientFactory;
        this.reference = reference;
        this.transportClass = transportClass;
        this.referenceClass = referenceClass;
        this.protocolClass = protocolClass;

        this.service = referenceClass.getName();
        this.version = reference.version();
        this.serviceName = StringUtils.isEmpty(reference.name()) ? referenceClass.getEnclosingClass().getSimpleName() : reference.name();
    }

    protected ThriftClientPoolFactory(ThriftServerProviderFactory providerFactory,
                                      TServiceClientFactory<TServiceClient> clientFactory,
                                      ThriftReference reference,
                                      Class<?> referenceClass,
                                      Class<TTransport> transportClass,
                                      Class<TProtocol> protocolClass,
                                      PoolOperationCallBack callback) throws Exception {
        this.providerFactory = providerFactory;
        this.clientFactory = clientFactory;
        this.callback = callback;
        this.reference = reference;
        this.transportClass = transportClass;
        this.referenceClass = referenceClass;
        this.protocolClass = protocolClass;

        this.service = referenceClass.getName();
        this.version = reference.version();
        this.serviceName = StringUtils.isEmpty(reference.name()) ? referenceClass.getEnclosingClass().getSimpleName() : reference.name();
    }

    @Override
    public TServiceClient makeObject() throws Exception {
        InetSocketAddress address = providerFactory.selector(service, version);
        TSocket tsocket = new TSocket(address.getHostName(), address.getPort());
        Constructor<TTransport> transportConstructor = Utils.getConstructorByParent(transportClass, TSocket.class);
        TTransport transport = transportConstructor.newInstance(tsocket);
        Constructor<TProtocol> protocolConstructor = Utils.getConstructorByParent(protocolClass, TTransport.class);
        TProtocol protocol = protocolConstructor.newInstance(transport);
        TMultiplexedProtocol mp = new TMultiplexedProtocol(protocol, serviceName);
        TServiceClient client = this.clientFactory.getClient(mp);
        transport.open();
        if (callback != null) {
            try {
                callback.make(client);
            } catch (Exception e) {
                log.warn("make callback client error", e);
            }
        }
        return client;
    }

    public void destroyObject(TServiceClient client) throws Exception {
        if (callback != null) {
            try {
                callback.destroy(client);
            } catch (Exception e) {
                log.warn("destroy callback client error", e);
            }
        }
        TTransport pin = client.getInputProtocol().getTransport();
        pin.close();
    }

    public boolean validateObject(TServiceClient client) {
        TTransport pin = client.getInputProtocol().getTransport();
        return pin.isOpen();
    }
}
