package com.kewenc.sharedstorage

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.provider.BaseColumns
import android.provider.DocumentsContract
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

        private const val SAF_DELETE: Int = 46
        private const val SAF_RENAME: Int = 47
        private const val SAF_APPLY_PERMISSION: Int = 48

        private const val AMEND_REQUEST_CODE: Int = 49




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
            MediaStore.Audio.Media.SIZE
//            MediaStore.Audio.Media.IS_PENDING
        )

        /**
         * 扫描图片
         */
        fun scanPicture(context: Context) {
            val cursor: Cursor? = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, null,
                null, null)
            cursor?.let {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndex(MediaStore.Images.Media._ID))
                    val path = it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
                    Log.i("TAGF", "图片id="+id+"_path=" + path+"_uri"+ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
                }
                cursor.close()
            }
        }

        /**
         * 扫描歌曲
         */
        fun scanMusic(context: Context) {
            Log.i("TAGF", "GGGGGGGGGGGGGGGGGG")
            val where = java.lang.StringBuilder()
            where.append(MediaStore.Audio.Media.TITLE + " != ''")
            where.append(" AND is_music = 1")
            val sortStr =  MediaStore.Audio.Media.ALBUM
            val cursor: Cursor? = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mCursorCols,
                where.toString(),
                null,
                sortStr
            )
            cursor?.let {
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getLong(cursor.getColumnIndex(mCursorCols[0]))  //id
                        val albumId = cursor.getLong(cursor.getColumnIndex(mCursorCols[1]))  //albumId
                        val artistId = cursor.getLong(cursor.getColumnIndex(mCursorCols[2]))  //artistId
                        val title = cursor.getString(cursor.getColumnIndex(mCursorCols[3]))  //title
                        val album = cursor.getString(cursor.getColumnIndex(mCursorCols[4]))  //album
                        val artist = cursor.getString(cursor.getColumnIndex(mCursorCols[5]))  //artist
                        val path = cursor.getString(cursor.getColumnIndex(mCursorCols[6]))  //path
                        val duration = cursor.getInt(cursor.getColumnIndex(mCursorCols[7])) //duration
                        val size = cursor.getInt(cursor.getColumnIndex(mCursorCols[8]))
                        Log.i("TAGF", "scanMusic_id="
                                + id +"_"
                                + albumId +"_"
                                + artistId +"_"
                                + title +"_"
                                + album +"_"
                                + artist +"_"
                                + path +"_"
                                + duration +"_"
                                + size+"字节")
                    } while (cursor.moveToNext())
                }
            }
        }

        fun amendMedia(context: Context, fileId: Long) {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fileId)
            Log.i("TAGF", "uri=$uri")
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Audio.Media.TITLE, "mTitle")
            contentValues.put(MediaStore.Audio.Media.ALBUM, "mAlbum")
            contentValues.put(MediaStore.Audio.Media.ARTIST, "mArtist")
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            var result: Int = -1
            var resultPending: Int = -1
//            var resultPendingLater: Int = -1
            try {
                val contentValuesPending = ContentValues()
                contentValuesPending.put(MediaStore.Audio.Media.IS_PENDING, 1)
                resultPending = context.contentResolver.update(uri, contentValuesPending,null, null)

                result = context.contentResolver.update(uri, contentValues, null, null)

//                contentValuesPending.clear()
//                contentValuesPending.put(MediaStore.Audio.Media.IS_PENDING, 0)
//                resultPendingLater = context.contentResolver.update(uri, contentValuesPending,null, null)
            } catch (e: RecoverableSecurityException) {
                Log.e("TAGF", "e:"+e.message.toString())
                try {
                    (context as MainActivity).startIntentSenderForResult(
                        e.userAction.actionIntent.intentSender,
                        AMEND_REQUEST_CODE, null, 0, 0, 0, null)
                } catch (e2: SendIntentException) {
                    Log.e("TAGF", "e2:"+e.message.toString())
                }
            }
            Log.e("TAGF", "result=${result}_resultPending=${resultPending}")
//            val result = context.contentResolver.update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                contentValues,
//                "_id=?",
//                arrayOf(fileId.toString()))

        }

        fun deleteMedia(context: Context, fileId: Long){
            var result: Int = -1
            var resultPending: Int = -1
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fileId)
            Log.i("TAGF", "uri=$uri")
            try {
                val contentValuesPending = ContentValues()
                contentValuesPending.put(MediaStore.Audio.Media.IS_PENDING, 1)
                resultPending = context.contentResolver.update(uri, contentValuesPending,null, null)

                result = context.contentResolver.delete(uri, null, null)
            } catch (e: RecoverableSecurityException) {
                Log.e("TAGF", "e:"+e.message.toString())
                try {
                    (context as MainActivity).startIntentSenderForResult(
                        e.userAction.actionIntent.intentSender,
                        DELETE_REQUEST_CODE, null, 0, 0, 0, null)
                } catch (e2: SendIntentException) {
                    Log.e("TAGF", "e2:"+e.message.toString())
                }
            }

            Log.e("TAGF", "result=${result}_resultPending=${resultPending}")

