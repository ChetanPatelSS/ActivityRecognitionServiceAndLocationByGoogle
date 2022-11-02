package com.google.android.gms.location.sample.activityrecognition

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.sample.activityrecognition.database.UserActivity

class UserActivityAdapter : RecyclerView.Adapter<UserActivityAdapter.ViewHolder>() {


    var activityList: List<UserActivity> = ArrayList()
    fun updateData(list: List<UserActivity>){
        if (list.isNotEmpty()) {
            activityList = list
            notifyDataSetChanged()
        }
    }


    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_row, parent, false)


        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = activityList[position]

        // sets the text to the textview from our itemHolder class
        holder.activityText.text = ItemsViewModel.activity + " - " + ItemsViewModel.confidence
        holder.dateTimeText.text = ItemsViewModel.dateAdded
        holder.lateLongText.text = "Lat:- " + ItemsViewModel.latitude + " Long:- " + ItemsViewModel.longitude
        holder.speedText.text = ItemsViewModel.speed.toString() + "km/h"

        val note = holder.itemView.findViewById<CardView>(R.id.activity)

    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return activityList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val activityText: TextView = itemView.findViewById(R.id.activity_text)
        val dateTimeText: TextView = itemView.findViewById(R.id.date_text)
        val lateLongText: TextView = itemView.findViewById(R.id.txtLatLong)
        val speedText: TextView = itemView.findViewById(R.id.txtSpeed)
    }
}