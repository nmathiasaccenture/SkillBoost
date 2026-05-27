package com.skillboost.config;

import com.skillboost.service.sandbox.BwrapSandbox;
import com.skillboost.service.sandbox.NoSandbox;
import com.skillboost.service.sandbox.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Locale;

@Configuration
public class SandboxConfig {

    private static final Logger log = LoggerFactory.getLogger(SandboxConfig.class);

    @Value("${skillboost.judge.sandbox.enabled:true}")
    private boolean enabled;

    @Bean
    public Sandbox sandbox() {
        if (!enabled) {
            return warn(new NoSandbox("disabled by config"));
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("linux")) {
            return warn(new NoSandbox("host OS is not Linux: " + os));
        }
        String bwrap = findOnPath("bwrap");
        if (bwrap == null) {
            return warn(new NoSandbox("bwrap not found on PATH"));
        }
        Sandbox sandbox = new BwrapSandbox(bwrap);
        log.info("Judge sandbox active: {}", sandbox.describe());
        return sandbox;
    }

    private Sandbox warn(Sandbox sandbox) {
        log.warn("Judge sandbox active: {} — user code is NOT isolated from the host. "
                + "Use only with trusted input.", sandbox.describe());
        return sandbox;
    }

    private static String findOnPath(String exe) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;
        for (String dir : pathEnv.split(File.pathSeparator)) {
            File candidate = new File(dir, exe);
            if (candidate.isFile() && candidate.canExecute()) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }
}
