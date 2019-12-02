package org.apache.dubbo.proxy;

import org.apache.dubbo.proxy.dao.ServiceMapping;
import org.apache.dubbo.proxy.metadata.MetadataCollector;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@ConfigurationProperties(prefix = "mapping")
@Configuration
public class Config {


    @Value("${proxy.registry.address}")
    private String registryAddress;

    @Value("${proxy.registry.group}")
    private String group;

    @Value("${proxy.metadata-report.address:}")
    private String metadataAddress;

    private List<Mapping> services;

    public List<Mapping> getServices() {
        return services;
    }

    public void setServices(List<Mapping> services) {
        this.services = services;
    }

    @Bean
    public ServiceMapping getServiceMapping() {
        ServiceMapping serviceMapping = new ServiceMapping();
        serviceMapping.setMappings(services);
        return serviceMapping;
    }

    @Bean
    Registry getRegistry() {
        URL url = URL.valueOf(registryAddress);
        if (StringUtils.isNotEmpty(group)) {
            url = url.addParameter(org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY, group);
        }
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
        Registry registry = registryFactory.getRegistry(url);
        return registry;
    }

    @Bean
    MetadataCollector getMetadataCollector() {
        MetadataCollector metaDataCollector = null;
        if (StringUtils.isNotEmpty(metadataAddress)) {
            URL metadataUrl = URL.valueOf(metadataAddress);
            metaDataCollector = ExtensionLoader.getExtensionLoader(MetadataCollector.class).
                    getExtension(metadataUrl.getProtocol());
            metaDataCollector.setUrl(metadataUrl);
            metaDataCollector.init();
        }
        return metaDataCollector;
    }



    public static class Mapping {
        private String name;
        private String interfaze;
        private String group;
        private String version;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getInterfaze() {
            return interfaze;
        }

        public void setInterfaze(String interfaze) {
            this.interfaze = interfaze;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
