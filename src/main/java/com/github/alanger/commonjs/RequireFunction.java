package com.github.alanger.commonjs;

import javax.script.ScriptException;

@FunctionalInterface
public interface RequireFunction {
    public Object require(String module) throws ScriptException;
}
