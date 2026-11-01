package io.github.corneloaie.admob

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

// One key for every ad: each ad has at most one delegate to keep.
private val delegateKey = nativeHeap.alloc<ByteVar>().ptr

/**
 * Has [ad] keep [delegate] alive, for as long as the ad lives (null releases it). The SDK holds its delegates weakly,
 * and Kotlin/Native frees an object nothing holds strongly; an app may keep only the SDK's ad, in its ad view.
 */
internal fun retainDelegate(ad: Any, delegate: Any?) {
    objc_setAssociatedObject(ad, delegateKey, delegate, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
}

internal fun onMainThread(block: () -> Unit) {
    if (NSThread.isMainThread) block() else dispatch_async(dispatch_get_main_queue(), block)
}
