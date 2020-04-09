package com.kewenc.sharedstorage

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*


/**
 * https://developer.android.com/guide/topics/providers/document-provider#kotlin
 * storage access framework -> SAF 存储访问框架
 */
class MainActivity : AppCompatActivity() , View.OnClickListener {

    companion object {
        private const val REQUEST_CODE_1 = 100
        private const val CACHE_NAME = "/cache_image.png"
        private const val READ_REQUEST_CODE: Int = 42
        private const val WRITE_REQUEST_CODE: Int = 43
        private const val EDIT_REQUEST_CODE: Int = 44
        private const val DELETE_REQUEST_CODE: Int = 45


//        /**
//         * Cursor查询时过滤字符串
//         * @return
//         */
//        fun cursorAppendForFilterSong(hasAnd: Boolean): String? {
//            val second: Int = CooApplication.getInstance().filterDuration
//            val secondLong = second * 1000.toLong()
//            val cursorAppendForFilterSong: String
//            cursorAppendForFilterSong = if (hasAnd) {
//                " AND " + MediaStore.Audio.Media.DURATION + " >= " + secondLong
//            } else {
//                MediaStore.Audio.Media.DURATION + " >= " + secondLong
//            }
//            //LogUtils.d("测试","SongsFilterUtils#cursorAppendForFilterSong"+" 过滤字符串为："+cursorAppendForFilterSong);
//            return cursorAppendForFilterSong
//        }

        private var mCursorCols = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_PENDING
        )


        fun getAllSongsAndType(context: Context) {
            val where = java.lang.StringBuilder()
            where.append(MediaStore.Audio.Media.TITLE + " != ''")
            where.append(" AND is_music = 1")
//            where.append(MusicUtils.cursorAppendForFilterSong(true))
//            LogUtils.d("", "##songSortOrder = $songSortOrder")
            var sortStr = ""
            sortStr = MediaStore.Audio.Media.ALBUM
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mCursorCols,
                    where.toString(),
                    null,
                    sortStr
                )
            } catch (e: Throwable) {
//                LogUtils.d("", "Error##" + e.message)
            }
            if (cursor == null) return
            if (cursor != null && cursor.moveToFirst()) {
                try {
                    do {
//                                cursor.getLong(0),  //id
//                                cursor.getLong(1),  //albumId
//                                cursor.getLong(2),  //artistId
//                                cursor.getString(3),  //title
//                                cursor.getString(4),  //album
//                                cursor.getString(5),  //artist
//                                cursor.getString(6),  //path
//                                cursor.getInt(7) //duration
                        Log.i("TAGF", "MusicLibraryUtils_id=" + cursor.getLong(0) + "_path=" + cursor.getString(6)+"_isPending=" + cursor.getString(7)
                        )
                    } while (cursor.moveToNext())
                } catch (e: Throwable) {
//                    LogUtils.d("", "Error##" + e.message)
                } finally {
                    cursor?.close()
                }
            }
