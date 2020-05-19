package com.github.alanger.commonjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

abstract public class AbstractModule extends SimpleBindings implements RequireFunction {

    protected ScriptEngine engine;
    protected Object jsonConstructor;

    protected Folder folder;
    protected ModuleCache cache;

    protected AbstractModule mainModule;
    public Object main;
    protected Object exports;
    protected Object module;
    protected List<Object> children = new ArrayList<>();

    protected static ThreadLocal<Map<String, Object>> refCache = new ThreadLocal<>();

    public AbstractModule(ScriptEngine engine, Folder folder, ModuleCache cache, String filename, Object module,
            Object exports, AbstractModule parent, AbstractModule root) throws ScriptException {
        super(new LinkedHashMap<String, Object>());
        this.engine = engine;

        if (parent != null) {
            this.jsonConstructor = parent.jsonConstructor;
        } else {
            this.jsonConstructor = engine.eval("JSON");
        }

        this.folder = folder;
        this.cache = cache;
        this.mainModule = root != null ? root : this;
        this.module = module;
        this.exports = exports;
        this.main = this.mainModule.module;

        this.init(cache, filename, module, exports, parent, root);
    }

    protected void init(ModuleCache cache, String filename, Object module, Object exports, AbstractModule parent,
            AbstractModule root) throws ScriptException {
        putObject(module, "exports", exports);
        putObject(module, "children", children);
        putObject(module, "filename", filename);
        putObject(module, "id", filename);
        putObject(module, "loaded", false);
        putObject(module, "exports", exports);
        putObject(module, "parent", parent != null ? parent.module : null);
    }

    public ScriptEngine getScriptEngine() {
        return this.engine;
    };

    public Object getModule() {
        return this.module;
    }

    protected void setLoaded() throws ScriptException {
        putObject(this.module, "loaded", true);
    }

    protected abstract AbstractModule compileJsonModule(Folder parent, String fullPath, String code)
            throws ScriptException;

    protected abstract AbstractModule compileJavaScriptModule(Folder parent, String fullPath, String code)
            throws ScriptException;

    protected abstract Object getNativeModule() throws ScriptException;

    @Override
    public Object require(String module) throws ScriptException {
        if (module == null) {
            throwModuleNotFoundException("<null>");
        }

        String[] parts = Paths.splitPath(module);
        if (parts.length == 0) {
            throwModuleNotFoundException(module);
        }

        String[] folderParts = Arrays.copyOfRange(parts, 0, parts.length - 1);

        String filename = parts[parts.length - 1];

        AbstractModule found = null;

        Folder resolvedFolder = resolveFolder(folder, folderParts);

        // Let's make sure each thread gets its own refCache
        if (refCache.get() == null) {
            refCache.set(new HashMap<>());
        }

        String requestedFullPath = null;
        if (resolvedFolder != null) {
            requestedFullPath = resolvedFolder.getPath() + filename;
            Object cachedExports = refCache.get().get(requestedFullPath);
            if (cachedExports != null) {
                return cachedExports;
            } else {
                // We must store a reference to currently loading module to avoid circular
                // requires
                refCache.get().put(requestedFullPath, createSafeBindings());
            }
        }

        try {
            // If not cached, we try to resolve the module from the current folder, ignoring
            // node_modules
            if (isPrefixedModuleName(module)) {
                found = attemptToLoadFromThisFolder(resolvedFolder, filename);
            }

            // Then, if not successful, we'll look at node_modules in the current folder and
            // then in all parent folders until we reach the top.
            if (found == null) {
                found = searchForModuleInNodeModules(folder, folderParts, filename);
            }

            if (found == null) {
                throwModuleNotFoundException(module);
            }

            assert found != null;
            this.children.add(found.module);

            return found.exports;

        } finally {
            // Finally, we remove the successful resolved module from the refCache
            if (requestedFullPath != null) {
                refCache.get().remove(requestedFullPath);
            }
        }
    }

    protected AbstractModule searchForModuleInNodeModules(Folder resolvedFolder, String[] folderParts, String filename)
            throws ScriptException {
        Folder current = resolvedFolder;
        while (current != null) {
            Folder nodeModules = current.getFolder("node_modules");

            if (nodeModules != null) {
                AbstractModule found = attemptToLoadFromThisFolder(resolveFolder(nodeModules, folderParts), filename);
                if (found != null) {
                    return found;
                }
            }

            current = current.getParent();
        }

        return null;
    }

    protected AbstractModule attemptToLoadFromThisFolder(Folder resolvedFolder, String filename)
            throws ScriptException {

        if (resolvedFolder == null) {
            return null;
        }

        String requestedFullPath = resolvedFolder.getPath() + filename;

        AbstractModule found = this.cache.get(requestedFullPath);
        if (found != null) {
            return found;
        }

        // First we try to load as a file, trying out various variations on the path
        found = loadModuleAsFile(resolvedFolder, filename);

        // Then we try to load as a directory
        if (found == null) {
            found = loadModuleAsFolder(resolvedFolder, filename);
        }

        if (found != null) {
            // We keep a cache entry for the requested path even though the code that
            // compiles the module also adds it to the cache with the potentially different
            // effective path. This avoids having to load package.json every time, etc.
            this.cache.put(requestedFullPath, found);
        }

        return found;
    }

