package com.eyecool.service.web;

import com.eyecool.abis.callmicroservice.impl.IBioFaceMicroServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * 启动类
 *
 * @author dcmsy
 * @date 2019-07-24
 */
@ComponentScan(value = "cn.eyecool,com.eyecool", excludeFilters = {
		@ComponentScan.Filter(type = FilterType.REGEX, pattern = "^cn\\.eyecool\\.abis\\.match\\.service\\.Iris.*$") })
@ImportResource(
		locations = {
				"classpath*:minisearch.xml"
		}
)
@Slf4j
@SpringBootApplication
public class ServiceWebApplication  {
	@Autowired
	private IBioFaceMicroServiceImpl microService;

	public static void main(String[] args) {
		SpringApplication.run(ServiceWebApplication.class, args);
	}

//	@Override
//	public void run(String... args) throws Exception {
//		System.out.println("init 9801 ssnowAgent ....... ");
//		microService.getMiniMatchService();
//	}

	/**
	 * onApplicationEvent
	 *
	 *
	 * @param event
	 */
	@EventListener
	public void onApplicationEvent(ContextRefreshedEvent event) {
		log.info("init 9801 ssnowAgent ....... ");
		try{
			microService.getMiniMatchService();
		} catch(Exception e){
			log.error(e.toString());
		}

	}
}
