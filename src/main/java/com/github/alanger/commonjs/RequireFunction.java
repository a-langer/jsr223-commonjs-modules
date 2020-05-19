package am.langer.commonjs;

import javax.script.ScriptException;

@FunctionalInterface
public interface RequireFunction {
    public Object require(String module) throws ScriptException;
}
