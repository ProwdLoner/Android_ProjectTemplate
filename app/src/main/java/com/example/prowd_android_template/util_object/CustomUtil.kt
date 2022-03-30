package com.example.prowd_android_template.util_object

import java.nio.ByteBuffer

object CustomUtil {
    fun cloneByteBuffer(original: ByteBuffer): ByteBuffer {
        val clone: ByteBuffer = ByteBuffer.allocate(original.capacity())
        original.rewind()
        clone.put(original)
        original.rewind()
        clone.flip()
        return clone
    }
}