package com.skillboost.service.sandbox;

import java.nio.file.Path;
import java.util.List;

/**
 * Pass-through sandbox used when no host-level isolation primitive is available
 * (e.g. on Windows, or on Linux when bubblewrap is not installed). The judge
 * timeout and per-submission temp dir still apply, but user code shares the
 * host's filesystem and network namespace.
 */
public class NoSandbox implements Sandbox {

    private final String reason;

    public NoSandbox(String reason) {
        this.reason = reason;
    }

    @Override
    public List<String> wrap(List<String> command, Path workDir) {
        return command;
    }

    @Override
    public String describe() {
        return "none (" + reason + ")";
    }
}
