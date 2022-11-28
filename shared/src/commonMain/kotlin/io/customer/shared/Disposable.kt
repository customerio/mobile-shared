package io.customer.shared

import kotlinx.coroutines.runBlocking

// The file is just to add dummy method to bridge multiple PRs
// As we continue merging more changes, the file will be replaced with actual calls and eventually
// be deleted

// TODO: Remove this file and its usages

// This is just dummy implementation
// Will be replaced with helper class that makes it easy to dispatch suspended blocks in
// background dispatchers conveniently without making blocking calls
fun runSuspended(block: suspend () -> Unit) = runBlocking { block() }
