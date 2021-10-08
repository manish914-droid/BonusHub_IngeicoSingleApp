package com.bonushub.crdb.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bonushub.crdb.R
import com.bonushub.crdb.model.TerminalCommunicationTable

import kotlinx.android.synthetic.main.main_list_item.view.*


class MainAdapter(var postList : ArrayList<TerminalCommunicationTable>) : RecyclerView.Adapter<MainAdapter.PostViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PostViewHolder(LayoutInflater.from(parent.context).inflate(
        R.layout.main_list_item , parent  , false))

    override fun getItemCount(): Int  = postList.size


    override fun onBindViewHolder(holder: MainAdapter.PostViewHolder, position: Int)  =  holder.bind(postList[position])

     fun refreshAdapter(newPostList : List<TerminalCommunicationTable>){
        postList.clear()
        postList.addAll(newPostList)
        notifyDataSetChanged()

    }

    inner class PostViewHolder(view : View) : RecyclerView.ViewHolder(view){

       private val  action_id = view.action_id
        private  val table_id = view.table_id

        fun  bind(model : TerminalCommunicationTable){
            action_id.text = model.pcNo.toString()
            table_id.text = model.primaryGateway.toString()

        }

    }

}