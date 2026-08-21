package com.hacybeyker.snapdoc.core.coroutines

import javax.inject.Qualifier

/**
 * Marks the dispatcher for disk/network work. Lives in `core/` rather than inside a feature because
 * every slice that touches storage needs the same binding, and a feature must never import another
 * feature's internals to get it.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
