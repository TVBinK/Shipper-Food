package com.example.adminfoodordering.model

data class Shipper(
    val name: String = "",
    val phone: String = "",
    val latitute: Long = 0L,  // Sửa thành Long
    val longitute: Long = 0L, // Sửa thành Long
    val timestamp: Long = 0L  // Đảm bảo tất cả trường kiểu Long khớp với dữ liệu Firebase
) {
    // No-argument constructor required by Firebase
    constructor() : this("", "", 0L, 0L, 0L)
}
