package com.github.alanger.commonjs;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptException;

public class ModuleException extends ScriptException implements Bindings {
    private static final long serialVersionUID = 1L;
    private String message = "Error loading module";
    private String code = "UNDEFINED";
    private Throwable cause;

    public ModuleException(String message, String code) {
        super(message);
        this.message = message != null ? message : "Error loading module";
        this.code = code != null ? code : "UNDEFINED";
        this.cause = getCause();
    }

    public ModuleException(String message) {
        this(message, null);
    }

    public ModuleException(Exception e) {
        super(e);
        this.cause = e.getCause();
    }

    public String getCode() {
        return this.code;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public void clear() {
    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public Collection<Object> values() {
        return null;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return null;
    }

    @Override
    public Object put(String name, Object value) {
        if (name.equals("code"))
            this.code = (String) value;
        if (name.equals("message"))
            this.message = (String) value;
        if (name.equals("cause"))
            this.cause = (Throwable) value;
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
    }

    @Override
    public boolean containsKey(Object key) {
        if (key.equals("code"))
            return true;
        if (key.equals("message"))
            return true;
        if (key.equals("cause"))
            return true;
        return false;
    }

    @Override
    public Object get(Object key) {
        if (key.equals("code"))
            return this.code;
        if (key.equals("message"))
            return this.message;
        if (key.equals("cause"))
            return this.cause;
        return null;
    }

    @Override
    public Object remove(Object key) {
        return null;
    }
}
