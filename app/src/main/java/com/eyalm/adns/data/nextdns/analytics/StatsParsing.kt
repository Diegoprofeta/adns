package com.eyalm.adns.data.nextdns.analytics

import com.eyalm.adns.data.Locales
import com.eyalm.adns.data.nextdns.model.BuiltInListIcon
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.countryFlag
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.text.NumberFormat
import java.util.Locale

private fun fmt(n: Int): String = NumberFormat.getNumberInstance(Locale.US).format(n)
private fun JsonArray.objs(): List<JsonObject> = this.map { it.asJsonObject }
private fun JsonObject.str(k: String): String? = get(k)?.takeIf { !it.isJsonNull }?.asString
private fun JsonObject.int(k: String): Int = get(k)?.takeIf { !it.isJsonNull }?.asInt ?: 0

fun fmtPercent(p: Float): String =
    if (p % 1f == 0f) "${p.toInt()}%" else String.format(Locale.US, "%.2f%%", p)


internal fun parseList(card: ListCard, data: JsonArray): List<StatRow> = when (card.kind) {

    ListKind.DOMAINS -> data.objs().map { o ->
        val d = o.str("domain") ?: ""
        val q = o.int("queries")
        val tracker = o.str("tracker")
        StatRow(
            id = d,
            title = d,
            highlightDomain = true,
            value = fmt(q),
            rawQueries = q,
            tracker = tracker,
            icon = nextDnsFaviconUrl(d)?.let(ListIcon::Url) ?: ListIcon.None,
        )
    }

    ListKind.REASONS -> data.objs().map { o ->
        val q = o.int("queries")
        StatRow(
            id = o.str("id") ?: "",
            title = o.str("name") ?: o.str("id") ?: "",
            value = fmt(q),
            rawQueries = q,
        )
    }

    ListKind.DEVICES -> data.objs().map { o ->
        val id = o.str("id") ?: ""
        val q = o.int("queries")
        val name = o.str("name") ?: if (id == "__UNIDENTIFIED__")
            Locales.getString("analytics", "devices", "unidentified", "name") else id
        StatRow(
            id = id,
            title = name,
            value = fmt(q),
            rawQueries = q,
        )
    }

    ListKind.IPS -> data.objs().map { o ->
        val net = o.getAsJsonObject("network")
        val geo = o.getAsJsonObject("geo")
        val isp = net?.str("isp")
        val country = geo?.str("country")
        val city = geo?.str("city")
        val loc = listOfNotNull(city, country).joinToString(", ").ifEmpty { null }
        val cellular = net?.get("cellular")?.takeIf { !it.isJsonNull }?.asBoolean == true
        val q = o.int("queries")
        StatRow(
            id = o.str("ip") ?: "",
            title = o.str("ip") ?: "",
            value = fmt(q),
            rawQueries = q,
            isp = isp,
            location = loc,
            isCellular = cellular,
            icon = ListIcon.BuiltIn(
                if (cellular) BuiltInListIcon.SignalCellular
                else BuiltInListIcon.Wifi
            )
        )
    }

    ListKind.COUNTRIES -> {
        val objs = if (card.limit != null) data.objs().take(card.limit) else data.objs()
        objs.map { o ->
            val code = o.str("code") ?: ""
            val q = o.int("queries")
            val domains = o.getAsJsonArray("domains")?.map { it.asString } ?: emptyList()
            StatRow(
                id = code,
                title = o.str("name") ?: code,
                value = fmt(q),
                rawQueries = q,
                countryCode = code,
                topDomains = domains,
                icon = ListIcon.Text(countryFlag(code))
            )
        }
    }

    ListKind.GAFAM -> {
        val objs = data.objs()
        val totalQueries = objs.sumOf { it.int("queries") }
        val rows = objs.map { o ->
            val companyKey = o.str("company") ?: "others"
            val localizedName = Locales.getString("analytics", "gafam", "companies", companyKey, "name")
            val fallbackName = companyKey.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            val name = localizedName.ifEmpty { fallbackName }
            val q = o.int("queries")
            val pct = if (totalQueries > 0) (q.toFloat() / totalQueries) * 100f else 0f
            val domain = companyDomain(companyKey)
            val icon = domain?.let(::nextDnsFaviconUrl)?.let(ListIcon::Url)
                ?: if (companyKey.lowercase() == "others") ListIcon.Text("?") else ListIcon.None
            StatRow(
                id = companyKey,
                title = name,
                value = fmtPercent(pct),
                rawQueries = q,
                percentage = pct,
                icon = icon
            )
        }
        val (others, main) = rows.partition { it.id.lowercase() == "others" }
        main.sortedByDescending { it.rawQueries } + others
    }

    ListKind.PROTOCOLS -> data.objs().map { o ->
        val proto = o.str("protocol") ?: ""
        val q = o.int("queries")
        StatRow(
            id = proto,
            title = proto,
            value = fmt(q),
            rawQueries = q
        )
    }

    ListKind.QUERY_TYPES -> data.objs().map { o ->
        val name = o.str("name") ?: o.int("type").toString()
        val q = o.int("queries")
        StatRow(
            id = name,
            title = name,
            value = fmt(q),
            rawQueries = q
        )
    }

    ListKind.IP_VERSIONS -> data.objs().map { o ->
        val v = o.int("version")
        val name = "IPv$v"
        val q = o.int("queries")
        StatRow(
            id = name,
            title = name,
            value = fmt(q),
            rawQueries = q
        )
    }
}

