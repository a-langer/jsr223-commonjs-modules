package am.langer.commonjs.graalvm;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

import am.langer.commonjs.AbstractModule;
import am.langer.commonjs.Folder;
import am.langer.commonjs.ModuleCache;
import am.langer.commonjs.Paths;

public class GraalModule extends AbstractModule {

    private Context context;

    public GraalModule(ScriptEngine engine, Folder folder, ModuleCache cache, String filename, Object module,
            Object exports, AbstractModule parent, AbstractModule root) throws ScriptException {
        super(engine, folder, cache, filename, module, exports, parent, root);

        GraalJSScriptEngine gjse = (GraalJSScriptEngine) engine;
        this.context = gjse.getPolyglotContext();
    }

    protected GraalModule compileJsonModule(Folder parent, String fullPath, String code) throws ScriptException {
        Object module = createSafeBindings();
        Object exports = createSafeBindings();
        GraalModule created = new GraalModule(engine, parent, cache, fullPath, module, exports, this, mainModule);
        created.exports = parseJson(code);
        created.setLoaded();
        return created;
    }

    protected GraalModule compileJavaScriptModule(Folder parent, String fullPath, String code) throws ScriptException {
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

        GraalModule created = new GraalModule(engine, parent, cache, fullPath, module, exports, this, mainModule);

        String[] split = Paths.splitPath(fullPath);
        String filename = split[split.length - 1];
        String dirname = fullPath.substring(0, Math.max(fullPath.length() - filename.length() - 1, 0));

        // The \n at the end is to take care of files ending with a comment
        Source source = Source.newBuilder("js",
                "(function (exports, require, module, __filename, __dirname) {" + code + "\n})", fullPath)
                .buildLiteral();
        Value function = context.eval(source);
        function.execute(created.exports, created, created.module, filename, dirname);

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