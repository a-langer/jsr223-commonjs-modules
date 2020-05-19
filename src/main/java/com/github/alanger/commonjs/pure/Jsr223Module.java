package com.github.alanger.commonjs.pure;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import com.github.alanger.commonjs.AbstractModule;
import com.github.alanger.commonjs.Folder;
import com.github.alanger.commonjs.ModuleCache;
import com.github.alanger.commonjs.ModuleException;
import com.github.alanger.commonjs.Paths;

public class Jsr223Module extends AbstractModule {

    private Object nativeModule;

    public Jsr223Module(ScriptEngine engine, Folder folder, ModuleCache cache, String filename, Object module,
            Object exports, AbstractModule parent, AbstractModule root) throws ScriptException {
        super(engine, folder, cache, filename, module, exports, parent, root);
        this.put("main", mainModule.getModule());
    }

    @Override
    protected Jsr223Module compileJsonModule(Folder parent, String fullPath, String code) throws ScriptException {
        Object module = createSafeBindings();
        Object exports = createSafeBindings();
        Jsr223Module created = new Jsr223Module(engine, parent, cache, fullPath, module, exports, this, mainModule);
        created.exports = parseJson(code);
        created.setLoaded();
        return created;
    }

    @Override
    protected Jsr223Module compileJavaScriptModule(Folder parent, String fullPath, String code) throws ScriptException {
        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        Object module = createSafeBindings();
        for (String key : engineScope.keySet()) {
            putObject(module, key, engineScope.get(key));
        }

        // If we have cached bindings, use them to rebind exports instead of creating
        // new ones
        Object exports = refCache.get().get(fullPath);
        if (exports == null) {
            exports = createSafeBindings();
        }

        Jsr223Module created = new Jsr223Module(engine, parent, cache, fullPath, module, exports, this, mainModule);

        String[] split = Paths.splitPath(fullPath);
        String filename = split[split.length - 1];
        String dirname = fullPath.substring(0, Math.max(fullPath.length() - filename.length() - 1, 0));

        String previousFilename = (String) engine.get(ScriptEngine.FILENAME);
        // Set filename before eval so file names/lines in exceptions are accurate
        engine.put(ScriptEngine.FILENAME, fullPath);

        try {
            Object function = engine
                    .eval("(function (exports, require, module, __filename, __dirname) {" + code + "\n})");
            Object[] args = { created.exports, created.getNativeModule(), created.module, filename, dirname };

            Invocable invocable = (Invocable) engine;
            try {
                invocable.invokeMethod(function, "apply", created, args);
            } catch (NoSuchMethodException e) {
                throw new ScriptException(e);
            }
        } finally {
            engine.put(ScriptEngine.FILENAME, previousFilename);
        }

        // Scripts are free to replace the global exports symbol with their own, so we
        // reload it from the module object after compiling the code.
        created.exports = getObject(created.module, "exports");

        created.setLoaded();
        return created;
    }

    @Override
    protected Object getNativeModule() throws ScriptException {
        if (this.nativeModule == null) {
            SimpleScriptContext ctx = new SimpleScriptContext();
            ctx.setAttribute("JAVA_MODULE", this, ScriptContext.ENGINE_SCOPE);

            String moduleException = ModuleException.class.getCanonicalName();
            this.nativeModule = engine.eval(
                    "(function(path) { \n" + "try { \n" + "    return JAVA_MODULE.require(path) \n" + "} catch (e) { \n"
                            + "    throw new " + moduleException + "(e.message, 'MODULE_NOT_FOUND') \n" + "} \n" + "})",
                    ctx);
            putObject(nativeModule, "main", this.mainModule.getModule());
        }
        return this.nativeModule;
    }

    @Override
    protected Object getObject(Object thizz, String key) throws ScriptException {
        SimpleScriptContext ctx = new SimpleScriptContext();
        ctx.setAttribute("thizz", thizz, ScriptContext.ENGINE_SCOPE);
        return engine.eval("thizz['" + key + "']", ctx);
    }

    @Override
    public Object putObject(Object thizz, String key, Object value) throws ScriptException {
        SimpleScriptContext ctx = new SimpleScriptContext();
        ctx.setAttribute("thizz", thizz, ScriptContext.ENGINE_SCOPE);
        ctx.setAttribute("value", value, ScriptContext.ENGINE_SCOPE);
        return engine.eval("thizz['" + key + "'] = value", ctx);
    }

}
