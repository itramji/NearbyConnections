package com.bornbytes.walkietalkie

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vincent.filepicker.Constant
import com.vincent.filepicker.activity.ImagePickActivity
import com.vincent.filepicker.activity.ImagePickActivity.IS_NEED_CAMERA
import com.vincent.filepicker.activity.NormalFilePickActivity
import com.vincent.filepicker.activity.VideoPickActivity
import com.vincent.filepicker.filter.entity.BaseFile
import com.vincent.filepicker.filter.entity.ImageFile
import com.vincent.filepicker.filter.entity.NormalFile
import com.vincent.filepicker.filter.entity.VideoFile
import kotlinx.android.synthetic.main.activity_category.*
import java.util.*

class CategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)



        image_tv.setOnClickListener {
            val intent = Intent(this, ImagePickActivity::class.java)
            intent.putExtra(Constant.MAX_NUMBER, 10)
            intent.putExtra(IS_NEED_CAMERA, true);
            startActivityForResult(intent, Constant.REQUEST_CODE_PICK_IMAGE);
        }

        videos_tv.setOnClickListener {
            val intent = Intent(this, VideoPickActivity::class.java)
            intent.putExtra(Constant.MAX_NUMBER, 10)
            intent.putExtra(IS_NEED_CAMERA, true);
            startActivityForResult(intent, Constant.REQUEST_CODE_PICK_VIDEO);
        }

        other_tv.setOnClickListener {
            val intent = Intent(this, NormalFilePickActivity::class.java)
            intent.putExtra(Constant.MAX_NUMBER, 10)
            intent.putExtra(NormalFilePickActivity.SUFFIX, arrayOf("xlsx", "xls", "doc", "docx", "ppt", "pptx", "pdf"))
            startActivityForResult(intent, Constant.REQUEST_CODE_PICK_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            var list: ArrayList<BaseFile>? = null

            when (requestCode) {
                Constant.REQUEST_CODE_PICK_IMAGE -> {
                    list = data?.getParcelableArrayListExtra(Constant.RESULT_PICK_IMAGE)
                }
                Constant.REQUEST_CODE_PICK_VIDEO -> {
                    list = data?.getParcelableArrayListExtra(Constant.RESULT_PICK_VIDEO)
                }
                Constant.REQUEST_CODE_PICK_FILE -> {
                    list = data?.getParcelableArrayListExtra(Constant.RESULT_PICK_FILE)
                }
            }

            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("file", list?.get(0)?.path)
            startActivity(intent)
        }
    }
}
