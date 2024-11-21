package com.example.mapslabdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.subscriber.R

class StudentSpeedAdapter(
    private val items: List<StudentSpeedInfo>,
    private val onViewMoreClick: (StudentSpeedInfo) -> Unit
) : RecyclerView.Adapter<StudentSpeedAdapter.StudentSpeedViewHolder>() {

    class StudentSpeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentId: TextView = itemView.findViewById(R.id.tvStudentId)
        val tvMinSpeed: TextView = itemView.findViewById(R.id.tvMinSpeed)
        val tvMaxSpeed: TextView = itemView.findViewById(R.id.tvMaxSpeed)
        val btnViewMore: Button = itemView.findViewById(R.id.btnViewMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentSpeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        return StudentSpeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentSpeedViewHolder, position: Int) {
        val item = items[position]
        holder.tvStudentId.text = item.studentId
        holder.tvMinSpeed.text = "min speed: ${item.minSpeed}"
        holder.tvMaxSpeed.text = "max speed: ${item.maxSpeed}"
        holder.btnViewMore.setOnClickListener { onViewMoreClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
