package com.github.alanger.commonjs;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.junit.Ignore;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

@Ignore
public class EngineFactory {

    // Example: mvn test -DargLine="-Djsr223.engine.name=graal.js"
    public static final String ENGINE_NAME_KEY = "jsr223.engine.name";

    public static ScriptEngine createEngine() {
        return createEngine(System.getProperty(ENGINE_NAME_KEY, Require.NASHORN));
    }

    public static synchronized ScriptEngine createEngine(String name) {

        ScriptEngine engine = null;

        // https://github.com/graalvm/graaljs/blob/master/docs/user/ScriptEngine.md
        if (name.equals(Require.GRAALJS)) {
            engine = GraalJSScriptEngine.create(null,
                    Context.newBuilder("js").allowHostAccess(HostAccess.ALL)
                            .allowHostClassLookup(s -> true)
                            .allowAllAccess(true)
                            .allowNativeAccess(true)
                            .option("js.nashorn-compat", "true")
                            .option("js.ecmascript-version", "2020"));
        } else if (name.equals(Require.NASHORN)) {
            if (System.getProperty("nashorn.args") == null)
                System.setProperty("nashorn.args", "--language=es6");
            engine = new ScriptEngineManager().getEngineByName(Require.NASHORN);
        } else {
            engine = new ScriptEngineManager().getEngineByName(name);
        }
        return engine;
    }

}
