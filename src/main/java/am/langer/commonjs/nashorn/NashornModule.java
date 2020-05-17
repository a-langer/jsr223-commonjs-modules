package am.langer.commonjs.nashorn;

import java.lang.reflect.Method;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import am.langer.commonjs.AbstractModule;
import am.langer.commonjs.Folder;
import am.langer.commonjs.ModuleCache;
import am.langer.commonjs.Paths;

public class NashornModule extends AbstractModule {

    public NashornModule(ScriptEngine engine, Folder folder, ModuleCache cache, String filename, Object module,
            Object exports, AbstractModule parent, AbstractModule root) throws ScriptException {
        super(engine, folder, cache, filename, module, exports, parent, root);
        this.put("main", this.mainModule.getModule());
    }

    @Override
    protected NashornModule compileJsonModule(Folder parent, String fullPath, String code) throws ScriptException {
        Object module = createSafeBindings();
        Object exports = createSafeBindings();
        NashornModule created = new NashornModule(engine, parent, cache, fullPath, module, exports, this, mainModule);
        created.exports = parseJson(code);
        created.setLoaded();
        return created;
    }

    @Override
    protected NashornModule compileJavaScriptModule(Folder parent, String fullPath, String code)
            throws ScriptException {
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

        NashornModule created = new NashornModule(engine, parent, cache, fullPath, module, exports, this, mainModule);

        String[] split = Paths.splitPath(fullPath);
        String filename = split[split.length - 1];
        String dirname = fullPath.substring(0, Math.max(fullPath.length() - filename.length() - 1, 0));

        String previousFilename = (String) engine.get(ScriptEngine.FILENAME);
        // Set filename before eval so file names/lines in exceptions are accurate
        engine.put(ScriptEngine.FILENAME, fullPath);

        try {
            // This mimics how Node wraps module in a function. I used to pass a 2nd
            // parameter
            // to eval to override global context, but it caused problems Object.create.
            //
            // The \n at the end is to take care of files ending with a comment
            Object function = engine
                    .eval("(function (exports, require, module, __filename, __dirname) {" + code + "\n})");
            Object[] args = { created.exports, created, created.module, filename, dirname };

            // Dirty fix for Nashorn, else one test returns "TypeError: [object Object] is
            // not an Object"
            // Calling ((jdk.nashorn.api.scripting.ScriptObjectMirror)function).call(created, args);
            try {
                Class<?> scriptObjectClass = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
                Method call = scriptObjectClass.getMethod("call", Object.class, Object[].class);
                call.invoke(function, created, args);
            } catch (Exception e) {
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
        return this;
    }
}
