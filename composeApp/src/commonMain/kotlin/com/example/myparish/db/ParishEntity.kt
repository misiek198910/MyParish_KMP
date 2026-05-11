package com.example.myparish.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable // Dodane dla Ktor (API)
@Entity(tableName = "parishes") // Dodane dla Room (Baza)
data class ParishEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String? = null,

    @ColumnInfo(name = "latitude")
    val latitude: Double = 0.0,

    @ColumnInfo(name = "longitude")
    val longitude: Double = 0.0,

    @ColumnInfo(name = "address")
    val address: String? = null,

    @ColumnInfo(name = "announcements")
    val announcements: String? = null,

    @ColumnInfo(name = "confessionInfo")
    val confessionInfo: String? = null,

    @ColumnInfo(name = "phoneNum")
    val phoneNum: String? = null,

    @ColumnInfo(name = "photoUrl")
    val photoUrl: String? = null,

    @ColumnInfo(name = "websiteUrl")
    val websiteUrl: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "bankAccountNumber")
    val bankAccountNumber: String? = null,

    @ColumnInfo(name = "donationInfo")
    val donationInfo: String? = null,

    @ColumnInfo(name = "massHoursSunday")
    val massHoursSunday: String? = null,

    @ColumnInfo(name = "massHoursMonday")
    val massHoursMonday: String? = null,

    @ColumnInfo(name = "massHoursTuesday")
    val massHoursTuesday: String? = null,

    @ColumnInfo(name = "massHoursWednesday")
    val massHoursWednesday: String? = null,

    @ColumnInfo(name = "massHoursThursday")
    val massHoursThursday: String? = null,

    @ColumnInfo(name = "massHoursFriday")
    val massHoursFriday: String? = null,

    @ColumnInfo(name = "massHoursSaturday")
    val massHoursSaturday: String? = null,

    @ColumnInfo(name = "hasMassForChildren", defaultValue = "0")
    val hasMassForChildren: Boolean = false,

    @ColumnInfo(name = "hasMassForChildrenHour")
    val hasMassForChildrenHour: String? = null,

    @ColumnInfo(name = "hasMassSunday", defaultValue = "0")
    val hasMassSunday: Boolean = false,

    @ColumnInfo(name = "hasMassSundayHour")
    val hasMassSundayHour: String? = null,

    @ColumnInfo(name = "adorationInfo")
    val adorationInfo: String? = null,

    @ColumnInfo(name = "diocese")
    val diocese: String? = null,

    @ColumnInfo(name = "deanery")
    val deanery: String? = null,

    @ColumnInfo(name = "foundingYear")
    val foundingYear: String? = null,

    @ColumnInfo(name = "pastorName")
    val pastorName: String? = null,

    @ColumnInfo(name = "officeHoursText")
    val officeHoursText: String? = null,

    @ColumnInfo(name = "socialMediaFacebook")
    val socialMediaFacebook: String? = null,

    @ColumnInfo(name = "socialMediaYouTube")
    val socialMediaYouTube: String? = null,

    @ColumnInfo(name = "socialMediaInstagram")
    val socialMediaInstagram: String? = null,

    @ColumnInfo(name = "isFavorite", defaultValue = "0")
    val isFavorite: Boolean = false,

    @SerialName("last_update") // W Ktor / kotlinx używamy @SerialName zamiast Gsonowego @SerializedName
    @ColumnInfo(name = "last_update")
    val lastUpdate: String? = null,

    @ColumnInfo(name = "isCathedral", defaultValue = "0")
    val isCathedral: Boolean = false,

    @ColumnInfo(name = "firstSaturdayOfMonth", defaultValue = "0")
    val firstSaturdayOfMonth: Boolean = false,

    @ColumnInfo(name = "firstSaturdayOfMonth_hour")
    val firstSaturdayOfMonthHour: String? = "",

    @ColumnInfo(name = "firstSaturdayOfMonth_info")
    val firstSaturdayOfMonthInfo: String? = "",

    @ColumnInfo(name = "active_intentions")
    val active_intentions: Int = 0,

    @SerialName("active_candles")
    @ColumnInfo(name = "active_candles", defaultValue = "0")
    val active_candles: Int = 0

)