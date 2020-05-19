package com.github.alanger.commonjs;

import org.junit.Test;

import com.github.alanger.commonjs.FilesystemFolder;
import com.github.alanger.commonjs.Folder;
import com.github.alanger.commonjs.Require;

import java.io.File;

import javax.script.ScriptEngine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FilesystemFolderTest {
    private File file = new File("src/test/resources/com/github/alanger/commonjs_modules/test1");
    private FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");

    private File subfile = new File(file, "subdir");
    private File subsubfile = new File(subfile, "subsubdir");

    private String rootPath = file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf(File.separator));

    @Test
    public void rootFolderHasTheExpectedProperties() {
        assertTrue(root.getPath().startsWith(rootPath));
        assertTrue(root.getPath().endsWith(file.getPath() + File.separator));
        assertNull(root.getParent());
    }

    @Test
    public void getFileReturnsTheContentOfTheFileWhenItExists() {
        assertTrue(root.getFile("foo.js").contains("foo"));
    }

    @Test
    public void getFileReturnsNullWhenFileDoesNotExists() {
        assertNull(root.getFile("invalid"));
    }

    @Test
    public void getFolderReturnsAnObjectWithTheExpectedProperties() {
        Folder sub = root.getFolder("subdir");
        assertTrue(sub.getPath().startsWith(rootPath));
        assertTrue(sub.getPath().endsWith(subfile.getPath() + File.separator));
        assertSame(root, sub.getParent());
        Folder subsub = sub.getFolder("subsubdir");
        assertTrue(subsub.getPath().startsWith(rootPath));
        assertTrue(subsub.getPath().endsWith(subsubfile.getPath() + File.separator));
        assertSame(sub, subsub.getParent());
    }

    @Test
    public void getFolderReturnsNullWhenFolderDoesNotExist() {
        assertNull(root.getFolder("invalid"));
    }

    @Test
    public void getFileCanBeUsedOnSubFolderIfFileExist() {
        assertTrue(root.getFolder("subdir").getFile("bar.js").contains("bar"));
    }

    @Test
    public void filesystemFolderWorksWhenUsedForReal() throws Throwable {
        ScriptEngine engine = EngineFactory.createEngine();
        Require.enable(engine, root);
        assertEquals(engine.eval("JSON.stringify('spam')"),
                engine.eval("JSON.stringify(require('./foo').bar.spam.spam)"));
    }
}
