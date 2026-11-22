import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URLClassLoader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

// Writes the release APK's baseline profile as dex metadata (.dm) files, one per profile format Android reads at
// install time: as is for Android 9 to 11, and transcoded for Android 12 and later by the same code ProfileInstaller
// runs on the device. The in-app update installs the matching one next to the APK, so a box compiles the new version
// while installing it.
class DexMetadataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val transcoder = target.configurations.create("dexMetadataTranscoder") {
            isCanBeConsumed = false
            isTransitive = false
        }
        target.dependencies.add(transcoder.name, "androidx.profileinstaller:profileinstaller:$PROFILE_INSTALLER@aar")
        target.tasks.register<DexMetadataTask>("dexMetadata") {
            dependsOn("assembleRelease")
            library.from(transcoder)
            apk.set(target.layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk"))
            outputDir.set(target.layout.buildDirectory.dir("outputs/dexmetadata"))
        }
    }

    private companion object {
        const val PROFILE_INSTALLER = "1.4.1"
    }
}

abstract class DexMetadataTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val library: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apk: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun write() {
        val (profile, metadata) = ZipFile(apk.get().asFile).use { zip ->
            zip.read("assets/dexopt/baseline.prof") to zip.read("assets/dexopt/baseline.profm")
        }
        val out = outputDir.get().asFile.apply { mkdirs() }
        writeDm(File(out, "profile-p.dm"), profile)
        writeDm(File(out, "profile-s.dm"), transcodeForS(profile, metadata))
    }

    private fun ZipFile.read(name: String): ByteArray =
        getInputStream(getEntry(name) ?: error("The APK has no $name")).use { it.readBytes() }

    private fun writeDm(target: File, profile: ByteArray) {
        ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("primary.prof").apply { time = 0L })
            zip.write(profile)
            zip.closeEntry()
        }
    }

    // ProfileTranscoder is internal to the library, so it is reached through its own class loader.
    private fun transcodeForS(profile: ByteArray, metadata: ByteArray): ByteArray {
        val classes = File(temporaryDir, "profileinstaller-classes.jar")
        ZipFile(library.singleFile).use { aar -> classes.writeBytes(aar.read("classes.jar")) }
        URLClassLoader(arrayOf(classes.toURI().toURL()), javaClass.classLoader).use { loader ->
            val transcoder = loader.loadClass("androidx.profileinstaller.ProfileTranscoder")
            val versions = loader.loadClass("androidx.profileinstaller.ProfileVersion")
            val data = loader.loadClass("androidx.profileinstaller.DexProfileData")
            val dataArray = java.lang.reflect.Array.newInstance(data, 0).javaClass
            fun constant(owner: Class<*>, name: String) = owner.getDeclaredField(name).apply { isAccessible = true }.get(null) as ByteArray
            fun method(name: String, vararg types: Class<*>) = transcoder.getDeclaredMethod(name, *types).apply { isAccessible = true }
            val targetVersion = constant(versions, "V015_S")
            val readHeader = method("readHeader", InputStream::class.java, ByteArray::class.java)
            val profileStream = ByteArrayInputStream(profile)
            val profileVersion = readHeader.invoke(null, profileStream, constant(transcoder, "MAGIC_PROF"))
            var entries = method("readProfile", InputStream::class.java, ByteArray::class.java, String::class.java)
                .invoke(null, profileStream, profileVersion, "base.apk")
            val metadataStream = ByteArrayInputStream(metadata)
            val metadataVersion = readHeader.invoke(null, metadataStream, constant(transcoder, "MAGIC_PROFM"))
            entries = method("readMeta", InputStream::class.java, ByteArray::class.java, ByteArray::class.java, dataArray)
                .invoke(null, metadataStream, metadataVersion, targetVersion, entries)
            val output = ByteArrayOutputStream()
            method("writeHeader", OutputStream::class.java, ByteArray::class.java).invoke(null, output, targetVersion)
            val written = method("transcodeAndWriteBody", OutputStream::class.java, ByteArray::class.java, dataArray)
                .invoke(null, output, targetVersion, entries) as Boolean
            check(written) { "The baseline profile could not be transcoded for Android 12" }
            return output.toByteArray()
        }
    }
}
