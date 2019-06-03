## Dubbo Proxy
[中文版本](README_zh.md)
Dubbo Proxy, a gateway of Dubbo, switch from HTTP request to Dubbo protocol，then invoke Dubbo service and return to the result. Later Dubbo Proxy would combine several features, including circuit breaker, current-limiting, api management. 


### instructions
 
HTTP request format:

```
{application Name}/​{Interface name}?version={version}&group={group}
```
Group and version is the mapping data in Dubbo service. 

http POST body: 

```json
{
    "methodName" : "sayHello",
    "paramTypes" : ["org.apache.dubbo.demo.model.User"],
    "paramValues": [
        {
            "id": 23,
            "username": "fwjoifjwie"
        }
    ]
}
```

* In the Dubbo 2.7 version  and later updates versions, paramTypes is optional data, if not filled in, Dubbo Proxy would get related mapping data from metadata center.
* You can set registry address and metadata center address in `application.yml`
```
proxy.registry.address: zookeeper://127.0.0.1:2181   #registry center address, same as Dubbo service's 
proxy.metadata-report.address: zookeeper://127.0.0.1:2181  #metadata center address, used by paramType search, support for dubbo 2.7 or later
```
