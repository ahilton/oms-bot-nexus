package org.bbtf.nexus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

@SpringBootApplication
//(scanBasePackages = {"org.bbtf", "com.microsoft.applicationinsights"})

//@EnableFeignClients
public class NexusApplication {

	public static void main(String[] args) {
		SpringApplication.run(NexusApplication.class, args);
	}
}