//            var result: Int = -1
//            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileId)
//
//            val docUri = MediaStore.getDocumentUri(context, uri)
//            DocumentsContract.deleteDocument(context.contentResolver, docUri)

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val docUri = MediaStore.getDocumentUri(context , uri)
//                DocumentsContract.deleteDocument(context.contentResolver, docUri)
//            }


//            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3Alingshi%2FmyPicture2.png")

//            Log.i("TAGF", "MusicLibraryUtils_deleteMedia_uri=$uri")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                try {
//
//                    MediaStore.setIncludePending(uri)
//                    result = context.contentResolver.delete(uri, null, null)
//                } catch (e: RecoverableSecurityException) {
//                    try {
//                        (context as MainActivity).startIntentSenderForResult(
//                            e.userAction.actionIntent.intentSender,
//                            DELETE_REQUEST_CODE, null, 0, 0, 0, null)
//                    } catch (e2: SendIntentException) {
//                        //                                        LogUtil.log("startIntentSender fail");
//                    }
//                }
//                result = -1
//            } else {
//                result = context.contentResolver.delete(uri, null, null)
//            }
//            return result
        }

        }

//    MusicLibraryUtils_addMusicToPlay_musicID=55116_playListID=41481
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn.setOnClickListener(this)
        btn2.setOnClickListener(this)
        btn3.setOnClickListener(this)
        btn4.setOnClickListener(this)
        btn41.setOnClickListener(this)
        btn5.setOnClickListener(this)
        btn6.setOnClickListener(this)
        btn61.setOnClickListener(this)
        btn7.setOnClickListener(this)
        btn8.setOnClickListener(this)
        btn9.setOnClickListener(this)
        btn10.setOnClickListener(this)
        btn11.setOnClickListener(this)
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
            Log.e("TAGF", "onActivityResult_100="+uri)
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
//            // The document selected by the user won't be returned in the intent.
//            // Instead, a URI to that document will be contained in the return intent
//            // provided to this method as a parameter.
//            // Pull that URI using resultData.getData().
//            data?.data?.also { uri ->
//                Log.i("TAGF", "Uri: $uri")
////                showImage(uri)
//                dumpImageMetaData(uri)
//            }

            Log.i("TAGF", "musicPlayList")
            addMusicPlayList()
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
            deleteMedia(this, edit.text.toString().toLong())

