package com.bornbytes.walkietalkie

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vincent.filepicker.Constant
import com.vincent.filepicker.filter.entity.NormalFile
import kotlinx.android.synthetic.shareFile.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(if (isTv()) R.layout.activity_main_tv else R.layout.activity_main)

        send_textView.setOnClickListener {

           // startActivity(Intent(this, CategoryActivity::class.java))

           /* val intent4 = Intent(this, NormalFilePickActivity::class.java)
            intent4.putExtra(Constant.MAX_NUMBER, 10)
            intent4.putExtra(NormalFilePickActivity.SUFFIX, arrayOf("xlsx", "xls", "doc", "docx", "ppt", "pptx", "pdf"))
            startActivityForResult(intent4, Constant.REQUEST_CODE_PICK_FILE)*/
           /* MaterialFilePicker()
                    .withActivity(this)
                    .withRequestCode(1)
                    .withCloseMenu(false)
                    .withFilter(Pattern.compile(".*\\.mp4$")) // Filtering files and directories by file name using regexp
                    //.withFilterDirectories(true) // Set directories filterable (false by default)
                    //.withHiddenFiles(true) // Show hidden files and folders
                    .start()*/

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            val i = Intent.createChooser(intent, "File")
            startActivityForResult(i, 1)
        }

        receive_textView.setOnClickListener {

            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
           // val filePath = data?.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)

           // val list: ArrayList<NormalFile>? = data?.getParcelableArrayListExtra(Constant.RESULT_PICK_FILE)
            val intent = Intent(this, HomeActivity::class.java)
          //  intent.putExtra("file", list?.get(0)?.path)
            intent.putExtra("file", data?.data)
            startActivity(intent)
        }
    }
}
