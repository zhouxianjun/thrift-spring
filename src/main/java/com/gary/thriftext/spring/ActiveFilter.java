package com.gary.thriftext.spring;

import com.gary.thriftext.register.InvokerFilter;
import com.gary.thriftext.register.dto.RpcStatus;
import com.gary.thriftext.spring.annotation.ThriftFilter;
import org.apache.thrift.TServiceClient;

import java.lang.reflect.Method;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 2016/4/27 15:55
 */
@ThriftFilter
public class ActiveFilter implements InvokerFilter {
    @Override
    public boolean before(TServiceClient client, Object proxy, Method method, Object[] args) {
        RpcStatus.beginCount(proxy.getClass().getName());
        return true;
    }

    @Override
    public void after(Object proxy, long time, boolean success) {
        RpcStatus.endCount(proxy.getClass().getName(), time, success);
    }
}
