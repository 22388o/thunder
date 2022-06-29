package channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import conf.ConfRepo
import db.Channel
import db.Db
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel
import sync.Sync

@KoinViewModel
class ChannelsModel(
    private val db: Db,
    confRepo: ConfRepo,
    sync: Sync,
) : ViewModel() {

    private val _state = MutableStateFlow<State?>(null)
    val state = _state.asStateFlow()

    init {
        combine(
            sync.state,
            confRepo.load().map { it.lastSyncDate }.distinctUntilChanged(),
        ) { _, lastSyncDate ->
            _state.update { State.LoadingData }

            if (lastSyncDate.isNotBlank()) {
                val channels = db.channelQueries.selectAll().asFlow().mapToList().first()
                _state.update { State.DisplayingData(channels) }
            }
        }.launchIn(viewModelScope)
    }

    sealed class State {
        data class ConnectingToTor(val status: String) : State()
        object LoadingData : State()
        data class DisplayingData(val channels: List<Channel>) : State()
    }
}