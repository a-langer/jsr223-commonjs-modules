package com.github.alanger.commonjs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.alanger.commonjs.AbstractModule;
import com.github.alanger.commonjs.FilesystemFolder;
import com.github.alanger.commonjs.Folder;
import com.github.alanger.commonjs.Require;

@RunWith(MockitoJUnitRunner.class)
public class ModuleTest {
    @Mock
    Folder root;
    @Mock
    Folder rootnm;
    @Mock
    Folder sub1;
    @Mock
    Folder sub1nm;
    @Mock
    Folder sub1sub1;
    @Mock
    Folder nmsub1;

    ScriptEngine engine;
    AbstractModule require;

    @Before
    public void before() throws Throwable {
        when(root.getPath()).thenReturn("/");
        when(root.getFolder("node_modules")).thenReturn(rootnm);
        when(root.getFolder("sub1")).thenReturn(sub1);
        when(root.getFile("file1.js")).thenReturn("exports.file1 = 'file1';");
        when(root.getFile("file2.json")).thenReturn("{ \"file2\": \"file2\" }");
        when(rootnm.getPath()).thenReturn("/node_modules/");
        when(rootnm.getParent()).thenReturn(root);
        when(rootnm.getFile("nmfile1.js")).thenReturn("exports.nmfile1 = 'nmfile1';");
        when(rootnm.getFolder("nmsub1")).thenReturn(nmsub1);
        when(nmsub1.getFile("nmsub1file1.js")).thenReturn("exports.nmsub1file1 = 'nmsub1file1';");
        when(nmsub1.getParent()).thenReturn(rootnm);
        when(sub1.getPath()).thenReturn("/sub1/");
        when(sub1.getParent()).thenReturn(root);
        when(sub1.getFolder("sub1")).thenReturn(sub1sub1);
        when(sub1.getFolder("node_modules")).thenReturn(sub1nm);
        when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub1file1 = 'sub1file1';");
        when(sub1nm.getPath()).thenReturn("/sub1/node_modules/");
        when(sub1nm.getFile("sub1nmfile1.js")).thenReturn("exports.sub1nmfile1 = 'sub1nmfile1';");
        when(sub1sub1.getPath()).thenReturn("/sub1/sub1/");
        when(sub1sub1.getFile("sub1sub1file1.js")).thenReturn("exports.sub1sub1file1 = 'sub1sub1file1';");

        engine = EngineFactory.createEngine();
        require = Require.enable(engine, root);
    }

    // Utility methods
    public Object get(Object thizz, String key) throws ScriptException {
        return require.getObject(thizz, key);
    }

    public String stringify(Object thizz) throws ScriptException {
        return require.stringifyObject(thizz);
    }

    public String stringify(Object thizz, String key) throws ScriptException {
        return stringify(get(thizz, key));
    }

    @Test
    public void itCanLoadSimpleModules() throws Throwable {
        assertEquals(stringify("file1"), stringify(get(require.require("./file1.js"), "file1")));
    }

    @Test
    public void itCanEnableRequireInDifferentBindingsOnTheSameEngine() throws Throwable {
        ScriptEngine engine = EngineFactory.createEngine();

        // Graal.js return error: cannot be passed from one context to another.
        // See task https://github.com/oracle/graal/issues/631
        if (engine.getFactory().getEngineName().equals(Require.GRAALJS_NAME))
            return;

        Bindings bindings1 = new SimpleBindings();
        Bindings bindings2 = new SimpleBindings();

        Require.enable(engine, root, bindings1);

        assertNull(engine.getBindings(ScriptContext.ENGINE_SCOPE).get("require"));
        assertNotNull(bindings1.get("require"));
        assertNull(bindings2.get("require"));
        assertEquals(stringify("file1"), stringify(engine.eval("require('./file1')", bindings1), "file1"));

        try {
            engine.eval("require('./file1')", bindings2);
            fail();
        } catch (ScriptException ignored) {
        }

        Require.enable(engine, root, bindings2);
        assertNull(engine.getBindings(ScriptContext.ENGINE_SCOPE).get("require"));
        assertNotNull(bindings2.get("require"));
        assertEquals(stringify("file1"), stringify(engine.eval("require('./file1')", bindings2), "file1"));
    }

