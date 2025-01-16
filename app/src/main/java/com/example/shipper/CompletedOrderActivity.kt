package com.example.shipper

import OrderDetails
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.adminfoodordering.model.Shipper
import com.example.shipper.adapter.ListOrderAdapter
import com.example.shipper.databinding.ActivityCompletedOrderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CompletedOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompletedOrderBinding
    private lateinit var databaseOrderDetails: DatabaseReference
    private lateinit var databaseShippers: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var listOfOrderItem: MutableList<OrderDetails> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompletedOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase references
        databaseOrderDetails = FirebaseDatabase.getInstance().getReference("OrderDetails")
        databaseShippers = FirebaseDatabase.getInstance().getReference("shippers")
        auth = FirebaseAuth.getInstance()

        // Back button functionality
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Get current UID from FirebaseAuth
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            getShipperDetails(currentUid)
            Log.d("CompletedOrderActivity", "Current UID: $currentUid")
        } else {
            Toast.makeText(this, "Failed to authenticate shipper", Toast.LENGTH_SHORT).show()
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
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CompletedOrderActivity", "Error fetching shipper details: ${error.message}")
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
                        // Filter orders by matching shipperPhone and orderAccepted = "Completed"
                        if (it.shipperPhone == shipperPhone && it.orderAccepted == "Completed") {
                            listOfOrderItem.add(it)
                        }
                    }
                }
                if (listOfOrderItem.isEmpty()) {
                    Toast.makeText(
                        this@CompletedOrderActivity,
                        "No completed orders found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                setUpAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CompletedOrderActivity", "Error fetching orders: ${error.message}")
            }
        })
    }

    private fun setUpAdapter() {
        binding.ListOrdersCompletedRecycleView.layoutManager = LinearLayoutManager(this)
        val adapter = ListOrderAdapter(this, listOfOrderItem) { selectedOrder ->
            navigateToDetailOrder(selectedOrder)
        }
        binding.ListOrdersCompletedRecycleView.adapter = adapter
    }

    private fun navigateToDetailOrder(selectedOrder: OrderDetails) {
        val intent = Intent(this, DetailOrderActivity::class.java)
        intent.putExtra("selectedOrder", selectedOrder)
        startActivity(intent)
    }
}
