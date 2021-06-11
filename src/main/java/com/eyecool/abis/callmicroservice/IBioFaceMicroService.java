package com.eyecool.abis.callmicroservice;

import com.eyecool.abis.callmicroservice.common.*;

import java.util.List;

/**
 * 基于V版本人脸微服务
 *
 * @author sanmu
 * @date 2019-05-13
 */
public interface IBioFaceMicroService {

    /**
     * 人脸提取特征
     *
     * @param node        节点
     * @param handleSeq   平台流水，便于跟踪日志
     * @param imageBase64 图片的base64
     * @return feature    图片特征
     */
    public FaceExtractResult faceExteaction(String node, String handleSeq, String imageBase64);

    /**
     * 人脸提取特征
     *
     * @param node        节点
     * @param handleSeq   平台流水，便于跟踪日志
     * @param imageBase64 图片的base64
     * @return feature    图片特征
     */
    public FaceExtractResult doubleFaceExteaction(String node, String handleSeq, String imageBase64);

    /**
     * 人脸提取特征
     *
     * @param node        节点
     * @param handleSeq   平台流水，便于跟踪日志
     * @param imageBase64 图片的base64
     * @return feature    图片特征
     */
    public String reFaceExteaction(String node, String handleSeq, String imageBase64);

    /**
     * 人脸(1:1)
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
    public List<MatchBean> faceMatch(String node, String handleSeq, List<ImageBean> imageBeanList, List<FeatureBean> featureBeanList);


    /**
     * 人脸(1:N)
     *
     * @param node        节点
     * @param handleSeq   平台流水，便于跟踪日志
     * @param feature     图片特征
     * @param channelCode 渠道编码   如果空串""则全库大N搜索;如果是具体渠道编码则代表小n
     * @param topN        找出前N个
     * @param threshold   阈值
     * @return 1-N返回的对象集合
     */
    public List<FaceSearchResult> faceMiniSearch(String node, String handleSeq, String feature, String channelCode, int topN, double threshold);

    /**
     * 人脸(1:N)
     *
     * @param node        节点
     * @param handleSeq   平台流水，便于跟踪日志
     * @param feature     图片特征
     * @param channelCode 渠道编码   如果空串""则全库大N搜索;如果是具体渠道编码则代表小n
     * @param topN        找出前N个
     * @param threshold   阈值
     * @return 1-N返回的对象集合
     */
    public List<FaceSearchResult> reFaceMiniSearch(String node, String handleSeq, String feature, String reFeature, String channelCode, int topN, double threshold);

    /**
     * 人脸(1:N)
     *
     * @param node        节点
     * @param handleSeq   平台流水，便于跟踪日志
     * @param userIds     图片特征
     * @param channelCode 渠道编码   如果空串""则全库大N搜索;如果是具体渠道编码则代表小n
     * @param topN        找出前N个
     * @param threshold   阈值
     * @return 1-N返回的对象集合
     */
    public List<FaceSearchResult> doubleFaceMiniSearch(String node, String handleSeq, String userIds, String reFeature, String channelCode, int topN, double threshold);

    /**
     * 人脸视频检活
     *
     * @param handleSeq 平台流水，便于跟踪日志
     * @param videoPath 视频路径
     * @param threshold 视频检活阈值
     * @return 视频检活结果
     */
    public boolean faceVideoLivenessDetection(String handleSeq, String videoPath, double threshold) throws Exception;


    /**
     * 人脸图像检活
     *
     * @param handleSeq   平台流水，便于跟踪日志
     * @param imageBase64 图片的base64
     * @param threshold   图像检活阈值
     * @return 图像检活结果
     */
    public boolean faceImageLivenessDetection(String handleSeq, String imageBase64, double threshold) throws Exception;


    /**
     * 多人脸检测
     *
     * @param handleSeq   平台流水，便于跟踪日志
     * @param imageBase64 图片的base64
     * @return faceCount 人脸个数
     */
    public int multiFaceDetection(String handleSeq, String imageBase64) throws Exception;


    /**
     * 业务增删改之后，触发内存数据同步
     *
     * @param handleSeq 平台流水，便于跟踪日志
     * @return
     */
    //public void load(String handleSeq);


}
