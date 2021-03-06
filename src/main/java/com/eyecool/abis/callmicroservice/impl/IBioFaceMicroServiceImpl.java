package com.eyecool.abis.callmicroservice.impl;

import cn.eyecool.abis.match.service.Detecter;
import cn.eyecool.abis.match.service.FeatureExtraction;
import cn.eyecool.abis.match.service.FeatureMatch;
import cn.eyecool.checklive.grpc.face.*;
import cn.eyecool.fox.minifeaturemng.DBBasedFeatureManager;
import cn.eyecool.fox.minifeaturemng.FileNameBasedUserIdParser;
import cn.eyecool.fox.minimatch.MiniMatchConfig;
import cn.eyecool.fox.minimatch.MiniMatchService;
import cn.eyecool.grpc.annotation.GrpcServiceConsumer;
import cn.eyecool.grpc.constants.Constant;
import cn.eyecool.grpc.function.Predicate;
import cn.eyecool.match.service.commons.ApiCallInfo;
import cn.eyecool.match.service.commons.FeatureData;
import cn.eyecool.match.service.commons.ServerInfo;
import cn.eyecool.match.service.detection.DetectionRequest;
import cn.eyecool.match.service.detection.MultiDetectionReply;
import cn.eyecool.match.service.extraction.ExtractionReply;
import cn.eyecool.match.service.extraction.ExtractionRequest;
import cn.eyecool.match.service.extraction.MultiExtractionReply;
import cn.eyecool.match.service.face.FaceParam;
import cn.eyecool.match.service.match.MatchResult;
import cn.eyecool.match.service.match.*;
import cn.eyecool.minisearch.service.search.CompareResult;
import cn.eyecool.minisearch.service.search.DataSearchServiceGrpc;
import cn.eyecool.minisearch.service.search.SearchRequest;
import cn.eyecool.minisearch.service.search.SearchResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.eyecool.abis.callmicroservice.IBioFaceMicroService;
import com.eyecool.abis.callmicroservice.common.*;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnsafeByteOperations;
import com.teso.drivers.face.BioCloudFacePoolDriver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.apache.commons.codec.binary.Base64;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ??????V????????????????????? ???????????????
 *
 * @author sanmu
 * @date 2019-04-25
 */
@Slf4j
@Service
public class IBioFaceMicroServiceImpl implements IBioFaceMicroService {

    private static final Logger LOG = LoggerFactory.getLogger(IBioFaceMicroServiceImpl.class);

    private final cn.eyecool.match.service.commons.Requester requester;

    private final AtomicLong requestSeqGen;

    @Autowired
    private FeatureExtraction featureExtraction;

    @Autowired
    private Detecter detecter;

    @Autowired
    private FeatureMatch featureMatch;

    @Resource(name = "globalRequesterBuilder")
    private cn.eyecool.minisearch.service.commons.Requester.Builder requestBuilder;

    @GrpcServiceConsumer(type = GrpcServiceConsumer.ConsumerType.BLOCKING)
    private DataSearchServiceGrpc.DataSearchServiceBlockingStub dataSearchStub;

    @GrpcServiceConsumer(type = GrpcServiceConsumer.ConsumerType.BLOCKING)
    private FaceDetectionServiceGrpc.FaceDetectionServiceBlockingStub faceDetectionStub;

    @Value("${eyecool.filestore.basePath}")
    private String basePath;
    @Value("${eyecool.reMatch}")
    private boolean reMatch;
    @Value("${eyecool.LD_LIBRARY_PATH}")
    private String ldLibraryPath;
    @Value("${eyecool.match.thread}")
    private Integer matchThread;

    /**
     * 9801???????????????
     */
    private static MiniMatchService miniMatchService;
    private static BioCloudFacePoolDriver faceAlg;

    /*@Resource(name = "rpcDataRegister1")
    private RpcDataRegister dataSynchronizer;*/

