package com.eyecool.service.web.model;

import java.io.Serializable;

/**
 * FaceModel
 */
public class FaceModel implements Serializable {

    /**
     * 默认的序列化ID
     */
    private static final long serialVersionUID = 1L;
    private String imageBase64;
    private String threshold;

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }
}
