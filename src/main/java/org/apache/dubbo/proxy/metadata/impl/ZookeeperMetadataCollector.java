package org.apache.dubbo.proxy.metadata.impl;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.proxy.metadata.MetadataCollector;
import org.apache.dubbo.proxy.utils.Constants;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;

public class ZookeeperMetadataCollector implements MetadataCollector {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperMetadataCollector.class);
    private CuratorFramework client;
    private URL url;
    private String root;
    private final static String METADATA_NODE_NAME = "service.data";
    private final static String DEFAULT_ROOT = "dubbo";


    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public URL getUrl() {
        return this.url;
    }

    @Override
    public void init() {
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        root = group;
        client = CuratorFrameworkFactory.newClient(url.getAddress(), new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    @Override
    public String getProviderMetaData(MetadataIdentifier key) {
        return doGetMetadata(key);
    }

    private String getNodePath(MetadataIdentifier metadataIdentifier) {
        return toRootDir() + metadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.PATH) +
                Constants.PATH_SEPARATOR + METADATA_NODE_NAME;
    }

    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }

    private String doGetMetadata(MetadataIdentifier identifier) {
        //TODO error handing
        try {
            String path = getNodePath(identifier);
            if (client.checkExists().forPath(path) == null) {
                //截取掉/service.data,用于兼容新版本的dubbo 2.7.3+
                path = path.substring(0, path.lastIndexOf(METADATA_NODE_NAME) - 1);
                if (client.checkExists().forPath(path) == null) {
                    return null;
                }
            }
            return new String(client.getData().forPath(path));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
