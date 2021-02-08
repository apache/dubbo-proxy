package org.apache.dubbo.proxy.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;

@SPI("zookeeper")
public interface MetadataCollector {
    void setUrl(URL url);

    URL getUrl();

    void init();

    String getProviderMetaData(MetadataIdentifier key);
}
