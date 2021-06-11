package com.eyecool.abis.callmicroservice.common;

import java.util.List;

public class MatchBean {
    private int left;
    private FeatureBean feature;
    private List<Result> results;
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

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public FeatureBean getFeature() {
        return feature;
    }

    public void setFeature(FeatureBean feature) {
        this.feature = feature;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
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

    public static class Result {
        private int right;
        private double score;

        public int getRight() {
            return right;
        }

        public void setRight(int right) {
            this.right = right;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }
}
