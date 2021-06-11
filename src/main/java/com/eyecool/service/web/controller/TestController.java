package com.eyecool.service.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("test")
@RestController
public class TestController {

    Logger logger = LoggerFactory.getLogger(TestController.class);

    /**
     * @return
     */
    @GetMapping("list")
    public List<String> getList() {
        try {
            List<String> list = new ArrayList<String>(0);
            list.add("姓名：1");
            list.add("姓名：2");
            logger.info("log4j2 success ===== info",list);
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}