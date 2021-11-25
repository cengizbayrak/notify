package com.bycen.notify.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.jetbrains.annotations.Contract
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors


/**
 * background async task runner
 */
class TaskRunner private constructor() {
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    /**
     * run callable on background then callback on main
     *
     * @param callable callable to run on background
     * @param callback callback to run on main
     * @param <R>      generic result type of callable & callback
    </R> */
    fun <R> executeAsync(callable: Callable<R>, callback: Callback<R>) {
        executor.execute {
            var result: R? = null
            try {
                result = callable.call()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "executeAsync: exception: " + e.message)
            }
            handler.post { callback.onComplete(result!!) }
        }
    }

    /**
     * run callable on background then callback on main
     *
     * @param callable callable to run on background
     * @param callback callback to run on main
     * @param <R>      generic result type of callable & callback
    </R> */
    fun <R> executeAsync(callable: Callable<R>, callback: (R) -> Unit) {
        executor.execute {
            var result: R? = null
            try {
                result = callable.call()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "executeAsync: exception: " + e.message)
            }
            handler.post { callback(result!!) }
        }
    }

    /**
     * generic callback
     *
     * @param <R> generic type of callback
    </R> */
    interface Callback<R> {
        fun onComplete(result: R)
    }

    companion object {
        private const val TAG = "TaskRunner"

        /**
         * singleton of task runner
         */
        @JvmField
        val singleton = TaskRunner()

        @Contract(" -> new")
        fun newInstance(): TaskRunner {
            return TaskRunner()
        }
    }
}