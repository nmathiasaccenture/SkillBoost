package com.skillboost.service.sandbox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs user code under bubblewrap with the network unshared and the filesystem
 * restricted to read-only system paths plus the per-submission work directory.
 */
public class BwrapSandbox implements Sandbox {

    private final String bwrapPath;

    public BwrapSandbox(String bwrapPath) {
        this.bwrapPath = bwrapPath;
    }

    @Override
    public List<String> wrap(List<String> command, Path workDir) {
        List<String> wrapped = new ArrayList<>();
        wrapped.add(bwrapPath);

        // Read-only system paths. --ro-bind-try silently skips missing ones,
        // so this works across distros where some of /lib, /lib32, /lib64 are symlinks.
        wrapped.add("--ro-bind-try"); wrapped.add("/usr");    wrapped.add("/usr");
        wrapped.add("--ro-bind-try"); wrapped.add("/etc");    wrapped.add("/etc");
        wrapped.add("--ro-bind-try"); wrapped.add("/bin");    wrapped.add("/bin");
        wrapped.add("--ro-bind-try"); wrapped.add("/sbin");   wrapped.add("/sbin");
        wrapped.add("--ro-bind-try"); wrapped.add("/lib");    wrapped.add("/lib");
        wrapped.add("--ro-bind-try"); wrapped.add("/lib32");  wrapped.add("/lib32");
        wrapped.add("--ro-bind-try"); wrapped.add("/lib64");  wrapped.add("/lib64");
        wrapped.add("--ro-bind-try"); wrapped.add("/opt");    wrapped.add("/opt");

        wrapped.add("--proc");  wrapped.add("/proc");
        wrapped.add("--dev");   wrapped.add("/dev");
        wrapped.add("--tmpfs"); wrapped.add("/tmp");

        // Writable work dir.
        String wd = workDir.toAbsolutePath().toString();
        wrapped.add("--bind"); wrapped.add(wd); wrapped.add(wd);
        wrapped.add("--chdir"); wrapped.add(wd);

        // Isolation.
        wrapped.add("--unshare-net");
        wrapped.add("--unshare-pid");
        wrapped.add("--unshare-ipc");
        wrapped.add("--unshare-uts");
        wrapped.add("--unshare-cgroup-try");
        wrapped.add("--die-with-parent");
        wrapped.add("--new-session");

        wrapped.add("--");
        wrapped.addAll(command);
        return wrapped;
    }

    @Override
    public String describe() {
        return "bwrap (network and filesystem isolated)";
    }
}
