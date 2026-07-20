package com.hitbosss.presentation.feature.main

import androidx.lifecycle.ViewModel
import com.hitbosss.core.upload.HitUploadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Expone el estado global de subida de HIT para el banner del MainScreen (iOS MainTabView #649). */
@HiltViewModel
class UploadBannerViewModel @Inject constructor(
    uploadManager: HitUploadManager,
) : ViewModel() {
    val uploadState: StateFlow<HitUploadManager.UploadUi> = uploadManager.state
}