    @Test
    public void itCanLoadSimpleJsonModules() throws Throwable {
        assertEquals(stringify("file2"), stringify(require.require("./file2.json"), "file2"));
    }

    @Test
    public void itCanLoadModulesFromSubFolders() throws Throwable {
        assertEquals(stringify("sub1file1"), stringify(require.require("./sub1/sub1file1.js"), "sub1file1"));
    }

    @Test
    public void itCanLoadModulesFromSubFoldersInNodeModules() throws Throwable {
        assertEquals(stringify("nmsub1file1"), stringify(require.require("nmsub1/nmsub1file1.js"), "nmsub1file1"));
    }

    @Test
    public void itCanLoadModulesFromSubSubFolders() throws Throwable {
        assertEquals(stringify("sub1sub1file1"),
                stringify(require.require("./sub1/sub1/sub1sub1file1.js"), "sub1sub1file1"));
    }

    @Test
    public void itCanLoadModulesFromParentFolders() throws Throwable {
        when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub1file1 = require('../file1').file1;");
        assertEquals(stringify("file1"), stringify(require.require("./sub1/sub1file1.js"), "sub1file1"));
    }

    @Test
    public void itCanGoUpAndDownInFolders() throws Throwable {
        when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub1file1 = require('../file1').file1;");
        assertEquals(stringify("file1"), stringify(require.require("./sub1/../sub1/sub1file1.js"), "sub1file1"));
    }

    @Test
    public void itCanGoUpAndDownInNodeModulesFolders() throws Throwable {
        assertEquals(stringify("nmsub1file1"),
                stringify(require.require("nmsub1/../nmsub1/nmsub1file1.js"), "nmsub1file1"));
    }

    @Test
    public void itCanLoadModulesSpecifyingOnlyTheFolderWhenPackageJsonHasAMainFile() throws Throwable {
        Folder dir = mock(Folder.class);
        when(dir.getFile("package.json")).thenReturn("{ \"main\": \"foo.js\" }");
        when(dir.getFile("foo.js")).thenReturn("exports.foo = 'foo';");
        when(root.getFolder("dir")).thenReturn(dir);
        assertEquals(stringify("foo"), stringify(require.require("./dir"), "foo"));
    }

    @Test
    public void itCanLoadModulesSpecifyingOnlyTheFolderWhenPackageJsonHasAMainFilePointingToAFileInSubDirectory()
            throws Throwable {
        Folder dir = mock(Folder.class);
        Folder lib = mock(Folder.class);
        when(dir.getFile("package.json")).thenReturn("{ \"main\": \"lib/foo.js\" }");
        when(dir.getFolder("lib")).thenReturn(lib);
        when(lib.getFile("foo.js")).thenReturn("exports.foo = 'foo';");
        when(root.getFolder("dir")).thenReturn(dir);
        assertEquals(stringify("foo"), stringify(require.require("./dir"), "foo"));
    }

    @Test
    public void itCanLoadModulesSpecifyingOnlyTheFolderWhenPackageJsonHasAMainFilePointingToASubDirectory()
            throws Throwable {
        Folder dir = mock(Folder.class);
        Folder lib = mock(Folder.class);
        when(root.getFolder("dir")).thenReturn(dir);
        when(dir.getFolder("lib")).thenReturn(lib);
        when(dir.getFile("package.json")).thenReturn("{\"main\": \"./lib\"}");
        when(lib.getFile("index.js")).thenReturn("exports.foo = 'foo';");
        assertEquals(stringify("foo"), stringify(require.require("./dir"), "foo"));
    }

    @Test
    public void itCanLoadModulesSpecifyingOnlyTheFolderWhenPackageJsonHasAMainFilePointingToAFileInSubDirectoryReferencingOtherFilesInThisDirectory()
            throws Throwable {
        Folder dir = mock(Folder.class);
        Folder lib = mock(Folder.class);
        when(dir.getFile("package.json")).thenReturn("{ \"main\": \"lib/foo.js\" }");
        when(dir.getFolder("lib")).thenReturn(lib);
        when(lib.getFile("foo.js")).thenReturn("exports.bar = require('./bar');");
        when(lib.getFile("bar.js")).thenReturn("exports.bar = 'bar';");
        when(root.getFolder("dir")).thenReturn(dir);
        assertEquals(stringify("bar"), stringify((get(require.require("./dir"), "bar")), "bar"));
    }

