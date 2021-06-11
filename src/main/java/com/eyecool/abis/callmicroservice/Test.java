package com.eyecool.abis.callmicroservice;

import cn.eyecool.fox.minifeaturemng.FileNameBasedUserIdParser;
import cn.eyecool.fox.minimatch.MiniMatchConfig;
import cn.eyecool.fox.minimatch.MiniMatchService;
import com.teso.drivers.face.BioCloudFacePoolDriver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Test {

    /**
     * 9801算法使用到
     */
    private static MiniMatchService miniMatchService;
    //    private static BioFaceCloudAlg faceAlg;
    private static BioCloudFacePoolDriver faceAlg;

    /**
     * Description miniMatch 根据特征值进行比对 9801
     *
     * @return double
     * @author zfx
     * @date 2020/1/9 18:30
     */
    public double miniMatch(byte[] imgFeature, String userId) throws Exception {
        long start = System.currentTimeMillis();
        if (miniMatchService == null) {
            log.error("------miniMatchService is null--------");
            throw new Exception("9801 comparison.second init fail,miniMatchService is null");
        }
        double score = miniMatchService.verifyUserByFeature(imgFeature, new FileNameBasedUserIdParser.FileNameBasedUserId(userId));
        log.info("9801 compare score[{}]usedTime[{}]ms,userId[{}]", score, System.currentTimeMillis() - start, userId);
        return score;
    }

//    /**
//     * 使用时候 初始化底库
//     */
//    public MiniMatchService getMiniMatchService() {
//        System.setProperty("tesoalgs.face.scoreFactor", "0.00000007874015748");
//        faceAlg = new BioCloudFacePoolDriver();
//        faceAlg.setLibBaseName("SsNowAgent");
//        int matchThread = Integer.valueOf(PropertyReader.getProperty("adminclient", "match.thread"));
//        faceAlg.setPoolSize(matchThread);
//        boolean res = faceAlg.init();
//        log.info("误识比对算法9801初始化结果[{}]", res);
//        miniMatchService = new MiniMatchService();
//        MiniMatchConfig config = new MiniMatchConfig();
//        String path = PropertyReader.getProperty("adminclient", "bottom.path");
//        log.info("误识比对算法加载的底库目录[{}]", path);
//        config.setImageRootDir(path);
//        miniMatchService.setMiniMatchConfig(config);
//        miniMatchService.setFaceAlg(faceAlg);
//        miniMatchService.init();
//        return miniMatchService;
//    }

//  byte[] feature = faceAlg.getFeatureByImg(org.apache.commons.codec.binary.Base64.decodeBase64(smallFace));
}
