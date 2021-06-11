package com.eyecool.service.web.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动输出
 *
 * @author dcmsy
 * @date 2019-07-24
 *
 */
@Component
public class ApplicationRunnerImpl implements ApplicationRunner {
    Logger logger = LoggerFactory.getLogger(ApplicationRunnerImpl.class);

    /**
     * run
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("=======Face-Service-Web-eyecool  is running=======");
        String[] sourceArgs = args.getSourceArgs();
        for (String arg : sourceArgs) {
            System.out.print(arg + " ");
        }
        System.out.println();
    }
}
