/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.boot.nacos.discovery.autoconfigure;

import com.alibaba.boot.nacos.discovery.properties.NacosDiscoveryProperties;
import com.alibaba.boot.nacos.discovery.properties.Register;
import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.utils.NetUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * web容器启动后，注册服务到nacos
 * @author <a href="mailto:liaochunyhm@live.com">liaochuntao</a>
 * @since 0.1.3
 */
@Component
public class NacosDiscoveryAutoRegister
		// 嵌入式 Servlet 容器启动并准备好接收请求时，会触发 EmbeddedServletContainerInitializedEvent 事件。
		// 事件还会提供 容器的端口号和上下文路径等其他容器信息
		implements ApplicationListener<EmbeddedServletContainerInitializedEvent> {

	private static final Logger logger = LoggerFactory
			.getLogger(NacosDiscoveryAutoRegister.class);

	@NacosInjected
	private NamingService namingService;

	@Autowired
	private NacosDiscoveryProperties discoveryProperties;

	@Value("${spring.application.name:spring.application.name}")
	private String application;

	@Override
	public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {

		if (!discoveryProperties.isAutoRegister()) {
			return;
		}

		// 根据配置文件获取注册中心
		Register register = discoveryProperties.getRegister();

		// 设置默认值值
		if (StringUtils.isEmpty(register.getIp())) {
			register.setIp(NetUtils.localIP());
		}

		if (register.getPort() == 0) {
			register.setPort(event.getSource().getPort());
		}

		// 设置注册的来源是SpringBoot应用程序
		register.getMetadata().put("preserved.register.source", "SPRING_BOOT");

		register.setInstanceId("");
		// 默认的服务名称是应用程序名称
		String serviceName = StringUtils.isEmpty(register.getServiceName()) ? application
				: register.getServiceName();

		try {
			// 调用注册服务注册服务实例
			namingService.registerInstance(serviceName, register.getGroupName(),
					register);
			logger.info("Finished auto register service : {}, ip : {}, port : {}",
					register.getServiceName(), register.getIp(), register.getPort());
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

}
