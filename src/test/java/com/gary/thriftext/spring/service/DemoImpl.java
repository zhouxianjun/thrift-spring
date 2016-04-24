package com.gary.thriftext.spring.service;

import com.gary.thrift.Demo;
import com.gary.thriftext.spring.annotation.ThriftService;
import org.apache.thrift.TException;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 16-4-23 上午1:41
 */
@ThriftService
public class DemoImpl implements Demo.Iface {
    @Override
    public String say(String name) throws TException {
        return "hello " + name;
    }
}
