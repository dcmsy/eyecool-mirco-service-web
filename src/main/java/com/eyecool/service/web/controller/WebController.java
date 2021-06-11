package com.eyecool.service.web.controller;

import cn.eyecool.match.service.commons.FeatureData;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.eyecool.abis.callmicroservice.IBioFaceMicroService;
import com.eyecool.abis.callmicroservice.IBioFingerMicroService;
import com.eyecool.abis.callmicroservice.IBioIrisMicroService;
import com.eyecool.abis.callmicroservice.common.*;
import com.eyecool.service.web.model.FaceModel;
import com.eyecool.service.web.model.ResultMsg;
import io.grpc.StatusRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author dcmsy
 *
 */
@RequestMapping("/callmicroservice")
@RestController
public class WebController {
    
    static Logger LOG = LoggerFactory.getLogger(WebController.class);

    private final AtomicLong requestSeqGen = new AtomicLong();

    @Autowired
    private IBioFaceMicroService iBioFaceMicroService;

    @Autowired
    private IBioIrisMicroService iBioIrisMicroService;

    @Autowired
    private IBioFingerMicroService iBioFingerMicroService;

    /**
     * 人脸提取特征
     * @param imageBase64
     * @return
     */
    @RequestMapping(value = {"/faceExteaction"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg faceExteaction(@RequestParam(name = "imageBase64") String imageBase64) {
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        FaceExtractResult faceExtractResult = null;
        try {
            faceExtractResult = iBioFaceMicroService.faceExteaction(node, handleSeq, imageBase64);
        } catch (Exception e) {
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        ResultMsg ret = ResultMsg.createOkMsg(faceExtractResult);
        String jsonStr = JSONObject.toJSONString(ret);
        LOG.info("=============faceExteaction:", jsonStr);
        return ret;
    }

    /**
     * 人脸提取特征
     * @param params
     * @return
     */
    @RequestMapping(value = {"/faceExteactionJson"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg faceExteactionJson(@RequestBody String params) {
        String imageBase64 = "";
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        FaceExtractResult faceExtractResult = null;
        FaceModel vo = JSON.parseObject(params,FaceModel.class);
        try {
            imageBase64 = vo.getImageBase64();
            faceExtractResult = iBioFaceMicroService.faceExteaction(node, handleSeq, imageBase64);
        } catch (Exception e) {
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        ResultMsg ret = ResultMsg.createOkMsg(faceExtractResult);
        String jsonStr = JSONObject.toJSONString(ret);
        LOG.info("=============faceExteaction:", jsonStr);
        return ret;
    }

    /**
     * 人脸提取特征
     * @param params
     * @return
     */
    @RequestMapping(value = {"/reFaceExteactionJson"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg reFaceExteactionJson(@RequestBody String params) {
        String imageBase64 = "";
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        FaceExtractResult faceExtractResult = null;
        FaceModel vo = JSON.parseObject(params,FaceModel.class);
        try {
            imageBase64 = vo.getImageBase64();
            faceExtractResult = iBioFaceMicroService.doubleFaceExteaction(node, handleSeq, imageBase64);
        } catch (Exception e) {
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        ResultMsg ret = ResultMsg.createOkMsg(faceExtractResult);
        String jsonStr = JSONObject.toJSONString(ret);
        LOG.info("=============reFaceExteactionJson:", jsonStr);
        return ret;
    }

    /**
     * 人脸(1:1)
     * @param feature0
     * @param feature1
     * @return
     */
    @RequestMapping(value = {"/faceMatch"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg faceMatch(@RequestParam(name = "feature0") String feature0, @RequestParam(name = "feature1") String feature1) {
        List<MatchBean> list = null;
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        if (StringUtils.isEmpty(feature0) || StringUtils.isEmpty(feature1)) {
            return ResultMsg.createOkMsg();
        }
        List<String> featureList = new ArrayList<>(0);
        featureList.add(feature0);
        featureList.add(feature1);
        List<FeatureBean> featureBeanList = new ArrayList<>();
        for (String feature : featureList) {
            FeatureBean featureBean = new FeatureBean();
            featureBean.setFeature(feature);
            featureBean.setType(String.valueOf(FeatureData.FeatureType.FaceFeature));
            featureBeanList.add(featureBean);
        }
        try {
            list = iBioFaceMicroService.faceMatch(node, handleSeq, new ArrayList<>(), featureBeanList);
        } catch (Exception e) {
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        double score = 0d;
        if (list != null && list.size() > 0) {
            MatchBean matchBean = list.get(0);
            score = matchBean.getResults().get(0).getScore();
        }
        ResultMsg ret = ResultMsg.createOkMsg(score);
        String jsonStr = JSONObject.toJSONString(ret);
        LOG.info("=============faceMatch:",jsonStr);
        return ret;
    }

    /**
     * 人脸(1:1)
     * @param featureList
     * @return
     */
    @RequestMapping(value = {"/faceMatchList"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg faceMatchList(@RequestBody List<String> featureList) {
        List<MatchBean> list;
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        if (featureList == null || featureList.size() == 0) {
            return ResultMsg.createOkMsg();
        }
        List<FeatureBean> featureBeanList = new ArrayList<>();
        for (String feature : featureList) {
            FeatureBean featureBean = new FeatureBean();
            featureBean.setType(String.valueOf(FeatureData.FeatureType.FaceFeature));
            featureBean.setFeature(feature);
            featureBeanList.add(featureBean);
        }
        try {
            list = iBioFaceMicroService.faceMatch(node, handleSeq, new ArrayList<>(), featureBeanList);
        } catch (Exception e) {
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        ResultMsg ret = ResultMsg.createOkMsg(list);
        String jsonStr = JSONObject.toJSONString(ret);
        LOG.info("=============faceMatchList:",jsonStr);
        return ret;
    }

    /**
     * 人脸(1:N)
     * @return
     */
    @RequestMapping(value = {"/faceMiniSearch"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg faceMiniSearch(@RequestParam(name = "feature") String feature, @RequestParam(name = "channelCode") String channelCode, @RequestParam(name = "threshold") Double threshold, @RequestParam(name = "topN") Integer topN) {
        String node = "";
        List<FaceSearchResult> list;
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();

        try {
            list = iBioFaceMicroService.faceMiniSearch(node, handleSeq, feature, channelCode, topN, threshold);
        } catch (Exception e) {
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        ResultMsg ret = ResultMsg.createOkMsg(list);
        String jsonStr = JSONObject.toJSONString(ret);
        LOG.info("=============faceMiniSearch:",jsonStr);
        return ret;
    }

    /**
     * 人脸(1:N)
     * @return
     */
    @RequestMapping(value = {"/reFaceMiniSearch"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg reFaceMiniSearch(@RequestParam(name = "feature") String feature, @RequestParam(name = "reFeature") String reFeature, @RequestParam(name = "channelCode") String channelCode, @RequestParam(name = "threshold") Double threshold, @RequestParam(name = "topN") Integer topN) {
        String node = "";
        List<FaceSearchResult> list;
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        try {
            list = iBioFaceMicroService.reFaceMiniSearch(node, handleSeq, feature, reFeature, channelCode, topN, threshold);
        } catch (Exception e) {
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        ResultMsg ret = ResultMsg.createOkMsg(list);
        String jsonStr = JSONObject.toJSONString(ret);
        LOG.info("=============faceMiniSearch:",jsonStr);
        return ret;
    }

    /**
     * 人脸(1:N)
     * @return
     */
    @RequestMapping(value = {"/doubleFaceMiniSearch"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg doubleFaceMiniSearch(@RequestParam(name = "userIds") String userIds, @RequestParam(name = "reFeature") String reFeature, @RequestParam(name = "channelCode") String channelCode, @RequestParam(name = "threshold") Double threshold, @RequestParam(name = "topN") Integer topN) {
        String node = "";
        List<FaceSearchResult> list;
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        try {
            list = iBioFaceMicroService.doubleFaceMiniSearch(node, handleSeq, userIds, reFeature, channelCode, topN, threshold);
        } catch (Exception e) {
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        ResultMsg ret = ResultMsg.createOkMsg(list);
        String jsonStr = JSONObject.toJSONString(ret);
        LOG.info("=============faceMiniSearch:",jsonStr);
        return ret;
    }

    /**
     * 人脸图像检活
     * @return
     */
    @RequestMapping(value = {"/faceImageLivenessDetection"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg faceImageLivenessDetection(@RequestParam(name = "imageBase64") String imageBase64, @RequestParam(name = "threshold") Double threshold) {
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        ResultMsg msg = ResultMsg.createOkMsg();
        boolean flag = false;
        try {
            flag = iBioFaceMicroService.faceImageLivenessDetection(handleSeq, imageBase64, threshold);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            msg = ResultMsg.createErrorMsg("无法连接到算法服务");
            LOG.error(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            msg = ResultMsg.createErrorMsg(e.toString());
            LOG.error(e.toString());
        }
        msg.setData(flag);
        return msg;
    }

    /**
     * 人脸图像检活
     * @return
     */
    @RequestMapping(value = {"/faceImageLivenessDetectionJson"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg faceImageLivenessDetectionJson(@RequestBody String params) {
        String imageBase64 = "";
        Double threshold = 80d;

        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        ResultMsg msg = ResultMsg.createOkMsg();
        boolean flag = false;
        try {
            FaceModel vo = JSON.parseObject(params,FaceModel.class);
            imageBase64 = vo.getImageBase64();
            if (StringUtils.isNotEmpty(vo.getThreshold())) {
                threshold = Double.parseDouble(vo.getThreshold());
            }
            flag = iBioFaceMicroService.faceImageLivenessDetection(handleSeq, imageBase64, threshold);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            msg = ResultMsg.createErrorMsg("无法连接到算法服务");
            LOG.error(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            msg = ResultMsg.createErrorMsg(e.toString());
            LOG.error(e.toString());
        }
        msg.setData(flag);
        return msg;
    }

    /**
     * 多人脸检测
     * @return
     */
    @RequestMapping(value = {"/multiFaceDetection"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg multiFaceDetection(@RequestParam(name = "imageBase64") String imageBase64) {
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        int result = 0;
        try {
            result = iBioFaceMicroService.multiFaceDetection(handleSeq, imageBase64);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        return ResultMsg.createOkMsg(result);
    }


    //*****************************虹膜*****************************
    /**
     * 虹膜提取特征
     * @return
     */
    @RequestMapping(value = {"/extractIrisFeature"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg extractIrisFeature(@RequestParam(name = "imageBase64") String imageBase64) {
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        //1.虹膜提取特征
        IrisExtractResult result = null;
        try {
            result = iBioIrisMicroService.extractIrisFeature(node, handleSeq, imageBase64);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        return ResultMsg.createOkMsg(result);
    }

    /**
     * 虹膜(1:1)
     * @return
     */
    @RequestMapping(value = {"/irisMatch"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg irisMatch(@RequestBody List<String> featureList) {
        List<MatchBean> list = null;
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        if (featureList == null || featureList.size() == 0) {
            ResultMsg.createOkMsg(list);
        }
        List<FeatureBean> featureBeanList = new ArrayList<>();
        for (String feature : featureList) {
            FeatureBean featureBean = new FeatureBean();
            featureBean.setFeature(feature);
            featureBean.setType(String.valueOf(FeatureData.FeatureType.IrisFeatureLeft));
            featureBeanList.add(featureBean);
        }

        try {
            list = iBioIrisMicroService.irisMatch(node, handleSeq, new ArrayList<>(), featureBeanList);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        return ResultMsg.createOkMsg(list);
    }

    /**
     * 虹膜(1:N)
     * @return
     */
    @RequestMapping(value = {"/irisMiniSearch"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg irisMiniSearch(@RequestParam(name = "imageBase64") String imageBase64,@RequestParam(name = "channelCode") String channelCode, @RequestParam(name = "eye") String eye, @RequestParam(name = "threshold") Double threshold, @RequestParam(name = "topN") Integer topN) {
        List<IrisSearchResult> list = null;
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();

        try {
            list = iBioIrisMicroService.irisMiniSearch(node, handleSeq, imageBase64, eye, channelCode, topN, threshold);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        return ResultMsg.createOkMsg(list);
    }

    //*****************************指纹*****************************
    /**
     * 指纹提取特征
     * @return
     */
    @RequestMapping(value = {"/fingerExteaction"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg fingerExteaction(@RequestParam(name = "imageBase64") String imageBase64) {
        String node = "";
        FingerExtractResult result = null;
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();

        try {
            result = iBioFingerMicroService.fingerExteaction(node, handleSeq, imageBase64);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        return ResultMsg.createOkMsg(result);
    }

    /**
     * 指纹(1:1)
     * @param featureList
     * @return
     */
    @RequestMapping(value = {"/fingerMatch"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg fingerMatch(@RequestBody List<String> featureList) {
        List<MatchBean> list = null;
        String node = "";
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();

        if (featureList == null || featureList.size() == 0) {
            ResultMsg.createOkMsg(list);
        }
        List<FeatureBean> featureBeanList = new ArrayList<>();
        for (String feature : featureList) {
            FeatureBean featureBean = new FeatureBean();
            featureBean.setFeature(feature);
            featureBean.setType(String.valueOf(FeatureData.FeatureType.FingerFeatureUnknown));
            featureBeanList.add(featureBean);
        }

        try {
            list = iBioFingerMicroService.fingerMatch(node, handleSeq, new ArrayList<>(), featureBeanList);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        return ResultMsg.createOkMsg(list);
    }

    /**
     * 指纹(1:N)
     * @param imageBase64
     * @param channelCode
     * @param threshold
     * @param topN
     * @return
     */
    @RequestMapping(value = {"/fingerMiniSearch"}, method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResultMsg fingerMiniSearch(@RequestParam(name = "imageBase64") String imageBase64,@RequestParam(name = "channelCode") String channelCode, @RequestParam(name = "threshold") Double threshold, @RequestParam(name = "topN") Integer topN) {
        String node = "";
        List<FingerSearchResult> list = null;
        String handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();

        try {
            list = iBioFingerMicroService.fingerMiniSearch(node, handleSeq, imageBase64, channelCode, topN, threshold);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.toString());
            return ResultMsg.createErrorMsg(e.toString());
        }
        return ResultMsg.createOkMsg(list);
    }

}