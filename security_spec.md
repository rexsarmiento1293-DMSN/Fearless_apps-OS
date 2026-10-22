# Security Specification — I.A.M Dimension OS v5.0 (Sovereign Vault 1293)

## 1. Data Invariants
1. **Zero-Trust User Isolation**: Every document in `/vault_dockets/{docketId}`, `/chat_messages/{messageId}`, and `/bioscience_audits/{auditId}` MUST belong to an authenticated user (`request.auth != null`) and enforce `userId == request.auth.uid`.
2. **Path Variable Hardening**: Every single-document ID (`docketId`, `messageId`, `auditId`) MUST match `^[a-zA-Z0-9_\-]+$` and be at most 128 characters.
3. **Strict Key Allowlisting & Anti-Update-Gap**:
   - `VaultDocket` requires `['userId', 'title', 'caseSummary', 'moduleTag', 'phase', 'planeStatus', 'rulingText', 'sealHash', 'createdAt', 'updatedAt']` and forbids any ghost fields.
   - `ChatMessage` requires `['userId', 'sender', 'moduleTag', 'phase', 'planeStatus', 'content', 'modelUsed', 'sealHash', 'groundingSources', 'createdAt']` and is append-only (updates disallowed, owner can delete).
   - `BioscienceAudit` requires `['userId', 'sampleName', 'bioscienceCategory', 'findings', 'regulatoryCitation', 'riskLevel', 'sealHash', 'createdAt']` and is append-only (updates disallowed, owner can delete).
4. **Immutable Fields & Temporal Integrity**:
   - `createdAt` and `userId` are strictly immutable on `update`.
   - `createdAt` and `updatedAt` must be Firestore `timestamp` values `<= request.time`.
5. **Query Alignment**: All `list` operations require `resource.data.userId == request.auth.uid`.

## 2. The Dirty Dozen Payloads
1. **Unauthenticated Read/Write**: `auth = null` attempting `get`, `list`, or `create` on `/vault_dockets`.
2. **Cross-User Read (PII / Case Theft)**: Alice (`alice_123`) attempting `get` or `list` on Bob's (`bob_456`) docket.
3. **Identity Spoofing on Create**: Alice creating a docket with `userId: "bob_456"`.
4. **Shadow Update (Ghost Field Injection)**: Updating a `VaultDocket` with extra field `isAdmin: true`.
5. **Immutable Field Mutation**: Updating `createdAt` or `userId` on an existing `VaultDocket`.
6. **Invalid Phase / Enum Poisoning**: Creating a `VaultDocket` with `phase: "HACKED_PHASE"` or `planeStatus: "GOD_MODE"`.
7. **Denial of Wallet (1MB String Injection)**: Injecting a 20,000-character `title` into `VaultDocket`.
8. **Future Timestamp Spoofing**: Writing a `createdAt` timestamp set 1 hour in the future (`> request.time`).
9. **Append-Only Mutation Attempt**: Attempting `update` on `/chat_messages/{messageId}` or `/bioscience_audits/{auditId}`.
10. **Unfiltered Collection List**: Authenticated user running `db.collection("vault_dockets").get()` without `.where("userId", "==", uid)`.
11. **Value Poisoning on Update**: Updating `rulingText` on `VaultDocket` with an integer `12345` instead of a bounded string.
12. **Cross-User Delete**: Alice attempting to `delete` Bob's `ChatMessage` or `BioscienceAudit`.
