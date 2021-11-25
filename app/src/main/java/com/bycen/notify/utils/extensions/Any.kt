package com.bycen.notify.utils.extensions

val Any.TAG: String
    get() {
        return this::class.java.simpleName
    }