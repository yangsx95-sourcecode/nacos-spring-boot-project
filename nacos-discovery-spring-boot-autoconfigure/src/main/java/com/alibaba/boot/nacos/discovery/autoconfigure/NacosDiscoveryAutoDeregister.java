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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * 应用程序上下文关闭时，从注册表移除服务实例
 * @author <a href="mailto:liaochunyhm@live.com">liaochuntao</a>
 * @since 0.1.4
 */
@Component
public class NacosDiscoveryAutoDeregister
		// 当应用程序上下文关闭时，会触发 ContextClosedEvent 事件。
		// 这个事件通常在应用程序关闭或停止时被触发，可以用于执行一些清理操作或释放资源的逻辑。
		implements ApplicationListener<ContextClosedEvent> {

	private static final Logger logger = LoggerFactory
			.getLogger(NacosDiscoveryAutoRegister.class);

	@NacosInjected
	private NamingService namingService;

	private final NacosDiscoveryProperties discoveryProperties;
	private final EmbeddedServletContainer webServer;

	@Value("${spring.application.name:spring.application.name}")
	private String applicationName;

	public NacosDiscoveryAutoDeregister(NacosDiscoveryProperties discoveryProperties,
			EmbeddedServletContainer webServer) {
		this.discoveryProperties = discoveryProperties;
		this.webServer = webServer;
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		if (!discoveryProperties.isAutoRegister()) {
			return;
		}

		Register register = discoveryProperties.getRegister();

		if (StringUtils.isEmpty(register.getIp())) {
			register.setIp(NetUtils.localIP());
		}

		if (register.getPort() == 0) {
			register.setPort(webServer.getPort());
		}

		String serviceName = StringUtils.isEmpty(register.getServiceName())
				? applicationName
				: register.getServiceName();

		try {
			namingService.deregisterInstance(serviceName, register.getGroupName(),
					register);
			logger.info("Finished auto deregister service : {}, ip : {}, port : {}",
					register.getServiceName(), register.getIp(), register.getPort());
		}
		catch (NacosException e) {
			throw new AutoDeregisterException(e);
		}
	}
}
