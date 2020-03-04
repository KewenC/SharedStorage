package com.kewenc.sharedstorage

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.util.Log
import android.view.View
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_1 = 100
    private val CACHE_NAME = "/cache_image.png"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun doWithBtn(view: View) {
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.action = Intent.ACTION_OPEN_DOCUMENT
        } else {
            intent.action = Intent.ACTION_GET_CONTENT
        }
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_1) {
            var path = ""
            val uri = data?.data ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = this.contentResolver
                var file: File? = null
                this.getExternalFilesDir(DIRECTORY_PICTURES)?.let { file = File(it.path) }
                if (file == null) return
                if (file?.exists() == false) {
                    file?.mkdirs()
                }
                resolver.openFileDescriptor(uri, "r").use { parcelFileDescriptor ->
                    parcelFileDescriptor?.let {
                        val fileInputStream = FileInputStream(it.fileDescriptor)
                        val fileOutputStream = FileOutputStream(file!!.path + CACHE_NAME)
                        val buffer = ByteArray(1024 * 4)
                        var count = 0
                        while ({ count = fileInputStream.read(buffer);count }() > 0) {
                            fileOutputStream.write(buffer, 0, count)
                        }
                        fileOutputStream.flush()
                        fileInputStream.close()
                        fileInputStream.close()
                        path = file!!.path + CACHE_NAME
                        Log.e("TAGF", "path=$path")
                    }
                }
            }
        }
    }
}
