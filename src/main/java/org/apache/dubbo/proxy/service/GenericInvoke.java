package org.apache.dubbo.proxy.service;

import org.apache.dubbo.proxy.utils.ResultCode;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GenericInvoke {

    private static ApplicationConfig applicationConfig;
    private static volatile AtomicBoolean init = new AtomicBoolean(false);

    private static Registry registry;

    public static void setRegistry(Registry registry) {
        GenericInvoke.registry = registry;
    }

    public static void init() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress(registry.getUrl().getProtocol() + "://" + registry.getUrl().getAddress());
        applicationConfig = new ApplicationConfig();
        applicationConfig.setName("dubbo-proxy");
        applicationConfig.setRegistry(registryConfig);
    }

    private static ConcurrentHashMap<String, ReferenceConfig> cachedConfig = new ConcurrentHashMap<>();
    private static Logger logger = LoggerFactory.getLogger(GenericInvoke.class);

    public static Object genericCall(String interfaceName, String group,
                                     String version, String methodName, String[] paramTypes,
                                     Object[] paramObjs) {
        if (init.compareAndSet(false, true)) {
            init();
        }
        ReferenceConfig<GenericService> reference = null;
        reference = addNewReference(interfaceName, group, version);

        try {
            GenericService svc = reference.get();
            logger.info("dubbo generic invoke, service is {}, method is {} , paramTypes is {} , paramObjs is {} , svc" +
                            " is {}.", interfaceName
                    , methodName,paramTypes,paramObjs,svc);
            Object result = svc.$invoke(methodName, paramTypes, paramObjs);
            return result;
        } catch (Exception e) {
            logger.error("Generic invoke failed",e);
            if (e instanceof RpcException) {
                RpcException e1 = (RpcException)e;
                if (e1.isTimeout()) {
                    return ResultCode.TIMEOUT;
                }
                if (e1.isBiz()) {
                    return ResultCode.BIZERROR;
                }
                if (e1.isNetwork()) {
                    return ResultCode.NETWORKERROR;
                }
                if (e1.isSerialization()) {
                    return ResultCode.SERIALIZATION;
                }
            }
            throw e;
        }
    }

    private static ReferenceConfig addNewReference(String interfaceName,
                                                     String group, String version) {
        ReferenceConfig reference;
        String cachedKey = interfaceName + group + version;
        reference = cachedConfig.get(cachedKey);
        if (reference == null) {
            ReferenceConfig<GenericService> newReference = initReference(interfaceName, group,
                    version);
            ReferenceConfig<GenericService> oldReference = cachedConfig.putIfAbsent(cachedKey, newReference);
            if (oldReference != null) {
                reference = oldReference;
            } else {
                reference = newReference;
            }
        }
        return reference;
    }

    private static ReferenceConfig initReference(String interfaceName, String group,
                                                String version) {
        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        reference.setGeneric(true);
        reference.setApplication(applicationConfig);
        reference.setGroup(group);
        reference.setVersion(version);
        reference.setInterface(interfaceName);
        return reference;
    }
}