    @Test
    public void itCanLoadModulesSpecifyingOnlyTheFolderWhenIndexJsIsPresent() throws Throwable {
        Folder dir = mock(Folder.class);
        when(dir.getFile("index.js")).thenReturn("exports.foo = 'foo';");
        when(root.getFolder("dir")).thenReturn(dir);
        assertEquals(stringify("foo"), stringify(require.require("./dir"), "foo"));
    }

    @Test
    public void itCanLoadModulesSpecifyingOnlyTheFolderWhenIndexJsIsPresentEvenIfPackageJsonExists() throws Throwable {
        Folder dir = mock(Folder.class);
        when(dir.getFile("package.json")).thenReturn("{ }");
        when(dir.getFile("index.js")).thenReturn("exports.foo = 'foo';");
        when(root.getFolder("dir")).thenReturn(dir);
        assertEquals(stringify("foo"), stringify(require.require("./dir"), "foo"));
    }

    @Test
    public void itUsesNodeModulesOnlyForNonPrefixedNames() throws Throwable {
        assertEquals(stringify("nmfile1"), stringify(require.require("nmfile1"), "nmfile1"));
    }

    @Test
    public void itFallbacksToNodeModulesWhenUsingPrefixedName() throws Throwable {
        assertEquals(stringify("nmfile1"), stringify(require.require("./nmfile1"), "nmfile1"));
    }

    @Test(expected = ScriptException.class)
    public void itDoesNotUseModulesOutsideOfNodeModulesForNonPrefixedNames() throws Throwable {
        require.require("file1.js");
    }

    @Test
    public void itUsesNodeModulesFromSubFolderForSubRequiresFromModuleInSubFolder() throws Throwable {
        when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub1nmfile1 = require('sub1nmfile1').sub1nmfile1;");
        assertEquals(stringify("sub1nmfile1"), stringify(require.require("./sub1/sub1file1"), "sub1nmfile1"));
    }

    @Test
    public void itLooksAtParentFoldersWhenTryingToResolveFromNodeModules() throws Throwable {
        when(sub1.getFile("sub1file1.js")).thenReturn("exports.nmfile1 = require('nmfile1').nmfile1;");
        assertEquals(stringify("nmfile1"), stringify(require.require("./sub1/sub1file1"), "nmfile1"));
    }

    @Test
    public void itCanUseDotToReferenceToTheCurrentFolder() throws Throwable {
        assertEquals(stringify("file1"), stringify(require.require("./file1.js"), "file1"));
    }

    @Test
    public void itCanUseDotAndDoubleDotsToGoBackAndForward() throws Throwable {
        assertEquals(stringify("file1"), stringify(require.require("./sub1/.././sub1/../file1.js"), "file1"));
    }