//            return arrayList
        }

        fun deleteMedia(context: Context, fileId: Long): Int {
            var result: Int
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fileId)
            Log.i("TAGF", "MusicLibraryUtils_deleteMedia_uri=$uri")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                try {

                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, values, null, null)

                    Log.i("TAGF", "update")

                    result = context.contentResolver.delete(uri, null, null)
                } catch (e: RecoverableSecurityException) {
                    try {
                        (context as MainActivity).startIntentSenderForResult(
                            e.userAction.actionIntent.intentSender,
                            DELETE_REQUEST_CODE, null, 0, 0, 0
                        )
                    } catch (e2: SendIntentException) {
                        //                                        LogUtil.log("startIntentSender fail");
                    }
                }
                result = -1
            } else {
                result = context.contentResolver.delete(uri, null, null)
            }
            return result
        }

        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn.setOnClickListener(this)
        btn2.setOnClickListener(this)
        btn3.setOnClickListener(this)
        btn4.setOnClickListener(this)
        btn5.setOnClickListener(this)
        btn6.setOnClickListener(this)
    }

    /**
     * 处理结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e("TAGF", "requestCode="+requestCode)
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
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            data?.data?.also { uri ->
                Log.i("TAGF", "Uri: $uri")
//                showImage(uri)
                dumpImageMetaData(uri)
            }
        }
        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                Log.i("TAGF", "创建文档_Uri: $uri")
            }
        }
        if (requestCode == EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                Log.i("TAGF", "编辑文档_Uri: $uri")
                alterDocument(uri)
            }
        }
        if (requestCode == DELETE_REQUEST_CODE) {
            Log.i("TAGF", "删除音乐")
            data?.data?.also { uri ->
                Log.i("TAGF", "删除音乐_Uri: $uri")
            }

            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, edit.text.toString().toLong())

            contentResolver.update(uri, values, null, null)
            Log.i("TAGF", "删")
            val result = contentResolver.delete(uri, null, null)
            Log.i("TAGF", "删除音乐onActivityRe_uri="+uri+"_result="+result)
        }
    }

    private fun doWithBtn() {
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.action = Intent.ACTION_OPEN_DOCUMENT
        } else {
            intent.action = Intent.ACTION_GET_CONTENT
        }
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_1)
    }


    /**
     * 搜索文档
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    private fun performFileSearch() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                Intent.ACTION_OPEN_DOCUMENT
            else
                Intent.ACTION_GET_CONTENT
        ).apply {
            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            addCategory(Intent.CATEGORY_OPENABLE)

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            type = "image/*"
//                        type = "audio/*"
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    private fun dumpImageMetaData(uri: Uri) {

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        val cursor: Cursor? = contentResolver.query( uri, null, null, null, null, null)

        cursor?.use {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (it.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                val displayName: String =
                    it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                Log.i("TAGF", "Display Name: $displayName")

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                val size: String = if (!it.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                Log.i("TAGF", "Size: $size")
            }
        }

        getBitmapFromUri(uri)?.let {
            img.setImageBitmap(it)
        }
    }

    /**
     * 属于耗时操作
     */
    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val parcelFileDescriptor: ParcelFileDescriptor? = contentResolver?.openFileDescriptor(uri, "r")
        val fileDescriptor: FileDescriptor? = parcelFileDescriptor?.fileDescriptor
        val image: Bitmap? = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor?.close()
        return image
    }

    /**
     * 在此代码段中，系统会将文件行读取到字符串中
     */
    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    /**
     * // Here are some examples of how you might call this method.
    // The first parameter is the MIME type, and the second parameter is the name
    // of the file you are creating:
    //
    // createFile("text/plain", "foobar.txt");
    // createFile("image/png", "mypicture.png");

    // Unique request code.
     */
    private fun createFile(mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            // Create a file with the requested MIME type.
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    /**
     * 如果您获得了文档的 URI，并且文档的 Document.COLUMN_FLAGS 包含 SUPPORTS_DELETE，则便可删除该文档
     */
    private fun deleteDoc() {
//        DocumentsContract.deleteDocument(contentResolver, uri)
    }

    /**
     * Open a file for writing and append some text to it.
     */
    private fun editDocument() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's
        // file browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            // Filter to show only text files.
            type = "text/txt"
        }

        startActivityForResult(intent, EDIT_REQUEST_CODE)
    }

    /**
     * 最佳做法是请求获得最少的所需访问权限，因此如果您只需要写入权限，请勿请求获得读取/写入权限：
     */
    private fun alterDocument(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                // use{} lets the document provider know you're done by automatically closing the stream
                FileOutputStream(it.fileDescriptor).use {
                    it.write(
                        ("-------------------------Overwritten by MyCloud at ${System.currentTimeMillis()}\n").toByteArray()
                    )
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 当应用打开文件进行读取或写入时，系统会为其提供针对该文件的 URI 授权，有效期直至用户设备重启。但假定您的应用是图像编辑应用，而且您希望用户能直接从应用中访问其编辑的最后 5 张图像。如果用户的设备已重启，则您必须让用户回到系统选择器以查找这些文件，而这显然不是理想的做法。

    为防止出现此情况，您可以保留系统向应用授予的权限。实际上，您的应用是“获取”了系统提供的 URI 持久授权。如此一来，用户便可通过您的应用持续访问文件，即使设备已重启也不受影响：

    还有最后一个步骤。应用最近访问的 URI 可能不再有效，原因是另一个应用可能删除或修改了文档。因此，您应始终调用 getContentResolver().takePersistableUriPermission()，以检查有无最新数据。
     */
    private fun baoliuquanxian() {
//        val takeFlags: Int = intent.flags and
//                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//// Check for the freshest data.
//        contentResolver.takePersistableUriPermission(uri, takeFlags)
    }


    override fun onClick(v: View?) {
        v?.let {
            when (v.id) {
                R.id.btn -> doWithBtn()
                R.id.btn2 -> performFileSearch()
                R.id.btn3 -> createFile("text/txt", "myTxt.txt")
                R.id.btn4 -> editDocument()
                R.id.btn5 -> getAllSongsAndType(this)
                R.id.btn6 -> deleteMedia(this, edit.text.toString().toLong())
                else -> Log.e("TAGF", "")
            }
        }
    }
}
