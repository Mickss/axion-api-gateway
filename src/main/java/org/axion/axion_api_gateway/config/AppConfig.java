package org.axion.axion_api_gateway.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Map;
import java.util.stream.Collectors;

public class AppConfig {

    private final Map<String, ServiceConfig> servicesMap;

    public AppConfig() {
        Config config = ConfigFactory.load("application.conf");
        Config servicesConfig = config.getConfig("services");

        this.servicesMap = servicesConfig.root().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            String serviceName = entry.getKey();
                            Config serviceConfig = servicesConfig.getConfig(serviceName);
                            return new ServiceConfig(serviceConfig.getInt("port"));
                        }
                ));
    }

    public ServiceConfig getServiceConfig(String serviceName) throws ConfigNotFoundException {
        ServiceConfig serviceConfig = servicesMap.get(serviceName);
        if (serviceConfig == null) {
            throw new ConfigNotFoundException("Config not found: " + serviceName);
        }
        return serviceConfig;
    }
}
