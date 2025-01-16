package com.example.shipper

import OrderDetails
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminfoodordering.model.Shipper
import com.example.shipper.adapter.ListOrderAdapter
import com.example.shipper.databinding.ActivityListOrderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ListOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListOrderBinding
    private lateinit var databaseOrderDetails: DatabaseReference
    private lateinit var databaseShippers: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var listOfOrderItem: MutableList<OrderDetails> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase references
        databaseOrderDetails = FirebaseDatabase.getInstance().getReference("OrderDetails")
        databaseShippers = FirebaseDatabase.getInstance().getReference("shippers")
        auth = FirebaseAuth.getInstance()

        // Back button functionality
        binding.btnBack3.setOnClickListener {
            finish()
        }

        // Get current UID from FirebaseAuth
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            getShipperDetails(currentUid)
            Log.d("ListOrderActivity", "Current UID: $currentUid")
        } else {
            Toast.makeText(this, "Failed to authenticate shipper", Toast.LENGTH_SHORT).show()
        }

        binding.tvViewCompleted.paintFlags = binding.tvViewCompleted.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvViewCompleted.setOnClickListener {
            val intent = Intent(this, CompletedOrderActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getShipperDetails(uid: String) {
        // Query the shipper details using UID
        val shipperRef = databaseShippers.child(uid)
        shipperRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val shipper = snapshot.getValue(Shipper::class.java)
                    if (shipper != null) {
                        getOrderDetailsForShipper(shipper.phone)
                    } else {
                        Toast.makeText(this@ListOrderActivity, "Shipper details not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ListOrderActivity, "Shipper not found in database", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ListOrderActivity", "Error fetching shipper details: ${error.message}")
                Toast.makeText(this@ListOrderActivity, "Error fetching shipper details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderDetailsForShipper(shipperPhone: String) {
        databaseOrderDetails.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOfOrderItem.clear()
                for (orderSnapshot in snapshot.children) {
                    val order = orderSnapshot.getValue(OrderDetails::class.java)
                    order?.let {
                        // Filter orders by matching shipperPhone
                        if (it.shipperPhone == shipperPhone && it.orderAccepted == "Accepted") {
                            listOfOrderItem.add(it)
                        }
                    }
                }

                if (listOfOrderItem.isEmpty()) {
                    //show imgEmptyList
                    binding.imgEmptyList.visibility = android.view.View.VISIBLE
                }
                setUpAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ListOrderActivity", "Error fetching orders: ${error.message}")
                Toast.makeText(this@ListOrderActivity, "Error loading orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setUpAdapter() {
        binding.ListOrdersRecycleView.layoutManager = LinearLayoutManager(this)
        val adapter = ListOrderAdapter(this, listOfOrderItem) { selectedOrder ->
            navigateToDetailOrder(selectedOrder)
        }
        binding.ListOrdersRecycleView.adapter = adapter
    }

    private fun navigateToDetailOrder(selectedOrder: OrderDetails) {
        val intent = Intent(this, DetailOrderActivity::class.java)
        intent.putExtra("selectedOrder", selectedOrder)
        startActivity(intent)
    }
}
