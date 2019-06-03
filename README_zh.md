[English Version](README.md)  
### Dubbo Proxy
Dubbo Proxy是一个Dubbo网关，可以将Http请求转换成Dubbo的协议，调用Dubbo服务并且返回结果，后续还会集成熔断，限流，api管理等功能。

### 用法
http请求格式如下： 
```
POST {application Name}/​{Interface name}?version={version}&group={group}
```
其中group和version是Dubbo服务对应的group和version，为可选参数

http POST body如下: 

```json
{
    "methodName" : "sayHello",
    "paramTypes" : ["org.apache.dubbo.demo.model.User"],
    "paramValues": [
        {
            "id": 23,
            "username": "testUser"
        }
    ]
}
```

* 在Dubbo 2.7及后续版本中，paramTypes为可选，如果不填写，Dubbo Proxy会在元数据中心获取对应的参数类型。
* 可以在`application.yml`中指定注册中心和元数据中心的地址
```
proxy.registry.address: zookeeper://127.0.0.1:2181   #注册中心地址，和Dubbo服务的注册中心相同
proxy.metadata-report.address: zookeeper://127.0.0.1:2181  #元数据中心的地址，未指定paramTypes时查找使用，支持Dubbo 2.7及以后版本
```
