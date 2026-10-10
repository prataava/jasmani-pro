package id.jasmanipro.domain.norm

import id.jasmanipro.domain.model.Applicability
import id.jasmanipro.domain.model.ComponentGroup
import id.jasmanipro.domain.model.ComponentId
import id.jasmanipro.domain.model.Direction
import id.jasmanipro.domain.model.Gender
import id.jasmanipro.domain.model.NormComponent
import id.jasmanipro.domain.model.NormMetadata
import id.jasmanipro.domain.model.NormRow
import id.jasmanipro.domain.model.NormTable
import id.jasmanipro.domain.model.RowStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class BundleDto(
    val schemaVersion: Int,
    val source: SourceDto,
    val tables: List<TableDto>,
    val boundaryLog: List<BoundaryLogDto> = emptyList(),
    val importAudit: JsonElement? = null,
)

@Serializable
internal data class SourceDto(val fileName: String, val sha256: String, val importedBy: String? = null)

@Serializable
internal data class BoundaryLogDto(
    val id: Int,
    val gender: String,
    val component: String,
    val input: Double,
    val score: Int,
    val change: String? = null,
    val evidence: String? = null,
)

@Serializable
internal data class TableDto(
    val id: String,
    val gender: String,
    val sheet: String? = null,
    val metadata: MetadataDto,
    val components: List<ComponentDto>,
    val rows: List<RowDto>,
)

@Serializable
internal data class MetadataDto(
    val sourceName: String,
    val documentName: String? = null,
    val decisionNumber: String? = null,
    val documentDate: String? = null,
    val validPeriod: String? = null,
    val category: String,
    val applicability: String,
    val verificationNote: String,
)

@Serializable
internal data class ComponentDto(
    val id: String,
    val header: String,
    val unit: String,
    val direction: String,
    val group: String,
    val unitNote: String? = null,
)

@Serializable
internal data class RowDto(
    val score: Int,
    val status: String,
    val reference: String? = null,
    val values: Map<String, Double>,
)

/** Hasil pemuatan satu berkas norma: tabel + jejak asal data (nama berkas dan SHA-256 sumber). */
data class NormBundle(
    val schemaVersion: Int,
    val sourceFileName: String,
    val sourceSha256: String,
    val tables: List<NormTable>,
) {
    fun tableFor(gender: Gender): NormTable? = tables.firstOrNull { it.gender == gender }
}

class NormFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

object NormRepository {
    const val DEFAULT_RESOURCE = "/norms/norma_polri_2026_v2.json"
    const val SUPPORTED_SCHEMA = 1

    private val json = Json { ignoreUnknownKeys = true }

    /** Memuat norma bawaan dari resource. Melempar [NormFormatException] bila rusak. */
    fun loadDefault(): NormBundle {
        val stream = NormRepository::class.java.getResourceAsStream(DEFAULT_RESOURCE)
            ?: throw NormFormatException("Berkas norma bawaan tidak ditemukan: $DEFAULT_RESOURCE")
        return parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    fun parse(text: String): NormBundle {
        val dto = try {
            json.decodeFromString(BundleDto.serializer(), text)
        } catch (e: Exception) {
            throw NormFormatException("Berkas norma tidak dapat dibaca: ${e.message}", e)
        }
        if (dto.schemaVersion != SUPPORTED_SCHEMA) {
            throw NormFormatException(
                "Versi skema norma ${dto.schemaVersion} tidak didukung (didukung: $SUPPORTED_SCHEMA).",
            )
        }
        return try {
            NormBundle(dto.schemaVersion, dto.source.fileName, dto.source.sha256, dto.tables.map { it.toDomain() })
        } catch (e: IllegalArgumentException) {
            throw NormFormatException("Isi norma tidak valid: ${e.message}", e)
        }
    }

    private fun TableDto.toDomain(): NormTable = NormTable(
        id = id,
        gender = Gender.valueOf(gender),
        metadata = NormMetadata(
            sourceName = metadata.sourceName,
            documentName = metadata.documentName,
            decisionNumber = metadata.decisionNumber,
            documentDate = metadata.documentDate,
            validPeriod = metadata.validPeriod,
            category = metadata.category,
            applicability = Applicability.valueOf(metadata.applicability),
            verificationNote = metadata.verificationNote,
        ),
        components = components.map {
            NormComponent(
                id = ComponentId.valueOf(it.id),
                header = it.header,
                unit = it.unit,
                direction = Direction.valueOf(it.direction),
                group = ComponentGroup.valueOf(it.group),
                unitNote = it.unitNote,
            )
        },
        rows = rows.map { r ->
            NormRow(
                score = r.score,
                status = RowStatus.valueOf(r.status),
                reference = r.reference,
                thresholds = r.values.mapKeys { ComponentId.valueOf(it.key) },
            )
        },
    )
}
