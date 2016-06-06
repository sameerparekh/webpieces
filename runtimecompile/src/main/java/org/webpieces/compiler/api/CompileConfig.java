package org.webpieces.compiler.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.webpieces.util.file.VirtualFile;
import org.webpieces.util.file.VirtualFileImpl;

public class CompileConfig {

	private List<VirtualFile> javaPath;
	private VirtualFile byteCodeCacheDir;
	
	/**
	 * VirtualFiles can be created with new VirtualFileImpl
	 * 
	 * @param javaPath The list of paths we will compile code from
	 * @param byteCodeCacheDir The directory we use to cache class byte code so we can avoid recompiling in certain cases
	 */
	public CompileConfig(List<VirtualFile> javaPath, VirtualFile byteCodeCacheDir) {
		this.javaPath = javaPath;
		this.byteCodeCacheDir = byteCodeCacheDir;
	}
	
	public CompileConfig(VirtualFile javaPath) {
		this(createList(javaPath), getTmpDir());
	}

	private static VirtualFile getTmpDir() {
		String tmpPath = System.getProperty("java.io.tmpdir");
		return new VirtualFileImpl(new File(tmpPath, "bytecode"));
	}

	private static List<VirtualFile> createList(VirtualFile javaPath) {
		List<VirtualFile> list = new ArrayList<>();
		list.add(javaPath);
		return list;
	}
	
	public List<VirtualFile> getJavaPath() {
		return javaPath;
	}

	public VirtualFile getByteCodeCacheDir() {
		return byteCodeCacheDir;
	}

}
