package com.skillboost.service.sandbox;

import java.nio.file.Path;
import java.util.List;

public interface Sandbox {

    List<String> wrap(List<String> command, Path workDir);

    String describe();
}
