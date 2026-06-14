package com.hitbosss.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordina recargas de datos dirigidas por acción: una acción "invalida" un área y los ViewModels
 * vivos de esa área recargan. Sin polling (no satura el servidor): solo recarga cuando hace falta.
 * Las pantallas además tienen pull-to-refresh manual.
 */
@Singleton
class RefreshCoordinator @Inject constructor() {
    private val _ranking = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val ranking: SharedFlow<Unit> = _ranking

    private val _profile = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val profile: SharedFlow<Unit> = _profile

    private val _community = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val community: SharedFlow<Unit> = _community

    // Ranking de un grupo/evento concreto (emite su id) tras subir un HIT en ese contexto.
    private val _group = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val group: SharedFlow<Int> = _group
    private val _event = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val event: SharedFlow<Int> = _event

    fun invalidateRanking() { _ranking.tryEmit(Unit) }
    fun invalidateProfile() { _profile.tryEmit(Unit) }
    fun invalidateCommunity() { _community.tryEmit(Unit) }
    fun invalidateGroup(id: Int) { _group.tryEmit(id) }
    fun invalidateEvent(id: Int) { _event.tryEmit(id) }

    /** Tras subir un HIT global: cambia el ranking global y el perfil del usuario. */
    fun onHitUploaded() {
        invalidateRanking()
        invalidateProfile()
    }
}
