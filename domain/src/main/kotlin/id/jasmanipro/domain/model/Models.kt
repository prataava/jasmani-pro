package id.jasmanipro.domain.model

enum class Gender { PRIA, WANITA }

/** Kelompok Kesamaptaan: A = lari, B = pull-up/chinning, sit-up, push-up, shuttle run, C = renang. */
enum class ComponentGroup { A, B, C }

enum class Direction { HIGHER_IS_BETTER, LOWER_IS_BETTER }

enum class ComponentId { LARI_12M, PULL_UP, CHINNING, SIT_UP, PUSH_UP, SHUTTLE_RUN, RENANG }

/** Status satu baris norma, sama seperti di file sumber. Tidak pernah dinaikkan otomatis. */
enum class RowStatus { VERIFIED, LINEAR_ESTIMATE }

/** Apakah norma sudah dikonfirmasi berlaku untuk jenis seleksi dan periode yang dipakai. */
enum class Applicability { UNCONFIRMED, CONFIRMED }

data class NormComponent(
    val id: ComponentId,
    val header: String,
    val unit: String,
    val direction: Direction,
    val group: ComponentGroup,
    val unitNote: String? = null,
)

data class NormRow(
    val score: Int,
    val status: RowStatus,
    val reference: String?,
    /** Ambang per komponen untuk meraih [score]. */
    val thresholds: Map<ComponentId, Double>,
)

data class NormMetadata(
    val sourceName: String,
    val documentName: String?,
    val decisionNumber: String?,
    val documentDate: String?,
    val validPeriod: String?,
    val category: String,
    val applicability: Applicability,
    val verificationNote: String,
)

/**
 * Satu tabel norma (misalnya pria atau wanita). Baris wajib terurut nilai menurun (100 -> 0),
 * dan ambang tiap komponen wajib monoton sesuai arahnya. Pelanggaran ditolak saat konstruksi
 * supaya data norma yang rusak tidak pernah sampai ke kalkulator.
 */
data class NormTable(
    val id: String,
    val gender: Gender,
    val metadata: NormMetadata,
    val components: List<NormComponent>,
    val rows: List<NormRow>,
) {
    init {
        require(rows.isNotEmpty()) { "Tabel norma '$id' tidak memiliki baris." }
        val scores = rows.map { it.score }
        require(scores == scores.sortedDescending() && scores.distinct().size == scores.size) {
            "Tabel norma '$id': nilai harus unik dan terurut menurun."
        }
        rows.forEach { row ->
            components.forEach { c ->
                require(row.thresholds.containsKey(c.id)) {
                    "Tabel norma '$id': baris nilai ${row.score} tidak memiliki ambang ${c.id}."
                }
            }
        }
        components.forEach { c ->
            val col = rows.map { it.thresholds.getValue(c.id) }
            val ok = when (c.direction) {
                Direction.HIGHER_IS_BETTER -> col.zipWithNext().all { (a, b) -> a >= b }
                Direction.LOWER_IS_BETTER -> col.zipWithNext().all { (a, b) -> a <= b }
            }
            require(ok) { "Tabel norma '$id': ambang ${c.id} tidak monoton sesuai arah ${c.direction}." }
        }
    }

    fun component(id: ComponentId): NormComponent? = components.firstOrNull { it.id == id }
}
