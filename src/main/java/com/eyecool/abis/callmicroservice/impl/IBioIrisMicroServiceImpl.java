package com.eyecool.abis.callmicroservice.impl;

import cn.eyecool.abis.match.service.FeatureExtraction;
import cn.eyecool.abis.match.service.FeatureMatch;
import cn.eyecool.grpc.annotation.GrpcServiceConsumer;
import cn.eyecool.grpc.constants.Constant;
import cn.eyecool.grpc.function.Predicate;
import cn.eyecool.match.service.commons.*;
import cn.eyecool.match.service.extraction.ExtractionReply;
import cn.eyecool.match.service.extraction.ExtractionRequest;
import cn.eyecool.match.service.extraction.MultiExtractionReply;
import cn.eyecool.match.service.face.FaceParam;
import cn.eyecool.match.service.match.*;
import cn.eyecool.minisearch.service.search.CompareResult;
import cn.eyecool.minisearch.service.search.DataSearchServiceGrpc;
import cn.eyecool.minisearch.service.search.SearchRequest;
import cn.eyecool.minisearch.service.search.SearchResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.eyecool.abis.callmicroservice.IBioIrisMicroService;
import com.eyecool.abis.callmicroservice.common.*;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于V版本虹膜微服务服务层实现
 *
 * @author sanmu
 * @date 2019-06-10
 */
@Service
public class IBioIrisMicroServiceImpl implements IBioIrisMicroService {

    private static final Logger LOG = LoggerFactory.getLogger(IBioIrisMicroServiceImpl.class);

    private final Requester requester;

    private final AtomicLong requestSeqGen;

    @Autowired
    private FeatureExtraction featureExtraction;

    @Autowired
    private FeatureMatch featureMatch;

    @GrpcServiceConsumer(type = GrpcServiceConsumer.ConsumerType.BLOCKING)
    private DataSearchServiceGrpc.DataSearchServiceBlockingStub dataSearchStub;

    public IBioIrisMicroServiceImpl() {
        //Requester作为跟踪选项，用于跟踪一条调用的整个生命周期。
        requester = Requester.newBuilder()
                .setCaller(
                        ServerInfo.newBuilder()
                                //必选，本应用的提供商
                                .setVendor("eyecool.cn")
                                //必选，本应用启动时间
                                .setStartedAt(System.currentTimeMillis())
                                //必选，本应用ID，需唯一
                                .setServerId("Iris-1:1-Server")
                                //必选，本应用IP地址
                                .setHost("127.0.0.1")
                                //必选，本应用服务端口号
                                .setPort(8080)
                                //可选，算法类型
                                .setAlgType("LyIris")
                                //可选，算法版本
                                .setAlgVersion("1.0.20")
                                //可选，特征版本
                                .setFeatVersion("1.0.20")
                                .build()
                )
                .build();

        requestSeqGen = new AtomicLong();
    }