    public IBioFaceMicroServiceImpl() {

        // Requester?????????????????????????????????????????????????????????????????????
        requester = cn.eyecool.match.service.commons.Requester.newBuilder()
                .setCaller(ServerInfo.newBuilder()
                        // ??????????????????????????????
                        .setVendor("eyecool.cn")
                        // ??????????????????????????????
                        .setStartedAt(System.currentTimeMillis())
                        // ??????????????????ID????????????
                        .setServerId("application-face-001")
                        // ??????????????????IP??????
                        .setHost("127.0.0.1")
                        // ?????????????????????????????????
                        .setPort(8080)
                        // ?????????????????????
                        .setAlgType("face")
                        // ?????????????????????
                        .setAlgVersion("1.0.1")
                        // ?????????????????????
                        .setFeatVersion("v1.1.1")
                        .build())
                .build();
        requestSeqGen = new AtomicLong();
    }

    @Override
    public FaceExtractResult faceExteaction(String node, String handleSeq, String imageBase64) {
        long startTime = System.currentTimeMillis();
        if (LOG.isTraceEnabled()) {
            LOG.trace("get face feature param : image length :[{}] ", imageBase64.length());
        }
        // ???????????????node?????????????????????????????????node?????????????????????????????????????????????
        Predicate predicate = Strings.isNullOrEmpty(node) ? Predicate.Any : Predicate.Tag.build(node);
        // ????????????????????????
        if (Strings.isNullOrEmpty(handleSeq)) {
            handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        }
        String finalHandleSeq = handleSeq;
        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
            String reFeature = reFaceExteaction(node, finalHandleSeq, imageBase64);
            return reFeature;
        });

        // ??????????????????????????????
        byte[] imageContent = Base64.decodeBase64(imageBase64);

        // ????????????????????????
        ExtractionRequest extractionRequest = ExtractionRequest.newBuilder().setRequester(
                // ??????????????????????????????????????????
                cn.eyecool.match.service.commons.Requester.newBuilder(requester).setRequestSequence(handleSeq).setRequestTime(System.currentTimeMillis())
                        .build()

        ).addFaceParam(FaceParam.newBuilder().build())
                // ???????????????????????????
                .addImage(cn.eyecool.match.service.commons.Image.newBuilder().setImageBytes(
                        // ??????????????????imageContent????????????????????????????????????ByteString.copyFrom(byte[])????????????????????????
                        UnsafeByteOperations.unsafeWrap(imageContent)).build())
                .build();

        // ??????????????????
        MultiExtractionReply features = featureExtraction.extract(predicate, extractionRequest);
        int replyCount = features.getReplyCount();

        // rpc????????????
        ApiCallInfo apiCallInfo = features.getApiCallInfo();
        FaceExtractResult faceExtractResult = new FaceExtractResult();
        String algType = apiCallInfo.getProvider().getAlgType();
        String algVersion = apiCallInfo.getProvider().getAlgVersion();
        String featVersion = apiCallInfo.getProvider().getFeatVersion();
        String serverId = apiCallInfo.getProvider().getServerId();
        String host = apiCallInfo.getProvider().getHost();
        List<String> featureList = new ArrayList<>();
        if (replyCount == 0) {
            //??????????????????
            faceExtractResult.setFeatures(featureList);
            faceExtractResult.setAlgType(algType);
            faceExtractResult.setAlgVersion(algVersion);
            faceExtractResult.setFeatVersion(featVersion);
            faceExtractResult.setServerId(serverId);
            faceExtractResult.setHost(host);
            return faceExtractResult;
        }
        LOG.info("{}, RECV: apiCallInfo --> {}", handleSeq, apiCallInfo);

        for (int i = 0; i < replyCount; i++) {
            ExtractionReply reply = features.getReply(i);
            FeatureData featureData = reply.getFeature();
            // ????????????
            String feature = Base64.encodeBase64String(featureData.getData().toByteArray());
            if (LOG.isDebugEnabled()) {
                LOG.debug("get face feature form abis-match-service is [{}] :", feature);
            }
            featureList.add(feature);
        }
        faceExtractResult.setFeatures(featureList);
        faceExtractResult.setAlgType(algType);
        faceExtractResult.setAlgVersion(algVersion);
        faceExtractResult.setFeatVersion(featVersion);
        faceExtractResult.setServerId(serverId);
        faceExtractResult.setHost(host);

        try {
            String reFeature = (String)future.get();
            faceExtractResult.setReFeatures(reFeature);
        } catch(Exception e) {
            log.error(e.toString());
        }

        LOG.info("????????????-???????????????[{}] ?????? [{}] use time [{}ms]", handleSeq, JSON.toJSONString(faceExtractResult, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return faceExtractResult;
    }

    @Override
    public FaceExtractResult doubleFaceExteaction(String node, String handleSeq, String imageBase64) {
        long startTime = System.currentTimeMillis();
        if (LOG.isTraceEnabled()) {
            LOG.trace("get face feature param : image length :[{}] ", imageBase64.length());
        }

        String finalHandleSeq = handleSeq;
        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
            String reFeature = reFaceExteaction(node, finalHandleSeq, imageBase64);
            return reFeature;
        });
        FaceExtractResult faceExtractResult = new FaceExtractResult();

        try {
            String reFeature = (String)future.get();
            faceExtractResult.setReFeatures(reFeature);
        } catch(Exception e) {
            log.error(e.toString());
        }

        LOG.info("????????????-???????????????[{}] ?????? [{}] use time [{}ms]", handleSeq, JSON.toJSONString(faceExtractResult, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return faceExtractResult;
    }

    /**
     * ??????????????????
     *
     * @param node        ??????
     * @param handleSeq   ?????????????????????????????????
     * @param imageBase64 ?????????base64
     * @return feature    ????????????
     */
    @Override
    public String reFaceExteaction(String node, String handleSeq, String imageBase64) {
        String feature = null;
        try {
            if (!reMatch) {
                return feature;
            }
            if (miniMatchService == null) {
                getMiniMatchService();
            }
            feature = faceAlg.getFeatureByImgB64(imageBase64);
        } catch (Exception e) {
            log.error(e.toString());
        }

        return feature;
    }

    /**
     * ???????????? ???????????????
     */
    public MiniMatchService getMiniMatchService() {
        if (miniMatchService != null) {
            log.info("miniMatchService is not null" );
            return miniMatchService;
        }
        DBBasedFeatureManager dBBasedFeatureManager = null;
        log.info("LD_LIBRARY_PATH = " + System.getProperty("LD_LIBRARY_PATH") );
        System.setProperty("tesoalgs.face.scoreFactor", "0.00000007874015748");
        faceAlg = new BioCloudFacePoolDriver();
        faceAlg.setLibBaseName("SsNowAgent");
        faceAlg.setPoolSize(matchThread);
        boolean res = faceAlg.init();
        log.info("??????????????????9801???????????????[{}]"+ res);
        miniMatchService = new MiniMatchService();
        MiniMatchConfig config = new MiniMatchConfig();
        String path = basePath+File.separator+"9801";
        log.info("???????????????????????????????????????[{}]"+ path);
        config.setImageRootDir(path);
        miniMatchService.setMiniMatchConfig(config);
        miniMatchService.setFaceAlg(faceAlg);
        miniMatchService.init();
        return miniMatchService;
    }

    @Override
    public List<MatchBean> faceMatch(String node, String handleSeq, List<ImageBean> imageBeanList, List<FeatureBean> featureBeanList) {
        long startTime = System.currentTimeMillis();
        // ???????????????node?????????????????????????????????node?????????????????????????????????????????????
        Predicate predicate = Strings.isNullOrEmpty(node) ? Predicate.Any : Predicate.Tag.build(node);
        // ????????????????????????
        if (Strings.isNullOrEmpty(handleSeq)) {
            handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        }
        // ??????????????????
        // ??????????????????????????????????????????????????????????????????????????????????????????????????????
        MultiMatchRequest.Builder multiMatchRequestBuilder = MultiMatchRequest.newBuilder().setRequester(
                // Requester.newBuilder(requester)?????????????????????requester????????????????????????
                // ??????????????????????????????????????????
                cn.eyecool.match.service.commons.Requester.newBuilder(requester).setRequestSequence(handleSeq).setRequestTime(System.currentTimeMillis())
                        .build());
        // ??????????????????????????????
        for (int i = 0; i < imageBeanList.size(); i++) {
            ImageBean request = imageBeanList.get(i);
            byte[] imageContent = Base64.decodeBase64(request.getImage());
            LOG.info("{}, SEND: index-->{},  node --> {}, image --> {} bytes", handleSeq, i, node, imageContent == null ? "null" : imageContent.length);
            if (imageContent == null) {
                throw new IllegalArgumentException(handleSeq + ", index : " + i + ", ImageContent is null");
            }
            if (imageContent.length == 0) {
                throw new IllegalArgumentException(handleSeq + ", index : " + i + ", ImageContent is empty");
            }
            // ??????????????????????????????
            multiMatchRequestBuilder.addRequest(MatchRequest.newBuilder().setImageRequest(MatchImageRequest.newBuilder().setFaceParam(FaceParam.newBuilder()).
                    setImage(cn.eyecool.match.service.commons.Image.newBuilder().setImageBytes(UnsafeByteOperations.unsafeWrap(imageContent)).build()).build()).build());
        }
        // ???????????????????????????
        for (int i = 0; i < featureBeanList.size(); i++) {
            FeatureBean request = featureBeanList.get(i);
            byte[] featureContent = Base64.decodeBase64(request.getFeature());
            LOG.info("{}, SEND: index --> {}, node --> {}, feature --> {} bytes", handleSeq, i, node,
                    featureContent == null ? "null" : featureContent.length);
            if (featureContent == null) {
                throw new IllegalArgumentException(handleSeq + ", index: " + i + ", FeatureContent is null");
            }
            if (featureContent.length == 0) {
                throw new IllegalArgumentException(handleSeq + ", index: " + i + ", FeatureContent is empty");
            }
            // ??????????????????????????????
            multiMatchRequestBuilder.addRequest(MatchRequest.newBuilder()
                    .setFeatureRequest(MatchFeatureRequest.newBuilder().setFeature(FeatureData.newBuilder().setFeatureType(request.getType())
                            .setData(UnsafeByteOperations.unsafeWrap(featureContent)).build()).build()).build());
        }
        // ????????????
        MultiMatchRequest multiMatchRequest = multiMatchRequestBuilder.build();
        if (LOG.isTraceEnabled()) {
            LOG.trace("{}, SEND: extractionRequest --> {}", handleSeq, multiMatchRequest);
        }
        // rpc????????????????????????????????????
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
                featureBean.setFeature(Base64.encodeBase64String(matchReply.getFeature().getData().toByteArray()));
                bean.setFeature(featureBean);
            }
            bean.setAlgType(algType);
            bean.setAlgVersion(algVersion);
            bean.setFeatVersion(featVersion);
            bean.setServerId(serverId);
            bean.setHost(host);
            beans.add(bean);
        }
        /*MultiMatchReplyHolder holder = new MultiMatchReplyHolder(results);
        try {
            Double score = holder.getScore(0, 1);
            LOG.info("Result: 0 vs 1 is {}", score);
            LOG.info("Result: 0 vs 2 is {}", holder.getScore(0, 2));
            LOG.info("Result: 1 vs 2 is {}", holder.getScore(1, 2));
        } catch (AbisException e) {
            LOG.error("Failed to get score of (0, 1)");
        }*/
        //LOG.info("{}, RECV: beans --> {}", handleSeq, beans.size());
        LOG.info("??????Match-???????????????{} ??????{} ??????{} use time [{}ms]", handleSeq, beans.size(), JSON.toJSONString(beans, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return beans;
    }

    @Override
    public List<FaceSearchResult> faceMiniSearch(String node, String handleSeq, String feature, String channelCode, int topN, double threshold) {
        long startTime = System.currentTimeMillis();
        //??????????????????
        SearchRequest request = SearchRequest.newBuilder()
                .setRequester(requestBuilder.setRequestSequence(handleSeq).setRequestTime(System.currentTimeMillis()).build())
                .setN(topN)
                .setLibrary(channelCode)
                .setThreshold(threshold)
                .setFeatureData(
                        cn.eyecool.minisearch.service.commons.FeatureData.newBuilder()
                                .setFeatureType(cn.eyecool.minisearch.service.commons.FeatureData.FeatureType.FaceFeature)
                                .setData(ByteString.copyFrom(Base64.decodeBase64(feature)))
                                .build()
                )
                .build();
        //????????????
        //???????????????node?????????????????????????????????node?????????????????????????????????????????????
        SearchResult result = Strings.isNullOrEmpty(node) ? dataSearchStub.search(
                SearchRequest.newBuilder(request)
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
                SearchRequest.newBuilder(request)
                        .setRequester(
                                cn.eyecool.minisearch.service.commons.Requester.newBuilder()
                                        .setRequestSequence(handleSeq)
                                        .build()
                        )
                        .build()
        );
        List<CompareResult> list = result.getResultList();
        List<FaceSearchResult> results = new ArrayList<>();
        if (list != null && list.size() > 0) {
            cn.eyecool.minisearch.service.commons.ApiCallInfo apiCallInfo = result.getApiCallInfo();
            String algType = apiCallInfo.getProvider().getAlgType();
            String algVersion = apiCallInfo.getProvider().getAlgVersion();
            String featVersion = apiCallInfo.getProvider().getFeatVersion();
            String serverId = apiCallInfo.getProvider().getServerId();
            String host = apiCallInfo.getProvider().getHost();
            for (CompareResult f : list) {
                FaceSearchResult faceSearchResult = new FaceSearchResult();
                faceSearchResult.setUserId(f.getPeopleFeature().getId());
                faceSearchResult.setFeatureId(f.getPeopleFeature().getFeature(0).getId());
                faceSearchResult.setScore(f.getScore());
                faceSearchResult.setAlgType(algType);
                faceSearchResult.setAlgVersion(algVersion);
                faceSearchResult.setFeatVersion(featVersion);
                faceSearchResult.setServerId(serverId);
                faceSearchResult.setHost(host);
                results.add(faceSearchResult);
            }
        }
        LOG.info("??????MiniSearch-???????????????{} ??????{} use time [{}ms]", handleSeq, JSON.toJSONString(results, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return results;
    }

    @Override
    public List<FaceSearchResult> reFaceMiniSearch(String node, String handleSeq, String feature, String reFeature, String channelCode, int topN, double threshold) {
        long startTime = System.currentTimeMillis();
        List<FaceSearchResult> results = faceMiniSearch(node, handleSeq, feature, channelCode, topN, threshold);
        List<FaceSearchResult> newResults = new ArrayList<>(0);
        if (CollectionUtils.isEmpty(results)) {
            return newResults;
        }
        byte[] reFeatureArr = null;
        if (StringUtils.isNotEmpty(reFeature)) {
            reFeatureArr = Base64.decodeBase64(reFeature);
        }
        if (reFeatureArr == null) {
            return newResults;
        }
        BigDecimal bdThreshold = new BigDecimal(threshold);
        for (FaceSearchResult faceSearchResult : results) {
            if (faceSearchResult == null) {
                continue;
            }
            String strUserId = faceSearchResult.getUserId();
            try {
                double reScore = miniMatch(reFeatureArr,  strUserId);
                BigDecimal bdReScore = new BigDecimal(reScore);
                LOG.info("??????????????? > userId :"+strUserId + " ?????????" + reScore + " ???????????????:" + reFeature);
                if (bdReScore.compareTo(bdThreshold) >= 0) {
                    newResults.add(faceSearchResult);
                }
            } catch(Exception e) {
                log.error(e.toString());
            }
        }
        LOG.info("??????MiniSearch-???????????????{} ??????{} use time [{}ms]", handleSeq, JSON.toJSONString(results, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return newResults;
    }

    /**
     * doubleFaceMiniSearch
     *
     * @param node        ??????
     * @param handleSeq   ?????????????????????????????????
     * @param userIds     ????????????
     * @param reFeature   9801??????
     * @param channelCode ????????????   ????????????""????????????N??????;???????????????????????????????????????n
     * @param topN        ?????????N???
     * @param threshold   ??????
     * @return
     */
    @Override
    public List<FaceSearchResult> doubleFaceMiniSearch(String node, String handleSeq, String userIds, String reFeature, String channelCode, int topN, double threshold) {
        long startTime = System.currentTimeMillis();
        String[] arr = userIds.split(",");
        List<FaceSearchResult> newResults = new ArrayList<>(0);
        byte[] reFeatureArr = null;
        if (StringUtils.isNotEmpty(reFeature)) {
            reFeatureArr = Base64.decodeBase64(reFeature);
        }
        if (reFeatureArr == null) {
            return newResults;
        }
        BigDecimal bdThreshold = new BigDecimal(threshold);
        for (String strUserId : arr) {
            if (strUserId == null) {
                continue;
            }
            try {
                double reScore = miniMatch(reFeatureArr,  strUserId);
                BigDecimal bdReScore = new BigDecimal(reScore);
                LOG.info("??????????????? > userId :"+strUserId + " ?????????" + reScore + " ???????????????:" + reFeature);
                if (bdReScore.compareTo(bdThreshold) >= 0) {
                    FaceSearchResult faceSearchResult = new FaceSearchResult();
                    faceSearchResult.setScore(reScore);
                    faceSearchResult.setUserId(strUserId);
                    newResults.add(faceSearchResult);
                }
            } catch(Exception e) {
                log.error(e.toString());
            }
        }
        LOG.info("??????MiniSearch-???????????????{} ??????{} use time [{}ms]", handleSeq, JSON.toJSONString(newResults, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.PrettyFormat), System.currentTimeMillis() - startTime);
        return newResults;
    }

    /**
     * Description miniMatch ??????????????????????????? 9801
     *
     * @return double
     * @author zfx
     * @date 2020/1/9 18:30
     */
    private double miniMatch(byte[] imgFeature, String userId) throws Exception {
        long start = System.currentTimeMillis();
        if (miniMatchService == null) {
            miniMatchService = getMiniMatchService();
        }
        double score = miniMatchService.verifyUserByFeature(imgFeature, new FileNameBasedUserIdParser.FileNameBasedUserId(userId));
        log.info("9801 compare score[{}]usedTime[{}]ms,userId[{}]", score, System.currentTimeMillis() - start, userId);
        return score;
    }



    @Override
    public boolean faceVideoLivenessDetection(String handleSeq, String videoPath, double threshold) throws Exception {
        long startTime = System.currentTimeMillis();
        //????????????
        //Channel channel = NettyChannelBuilder.forAddress(ipLivenessDetect, portLivenessDetect).usePlaintext().build();
        //FaceDetectionServiceGrpc.FaceDetectionServiceBlockingStub stub = FaceDetectionServiceGrpc.newBlockingStub(channel);
        ByteString videoContent;
        LOG.info("???????????? --> {}", new File(videoPath).getCanonicalPath());
        try (InputStream videoFile = new FileInputStream(new File(videoPath))) {
            videoContent = ByteString.readFrom(videoFile);
        }
        //????????????????????????
        LivenessDetectionVideoRequest videoRequest =
                LivenessDetectionVideoRequest
                        .newBuilder()
                        .setVideo(
                                Video.newBuilder()
                                        .setVideoBytes(videoContent)
                        )
                        .setRequester(
                                Requester.newBuilder()
                                        .setCompanyId("eyecool.cn")
                                        .setRequestSequence(handleSeq)
                                        .build()
                        )
                        .build();
        //??????????????????
        LivenessReply reply = faceDetectionStub.livenessDetectionWithVideo(videoRequest);

        AtomicDouble score_video = new AtomicDouble(0.0d);
        int count_video = reply.getFaceReply().getFaceParamCount();
        if (count_video > 0) {
            for (int i = 0; i < count_video; i++) {
                double tempScore = reply.getFaceReply().getFaceParam(i).getLivenessScore();
                if (tempScore > score_video.get()) {
                    score_video.set(tempScore);
                }
            }
        }
        //??????????????????
        LOG.info("????????????-???????????????{} ?????? [{}] use time [{}ms]", handleSeq, score_video.get() < threshold, System.currentTimeMillis() - startTime);
        //??????????????????
        return score_video.get() < threshold;
    }

    @Override
    public boolean faceImageLivenessDetection(String handleSeq, String imageBase64, double threshold) throws Exception {
        long startTime = System.currentTimeMillis();
        /*//????????????
        Channel channel = NettyChannelBuilder.forAddress(ipLivenessDetect, portLivenessDetect).usePlaintext().build();
        FaceDetectionServiceGrpc.FaceDetectionServiceBlockingStub stub = FaceDetectionServiceGrpc.newBlockingStub(channel);
        */
        ByteString imageContent;
        //LOG.info("???????????? --> {}", new File(imagePath).getCanonicalPath());
        //??????????????????????????????
        byte[] imageContentReq = Base64.decodeBase64(imageBase64);

        /*try (InputStream imageFile = new FileInputStream(new File(imagePath))) {
        }*/
        imageContent = ByteString.copyFrom(imageContentReq);

        //????????????????????????
        LivenessDetectionImageRequest imageRequest = LivenessDetectionImageRequest.newBuilder()
                .setImage(Image.newBuilder()
                        .setImageBytes(imageContent))
                .setRequester(
                        Requester.newBuilder()
                                .setCompanyId("eyecool.cn")
                                .setRequestSequence(handleSeq)
                                .build()
                )
                .build();

        //??????????????????
        FaceReply faceReply = faceDetectionStub.livenessDetectionWithImage(imageRequest);

        AtomicDouble score_image = new AtomicDouble(0.0d);
        int count_image = faceReply.getFaceParamCount();
        if (count_image > 0) {
            for (int i = 0; i < count_image; i++) {
                double tempScore = faceReply.getFaceParam(0).getLivenessScore();
                if (tempScore > score_image.get()) {
                    score_image.set(tempScore);
                }
            }
        }
        //??????????????????
        LOG.info("????????????-???????????????{} score_image [{}] threshold [{}] ?????? [{}] use time [{}ms]", handleSeq, score_image.get(), threshold,score_image.get() < threshold, System.currentTimeMillis() - startTime);
        return score_image.get() < threshold;
    }

    @Override
    public int multiFaceDetection(String handleSeq, String imageBase64) throws Exception {
        long startTime = System.currentTimeMillis();
        // ????????????
        int faceCount = 0;
        // ????????????????????????
        if (Strings.isNullOrEmpty(handleSeq)) {
            handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        }
        // ??????????????????????????????
//        byte[] imageContent = null;
//        try {
//            imageContent = FileUtils.readFileToByteArray(new File(imagePath));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //??????????????????????????????
        byte[] imageContentReq = Base64.decodeBase64(imageBase64);
        ByteString imageContent = ByteString.copyFrom(imageContentReq);
        // ??????????????????
        DetectionRequest detectionRequest = DetectionRequest.newBuilder()
                .setRequester(
                        // Requester.newBuilder(requester)?????????????????????requester????????????????????????
                        // ??????????????????????????????????????????
                        cn.eyecool.match.service.commons.Requester.newBuilder(requester)
                                .setRequestSequence(handleSeq)
                                .setRequestTime(System.currentTimeMillis())
                                .build())
                // ???????????????????????????????????????
                // .setFingerParam(FingerParam.newBuilder().setType(FeatureType.FaceFeature).build())
                // ???????????????????????????
                .setImage(cn.eyecool.match.service.commons.Image.newBuilder()
                        .setImageBytes(
                                // ??????????????????imageContent????????????????????????????????????ByteString.copyFrom(byte[])????????????????????????
                                imageContent)
                        .build())
                .build();

        if (LOG.isTraceEnabled()) {
            LOG.trace("{}, SEND: extractionRequest --> {}", handleSeq, detectionRequest);
        }

        MultiDetectionReply multiDetectionReply = detecter.detect(Predicate.Any, detectionRequest);
        // rpc????????????
        //ApiCallInfo apiCallInfo = multiDetectionReply.getApiCallInfo();
        //LOG.info("{}, RECV: apiCallInfo --> {}", reqSeq, apiCallInfo);
        faceCount = multiDetectionReply.getReplyCount();
        LOG.info("???????????????-???????????????{} ???????????? [{}] use time [{}ms]", handleSeq, faceCount, System.currentTimeMillis() - startTime);
        return faceCount;
    }


 /*   @Override
    public void load(String handleSeq) {
        long startTime = System.currentTimeMillis();
        // ????????????????????????
        if (Strings.isNullOrEmpty(handleSeq)) {
            handleSeq = new UUID(System.currentTimeMillis(), requestSeqGen.incrementAndGet()).toString();
        }
        if (!dataSynchronizer.isHealth()) {
            dataSynchronizer.disconnect();
            dataSynchronizer.connect();
        }
        DataRegRequest syncRequest = DataRegRequest.newBuilder()
                .setLoad(DataLoad.newBuilder().build())
                .setRequester(requestBuilder.setRequestSequence(handleSeq).setRequestTime(System.currentTimeMillis()).build())
                .build();
        try {
            DataRegResponse generalResponse = dataSynchronizer.send(syncRequest);
            LOG.info("??????????????????-??????-???????????????{} use time [{}ms]", handleSeq, System.currentTimeMillis() - startTime);
        } catch (MinisearchException e) {
            LOG.info("??????????????????-??????-???????????????{} use time [{}ms]", handleSeq, System.currentTimeMillis() - startTime);
            e.printStackTrace();
        }
    }*/
}
