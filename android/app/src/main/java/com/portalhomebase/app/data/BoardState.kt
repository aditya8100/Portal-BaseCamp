package com.portalhomebase.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BoardState(private val prefs: Prefs) {
    private var api = BoardApi(prefs.serverUrl, prefs.token)

    private val _cards = MutableStateFlow<List<Card>>(emptyList())
    val cards: StateFlow<List<Card>> = _cards

    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather

    private val _events = MutableStateFlow<List<CalEvent>>(emptyList())
    val events: StateFlow<List<CalEvent>> = _events

    private val _tz = MutableStateFlow("America/Chicago")
    val tz: StateFlow<String> = _tz

    private val _accessories = MutableStateFlow<List<Accessory>>(emptyList())
    val accessories: StateFlow<List<Accessory>> = _accessories

    private val _week = MutableStateFlow<List<WeekDay>>(emptyList())
    val week: StateFlow<List<WeekDay>> = _week

    private val _status = MutableStateFlow("connecting…")
    val status: StateFlow<String> = _status

    private val _updatedAt = MutableStateFlow(0L)
    val updatedAt: StateFlow<Long> = _updatedAt

    fun rebuildApi() {
        api = BoardApi(prefs.serverUrl, prefs.token)
    }

    fun currentConnection(): Pair<String, String> = Pair(prefs.serverUrl, prefs.token)

    fun themeMode(): String = prefs.themeMode

    fun saveThemeMode(mode: String) {
        prefs.themeMode = mode
    }

    fun saveConnection(url: String, token: String) {
        prefs.serverUrl = url
        prefs.token = token
        rebuildApi()
    }

    suspend fun refreshAll() {
        try {
            val cards = api.getCards()
            val weather = api.getWeather()
            val (tz, events) = api.getEvents()
            val accessories = try {
                api.getAccessories()
            } catch (e: ApiException) {
                if (e.status == 503) emptyList() else throw e
            }
            val week = api.getWeek()
            _week.value = week
            // Hidden cards are agent-only: readable via API, never shown on the display.
            _cards.value = cards.filter { !it.hidden }
            _weather.value = weather
            _tz.value = tz
            _events.value = events
            _accessories.value = accessories
            _updatedAt.value = System.currentTimeMillis()
            _status.value = ""
        } catch (e: Exception) {
            _status.value = "offline — ${e.message?.take(80)}"
        }
    }

    fun startPolling(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                refreshAll()
                delay(30_000)
            }
        }
    }

    suspend fun runAction(block: suspend BoardApi.() -> Unit) {
        try {
            api.block()
            refreshAll()
        } catch (e: Exception) {
            _status.value = "action failed — ${e.message?.take(80)}"
        }
    }
}