private fun companyDomain(companyKey: String): String? = when (companyKey.lowercase()) {
    "facebook" -> "facebook.com"
    "google" -> "google.com"
    "microsoft" -> "microsoft.com"
    "amazon" -> "amazon.com"
    "apple" -> "apple.com"
    else -> null
}

internal fun parsePercent(card: PercentCard, data: JsonArray): Float {
    val flag = if (card.kind == PercentKind.ENCRYPTED) "encrypted" else "validated"
    val total = data.objs().sumOf { it.int("queries") }.coerceAtLeast(1)
    val on = data.objs()
        .filter { it.get(flag)?.takeIf { e -> !e.isJsonNull }?.asBoolean == true }
        .sumOf { it.int("queries") }
    return on.toFloat() / total * 100f
}

fun parseNamedSeries(item: JsonObject): String {
    item.str("name")?.let { return it }
    item.str("company")?.let { company ->
        return when (company.lowercase(Locale.US)) {
            "google" -> "Google"
            "facebook" -> "Facebook"
            "microsoft" -> "Microsoft"
            "amazon" -> "Amazon"
            "apple" -> "Apple"
            "others" -> "Others"
            else -> company.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }
    item.str("protocol")?.let { return it }
    item.str("ip")?.let { return it }
    item.str("domain")?.let { return it }
    if (item.has("version")) {
        val v = item.get("version")
        if (!v.isJsonNull) return "IPv${v.asInt}"
    }
    if (item.has("status")) {
        return when (item.str("status")) {
            "blocked" -> "Blocked"
            "allowed" -> "Allowed"
            "default" -> "Resolved"
            else -> item.str("status") ?: "Status"
        }
    }
    if (item.has("encrypted")) {
        val e = item.get("encrypted")
        if (!e.isJsonNull) return if (e.asBoolean) "Encrypted" else "Unencrypted"
    }
    if (item.has("validated")) {
        val valNode = item.get("validated")
        if (!valNode.isJsonNull) return if (valNode.asBoolean) "Validated" else "Unvalidated"
    }
    item.str("id")?.let { return it }
    return "Queries"
}

internal fun parseSeries(json: JsonObject): AnalyticsSeriesResult {
    val data = json.getAsJsonArray("data")?.objs() ?: emptyList()
    if (data.isEmpty()) return AnalyticsSeriesResult()

    var maxLen = 0
    val rawSeries = data.map { obj ->
        val name = parseNamedSeries(obj)
        val qArray = obj.getAsJsonArray("queries")
        val list = if (qArray != null) {
            val ints = qArray.map { if (it.isJsonNull) 0 else it.asInt }
            if (ints.size > maxLen) maxLen = ints.size
            ints
        } else {
            emptyList()
        }
        Triple(name, list, list.sum())
    }

    if (maxLen == 0) return AnalyticsSeriesResult()

    val rawNamedSeries = rawSeries
        .filter { it.third > 0 }
        .map { (name, list, total) ->
            val points = (0 until maxLen).map { idx -> list.getOrElse(idx) { 0 }.toFloat() }
            NamedSeries(name = name, points = points, totalQueries = total)
        }
        .sortedByDescending { it.totalQueries }

    val namedSeriesList = if (rawNamedSeries.size > 6) {
        val top5 = rawNamedSeries.take(5)
        val rest = rawNamedSeries.drop(5)
        val otherPoints = (0 until maxLen).map { idx ->
            rest.sumOf { it.points.getOrElse(idx) { 0f }.toDouble() }.toFloat()
        }
        val otherTotal = rest.sumOf { it.totalQueries }
        if (otherTotal > 0) {
            top5 + NamedSeries(name = "Other", points = otherPoints, totalQueries = otherTotal)
        } else {
            top5
        }
    } else {
        rawNamedSeries
    }

    val aggregatePoints = (0 until maxLen).map { idx ->
        rawSeries.sumOf { it.second.getOrElse(idx) { 0 } }.toFloat()
    }
    val total = rawSeries.sumOf { it.third }

    return AnalyticsSeriesResult(
        points = aggregatePoints,
        totalQueries = total,
        seriesList = namedSeriesList,
    )
}

