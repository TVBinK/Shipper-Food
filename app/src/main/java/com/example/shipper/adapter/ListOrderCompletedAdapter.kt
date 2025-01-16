// PendingOrderAdapter.kt
package com.example.shipper.adapter

import OrderDetails
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shipper.databinding.ListOrderItemBinding

class ListOrderCompletedAdapter(
    private val context: Context,
    private val listOfOrderItem: MutableList<OrderDetails>,
    private val onItemClick: (OrderDetails) -> Unit
) : RecyclerView.Adapter<ListOrderCompletedAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding =
            ListOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = listOfOrderItem.size

    inner class MenuViewHolder(private val binding: ListOrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val order = listOfOrderItem[position]
            binding.apply {
                tvCustomerName.text = order.userName
                tvQuantity.text = order.foodQuantities?.sum()?.toString() ?: "0"
                tvPrice.text = order.totalPrice


                root.setOnClickListener {
                    onItemClick(order)
                }
            }
        }
    }


}
