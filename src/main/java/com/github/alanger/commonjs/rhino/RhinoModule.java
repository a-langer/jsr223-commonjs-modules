package am.langer.commonjs.rhino;

import java.util.ArrayList;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import am.langer.commonjs.AbstractModule;
import am.langer.commonjs.Folder;
import am.langer.commonjs.ModuleCache;
import am.langer.commonjs.pure.Jsr223Module;

public class RhinoModule extends Jsr223Module implements Function {

    private Scriptable prototype;
    private Scriptable parent;

    public RhinoModule(ScriptEngine engine, Folder folder, ModuleCache cache, String filename, Object module,
            Object exports, AbstractModule parent, AbstractModule root) throws ScriptException {
        super(engine, folder, cache, filename, module, exports, parent, root);
    }

    @Override
    protected Object getNativeModule() throws ScriptException {
        return this;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args != null && args.length > 0) {
            try {
                return require((String) args[0]);
            } catch (ScriptException e) {
                throw Context.reportRuntimeError(e.getMessage());
            }
        }
        return Undefined.instance;
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getClassName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Object get(String name, Scriptable start) {
        return super.get(name);
    }

    @Override
    public Object get(int index, Scriptable start) {
        return (new ArrayList<Object>(super.values())).get(index);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return super.containsKey(name);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return (new ArrayList<Object>(super.values())).get(index) != null;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        super.put(name, value);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (index + 1 < super.size()) {
            int i = 0;
            for (Map.Entry<String, Object> entry : super.entrySet()) {
                if (i == index) {
                    super.put(entry.getKey(), value);
                    break;
                }
                i++;
            }
        } else {
            throw Context.reportRuntimeError("External array index out of bounds ");
        }
    }

    @Override
    public void delete(String name) {
        super.remove(name);
    }

    @Override
    public void delete(int index) {
        if (index + 1 < super.size()) {
            int i = 0;
            for (Map.Entry<String, Object> entry : super.entrySet()) {
                if (i == index) {
                    super.remove(entry.getKey());
                    break;
                }
                i++;
            }
        }
    }

    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    @Override
    public Scriptable getParentScope() {
        return parent;
    }

    @Override
    public void setParentScope(Scriptable parent) {
        this.parent = parent;
    }

    @Override
    public Object[] getIds() {
        return Context.emptyArgs;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return ScriptableObject.getDefaultValue(this, hint);
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        return (instance instanceof RhinoModule);
    }
}