    @Test
    public void thePathOfModulesContainsNoDots() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports.path = module.filename");
        assertEquals(stringify("/file1.js"), stringify(require.require("./sub1/.././sub1/../file1.js"), "path"));
    }

    @Test
    public void itCanLoadModuleIfTheExtensionIsOmitted() throws Throwable {
        assertEquals(stringify("file1"), stringify(require.require("./file1"), "file1"));
    }

    @Test(expected = ScriptException.class)
    public void itThrowsAnExceptionIfFileDoesNotExists() throws Throwable {
        require.require("./invalid");
    }

    @Test(expected = ScriptException.class)
    public void itThrowsAnExceptionIfSubFileDoesNotExists() throws Throwable {
        require.require("./sub1/invalid");
    }

    @Test(expected = ScriptException.class)
    public void itThrowsEnExceptionIfFolderDoesNotExists() throws Throwable {
        require.require("./invalid/file1.js");
    }

    @Test(expected = ScriptException.class)
    public void itThrowsEnExceptionIfSubFolderDoesNotExists() throws Throwable {
        require.require("./sub1/invalid/file1.js");
    }

    @Test(expected = ScriptException.class)
    public void itThrowsAnExceptionIfTryingToGoAboveTheTopLevelFolder() throws Throwable {
        // We need two ".." because otherwise the resolving attempts to load from
        // "node_modules" and
        // ".." validly points to the root folder there.
        require.require("../../file1.js");
    }

    @Test
    public void theExceptionThrownForAnUnknownFileCanBeCaughtInJavaScriptAndHasTheProperCode() throws Throwable {
        String code = (String) engine
                .eval("(function() { try { require('./invalid'); } catch (ex) { return ex.message} })();");
        assertTrue(code.matches("Module not found: ./invalid.*"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void rootModulesExposeTheExpectedFields() throws Throwable {
        Object module = engine.eval("module");
        Object exports = engine.eval("exports");
        Object main = engine.eval("require.main");

        assertEquals(exports, get(module, "exports"));
        assertEquals(new ArrayList(), get(module, "children"));
        assertEquals("<main>", get(module, "filename"));
        assertEquals("<main>", get(module, "id"));
        assertEquals(true, get(module, "loaded"));
        assertEquals(null, get(module, "parent"));
        assertNotNull(exports);
        assertEquals(module, main);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void topLevelModulesExposeTheExpectedFields() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports._module = module; exports._exports = exports; "
                + "exports._main = require.main; exports._filename = __filename; exports._dirname = __dirname;");

        Object top = engine.eval("module");
        Object module = engine.eval("require('./file1')._module");
        Object exports = engine.eval("require('./file1')._exports");
        Object main = engine.eval("require('./file1')._main");

        assertEquals(exports, get(module, "exports"));
        assertEquals(new ArrayList(), get(module, "children"));
        assertEquals("/file1.js", get(module, "filename"));
        assertEquals("/file1.js", get(module, "id"));
        assertEquals(true, get(module, "loaded"));
        assertEquals(top, get(module, "parent"));
        assertNotNull(exports);
        assertEquals(top, main);

        assertEquals("file1.js", get(exports, "_filename"));
        assertEquals("", get(exports, "_dirname"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void subModulesExposeTheExpectedFields() throws Throwable {
        when(sub1.getFile("sub1file1.js")).thenReturn(
                "exports._module = module; exports._exports = exports; exports._main = require.main; exports._filename = __filename; exports._dirname = __dirname");

        Object top = engine.eval("module");
        Object module = engine.eval("require('./sub1/sub1file1')._module");
        Object exports = engine.eval("require('./sub1/sub1file1')._exports");
        Object main = engine.eval("require('./sub1/sub1file1')._main");

        assertEquals(exports, get(module, "exports"));
        assertEquals(new ArrayList(), get(module, "children"));
        assertEquals("/sub1/sub1file1.js", get(module, "filename"));
        assertEquals("/sub1/sub1file1.js", get(module, "id"));
        assertEquals(true, get(module, "loaded"));
        assertEquals(top, get(module, "parent"));
        assertNotNull(exports);
        assertEquals(top, main);

        assertEquals("sub1file1.js", get(exports, "_filename"));
        assertEquals("/sub1", get(exports, "_dirname"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void subSubModulesExposeTheExpectedFields() throws Throwable {
        when(sub1sub1.getFile("sub1sub1file1.js"))
                .thenReturn("exports._module = module; exports._exports = exports; exports._main = require.main;");

        Object top = engine.eval("module");
        Object module = engine.eval("require('./sub1/sub1/sub1sub1file1')._module");
        Object exports = engine.eval("require('./sub1/sub1/sub1sub1file1')._exports");
        Object main = engine.eval("require('./sub1/sub1/sub1sub1file1')._main");

        assertEquals(exports, get(module, "exports"));
        assertEquals(new ArrayList(), get(module, "children"));
        assertEquals("/sub1/sub1/sub1sub1file1.js", get(module, "filename"));
        assertEquals("/sub1/sub1/sub1sub1file1.js", get(module, "id"));
        assertEquals(true, get(module, "loaded"));
        assertEquals(top, get(module, "parent"));
        assertNotNull(exports);
        assertEquals(top, main);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void requireInRequiredModuleYieldExpectedParentAndChildren() throws Throwable {
        when(root.getFile("file1.js"))
                .thenReturn("exports._module = module; exports.sub = require('./sub1/sub1file1');");
        when(sub1.getFile("sub1file1.js")).thenReturn("exports._module = module;");

        Object top = engine.eval("module");
        Object module = engine.eval("require('./file1')._module");
        Object subModule = engine.eval("require('./file1').sub._module");

        assertEquals(null, get(top, "parent"));
        assertEquals(top, get(module, "parent"));
        assertEquals(module, get(subModule, "parent"));
        assertEquals(module, ((ArrayList) get(top, "children")).get(0));
        assertEquals(subModule, ((ArrayList) get(module, "children")).get(0));
        assertEquals(new ArrayList(), get(subModule, "children"));
    }

    @Test
    public void loadedIsFalseWhileModuleIsLoadingAndTrueAfter() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports._module = module; exports._loaded = module.loaded;");

        Object top = engine.eval("module");
        Object module = engine.eval("require('./file1')._module");
        boolean loaded = (boolean) engine.eval("require('./file1')._loaded");

        assertTrue((boolean) get(top, "loaded"));
        assertFalse(loaded);
        assertTrue((boolean) get(module, "loaded"));
    }

    @Test
    public void loadingTheSameModuleTwiceYieldsTheSameObject() throws Throwable {
        Object first = engine.eval("require('./file1');");
        Object second = engine.eval("require('./file1');");
        assertTrue(first.equals(second));
    }

    @Test
    public void loadingTheSameModuleFromASubModuleYieldsTheSameObject() throws Throwable {
        when(root.getFile("file2.js")).thenReturn("exports.sub = require('./file1');");
        Object first = engine.eval("require('./file1');");
        Object second = engine.eval("require('./file2').sub;");
        assertTrue(first.equals(second));
    }

    @Test
    public void loadingTheSameModuleFromASubPathYieldsTheSameObject() throws Throwable {
        when(sub1.getFile("sub1file1.js")).thenReturn("exports.sub = require('../file1');");
        Object first = engine.eval("require('./file1');");
        Object second = engine.eval("require('./sub1/sub1file1').sub;");
        assertTrue(first.equals(second));
    }

    @Test
    public void scriptCodeCanReplaceTheModuleExportsSymbol() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("module.exports = { 'foo': 'bar' }");
        assertEquals("bar", engine.eval("require('./file1').foo;"));
    }

    @Test
    public void itIsPossibleToRegisterGlobalVariablesForAllModules() throws Throwable {
        engine.put("bar", "bar");
        when(root.getFile("file1.js")).thenReturn("exports.foo = function() { return bar; }");
        assertEquals("bar", engine.eval("require('./file1').foo();"));
    }

    @Test
    public void engineScopeVariablesAreVisibleDuringModuleLoad() throws Throwable {
        engine.put("bar", "bar");
        when(root.getFile("file1.js"))
                .thenReturn("var found = bar == 'bar'; exports.foo = function() { return found; }");
        assertEquals(true, engine.eval("require('./file1').foo();"));
    }

    @Test
    public void itCanLoadModulesFromModulesFromModules() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports.sub = require('./file2.js');");
        when(root.getFile("file2.js")).thenReturn("exports.sub = require('./file3.js');");
        when(root.getFile("file3.js")).thenReturn("exports.foo = 'bar';");
        assertEquals("bar", engine.eval("require('./file1.js').sub.sub.foo"));
    }

    // Check for https://github.com/coveo/nashorn-commonjs-modules/issues/2
    @Test
    public void itCanCallFunctionsNamedGetFromModules() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports.get = function(foo) { return 'bar'; };");

        assertEquals("bar", engine.eval("require('./file1.js').get(123, 456)"));
    }

    // Checks for https://github.com/coveo/nashorn-commonjs-modules/issues/3

    // This one only failed on older JREs
    @Test
    public void itCanUseHighlightJsLibraryFromNpm() throws Throwable {
        File file = new File("src/test/resources/com/github/alanger/commonjs_modules/test2");
        FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");
        require = Require.enable(engine, root);
        engine.eval("require('highlight.js').highlight('java', '\"foo\"')");
    }

    // This one failed on more recent ones too
    @Test
    public void anotherCheckForIssueNumber3() throws Throwable {
        when(root.getFile("file1.js")).thenReturn(
                "var a = require('./file2'); function b() {}; b.prototype = Object.create(a.prototype, {});");
        when(root.getFile("file2.js"))
                .thenReturn("module.exports = a; function a() {}; a.prototype = Object.create(Object.prototype, {})");
        require = Require.enable(engine, root);
        engine.eval("require('./file1');");
    }

    // Check for https://github.com/coveo/nashorn-commonjs-modules/issues/4
    @Test
    public void itSupportOverwritingExportsWithAString() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("module.exports = 'foo';");
        assertEquals("foo", engine.eval("require('./file1.js')"));
    }

    // Check for https://github.com/coveo/nashorn-commonjs-modules/issues/4
    @Test
    public void itSupportOverwritingExportsWithAnInteger() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("module.exports = 123;");
        assertEquals(123, engine.eval("require('./file1.js')"));
    }

    // Checks for https://github.com/coveo/nashorn-commonjs-modules/issues/11

    @Test
    public void itCanLoadInvariantFromFbjs() throws Throwable {
        File file = new File("src/test/resources/com/github/alanger/commonjs_modules/test3");
        FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");
        require = Require.enable(engine, root);
        engine.eval("require('fbjs/lib/invariant')");
    }

    // Checks for https://github.com/coveo/nashorn-commonjs-modules/pull/14

    @Test
    public void itCanShortCircuitCircularRequireReferences() throws Throwable {
        File file = new File("src/test/resources/com/github/alanger/commonjs_modules/test4/cycles");
        FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");
        require = Require.enable(engine, root);
        engine.eval("require('./main.js')");
    }

    @Test
    public void itCanShortCircuitDeepCircularRequireReferences() throws Throwable {
        File file = new File("src/test/resources/com/github/alanger/commonjs_modules/test4/deep");
        FilesystemFolder root = FilesystemFolder.create(file, "UTF-8");
        require = Require.enable(engine, root);
        engine.eval("require('./main.js')");
    }

    // Checks for https://github.com/coveo/nashorn-commonjs-modules/issues/15

    @Test
    public void itCanDefinePropertiesOnExportsObject() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("Object.defineProperty(exports, '__esModule', { value: true });");
        engine.eval("require('./file1.js')");
    }

    @Test
    public void itIncludesFilenameInException() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("\n\nexports.foo = function() { throw \"bad thing\";};");
        try {
            engine.eval("require('./file1').foo();");
            fail("should throw exception");
        } catch (ScriptException e) {
            // Nashorn javax.script.ScriptException: bad thing in /file1.js at line number 3 at column number 27
            // Rhino org.mozilla.javascript.JavaScriptException: bad thing (/file1.js#3) in /file1.js at line number 3
            // Graaljs javax.script.ScriptException: org.graalvm.polyglot.PolyglotException: bad thing
            assertTrue(e.getMessage().matches(".*bad thing .*in /file1.js at line number 3.*")
                    || e.getCause().getStackTrace()[0].toString().matches(".*\\(/file1.js:3\\)"));
        }
    }

    // Checks for https://github.com/coveo/nashorn-commonjs-modules/issues/22

    @Test
    public void itCanLoadModulesWhoseLastLineIsAComment() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports.foo = \"bar\";\n// foo");
        assertEquals("bar", engine.eval("require('./file1.js').foo"));
    }

    @Test
    public void createJavaString() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports.foo = new java.lang.String('bar')");
        assertEquals("bar", engine.eval("require('./file1.js').foo"));
    }

    @Test
    public void createJavaFile() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports.foo = new java.io.File('.')");
        assertEquals(new java.io.File("."), engine.eval("require('./file1.js').foo"));
    }

    @Test
    public void itCanLoadJavaClass() throws Throwable {
        when(root.getFile("file1.js")).thenReturn("exports.foo = java.lang.Class.forName('java.lang.String')");
        assertEquals(String.class, engine.eval("require('./file1.js').foo"));
    }

    @Test
    public void itCanImplementsJavaInterfaces() throws Throwable {
        when(root.getFile("file1.js"))
                .thenReturn("exports.foo = new java.lang.Runnable({run: function(){}, field: 'bar'})");
        assertTrue(engine.eval("require('./file1.js').foo") instanceof Runnable);
        assertTrue(Runnable.class.cast(engine.eval("require('./file1.js').foo")) instanceof Runnable);
    }

}
