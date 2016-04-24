package com.gary.thriftext.spring;

import org.apache.thrift.TServiceClient;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description: 连接池事件
 * @date 16-4-23 上午12:07
 */
public interface PoolOperationCallBack {
    // 销毁client之前执行
    void destroy(TServiceClient client);

    // 创建成功时执行
    void make(TServiceClient client);
}