    private AbstractModule loadModuleAsFile(Folder parent, String filename) throws ScriptException {

        String[] filenamesToAttempt = getFilenamesToAttempt(filename);
        for (String tentativeFilename : filenamesToAttempt) {

            String code = parent.getFile(tentativeFilename);
            if (code != null) {
                String fullPath = parent.getPath() + tentativeFilename;
                return compileModuleAndPutInCache(parent, fullPath, code);
            }
        }

        return null;
    }

    private AbstractModule loadModuleAsFolder(Folder parent, String name) throws ScriptException {
        Folder fileAsFolder = parent.getFolder(name);
        if (fileAsFolder == null) {
            return null;
        }

        AbstractModule found = loadModuleThroughPackageJson(fileAsFolder);

        if (found == null) {
            found = loadModuleThroughIndexJs(fileAsFolder);
        }

        if (found == null) {
            found = loadModuleThroughIndexJson(fileAsFolder);
        }

        return found;
    }

    private AbstractModule loadModuleThroughPackageJson(Folder parent) throws ScriptException {
        String packageJson = parent.getFile("package.json");
        if (packageJson == null) {
            return null;
        }

        String mainFile = getMainFileFromPackageJson(packageJson);
        if (mainFile == null) {
            return null;
        }

        String[] parts = Paths.splitPath(mainFile);
        String[] folders = Arrays.copyOfRange(parts, 0, parts.length - 1);
        String filename = parts[parts.length - 1];
        Folder folder = resolveFolder(parent, folders);
        if (folder == null) {
            return null;
        }

        AbstractModule module = loadModuleAsFile(folder, filename);

        if (module == null) {
            folder = resolveFolder(parent, parts);
            if (folder != null) {
                module = loadModuleThroughIndexJs(folder);
            }
        }

        return module;
    }

    private AbstractModule loadModuleThroughIndexJs(Folder parent) throws ScriptException {
        String code = parent.getFile("index.js");
        if (code == null) {
            return null;
        }

        return compileModuleAndPutInCache(parent, parent.getPath() + "index.js", code);
    }

    private AbstractModule loadModuleThroughIndexJson(Folder parent) throws ScriptException {
        String code = parent.getFile("index.json");
        if (code == null) {
            return null;
        }

        return compileModuleAndPutInCache(parent, parent.getPath() + "index.json", code);
    }

    private AbstractModule compileModuleAndPutInCache(Folder parent, String fullPath, String code)
            throws ScriptException {

        AbstractModule created;
        String lowercaseFullPath = fullPath.toLowerCase();
        if (lowercaseFullPath.endsWith(".js")) {
            created = compileJavaScriptModule(parent, fullPath, code);
        } else if (lowercaseFullPath.endsWith(".json")) {
            created = compileJsonModule(parent, fullPath, code);
        } else {
            // Unsupported module type
            return null;
        }

        // We keep a cache entry for the compiled module using it's effective path, to
        // avoid
        // recompiling even if module is requested through a different initial path.
        this.cache.put(fullPath, created);

        return created;
    }

    protected String getMainFileFromPackageJson(String packageJson) throws ScriptException {
        Object jsonObject = parseJson(packageJson);
        return (String) getObject(jsonObject, "main");
    }

    protected Object parseJson(String json) throws ScriptException {
        try {
            Invocable invocable = (Invocable) this.engine;
            return invocable.invokeMethod(this.jsonConstructor, "parse", json);
        } catch (NoSuchMethodException e) {
            throw new ScriptException(e);
        }
    }

    protected String stringifyObject(Object json) throws ScriptException {
        try {
            Invocable invocable = (Invocable) this.engine;
            return (String) invocable.invokeMethod(this.jsonConstructor, "stringify", json);
        } catch (NoSuchMethodException e) {
            throw new ScriptException(e);
        }
    }

    protected void throwModuleNotFoundException(String module) throws ScriptException {
        throw new ModuleException("Module not found: " + module, "MODULE_NOT_FOUND");
    }

    protected static Folder resolveFolder(Folder from, String[] folders) {
        Folder current = from;
        for (String name : folders) {
            switch (name) {
            case "":
                throw new IllegalArgumentException();
            case ".":
                continue;
            case "..":
                current = current.getParent();
                break;
            default:
                current = current.getFolder(name);
                break;
            }

            // Whenever we get stuck we bail out
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    protected Object createSafeBindings() throws ScriptException {
        return this.engine.eval("({})");
    }

    protected static boolean isPrefixedModuleName(String module) {
        return module.startsWith("/") || module.startsWith("../") || module.startsWith("./");
    }

    protected static String[] getFilenamesToAttempt(String filename) {
        return new String[] { filename, filename + ".js", filename + ".json" };
    }

    @SuppressWarnings("unchecked")
    protected Object getObject(Object thizz, String key) throws ScriptException {
        Map<String, Object> m = (Map<String, Object>) thizz;
        return m.get(key);
    }

    @SuppressWarnings("unchecked")
    protected Object putObject(Object thizz, String key, Object value) throws ScriptException {
        Map<String, Object> m = (Map<String, Object>) thizz;
        return m.put(key, value);
    }
}
