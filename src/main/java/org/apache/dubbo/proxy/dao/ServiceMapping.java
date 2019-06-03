package org.apache.dubbo.proxy.dao;

import org.apache.dubbo.proxy.Config;

import java.util.List;

public class ServiceMapping {

    List<Config.Mapping> mappings;

    public List<Config.Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Config.Mapping> mappings) {
        this.mappings = mappings;
    }
}
