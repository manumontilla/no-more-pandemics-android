package com.net.mjmon.myapplication.ui

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.net.mjmon.myapplication.R
import com.net.mjmon.myapplication.model.SymptomModel
import java.time.Instant
import java.time.Instant.*
import java.time.format.DateTimeFormatter
import java.util.*

class SymptomAdapter(private val list: List<SymptomModel>,val listener: (SymptomModel,Int) -> Unit)
    : RecyclerView.Adapter<SymptomViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SymptomViewHolder(inflater, parent)
    }


    override fun onBindViewHolder(holder: SymptomViewHolder, position: Int) {
        val symtom: SymptomModel = list[position]
        holder.bind(symtom,listener)
    }

    override fun getItemCount(): Int = list.size

}

class SymptomViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.symtoms_list_item, parent, false)) {
    private val db = FirebaseFirestore.getInstance()
    private var mNameView: TextView? = null
    private var mId: TextView? = null
    private var mChecked: CheckBox? = null


    init {
        mNameView = itemView.findViewById(R.id.list_symtom_name)
        mId = itemView.findViewById(R.id.list_symtom_id)
        mChecked = itemView.findViewById(R.id.list_symtom_checked)
    }


    fun bind(symtom: SymptomModel, listener: (SymptomModel, Int) -> Unit) = with(itemView) {
        mNameView?.text = symtom.name.toString()
        mId?.text = symtom.id.toString()
        mChecked?.isChecked = symtom.checked
        setOnClickListener { listener(symtom,itemView.id) }
    }

}