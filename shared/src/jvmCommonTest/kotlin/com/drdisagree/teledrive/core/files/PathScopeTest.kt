package com.drdisagree.teledrive.core.files

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PathScopeTest {

    @Test
    fun `file directly in the folder is inside`() {
        assertTrue(PathScope.inside("/sdcard/DCIM/a.jpg", "/sdcard/DCIM"))
    }

    @Test
    fun `file in a subfolder is inside`() {
        assertTrue(PathScope.inside("/sdcard/DCIM/camera/a.jpg", "/sdcard/DCIM"))
    }

    @Test
    fun `the folder itself is inside`() {
        assertTrue(PathScope.inside("/sdcard/DCIM", "/sdcard/DCIM/"))
    }

    @Test
    fun `sibling with a shared prefix is outside`() {
        assertFalse(PathScope.inside("/sdcard/DCIM2/a.jpg", "/sdcard/DCIM"))
    }

    @Test
    fun `windows separators and case are ignored`() {
        assertTrue(PathScope.inside("C:\\Users\\me\\DCIM\\a.jpg", "c:/users/me/dcim"))
    }

    @Test
    fun `blank folder matches nothing`() {
        assertFalse(PathScope.inside("/sdcard/DCIM/a.jpg", "/"))
    }
}
