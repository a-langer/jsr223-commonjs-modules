package am.langer.commonjs;

public interface Folder {
    public Folder getParent();

    public String getPath();

    public String getFile(String name);

    public Folder getFolder(String name);
}
