package org.apiwiz.scriptingengine.config;

import org.apiwiz.scriptingengine.executor.ScriptExecutor;
import org.apiwiz.scriptingengine.models.ScriptLanguage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class ScriptExecutorConfig {

    @Bean
    public Map<ScriptLanguage, ScriptExecutor> scriptExecutorMap(List<ScriptExecutor> executors) {
        return executors.stream()
                .collect(Collectors.toMap(ScriptExecutor::getSupportedLanguage, e -> e));
    }
}
