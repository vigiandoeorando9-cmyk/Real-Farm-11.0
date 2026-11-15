# Farm Brasil — Security Specification (Zero-Trust ABAC)

## 1. Data Invariants
1. **Owner-Only PII Isolation**: `/users/{userId}/private/{docId}`, `/users/{userId}/devices/{deviceId}`, and `/users/{userId}/cloudSaves/{slotId}` are strictly readable and writable only by `request.auth.uid == userId` (with `email_verified == true` for writes).
2. **Identity & ID Hardening**: All document IDs (`userId`, `docId`, `deviceId`, `slotId`, `roomCode`, `memberUid`, `messageId`) must match `^[a-zA-Z0-9_-]+$` and length `<= 128`. `ownerId`/`hostUid`/`memberUid`/`senderUid` in document data must strictly equal `request.auth.uid` on creation and never mutate.
3. **Strict Timestamps & Immortals**: `createdAt` must equal `request.time` on `create` and stay equal to `existing().createdAt` on `update`. `updatedAt` must equal `request.time` on `create` and `update`.
4. **CloudSave Revision & Payload Bounds**: `compactCellsRle` is strictly bounded (`<= 450000` bytes), list fields are strictly bounded (`.size()` checks + element validation), and `revision >= 1`.
5. **Multiplayer Room Master Gate**:
   - Only verified users can create a room (`hostUid == request.auth.uid`, `status == 'WAITING'`).
   - Once `status == 'CLOSED'` (terminal state), no further updates to `/mpRooms/{roomCode}` are allowed.
   - Subcollections (`members`, `messages`) verify parent room existence and non-`CLOSED` state via `get()`.
   - A guest can only join as `role == 'GUEST'` and `isApproved == false` (unless `memberUid == hostUid`), and cannot self-approve (`isApproved` or `role` escalation blocked; only the room Host can promote/approve a member).
6. **Zero Blanket List Scraping**: No `allow list: if isSignedIn();` rules exist. `/mpRooms` list queries require `resource.data.status != 'CLOSED'`; owner-scoped subcollections restrict `list` to `request.auth.uid == userId`.

## 2. "Dirty Dozen" Attack Payloads (Expected: PERMISSION_DENIED)
1. **D01 — Cross-User PII Read**: User B calls `get(/users/userA/private/info)`.
2. **D02 — Identity Spoofing on CloudSave Create**: User B creates `/users/userB/cloudSaves/slot_0` with `ownerId: "userA"`.
3. **D03 — Shadow / Ghost Field Injection**: User A creates `/users/userA` with extra undeclared field `isAdmin: true`.
4. **D04 — ID Poisoning (1.5KB Oversized Doc ID)**: User A writes `/users/userA/devices/${"x".repeat(1500)}`.
5. **D05 — Timestamp Forgery**: User A creates CloudSave with `createdAt: Timestamp(2020-01-01)`.
6. **D06 — Immortal Field Mutation**: User A updates `/users/userA/cloudSaves/slot_0` attempting to mutate `createdAt` or `ownerId`.
7. **D07 — Unverified Email Spoof Write**: Unverified Auth token (`email_verified: false`) attempts `setDoc` on `/users/userA/cloudSaves/slot_0`.
8. **D08 — Self-Approval Privilege Escalation in MP Room**: Guest B joins `/mpRooms/FB-ROOM1/members/userB` setting `isApproved: true, role: "HOST"`.
9. **D09 — Member Update Escalation**: Approved Member B updates `/mpRooms/FB-ROOM1/members/userB` modifying `isApproved` or `role`.
10. **D10 — Orphaned Chat Write into Closed Room**: Member writes message into `/mpRooms/CLOSED_ROOM/messages/msg1` where parent room has `status == "CLOSED"`.
11. **D11 — Terminal Room Re-open**: Host tries to update `/mpRooms/CLOSED_ROOM` after `status == "CLOSED"`.
12. **D12 — Array/String Resource Poisoning**: Authenticated user injects 600KB string into `compactCellsRle` or 2KB `roomCode` in `MultiplayerRoom`.
