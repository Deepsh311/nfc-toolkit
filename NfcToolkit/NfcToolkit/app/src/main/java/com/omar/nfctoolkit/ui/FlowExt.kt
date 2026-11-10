package com.omar.nfctoolkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

/** Thin wrappers so screens don't each import collectAsState. StateFlow already has a value. */
@Composable
fun <T> StateFlow<T>.collectAsStateSafe(): State<T> = collectAsState()

@Composable
fun <T> StateFlow<T>.collectAsStateSafe(@Suppress("UNUSED_PARAMETER") initial: T): State<T> =
    collectAsState()
