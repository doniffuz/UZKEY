package uz.ozkey.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

/** gradle-wrapper.properties: -all distributiv va rasmiy SHA-256 mosligi. */
class WrapperConfigTest {

    private val official811All = "89d4e70e4e84e2d2dfbb63e4daa53e21b25017cc70c37e4eea31ee51fb15098a"

    private fun wrapperDir(): File? {
        var d: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (d != null) {
            val f = File(d, "gradle/wrapper/gradle-wrapper.properties")
            if (f.isFile) return f.parentFile
            d = d.parentFile
        }
        return null
    }

    private fun props(): Properties {
        val dir = wrapperDir()
        assertNotNull("gradle/wrapper topilmadi", dir)
        return Properties().also { p -> File(dir, "gradle-wrapper.properties").inputStream().use { p.load(it) } }
    }

    @Test fun distributionUrlIsOfficialHttpsAll() {
        val url = props().getProperty("distributionUrl")
        assertNotNull(url)
        assertTrue(url, url.startsWith("https://services.gradle.org/distributions/"))
        assertTrue("-all distributiv bo'lishi kerak: $url", url.endsWith("-all.zip"))
        assertEquals("true", props().getProperty("validateDistributionUrl"))
    }

    @Test fun checksumMatchesOfficial811All() {
        val p = props()
        val sum = p.getProperty("distributionSha256Sum")
        assertNotNull("distributionSha256Sum yo'q", sum)
        assertTrue("64 ta kichik hex bo'lishi kerak", Regex("^[0-9a-f]{64}$").matches(sum))
        if (p.getProperty("distributionUrl").endsWith("/gradle-8.11.1-all.zip")) {
            assertEquals(official811All, sum)
        }
    }

    @Test fun ciValidatesWrapperJarAndScriptsExist() {
        val root = wrapperDir()!!.parentFile.parentFile // gradle/wrapper -> gradle -> loyiha ildizi
        assertTrue(File(root, "gradlew").isFile)
        assertTrue(File(root, "gradlew.bat").isFile)
        val wf = File(root, ".github/workflows/build.yml")
        assertTrue(wf.isFile)
        assertTrue("CI da gradle/actions/wrapper-validation bo'lishi kerak", wf.readText().contains("gradle/actions/wrapper-validation"))
    }

    @Test fun wrapperJarIsPresentAndLooksLikeZip() {
        val jar = File(wrapperDir(), "gradle-wrapper.jar")
        assertTrue("gradle-wrapper.jar yo'q", jar.isFile)
        val head = jar.inputStream().use { s -> ByteArray(2).also { s.read(it) } }
        assertEquals('P'.code, head[0].toInt())
        assertEquals('K'.code, head[1].toInt())
    }
}