    @Override
    public IrisExtractResult extractIrisFeature(String node, String handleSeq, String base64) {
        long startTime = System.currentTimeMillis();
        String feature = "";
        //如果不指定node，则进行负载；如果指定node，则路由到指定的节点进行调用。
        Predicate predicate = Strings.isNullOrEmpty(node) ? Predicate.Any : Predicate.Tag.build(node);
        //平台流水号，必选
        if (Strings.isNullOrEmpty(handleSeq)) {
            handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        }
        byte[] imageContent = Base64.getMimeDecoder().decode(base64);

        //LOG.info("imageContent md5 {}", org.apache.commons.codec.digest.DigestUtils.md5Hex(imageContent));

        //创建特征提取请求
        ExtractionRequest extractionRequest = ExtractionRequest.newBuilder()
                .setRequester(
                        //Requester.newBuilder(requester)，从持久化参数requester中拷贝相关内容。
                        //设置请求流水及请求时间，必选
                        Requester.newBuilder(requester).setRequestSequence(handleSeq).setRequestTime(System.currentTimeMillis()).build()
                )
                //指定要提取的特征类型，必选
//                .setFingerParam(
//                        FingerParam.newBuilder()
//                                .setType(request.getType())
//                                .build()
//                )
                //指定图像内容，必选
                .addImage(
                        Image.newBuilder()
                                .setImageBytes(
                                        //如果无法保证imageContent内容是不变的，则需要使用ByteString.copyFrom(byte[])进行一次内存拷贝
                                        UnsafeByteOperations.unsafeWrap(imageContent)
                                )
                                .build()
                )
                .build();
        //进行特征提取
        MultiExtractionReply features = featureExtraction.extract(predicate, extractionRequest);
        int replyCount = features.getReplyCount();

        //rpc调用跟踪
        ApiCallInfo apiCallInfo = features.getApiCallInfo();
        IrisExtractResult irisExtractResult = new IrisExtractResult();
        String algType = apiCallInfo.getProvider().getAlgType();
        String algVersion = apiCallInfo.getProvider().getAlgVersion();
        String featVersion = apiCallInfo.getProvider().getFeatVersion();
        String serverId = apiCallInfo.getProvider().getServerId();
        String host = apiCallInfo.getProvider().getHost();
        List<String> featureList = new ArrayList<>();
        if (replyCount == 0) {
            //未提取到特征
            irisExtractResult.setFeatures(featureList);
            irisExtractResult.setAlgType(algType);
            irisExtractResult.setAlgVersion(algVersion);
            irisExtractResult.setFeatVersion(featVersion);
            irisExtractResult.setServerId(serverId);
            irisExtractResult.setHost(host);
            return irisExtractResult;
        }
        LOG.info("{}, RECV: apiCallInfo --> {}", handleSeq, apiCallInfo);

        for (int i = 0; i < replyCount; i++) {
            ExtractionReply reply = features.getReply(i);
            FeatureData featureData = reply.getFeature();
            //获取结果
            feature = java.util.Base64.getEncoder().encodeToString(featureData.getData().toByteArray());
            featureList.add(feature);
        }
        irisExtractResult.setFeatures(featureList);
        irisExtractResult.setAlgType(algType);
        irisExtractResult.setAlgVersion(algVersion);
        irisExtractResult.setFeatVersion(featVersion);
        irisExtractResult.setServerId(serverId);
        irisExtractResult.setHost(host);
        LOG.info("提取特征-平台流水号[{}] 特征 [{}] use time [{}ms]", handleSeq, JSON.toJSONString(irisExtractResult, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return irisExtractResult;
    }


    @Override
    public List<MatchBean> irisMatch(String node, String handleSeq, List<ImageBean> imageBeanList, List<FeatureBean> featureBeanList) {
        long startTime = System.currentTimeMillis();
        // 如果不指定node，则进行负载；如果指定node，则路由到指定的节点进行调用。
        Predicate predicate = Strings.isNullOrEmpty(node) ? Predicate.Any : Predicate.Tag.build(node);
        // 平台流水号，必选
        if (Strings.isNullOrEmpty(handleSeq)) {
            handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        }
        // 设置比对请求
        // 比对请求可以包含图像数组和特征数组，将图像数组提取特征后，进行互比。
        MultiMatchRequest.Builder multiMatchRequestBuilder = MultiMatchRequest.newBuilder().setRequester(
                // Requester.newBuilder(requester)，从持久化参数requester中拷贝相关内容。
                // 设置请求流水及请求时间，必选
                cn.eyecool.match.service.commons.Requester.newBuilder(requester).setRequestSequence(handleSeq).setRequestTime(System.currentTimeMillis())
                        .build());
        // 设置所有图像数组数据
        for (int i = 0; i < imageBeanList.size(); i++) {
            ImageBean request = imageBeanList.get(i);
            byte[] imageContent = java.util.Base64.getMimeDecoder().decode(request.getImage());
            LOG.info("{}, SEND: index-->{},  node --> {}, image --> {} bytes", handleSeq, i, node, imageContent == null ? "null" : imageContent.length);
            if (imageContent == null) {
                throw new IllegalArgumentException(handleSeq + ", index : " + i + ", ImageContent is null");
            }
            if (imageContent.length == 0) {
                throw new IllegalArgumentException(handleSeq + ", index : " + i + ", ImageContent is empty");
            }
            // 添加到请求构建器中。
            multiMatchRequestBuilder.addRequest(MatchRequest.newBuilder().setImageRequest(MatchImageRequest.newBuilder().setFaceParam(FaceParam.newBuilder()).
                    setImage(cn.eyecool.match.service.commons.Image.newBuilder().setImageBytes(UnsafeByteOperations.unsafeWrap(imageContent)).build()).build()).build());
        }
        // 设置所有的特征数组
        for (int i = 0; i < featureBeanList.size(); i++) {
            FeatureBean request = featureBeanList.get(i);
            byte[] featureContent = java.util.Base64.getMimeDecoder().decode(request.getFeature());
            LOG.info("{}, SEND: index --> {}, node --> {}, feature --> {} bytes", handleSeq, i, node, featureContent == null ? "null" : featureContent.length);
            if (featureContent == null) {
                throw new IllegalArgumentException(handleSeq + ", index: " + i + ", FeatureContent is null");
            }
            if (featureContent.length == 0) {
                throw new IllegalArgumentException(handleSeq + ", index: " + i + ", FeatureContent is empty");
            }
            // 添加到请求构建器中。
            multiMatchRequestBuilder.addRequest(MatchRequest.newBuilder()
                    .setFeatureRequest(MatchFeatureRequest.newBuilder().setFeature(FeatureData.newBuilder().setFeatureType(request.getType())
                            .setData(UnsafeByteOperations.unsafeWrap(featureContent)).build()).build()).build());
        }
        // 构建请求
        MultiMatchRequest multiMatchRequest = multiMatchRequestBuilder.build();
        if (LOG.isTraceEnabled()) {
            LOG.trace("{}, SEND: extractionRequest --> {}", handleSeq, multiMatchRequest);
        }
        // rpc调用进行图像或特征比对。
        MultiMatchReply results = featureMatch.match(predicate, multiMatchRequest);
        ApiCallInfo apiCallInfo = results.getApiCallInfo();
        LOG.info("{}, RECV: apiCallInfo --> {},used time {}ms", handleSeq, apiCallInfo, System.currentTimeMillis() - startTime);
        List<MatchBean> beans = new ArrayList<>();
        String algType = apiCallInfo.getProvider().getAlgType();
        String algVersion = apiCallInfo.getProvider().getAlgVersion();
        String featVersion = apiCallInfo.getProvider().getFeatVersion();
        String serverId = apiCallInfo.getProvider().getServerId();
        String host = apiCallInfo.getProvider().getHost();
        for (int i = 0; i < results.getReplyCount(); i++) {
            MatchReply matchReply = results.getReply(i);
            int left = matchReply.getIndex();
            List<MatchBean.Result> matchResults = new ArrayList<>();
            for (int j = 0; j < matchReply.getResultCount(); j++) {
                MatchResult result = matchReply.getResult(j);
                double score = result.getScore();
                int right = result.getIndex();
                MatchBean.Result matchResult = new MatchBean.Result();
                matchResult.setRight(right);
                matchResult.setScore(score);
                matchResults.add(matchResult);
            }
            MatchBean bean = new MatchBean();
            bean.setLeft(left);
            bean.setResults(matchResults);
            if (matchReply.hasFeature()) {
                FeatureBean featureBean = new FeatureBean();
                featureBean.setType(matchReply.getFeature().getFeatureType().name());
                featureBean.setFeature(java.util.Base64.getEncoder().encodeToString(matchReply.getFeature().getData().toByteArray()));
                bean.setFeature(featureBean);
            }
            bean.setAlgType(algType);
            bean.setAlgVersion(algVersion);
            bean.setFeatVersion(featVersion);
            bean.setServerId(serverId);
            bean.setHost(host);
            beans.add(bean);
        }
        LOG.info("虹膜Match-平台流水号{} 大小{} 结果{} use time [{}ms]", handleSeq, beans.size(), JSON.toJSONString(beans, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return beans;
    }

    @Override
    public List<IrisSearchResult> irisMiniSearch(String node, String handleSeq, String feature, String eye, String channelCode, int topN, double threshold) {
        long startTime = System.currentTimeMillis();
        //平台流水号，必选
        if (Strings.isNullOrEmpty(handleSeq)) {
            handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        }
        //特征类型
        cn.eyecool.minisearch.service.commons.FeatureData.FeatureType featureType = "1".equals(eye) ? cn.eyecool.minisearch.service.commons.FeatureData.FeatureType.IrisFeatureRight : cn.eyecool.minisearch.service.commons.FeatureData.FeatureType.IrisFeatureLeft;
        //创建查询请求
        SearchRequest searchRequest = SearchRequest.newBuilder()
                .setN(topN)
                .setLibrary(channelCode)
                .setThreshold(threshold)
                .setFeatureData(
                        cn.eyecool.minisearch.service.commons.FeatureData.newBuilder()
                                .setFeatureType(featureType)
                                .setData(ByteString.copyFrom(java.util.Base64.getMimeDecoder().decode(feature)))
                                .build()
                )
                .build();

        //查询结果
        //如果不指定node，则进行负载；如果指定node，则路由到指定的节点进行调用。
        SearchResult result = Strings.isNullOrEmpty(node) ? dataSearchStub.search(
                SearchRequest.newBuilder(searchRequest)
                        .setRequester(
                                cn.eyecool.minisearch.service.commons.Requester.newBuilder()
                                        .setRequestSequence(handleSeq)
                                        .build()
                        )
                        .build()
        ) : dataSearchStub.withOption(
                Constant.channelPredicateOption,
                Predicate.Tag.build(node)
        ).search(
                SearchRequest.newBuilder(searchRequest)
                        .setRequester(
                                cn.eyecool.minisearch.service.commons.Requester.newBuilder()
                                        .setRequestSequence(handleSeq)
                                        .build()
                        )
                        .build()
        );
        //查询结果集
        List<CompareResult> compareResultList = result.getResultList();
        String userId = "";
        //虹膜1-N对象集合
        List<IrisSearchResult> results = new ArrayList<>();
        IrisSearchResult irisSearchResult = null;
        if (compareResultList != null && compareResultList.size() > 0) {
            cn.eyecool.minisearch.service.commons.ApiCallInfo apiCallInfo = result.getApiCallInfo();
            String algType = apiCallInfo.getProvider().getAlgType();
            String algVersion = apiCallInfo.getProvider().getAlgVersion();
            String featVersion = apiCallInfo.getProvider().getFeatVersion();
            String serverId = apiCallInfo.getProvider().getServerId();
            String host = apiCallInfo.getProvider().getHost();
            for (CompareResult compareResult : compareResultList) {
                irisSearchResult = new IrisSearchResult();
                double score = compareResult.getScore();
                cn.eyecool.minisearch.service.commons.PeopleFeature peopleFeature = compareResult.getPeopleFeature();
                userId = peopleFeature.getId();
                String featureId = peopleFeature.getFeature(0).getId();
                irisSearchResult.setUserId(userId);
                irisSearchResult.setFeatureId(featureId);
                irisSearchResult.setScore(score);
                irisSearchResult.setAlgType(algType);
                irisSearchResult.setAlgVersion(algVersion);
                irisSearchResult.setFeatVersion(featVersion);
                irisSearchResult.setServerId(serverId);
                irisSearchResult.setHost(host);
                results.add(irisSearchResult);
            }
        }
        LOG.info("虹膜MiniSearch-平台流水号{} 结果{} use time [{}ms]", handleSeq, JSON.toJSONString(results, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return results;
    }
}
