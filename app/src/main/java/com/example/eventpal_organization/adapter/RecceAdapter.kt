import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.StorageFile
import java.net.HttpURLConnection
import java.net.URL


class RecceAdapter(
    private val fileList: MutableList<StorageFile>,
    private val onDelete: (StorageFile) -> Unit,
    private val context: Context  // Pass context here
) : RecyclerView.Adapter<RecceAdapter.ViewHolder>() {


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileThumbnail: ImageView = view.findViewById(R.id.fileThumbnail)
        val fileNameText: TextView = view.findViewById(R.id.fileNameText)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.attachment_item, parent, false)
        return ViewHolder(view)
    }

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

        // Handle file click to open in external apps
        holder.itemView.setOnClickListener {
            openFileInExternalApp(context, file.downloadUrl, file.name)
        }


        // Delete button action
        holder.btnDelete.setOnClickListener {
            onDelete(file)
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
            android.widget.Toast.makeText(context, "No app found to open this file", android.widget.Toast.LENGTH_SHORT).show()
        }
    }



    override fun getItemCount(): Int {
        return fileList.size
    }

    // AsyncTask to load an image from a URL
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
