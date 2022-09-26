package seed.config

import java.io.FileNotFoundException
import java.net.URL

fun classpathResource(path: String): URL {
    if (path[0] != '/') {
        throw IllegalArgumentException("Class path resource $path must begin with a /")
    }
    return object {}.javaClass.getResource(path) ?: throw FileNotFoundException(path)
}
