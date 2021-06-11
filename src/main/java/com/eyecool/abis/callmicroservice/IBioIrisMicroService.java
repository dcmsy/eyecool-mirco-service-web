package com.eyecool.abis.callmicroservice;

import com.eyecool.abis.callmicroservice.common.*;

import java.util.List;

/**
 * 基于V版本虹膜微服务
 *
 * @author sanmu
 * @date 2019-06-10
 */
public interface IBioIrisMicroService {


    /**
     * 虹膜数据提取特征
     *
     * @param node      节点
     * @param handleSeq 平台流水，便于跟踪日志
     * @param base64    虹膜照片base64流
     *                  只能进行单张提取
     * @return feature 特征
     */
    public IrisExtractResult extractIrisFeature(String node, String handleSeq, String base64);

    /**
     * 虹膜(1:1)
     *
     * @param node            节点
     * @param handleSeq       平台流水，便于跟踪日志
     * @param imageBeanList   图像数组数据
     * @param featureBeanList 特征数组数据
     * @return 1-1返回的对象集合
     * // 对结果进行遍历，例如请求参数为3个图像或特征，则结果集为
     * //	            [
     * //	              {
     * //	                "left": 0,
     * //	                "feature": "提取的特征内容，可能为空，通过hasFeature()进行判断",
     * //	                "results": [
     * //	                  {
     * //	                    "right": 1,
     * //	                    "score": 100.0
     * //	                  },
     * //	                  {
     * //	                    "right": 2,
     * //	                    "score": 100.0
     * //	                  }
     * //	                ]
     * //	              },
     * //	              {
     * //	                "left": 1,
     * //	                "feature": "提取的特征内容，可能为空，通过hasFeature()进行判断",
     * //	                "results": [
     * //	                  {
     * //	                    "right": 2,
     * //	                    "score": 100.0
     * //	                  }
     * //	                ]
     * //	              }
     * //	            ]
     */
    public List<MatchBean> irisMatch(String node, String handleSeq, List<ImageBean> imageBeanList, List<FeatureBean> featureBeanList);


    /**
     * 虹膜(1:N)
     *
     * @param node        节点
     * @param handleSeq   平台流水，便于跟踪日志
     * @param feature     虹膜特征
     * @param eye         左右眼  1_右眼;2_左眼
     * @param channelCode 渠道编码   如果空串""则全库大N搜索;如果是具体渠道编码则代表小n
     * @param topN        找出前N个
     * @param threshold   阈值
     * @return
     */
    public List<IrisSearchResult> irisMiniSearch(String node, String handleSeq, String feature, String eye, String channelCode, int topN, double threshold);


}
