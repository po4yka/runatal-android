package com.po4yka.runatal.data.seed

import com.po4yka.runatal.data.local.entity.RuneReferenceEntity
import java.security.MessageDigest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Exact tuple identity and last-generated metadata proof; nullable ownership leaves custom entries independent. */
internal object RuneReferenceCanonicalIdentity {
    fun key(reference: RuneReferenceEntity): String = "${reference.script}:${reference.name}"

    fun fingerprint(reference: RuneReferenceEntity): String {
        val metadata = listOf(reference.script, reference.name, reference.character,
            reference.pronunciation, reference.meaning, reference.history)
        val bytes = Json.encodeToString(ListSerializer(String.serializer()), metadata).toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    fun owned(reference: RuneReferenceEntity): RuneReferenceEntity = reference.copy(
        canonicalKey = key(reference), canonicalFingerprint = fingerprint(reference)
    )

    /** Only complete published seed tuples confer ownership; a same name or glyph is insufficient. */
    fun provenDuplicates(
        canonical: RuneReferenceEntity,
        rows: List<RuneReferenceEntity>,
        known: List<RuneReferenceEntity>
    ): List<RuneReferenceEntity> {
        val proofs = (known + canonical).filter { key(it) == key(canonical) }.map(::fingerprint).toSet()
        return rows.filter { it.canonicalKey == null && key(it) == key(canonical) && fingerprint(it) in proofs }
            .sortedBy { it.id }
    }
}
