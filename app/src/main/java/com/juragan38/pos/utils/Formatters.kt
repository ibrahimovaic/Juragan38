package com.juragan38.pos.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val rupiahFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))

fun Double.toRupiah(): String = "Rp " + rupiahFormat.format(this)

fun Long.toRupiah(): String = "Rp " + rupiahFormat.format(this)

private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("in", "ID"))
private val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))

fun Long.toDateTime(): String = dateTimeFormat.format(Date(this))

fun Long.toDateOnly(): String = dateOnlyFormat.format(Date(this))
