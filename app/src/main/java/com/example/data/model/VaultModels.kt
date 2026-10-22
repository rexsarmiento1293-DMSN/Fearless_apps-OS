package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class VaultDocket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val caseSummary: String = "",
    val moduleTag: String = "MODULE_01",
    val phase: String = "GENESIS",
    val planeStatus: String = "MORTAL",
    val rulingText: String = "",
    val sealHash: String = "#VAULT1293-SECURE-INIT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "USER",
    val moduleTag: String = "MODULE_01",
    val phase: String = "GENESIS",
    val planeStatus: String = "MORTAL",
    val content: String = "",
    val modelUsed: String = "gemini-4-argon",
    val sealHash: String = "#VAULT1293-SECURE-MSG",
    val groundingSources: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BioscienceAudit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sampleName: String = "",
    val bioscienceCategory: String = "Water Quality & Toxicology",
    val findings: String = "",
    val regulatoryCitation: String = "RA 9275 Clean Water Act",
    val riskLevel: String = "MODERATE",
    val sealHash: String = "#VAULT1293-SECURE-BIO",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class JusticeModule(
    val tag: String,
    val code: String,
    val emoji: String,
    val title: String,
    val statute: String,
    val summary: String,
    val samplePrompt: String
) {
    MODULE_01(
        tag = "MODULE_01",
        code = "MOD-01",
        emoji = "🏛️",
        title = "Barangay Katarungan",
        statute = "RA 7160 / KP Law",
        summary = "Lupong Tagapamayapa, missing blotter, palakasan counter-ops, CFA issuance, at MTC escalation.",
        samplePrompt = "Ayaw i-blotter ng Barangay Captain sa Bacoor yung reklamo ko sa kapitbahay dahil kamag-anak niya. Paano ko makukuha ang CFA at ma-escalate sa DILG/MTC?"
    ),
    MODULE_02(
        tag = "MODULE_02",
        code = "MOD-02",
        emoji = "✈️",
        title = "OFW Foul Detector",
        statute = "RA 8042 / RA 10022",
        summary = "Contract substitution audit, illegal placement fees, DMW/POEA red flags, at NLRC money claims.",
        samplePrompt = "Pinapirma ako ng bagong kontrata pagdating sa Riyadh na mas mababa ang sahod kaysa sa DMW-approved contract ko. Ano ang hakbang sa POLO/DMW at NLRC?"
    ),
    MODULE_03(
        tag = "MODULE_03",
        code = "MOD-03",
        emoji = "🏪",
        title = "Sari-Sari & Micro-Business BIR Shield",
        statute = "RA 9178 BMBE Law",
        summary = "BMBE income tax exemption, DTI/Barangay permit, BIR registration shield, at iwas-kotong bookkeeping.",
        samplePrompt = "May maliit akong sari-sari store at carinderia sa Cavite. Paano ako mag-aapply ng BMBE under RA 9178 para libre sa Income Tax at hindi ma-harass sa BIR mapping?"
    ),
    MODULE_04(
        tag = "MODULE_04",
        code = "MOD-04",
        emoji = "🌊",
        title = "Baha Early Action",
        statute = "RA 10121 / DRRMO",
        summary = "PAGASA rainfall warnings, Bacoor Zapote/Imus River flood protocols, forced evacuation rights, at calamity aid.",
        samplePrompt = "Orange Warning Level na sa Cavite at tumataas na ang tubig sa Zapote River, Bacoor. Ano ang obligasyon ng Barangay DRRMO under RA 10121 at saan ang legal evacuation protocol?"
    ),
    MODULE_05(
        tag = "MODULE_05",
        code = "MOD-05",
        emoji = "👴",
        title = "Senior Citizen & PWD Navigator",
        statute = "RA 9994 / RA 7277",
        summary = "20% discount + 12% VAT exemption enforcement, OSCA/PDAO complaint filing, at botika/restaurant violations.",
        samplePrompt = "Ayaw ibigay ng botika ang 20% discount at 12% VAT exemption sa maintenance medicine ng lolo ko dahil promo daw. Ano ang violation nila sa RA 9994 at saan isusumbong?"
    ),
    MODULE_06(
        tag = "MODULE_06",
        code = "MOD-06",
        emoji = "📜",
        title = "Land Title & LRA Verification",
        statute = "PD 1529 Property Registration",
        summary = "TCT/OCT authenticity check, Registry of Deeds Certified True Copy, double-sale defense (Art. 1544), at fake title alert.",
        samplePrompt = "Bibili ako ng lote sa Bacoor pero rights lang at photocopy ng TCT ang pinapakita ng seller. Paano ko mave-verify sa LRA / Registry of Deeds kung peke o may adverse claim?"
    ),
    MODULE_07(
        tag = "MODULE_07",
        code = "MOD-07",
        emoji = "🦈",
        title = "Loan Shark & OLA Harassment Alert",
        statute = "RA 3765 / RA 10173",
        summary = "Online Lending App (OLA) doxxing counter-strike, NPC Data Privacy complaint, SEC CGFD revocation, at unconscionable interest.",
        samplePrompt = "Nag-text blast sa lahat ng contacts ko ang isang Online Lending App at nagbabanta ng ipahiya ako sa FB kahit bukas pa ang due date. Paano mag-file sa NPC at SEC?"
    ),
    BIOSCIENCE(
        tag = "BIOSCIENCE",
        code = "BIO-LAB",
        emoji = "🧬",
        title = "Bioscience & Forensics Lab",
        statute = "RA 9275 / RA 9711 / Medico-Legal",
        summary = "Water & environmental toxicology, shellfish/red tide bio-surveillance sa Bacoor Bay, FDA product safety, at forensic evidence.",
        samplePrompt = "May kahina-hinalang kemikal at kulay-itim na tagas sa creek malapit sa tahungan sa Bacoor Bay. I-analyze ang bio-toxicology risk at legal escalation sa DENR-EMB / BFAR under RA 9275."
    );

    companion object {
        fun fromTag(tag: String): JusticeModule {
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: MODULE_01
        }
    }
}

enum class LoopPhase(val label: String, val stepNumber: String, val description: String) {
    GENESIS("GENESIS", "01", "Raw facts, initial intake, blotter, complaint."),
    EXECUTION("EXECUTION", "02", "Action steps, filing, agency escalation."),
    AUDIT_GATE("AUDIT_GATE", "03", "Verification — CFA, notarized docs, agency receipt."),
    IMMORTALIZED("IMMORTALIZED", "04", "Locked. Sealed. Non-reversible unless new evidence.");

    fun nextPhase(): LoopPhase = when (this) {
        GENESIS -> EXECUTION
        EXECUTION -> AUDIT_GATE
        AUDIT_GATE -> IMMORTALIZED
        IMMORTALIZED -> IMMORTALIZED
    }
}
