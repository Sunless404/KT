package com.sunless404.keeptrack

import com.sunless404.keeptrack.update.ReleaseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseParserTest {
    @Test
    fun parsesLatestRelease() {
        val r = ReleaseParser.parse(
            """{"tag_name":"build-57","name":"KeepTrack 1.57","body":"Nuevo perfil\n","assets":[
               {"name":"notas.txt","browser_download_url":"https://x/notas.txt","size":3},
               {"name":"keeptrack-57.apk","browser_download_url":"https://github.com/Sunless404/KT/releases/download/build-57/keeptrack-57.apk","size":12345}]}"""
        )!!
        assertEquals(57, r.versionCode)
        assertEquals("KeepTrack 1.57", r.name)
        assertEquals("Nuevo perfil", r.notes)
        assertEquals(12345, r.sizeBytes)
        assertEquals("https://github.com/Sunless404/KT/releases/download/build-57/keeptrack-57.apk", r.apkUrl)
    }

    @Test
    fun ignoresReleasesWithoutApkOrWithOtherTags() {
        assertNull(ReleaseParser.parse("""{"tag_name":"build-3","assets":[]}"""))
        assertNull(ReleaseParser.parse("""{"tag_name":"v1.0","assets":[{"name":"a.apk","browser_download_url":"u"}]}"""))
        assertNull(ReleaseParser.parse("""[]"""))
    }
}
