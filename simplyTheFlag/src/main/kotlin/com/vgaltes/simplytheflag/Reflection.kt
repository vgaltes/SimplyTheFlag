package com.vgaltes.simplytheflag

import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.JarURLConnection
import java.net.URL
import java.net.URLConnection
import java.net.URLDecoder
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile

// https://stackoverflow.com/questions/520328/can-you-find-all-classes-in-a-package-using-reflection/22462785#22462785
@Throws(ClassNotFoundException::class)
private fun checkDirectory(
    directory: File, pckgname: String,
    availableFlags: MutableMap<String, String>
) {
    var tmpDirectory: File
    if (directory.exists() && directory.isDirectory) {
        val files = directory.list()
        for (file: String in files) {
            if (file.endsWith(".class")) {
                try {
                    val name = pckgname + '.' + file.substring(0, file.length - 6)
                    val maybeFlag = Class.forName(name)
                    val isFlag = maybeFlag.interfaces.any { i -> i.simpleName == Flag::class.java.simpleName }
                    if (isFlag) {
                        availableFlags[maybeFlag.simpleName] = maybeFlag.typeName
                    }
                } catch (e: NoClassDefFoundError) {
                    // do nothing. this class hasn't been found by the
                    // loader, and we don't care.
                }
            } else if (File(directory, file).also { tmpDirectory = it }
                    .isDirectory
            ) {
                checkDirectory(tmpDirectory, "$pckgname.$file", availableFlags)
            }
        }
    }
}


@Throws(ClassNotFoundException::class, IOException::class)
private fun checkJarFile(
    connection: JarURLConnection,
    pckgname: String,
    availableFlags: MutableMap<String, String>
) {
    val jarFile: JarFile = connection.getJarFile()
    val entries: Enumeration<JarEntry> = jarFile.entries()
    var name: String
    var jarEntry: JarEntry? = null
    while (entries.hasMoreElements() && entries.nextElement().also { jarEntry = it } != null) {
        name = jarEntry!!.getName()
        if (name.contains(".class")) {
            name = name.substring(0, name.length - 6).replace('/', '.')
            if (name.contains(pckgname)) {
                // classes.add(Class.forName(name))
                val maybeFlag = Class.forName(name)
                val isFlag = maybeFlag.interfaces.any { i -> i.simpleName == Flag::class.java.simpleName }
                if (isFlag) {
                    availableFlags[maybeFlag.simpleName] = maybeFlag.typeName
                }
            }
        }
    }
}

@Throws(ClassNotFoundException::class)
fun getClassesForPackage(pckgname: String, availableFlags: MutableMap<String, String>) {
    val classes = ArrayList<Class<*>>()
    try {
        val cld = Thread.currentThread()
            .contextClassLoader ?: throw ClassNotFoundException("Can't get class loader.")
        val resources = cld.getResources(
            pckgname
                .replace('.', '/')
        )
        var connection: URLConnection
        var url: URL? = null
        while (resources.hasMoreElements() && resources.nextElement().also { url = it } != null) {
            try {
                connection = url!!.openConnection()
                if (connection is JarURLConnection) {
                    checkJarFile(connection, pckgname, availableFlags)
                } else {
                    try {
                        checkDirectory(File(URLDecoder.decode(url!!.path, "UTF-8")), pckgname, availableFlags)
                    } catch (ex: UnsupportedEncodingException) {
                        throw ClassNotFoundException("$pckgname does not appear to be a valid package (Unsupported encoding)", ex)
                    }
                }
            } catch (ioex: IOException) {
                throw ClassNotFoundException(
                    ("IOException was thrown when trying to get all resources for $pckgname"), ioex)
            }
        }
    } catch (ex: NullPointerException) {
        throw ClassNotFoundException(
            ("$pckgname does not appear to be a valid package (Null pointer exception)"), ex)
    } catch (ioex: IOException) {
        throw ClassNotFoundException(
            ("IOException was thrown when trying to get all resources for $pckgname"), ioex)
    }
}