package com.eyecool.abis.callmicroservice.common;

import java.util.List;

public class IrisExtractResult {
    private List<String> features;

    //算法类型
    private String algType;
    //算法版本
    private String algVersion;
    //特征版本
    private String featVersion;
    //节点标识
    private String serverId;
    //服务器标识
    private String host;

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public String getAlgType() {
        return algType;
    }

    public void setAlgType(String algType) {
        this.algType = algType;
    }

    public String getAlgVersion() {
        return algVersion;
    }

    public void setAlgVersion(String algVersion) {
        this.algVersion = algVersion;
    }

    public String getFeatVersion() {
        return featVersion;
    }

    public void setFeatVersion(String featVersion) {
        this.featVersion = featVersion;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
