package com.neml.badminton.certificate;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

/** Immutable issuance and result snapshots. Versioned independently of mutable domain names. */
public final class CertificateModels {
    private CertificateModels() {}
    public enum Category { WINNER, RUNNER_UP, PARTICIPATION }
    public enum RecipientType { TEAM, PLAYER }
    public record Recipient(RecipientType type, UUID id, String name, String team, String photoUrl) {}
    public record Source(UUID matchId, int revision, String stage, UUID winner, UUID teamA, UUID teamB) {}
    public record Approval(UUID id, UUID championshipId, String championship, String sport,
                           UUID champion, UUID runnerUp, List<Source> sources, List<Recipient> participants,
                           Instant approvedAt, String approvedBy, String justification) {}
    public record Eligibility(Approval approval, List<Recipient> recipients, String guidance) {}
    public record ApproveRequest(@NotBlank @Size(min=10,max=1000) String justification) {}
    public record IssueRequest(@NotNull UUID requestId, @NotNull UUID approvalId,
            @NotNull Category category, @NotNull RecipientType recipientType, @NotNull UUID recipientId,
            UUID reissuedFrom, @Size(max=1000) String reason) {}
    public record RevokeRequest(@NotBlank @Size(min=10,max=1000) String reason) {}
    public record Snapshot(int templateVersion, UUID id, String reference, UUID championshipId,
            String championship, String sport, String event, UUID approvalId, Category category,
            Recipient recipient, String achievement, Instant issuedAt, String issuer, String issuerTitle,
            String accent, UUID reissuedFrom, String verificationUrl) {}
    public record Issued(Snapshot certificate, String status, Instant revokedAt, String revocationReason) {}
    public record History(List<Issued> items, long total, Map<String,Long> counts) {}
    public record BulkRequest(@NotNull UUID requestId,@NotNull UUID approvalId,@NotNull Category category,
            @NotNull RecipientType recipientType,@NotEmpty @Size(max=25) List<@NotNull UUID> recipientIds) {}
    public record Batch(UUID id,List<Issued> certificates) {}
    public record Verification(String status,String reference,String recipient,String championship,
            String sport,Category category,String issueDate) {
        public static Verification invalid(){return new Verification("INVALID",null,null,null,null,null,null);}
    }
}
