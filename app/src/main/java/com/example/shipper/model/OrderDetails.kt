import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class OrderDetails(
    var userUid: String? = null,
    var userName: String? = null,
    var foodNames: List<String>? = null,
    var foodImages: List<String>? = null,
    var foodPrices: List<String>? = null,
    var foodQuantities: List<Int>? = null,
    var foodDescription: List<String>? = null,
    var address: String? = null,
    var totalPrice: String? = null,
    var phoneNumber: String? = null,
    var orderAccepted: String? = "Watting",
    var paymentReceived: Boolean = false,
    var itemPushKey: String? = null,
    var currentTime: Long = 0,
    var orderNumber: Int = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var shipperPhone: String? = null
) : Serializable {
    constructor(parcel: Parcel) : this(
        userUid = parcel.readString(),
        userName = parcel.readString(),
        foodNames = parcel.createStringArrayList(),
        foodImages = parcel.createStringArrayList(),
        foodPrices = parcel.createStringArrayList(),
        foodQuantities = parcel.createIntArray()?.toList(),
        foodDescription = parcel.createStringArrayList(),
        address = parcel.readString(),
        totalPrice = parcel.readString(),
        phoneNumber = parcel.readString(),
        orderAccepted = parcel.readString(),
        paymentReceived = parcel.readByte() != 0.toByte(),
        itemPushKey = parcel.readString(),
        currentTime = parcel.readLong(),
        orderNumber = parcel.readInt(),
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble(),
        shipperPhone = parcel.readString()
    )

    fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userUid)
        parcel.writeString(userName)
        parcel.writeStringList(foodNames)
        parcel.writeStringList(foodImages)
        parcel.writeStringList(foodPrices)
        parcel.writeIntArray(foodQuantities?.toIntArray())
        parcel.writeStringList(foodDescription)
        parcel.writeString(address)
        parcel.writeString(totalPrice)
        parcel.writeString(phoneNumber)
        parcel.writeString(orderAccepted)
        parcel.writeByte(if (paymentReceived) 1 else 0)
        parcel.writeString(itemPushKey)
        parcel.writeLong(currentTime)
        parcel.writeInt(orderNumber)
    }

    fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<OrderDetails> {
        override fun createFromParcel(parcel: Parcel): OrderDetails = OrderDetails(parcel)
        override fun newArray(size: Int): Array<OrderDetails?> = arrayOfNulls(size)
    }
}
