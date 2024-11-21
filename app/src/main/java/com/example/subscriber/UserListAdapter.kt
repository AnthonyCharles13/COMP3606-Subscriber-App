package com.example.subscriber

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserListAdapter(private val iFaceImpl: UserListAdapterInterface): RecyclerView.Adapter<UserListAdapter.ViewHolder>() {
    private var studentList = mutableMapOf<String, Int>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentID : TextView = itemView.findViewById(R.id.stuId)
        val minimumSpeed : TextView = itemView.findViewById(R.id.minSpeed)
        val maximumSpeed : TextView = itemView.findViewById(R.id.maxSpeed)
        val viewMore : Button = itemView.findViewById(R.id.viewMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return studentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = studentList.keys.toList()[position]
        val color = studentList[key]!!
        holder.studentID.text = studentList.keys.toList()[position]
        holder.studentID.setTextColor(color)

        val dbHelper = DatabaseHelper(holder.itemView.context, null)
        val speeds = dbHelper.getMinMaxSpeeds(key)

        if (speeds != null) {
            val (minSpeed, maxSpeed) = speeds
            holder.minimumSpeed.text = "Min: %.4f km/h".format(minSpeed)
            holder.maximumSpeed.text = "Max: %.4f km/h".format(maxSpeed)
        } else {
            holder.minimumSpeed.text = "Min: N/A"
            holder.maximumSpeed.text = "Max: N/A"
        }

        holder.viewMore.setOnClickListener {
            iFaceImpl.onItemClick(key)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newAttendeesList: Map<String, Int>) {
        studentList.clear()
        studentList.putAll(newAttendeesList)
        Log.d("AttendeeListAdapter", "Updated list: $studentList")
        notifyDataSetChanged()
    }
}