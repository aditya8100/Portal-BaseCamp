package com.portalhomebase.app.data

import org.json.JSONArray
import org.json.JSONObject

data class CardItem(
    val text: String,
    val done: Boolean,
    val name: String = "",
    val qty: Double? = null,
    val unit: String = "",
    val prep: String = "",
    val anchor: Boolean = false,
)

data class MealSlot(val label: String, val ref: String)

data class MealDay(val day: String, val lunch: MealSlot?, val dinner: MealSlot?)

data class Card(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val items: List<CardItem>,
    val steps: List<String>,
    val plan: List<MealDay>,
    val meta: Map<String, String>,
    val image: String,
    val source: String,
    val priority: Int,
    val favorite: Boolean,
    val hidden: Boolean,
    val pinned: Boolean,
    val lastCookedAt: Long?,
    val ts: Long,
) {
    companion object {
        fun parse(o: JSONObject): Card {
            val items = mutableListOf<CardItem>()
            o.optJSONArray("items")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val it = arr.getJSONObject(i)
                    val qty = if (it.has("qty")) {
                        it.optDouble("qty").takeUnless { d -> d.isNaN() }
                    } else {
                        null
                    }
                    items.add(
                        CardItem(
                            text = it.optString("text"),
                            done = it.optBoolean("done"),
                            name = it.optString("name"),
                            qty = qty,
                            unit = it.optString("unit"),
                            prep = it.optString("prep"),
                            anchor = it.optBoolean("anchor"),
                        ),
                    )
                }
            }
            val steps = mutableListOf<String>()
            o.optJSONArray("steps")?.let { arr ->
                for (i in 0 until arr.length()) steps.add(arr.optString(i))
            }
            val plan = mutableListOf<MealDay>()
            o.optJSONArray("plan")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    plan.add(
                        MealDay(
                            day = d.optString("day"),
                            lunch = d.optJSONObject("lunch")?.let { s ->
                                MealSlot(s.optString("label"), s.optString("ref"))
                            },
                            dinner = d.optJSONObject("dinner")?.let { s ->
                                MealSlot(s.optString("label"), s.optString("ref"))
                            },
                        ),
                    )
                }
            }
            val meta = mutableMapOf<String, String>()
            o.optJSONObject("meta")?.let { m ->
                for (k in m.keys()) meta[k] = m.opt(k)?.toString() ?: ""
            }
            return Card(
                id = o.getString("id"),
                type = o.optString("type", "note"),
                title = o.optString("title"),
                body = o.optString("body"),
                items = items,
                steps = steps,
                plan = plan,
                meta = meta,
                image = o.optString("image"),
                source = o.optString("source"),
                priority = o.optInt("priority"),
                favorite = o.optBoolean("favorite"),
                hidden = o.optBoolean("hidden"),
                pinned = o.optBoolean("pinned"),
                lastCookedAt = if (o.isNull("lastCookedAt")) null else o.optLong("lastCookedAt"),
                ts = o.optLong("ts"),
            )
        }
    }
}

data class DayForecast(
    val date: String,
    val label: String,
    val hi: Int,
    val lo: Int,
    val precip: Int?,
)

data class HourPoint(
    val t: Long,
    val temp: Int,
    val precip: Int,
    val label: String,
)

data class Weather(
    val temp: Int,
    val label: String,
    val days: List<DayForecast>,
    val hours: List<HourPoint>,
    val stale: Boolean,
    val fetchedAt: Long,
    val sunrise: String,
    val sunset: String,
) {
    companion object {
        fun parse(o: JSONObject): Weather {
            val cur = o.optJSONObject("current")
            val days = mutableListOf<DayForecast>()
            o.optJSONArray("days")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i)
                    days.add(
                        DayForecast(
                            date = d.optString("date"),
                            label = d.optString("label"),
                            hi = d.optInt("hi"),
                            lo = d.optInt("lo"),
                            precip = if (d.isNull("precip")) null else d.optInt("precip"),
                        ),
                    )
                }
            }
            val hours = mutableListOf<HourPoint>()
            o.optJSONArray("hours")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val h = arr.getJSONObject(i)
                    hours.add(
                        HourPoint(
                            t = h.optLong("t"),
                            temp = h.optInt("temp"),
                            precip = h.optInt("precip"),
                            label = h.optString("label"),
                        ),
                    )
                }
            }
            val sun = o.optJSONObject("sun")
            return Weather(
                temp = cur?.optInt("temp") ?: 0,
                label = cur?.optString("label") ?: "",
                days = days,
                hours = hours,
                stale = o.optBoolean("stale"),
                fetchedAt = o.optLong("fetchedAt"),
                sunrise = if (sun == null || sun.isNull("rise")) "" else sun.optString("rise"),
                sunset = if (sun == null || sun.isNull("set")) "" else sun.optString("set"),
            )
        }
    }
}

data class CalEvent(
    val title: String,
    val start: Long,
    val location: String,
    val allDay: Boolean,
) {
    companion object {
        fun parse(o: JSONObject): CalEvent = CalEvent(
            title = o.optString("title"),
            start = o.optLong("start"),
            location = o.optString("location"),
            allDay = o.optBoolean("allDay"),
        )
    }
}

data class WeekTimed(
    val title: String,
    val location: String,
    val startMin: Int,
    val endMin: Int,
)

data class WeekAllDay(val title: String, val location: String)

data class WeekDay(
    val key: String,
    val label: String,
    val dayNum: Int,
    val allDay: List<WeekAllDay>,
    val timed: List<WeekTimed>,
) {
    companion object {
        fun parse(o: JSONObject): WeekDay {
            val allDay = mutableListOf<WeekAllDay>()
            o.optJSONArray("allDay")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    allDay.add(WeekAllDay(e.optString("title"), e.optString("location")))
                }
            }
            val timed = mutableListOf<WeekTimed>()
            o.optJSONArray("timed")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    timed.add(
                        WeekTimed(
                            e.optString("title"), e.optString("location"),
                            e.optInt("startMin"), e.optInt("endMin"),
                        ),
                    )
                }
            }
            return WeekDay(
                key = o.optString("key"), label = o.optString("label"), dayNum = o.optInt("dayNum"),
                allDay = allDay, timed = timed,
            )
        }
    }
}

data class Accessory(
    val uniqueId: String,
    val name: String,
    val type: String,
    val on: Boolean?,
    val onWritable: Boolean,
    val brightness: Int?,
    val brightnessWritable: Boolean,
    val tempC: Double?,
) {
    companion object {
        fun parse(o: JSONObject): Accessory {
            val values = o.optJSONObject("values")
            val writable = mutableSetOf<String>()
            o.optJSONArray("writable")?.let { arr ->
                for (i in 0 until arr.length()) writable.add(arr.optString(i))
            }
            return Accessory(
                uniqueId = o.optString("uniqueId"),
                name = o.optString("name"),
                type = o.optString("type"),
                on = if (values?.has("On") == true) values.opt("On") == true || values.optInt("On", 0) == 1 else null,
                onWritable = writable.contains("On"),
                brightness = if (values?.has("Brightness") == true) values.optInt("Brightness") else null,
                brightnessWritable = writable.contains("Brightness"),
                tempC = if (values?.has("CurrentTemperature") == true) values.optDouble("CurrentTemperature") else null,
            )
        }

        fun parseList(arr: JSONArray): List<Accessory> =
            (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
    }
}
