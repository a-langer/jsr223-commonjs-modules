package com.github.alanger.commonjs;

public interface Folder {
    public Folder getParent();

    public String getPath();

    public String getFile(String name);

    public Folder getFolder(String name);
}
