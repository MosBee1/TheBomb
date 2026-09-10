package io.github.mosbee1.thebomb.data

import android.content.IntentSender

sealed interface DeletionOutcome {

    data object DeletedDirectly : DeletionOutcome

    data class ConsentRequired(val intentSender: IntentSender) : DeletionOutcome

    data class Failed(val cause: Throwable) : DeletionOutcome
}
