package org.apache.dubbo.proxy.metadata.impl;


import org.apache.dubbo.proxy.metadata.MetadataCollector;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.dubbo.proxy.utils.Constants;

import java.util.Properties;

import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;

public class NacosMetadataCollector implements MetadataCollector {
    private static final Logger logger = LoggerFactory.getLogger(NacosMetadataCollector.class);
    private ConfigService configService;
    private String group;
    private URL url;
    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void init() {
        group = url.getParameter(Constants.GROUP_KEY, "DEFAULT_GROUP");

        configService = buildConfigService(url);
    }

    private ConfigService buildConfigService(URL url) {
        Properties nacosProperties = buildNacosProperties(url);
        try {
            configService = NacosFactory.createConfigService(nacosProperties);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        return configService;
    }

    private Properties buildNacosProperties(URL url) {
        Properties properties = new Properties();
        setServerAddr(url, properties);
        return properties;
    }

    private void setServerAddr(URL url, Properties properties) {

        String serverAddr = url.getHost() + // Host
                ":" +
                url.getPort() // Port
                ;
        properties.put(SERVER_ADDR, serverAddr);
    }

    @Override
    public String getProviderMetaData(MetadataIdentifier key) {
        return getMetaData(key);
    }

//    @Override
    public String getConsumerMetaData(MetadataIdentifier key) {
        return getMetaData(key);
    }

    private String getMetaData(MetadataIdentifier identifier) {
        try {
            return configService.getConfig(identifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY),
                    group, 1000 * 10);
        } catch (NacosException e) {
            logger.warn("Failed to get " + identifier + " from nacos, cause: " + e.getMessage(), e);
        }
        return null;
    }
}