////            val values = ContentValues().apply {
////                put(MediaStore.Audio.Media.IS_PENDING, 0)
////            }
//            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, edit.text.toString().toLong())
////            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3Alingshi%2FmyPicture2.png")
//
////            contentResolver.update(uri, values, null, null)
////            Log.i("TAGF", "删")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                MediaStore.setIncludePending(uri)
//            }
//            val result = contentResolver.delete(uri, null, null)
//
////            val docUri = MediaStore.getDocumentUri(this, uri)
////            DocumentsContract.deleteDocument(contentResolver, docUri)
//
//            Log.i("TAGF", "删除音乐onActivityRe_uri="+uri+"_result=")
        }
        if (requestCode == 0x400) {
            Log.i("TAGF", "on_addMusicPlayList")
            addMusicPlayList()
        }
        if (requestCode == SAF_DELETE) {
            data?.data?.also { uri ->
                Log.i("TAGF", "SAF删除音乐_Uri: $uri")
                DocumentsContract.deleteDocument(contentResolver, uri)
            }
        }
        if (requestCode == SAF_RENAME) {
            data?.data?.also { uri ->
                Log.i("TAGF", "SAF重命名音乐_Uri: $uri")
                DocumentsContract.renameDocument(contentResolver, uri, "newFileName.jpeg")
            }
        }
        if (requestCode == SAF_APPLY_PERMISSION) {
//            var uriTree = null
            data?.flags
            data?.data?.also { uriTree ->

                val takeFlags = data.flags.and(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                contentResolver.takePersistableUriPermission(uriTree, takeFlags)

                // create DocumentFile which represents the selected directory
//                val root  = DocumentFile.fromTreeUri(this, uriTree)
                // list all sub dirs of root
//                    DocumentFile[] files = root.listFiles()
                // do anything you want with APIs provided by DocumentFile
                // ...
//
//                for (DocumentFile file : documentFile.listFiles()) {
//                if (file.isFile() && "test.txt".equals(file.getName())) {
//                    boolean delete = file.delete();
//                    LogUtil.log("deleteFile: " + delete);
//                    break;
//                }
//            }

//                DocumentFile root = DocumentFile.fromTreeUri(this,path);
//                //在根目录下，查找名为handleCreateDocument的子目录
//                DocumentFile dpath = root.findFile("handleCreateDocument");
//                //如果该子目录不存在，则创建
//                if(dpath==null) {
//                    dpath = root.createDirectory("handleCreateDocument");
//                }
//                //在handleCreateDocument子目录下，创建一个text类型的Document文件
//                DocumentFile dfile = dpath.createFile("text/*",name);
//                //获取该Document的输入流，并写入数据
//                os = getContentResolver().openOutputStream(dfile.getUri());
//                ————————————————
//                版权声明：本文为CSDN博主「喵咪星球长」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
//                原文链接：https://blog.csdn.net/hyc1988107/article/details/83825237


                Log.i("TAGF", "SAF申请目录权限_Uri: $uriTree")

//                val contentValues = ContentValues()
//                contentValues.put(MediaStore.Audio.Media.TITLE, "mTitle")
//                contentValues.put(MediaStore.Audio.Media.ALBUM, "mAlbum")
//                contentValues.put(MediaStore.Audio.Media.ARTIST, "mArtist")
//                val result = contentResolver.update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                    contentValues,
//                    "_id=?",
//                    arrayOf(edit.text.toString().toLong().toString()))
//                Log.e("TAGF", "result = $result")

                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, edit.text.toString().toLong())
                val docUri = MediaStore.getDocumentUri(this, uri)
                DocumentsContract.deleteDocument(contentResolver, docUri)

                val uri2 = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, edit2.text.toString().toLong())
                val docUri2 = MediaStore.getDocumentUri(this, uri2)
                DocumentsContract.deleteDocument(contentResolver, docUri2)

//                DocumentsContract.renameDocument(contentResolver, docUri, "aa.jpeg")
            }

        }
        if (requestCode == AMEND_REQUEST_CODE) {
            data?.data?.also { uri ->
                Log.i("TAGF", "修改音乐_Uri: $uri")
            }
            amendMedia(this, edit.text.toString().toLong())
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
//            type = "image/*"
                        type = "audio/*"
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
                R.id.btn41 -> scanPicture(this)
                R.id.btn5 -> scanMusic(this)
                R.id.btn6 -> deleteMedia(this, edit.text.toString().toLong())
                R.id.btn61 -> amendMedia(this, edit.text.toString().toLong())
                R.id.btn7 -> getPlayLists(this)
                R.id.btn8 -> addMusicPlayList()
                R.id.btn9 -> SafDelete()
                R.id.btn10 -> SafRename()
                R.id.btn11 -> SafApplyPermission()
                else -> Log.e("TAGF", "")
            }
        }
    }

    private fun SafApplyPermission() {
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//        startActivityForResult(intent, SAF_APPLY_PERMISSION)
        val storageManager = getSystemService(StorageManager::class.java)
        val storageVolume = storageManager.primaryStorageVolume
//        val storageVolume = storageManager.get
        startActivityForResult(storageVolume.createOpenDocumentTreeIntent(), SAF_APPLY_PERMISSION)
    }

    private fun SafRename() {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
//        intent.data = Uri.parse("content://media/external/images/media/53")
        intent.type = "image/jpeg"
        startActivityForResult(intent, SAF_RENAME)
    }

    private fun SafDelete() {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
//        intent.data = Uri.parse("content://media/external/images/media/53")
        intent.type = "image/jpeg"
        startActivityForResult(intent, SAF_DELETE)
    }

    private fun addMusicPlayList(): Boolean {
        var uri = MediaStore.Audio.Playlists.Members.getContentUri("external", 41481)
        val v = ContentValues()
        v.put("audio_id", 55116)
        v.put("playlist_id", 41481)
//        v.put("play_order", musicID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return try {
                contentResolver.insert(uri, v) != null
            } catch (e: RecoverableSecurityException) {
                try {
                    startIntentSenderForResult(
                        e.userAction.actionIntent.intentSender,
                        0x400,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (ex: SendIntentException) {
                    ex.printStackTrace()
                }
                false
            }
        } else {
            return contentResolver.insert(uri, v) != null
        }
    }


    fun makePlaylistCursor(context: Context): Cursor? {
        return try {
            context.contentResolver.query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, arrayOf(
                    BaseColumns._ID,
                    MediaStore.Audio.PlaylistsColumns.NAME
                ), null, null, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
            )
        } catch (e: SQLiteException) {
            null
        }
    }

    fun getPlayLists(context: Context) {
        val mCursor = makePlaylistCursor(context)
        Log.e("TAGF", "getPlayLists_mCursor="+(mCursor == null))
        if (mCursor != null && mCursor.moveToFirst()) {
            do {
                val id = mCursor.getLong(0);
                val name = mCursor.getString(1)
//                val songCount = getSongCountForPlaylist(context, id);
                Log.e("TAGF", "getPlayLists_id="+id+"_name="+name)
            } while (mCursor.moveToNext());
        }
        mCursor?.close()
    }
}
