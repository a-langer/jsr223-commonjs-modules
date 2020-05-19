package com.github.alanger.commonjs;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.github.alanger.commonjs.graalvm.GraalModule;
import com.github.alanger.commonjs.nashorn.NashornModule;
import com.github.alanger.commonjs.pure.Jsr223Module;
import com.github.alanger.commonjs.rhino.RhinoModule;

public class Require {

    public static final String GRAALJS = "graal.js";
    public static final String GRAALJS_NAME = "Graal.js";
    public static final String RHINO = "rhino";
    public static final String RHINO_NAME = "Mozilla Rhino";
    public static final String NASHORN = "nashorn";
    public static final String NASHORN_NAME = "Oracle Nashorn";

    // This overload registers the require function globally in the engine scope
    public static AbstractModule enable(ScriptEngine engine, Folder folder) throws ScriptException {
        Bindings global = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        return enable(engine, folder, global);
    }

    // This overload registers the require function in a specific Binding. It is
    // useful when re-using the same script engine across multiple threads
    // (each thread should have his own global scope defined
    // through the binding that is passed as an argument).
    public static AbstractModule enable(ScriptEngine engine, Folder folder, Bindings bindings) throws ScriptException {

        AbstractModule created = null;

        Object module = engine.eval("({})");
        Object exports = engine.eval("({})");

        String name = engine.getFactory().getEngineName();
        String className = engine.getClass().getCanonicalName();

        // Engine implementation may be changed, e.x.: -Dcustom.engine.class.Name="rhino"
        if (NASHORN_NAME.equals(name) || NASHORN.equals(System.getProperty(className))) {
            created = new NashornModule(engine, folder, new ModuleCache(), "<main>", module, exports, null, null);
        } else if (RHINO_NAME.equals(name) || RHINO.equals(System.getProperty(className))) {
            created = new RhinoModule(engine, folder, new ModuleCache(), "<main>", module, exports, null, null);
        } else if (GRAALJS_NAME.equals(name) || GRAALJS.equals(System.getProperty(className))) {
            created = new GraalModule(engine, folder, new ModuleCache(), "<main>", module, exports, null, null);
        } else {
            created = new Jsr223Module(engine, folder, new ModuleCache(), "<main>", module, exports, null, null);
        }
        created.setLoaded();

        bindings.put("require", created.getNativeModule());
        bindings.put("module", module);
        bindings.put("exports", exports);

        return created;
    }
}
