package com.eyecool.abis.callmicroservice.common;

import cn.eyecool.match.service.commons.FeatureData;

public abstract class BioBean {

    private FeatureData.FeatureType type;

    public FeatureData.FeatureType getType() {
        return type;
    }

    public void setType(String type) {
        this.type = FeatureData.FeatureType.valueOf(type);
    }
}
