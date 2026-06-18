package com.example.mojaparafia.ui.components

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class ParishClusterItem(
    lat: Double,
    lng: Double,
    private val title: String?,
    private val snippet: String?,
    val parishId: String?,
    val isCathedral: Int,
    val isFavorite: Boolean,
    val activeIntentions: Int,
    val activeCandles: Int,
    val isHomeParish: Boolean,
    val userHasCrown: Boolean
) : ClusterItem {
    private val position: LatLng = LatLng(lat, lng)

    override fun getPosition(): LatLng = position
    override fun getTitle(): String? = title
    override fun getSnippet(): String? = snippet

    // Ustawiamy zIndex dynamicznie – korona najwyżej, potem ulubione
    override fun getZIndex(): Float {
        return if (isHomeParish && userHasCrown) 3.0f
        else if (isFavorite) 2.0f
        else 1.0f
    }
}