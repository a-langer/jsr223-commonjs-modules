package com.github.alanger.commonjs;

import java.io.IOException;
import java.io.InputStream;

public class ResourceFolder extends AbstractFolder {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private ClassLoader loader;
    private String resourcePath;
    private String encoding;

    public String getResourcePath() {
        return resourcePath;
    }

    @Override
    public String getFile(String name) {
        String resPath = (resourcePath == null || resourcePath.isEmpty()) ? "" : resourcePath + "/";
        InputStream stream = loader.getResourceAsStream(resPath + name);
        if (stream == null) {
            return null;
        }

        try {
            return inputStreamToString(stream, encoding);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public Folder getFolder(String name) {
        return new ResourceFolder(loader, resourcePath + "/" + name, this, getPath() + name + "/", encoding);
    }

    private ResourceFolder(ClassLoader loader, String resourcePath, Folder parent, String displayPath,
            String encoding) {
        super(parent, displayPath);
        this.loader = loader;
        this.resourcePath = resourcePath.startsWith(CLASSPATH_PREFIX)
                ? resourcePath.substring(CLASSPATH_PREFIX.length()) : resourcePath;
        this.encoding = encoding;
    }

    public static ResourceFolder create(ClassLoader loader, String path, String encoding) {
        return new ResourceFolder(loader, path, null, "/", encoding);
    }
}
