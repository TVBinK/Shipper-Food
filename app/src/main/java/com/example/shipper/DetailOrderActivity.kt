package com.example.shipper

import OrderDetails
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shipper.adapter.DetailOrderAdapter
import com.example.shipper.databinding.ActivityDetailOrderBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DetailOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailOrderBinding
    private lateinit var adapter: DetailOrderAdapter
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().reference

        // Back button functionality
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Get the selected order passed from the previous activity
        val selectedOrder = intent.getSerializableExtra("selectedOrder") as? OrderDetails
        if (selectedOrder == null) {
            Toast.makeText(this, "Order details not found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        // Derection button
        binding.btnDirection.setOnClickListener {
            //intent to map activity
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("selectedOrder", selectedOrder)
            startActivity(intent)
        }

        // Completed button
        binding.btnCompleted.setOnClickListener {
            showConfirmationDialog(
                "Complete Order",
                "Are you sure you want to mark this order as completed?",
                onConfirm = { updateOrderStatus(selectedOrder, "Completed") }
            )
        }

        // Display customer information
        binding.tvName.text = selectedOrder.userName ?: "N/A"
        binding.tvAddress.text = selectedOrder.address ?: "N/A"
        binding.tvPhone.text = selectedOrder.phoneNumber ?: "N/A"
        binding.tvTotal.text = selectedOrder.totalPrice?.toString() ?: "0.00"
        binding.tvStatus.text = selectedOrder.orderAccepted ?: "Not delivered yet"

        // Get food details for displaying in the list
        val foodNameList = selectedOrder.foodNames ?: emptyList()
        val foodPriceList = selectedOrder.foodPrices ?: emptyList()
        val foodImageList = selectedOrder.foodImages ?: emptyList()
        val foodQuantityList = selectedOrder.foodQuantities ?: emptyList()

        // Set up the adapter to display food items
        setUpAdapter(foodNameList, foodPriceList, foodImageList, foodQuantityList)
    }

    // Function to show confirmation dialog
    private fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Function to update the order status
    private fun updateOrderStatus(selectedOrder: OrderDetails, status: String) {
        val userId = selectedOrder.userUid
        val pushKey = selectedOrder.itemPushKey

        if (userId != null && pushKey != null) {
            // Update the order status for the user
            val buyHistoryRef =
                databaseReference.child("users").child(userId).child("OrderDetails").child(pushKey)
            buyHistoryRef.child("orderAccepted").setValue(status)

            // Update the order status for the admin
            databaseReference.child("OrderDetails").child(pushKey).child("orderAccepted")
                .setValue(status)

            Toast.makeText(this, "$status Order successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    // Set up the adapter to display the list of food items
    private fun setUpAdapter(
        foodNameList: List<String>,
        foodPriceList: List<String>,
        foodImageList: List<String>,
        foodQuantityList: List<Int>
    ) {
        binding.RecycleView.layoutManager = LinearLayoutManager(this)
        adapter = DetailOrderAdapter(
            this,
            foodNameList,
            foodPriceList,
            foodImageList,
            foodQuantityList
        )
        binding.RecycleView.adapter = adapter
    }
}
