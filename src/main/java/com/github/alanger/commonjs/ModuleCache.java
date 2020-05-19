package com.github.alanger.commonjs;

import java.util.HashMap;
import java.util.Map;

public class ModuleCache {
    private Map<String, AbstractModule> modules = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected <T extends AbstractModule> T get(String fullPath) {
        return (T) modules.get(fullPath);
    }

    public void put(String fullPath, AbstractModule module) {
        modules.put(fullPath, module);
    }
}
