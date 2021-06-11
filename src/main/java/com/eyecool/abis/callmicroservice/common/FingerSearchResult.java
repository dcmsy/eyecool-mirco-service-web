package com.eyecool.abis.callmicroservice.common;

/**
 * 指纹1-N返回的对象
 */
public class FingerSearchResult {
    private String userId;
    private String featureId;
    private double score;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
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
