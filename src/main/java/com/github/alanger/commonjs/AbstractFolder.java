package am.langer.commonjs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public abstract class AbstractFolder implements Folder {
    private Folder parent;
    private String path;

    public Folder getParent() {
        return parent;
    }

    public String getPath() {
        return path;
    }

    protected AbstractFolder(Folder parent, String path) {
        this.parent = parent;
        this.path = path;
    }

    public static String inputStreamToString(InputStream input, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(input, Charset.forName(encoding));
                BufferedReader br = new BufferedReader(reader)) {
            for (int c = br.read(); c != -1; c = br.read())
                sb.append((char) c);
        }
        return sb.toString();
    }
}
