package de.dralle.wetherapp

interface IUpdateListener {
    fun update()
    fun setSharedDataContainer(container:SharedDataContainer)
}