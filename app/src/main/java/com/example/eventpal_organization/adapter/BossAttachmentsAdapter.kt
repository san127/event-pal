package com.example.eventpal_organization.boss

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.StorageFile
import java.net.HttpURLConnection
import java.net.URL

class BossAttachmentsAdapter(
    private val context: Context,
    private val fileList: MutableList<StorageFile>
) : RecyclerView.Adapter<BossAttachmentsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileThumbnail: ImageView = itemView.findViewById(R.id.fileThumbnail)
        val fileNameText: TextView = itemView.findViewById(R.id.fileNameText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.boss_attachment_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = fileList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = fileList[position]

        // Load image preview or use default icons
        if (file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".png")) {
            LoadImageTask(holder.fileThumbnail).execute(file.downloadUrl)
        } else if (file.name.endsWith(".pdf")) {
            holder.fileThumbnail.setImageResource(R.drawable.pdf_icon)
        } else {
            holder.fileThumbnail.setImageResource(R.drawable.default_file_icon)
        }

        holder.fileNameText.text = file.name


        // Open file on click
        holder.itemView.setOnClickListener {
            openFileInExternalApp(context, file.downloadUrl, file.name)
        }
    }

    private fun openFileInExternalApp(context: Context, fileUrl: String, fileName: String) {
        val uri = Uri.parse(fileUrl)

        val mimeType = when {
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png") -> "image/*"
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".txt") -> "text/plain"
            fileName.endsWith(".doc") || fileName.endsWith(".docx") -> "application/msword"
            fileName.endsWith(".ppt") || fileName.endsWith(".pptx") -> "application/vnd.ms-powerpoint"
            fileName.endsWith(".xls") || fileName.endsWith(".xlsx") -> "application/vnd.ms-excel"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }
    private class LoadImageTask(val imageView: ImageView) : AsyncTask<String, Void, Bitmap?>() {
        override fun doInBackground(vararg params: String?): Bitmap? {
            return try {
                val url = URL(params[0])
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: Bitmap?) {
            result?.let {
                imageView.setImageBitmap(it)
            }
        }
    }
}
