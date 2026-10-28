# Play-Neml — source inventory

Source baseline: `636f30d11ae50b300f32d96f6a901c908dfb2433`. Generated from the existing source during Phase 0 completion. This is an inventory, not evidence that every route or permission was runtime-tested. See [REPORT.md](REPORT.md) for module responsibilities and the role matrix.

## Backend files and DTO records

### controller

- [backend/src/main/java/com/neml/badminton/controller/AdministrativeConfigurationController.java](<../../backend/src/main/java/com/neml/badminton/controller/AdministrativeConfigurationController.java>)
- [backend/src/main/java/com/neml/badminton/controller/ApiExceptionHandler.java](<../../backend/src/main/java/com/neml/badminton/controller/ApiExceptionHandler.java>)
- [backend/src/main/java/com/neml/badminton/controller/AuthController.java](<../../backend/src/main/java/com/neml/badminton/controller/AuthController.java>)
- [backend/src/main/java/com/neml/badminton/controller/ChampionshipController.java](<../../backend/src/main/java/com/neml/badminton/controller/ChampionshipController.java>)
- [backend/src/main/java/com/neml/badminton/controller/CompetitionController.java](<../../backend/src/main/java/com/neml/badminton/controller/CompetitionController.java>)
- [backend/src/main/java/com/neml/badminton/controller/CricketController.java](<../../backend/src/main/java/com/neml/badminton/controller/CricketController.java>)
- [backend/src/main/java/com/neml/badminton/controller/HealthController.java](<../../backend/src/main/java/com/neml/badminton/controller/HealthController.java>)
- [backend/src/main/java/com/neml/badminton/controller/NavigationController.java](<../../backend/src/main/java/com/neml/badminton/controller/NavigationController.java>)
- [backend/src/main/java/com/neml/badminton/controller/SportConfigurationController.java](<../../backend/src/main/java/com/neml/badminton/controller/SportConfigurationController.java>)
- [backend/src/main/java/com/neml/badminton/controller/SquadConfirmationController.java](<../../backend/src/main/java/com/neml/badminton/controller/SquadConfirmationController.java>)
- [backend/src/main/java/com/neml/badminton/controller/TenantAnalyticsController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantAnalyticsController.java>)
- [backend/src/main/java/com/neml/badminton/controller/TenantAuctionController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantAuctionController.java>)
- [backend/src/main/java/com/neml/badminton/controller/TenantDataController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantDataController.java>)
- [backend/src/main/java/com/neml/badminton/controller/TenantMatchController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantMatchController.java>)
- [backend/src/main/java/com/neml/badminton/controller/TenantPlayerAdminController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantPlayerAdminController.java>) — records: `PlayerRequest`

### service

- [backend/src/main/java/com/neml/badminton/service/AdministrativeConfigurationService.java](<../../backend/src/main/java/com/neml/badminton/service/AdministrativeConfigurationService.java>)
- [backend/src/main/java/com/neml/badminton/service/AnalyticsService.java](<../../backend/src/main/java/com/neml/badminton/service/AnalyticsService.java>)
- [backend/src/main/java/com/neml/badminton/service/AuthService.java](<../../backend/src/main/java/com/neml/badminton/service/AuthService.java>)
- [backend/src/main/java/com/neml/badminton/service/ChampionshipService.java](<../../backend/src/main/java/com/neml/badminton/service/ChampionshipService.java>)
- [backend/src/main/java/com/neml/badminton/service/CompetitionService.java](<../../backend/src/main/java/com/neml/badminton/service/CompetitionService.java>)
- [backend/src/main/java/com/neml/badminton/service/ConfigurationSectionService.java](<../../backend/src/main/java/com/neml/badminton/service/ConfigurationSectionService.java>)
- [backend/src/main/java/com/neml/badminton/service/CricketScoringService.java](<../../backend/src/main/java/com/neml/badminton/service/CricketScoringService.java>)
- [backend/src/main/java/com/neml/badminton/service/MatchService.java](<../../backend/src/main/java/com/neml/badminton/service/MatchService.java>)
- [backend/src/main/java/com/neml/badminton/service/MessageTrackerGateway.java](<../../backend/src/main/java/com/neml/badminton/service/MessageTrackerGateway.java>)
- [backend/src/main/java/com/neml/badminton/service/NavigationService.java](<../../backend/src/main/java/com/neml/badminton/service/NavigationService.java>)
- [backend/src/main/java/com/neml/badminton/service/SportConfigurationService.java](<../../backend/src/main/java/com/neml/badminton/service/SportConfigurationService.java>)
- [backend/src/main/java/com/neml/badminton/service/SquadConfirmationService.java](<../../backend/src/main/java/com/neml/badminton/service/SquadConfirmationService.java>) — records: `squad`, `RosterValidation`
- [backend/src/main/java/com/neml/badminton/service/TemporaryPasswordGenerator.java](<../../backend/src/main/java/com/neml/badminton/service/TemporaryPasswordGenerator.java>)
- [backend/src/main/java/com/neml/badminton/service/TenantAuctionService.java](<../../backend/src/main/java/com/neml/badminton/service/TenantAuctionService.java>)

### dto

- [backend/src/main/java/com/neml/badminton/dto/AdministrativeConfigurationDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/AdministrativeConfigurationDtos.java>) — records: `BrandingDto`, `BrandingRequest`, `AuctionConfigDto`, `AuctionConfigRequest`
- [backend/src/main/java/com/neml/badminton/dto/AuthDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/AuthDtos.java>) — records: `LoginRequest`, `AuthResponse`, `ChampionshipAccess`, `UserInfo`
- [backend/src/main/java/com/neml/badminton/dto/ChampionshipDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/ChampionshipDtos.java>) — records: `CreateChampionshipRequest`, `ConfigureChampionshipRequest`, `ChampionshipSettingsDto`, `CreateTeamCaptainRequest`, `ProvisionedTeamDto`, `JoinRoomRequest`, `AssignRoleRequest`, `ChampionshipDto`, `ProvisionedAdminDto`, `CreateChampionshipResponse`, `JoinRoomResponse`
- [backend/src/main/java/com/neml/badminton/dto/CompetitionDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/CompetitionDtos.java>) — records: `CompetitionConfigurationRequest`, `CompetitionConfigurationDto`, `StageDto`, `QualificationDto`, `AuditEventDto`, `CompetitionOverviewDto`, `DashboardCapabilitiesDto`, `SportCapabilitiesDto`, `RescheduleFixtureRequest`, `ConfigurationActionRequest`, `ConfigurationFieldSchema`, `ConfigurationSectionDto`, `ConfigurationWorkspaceDto`, `PhaseReadinessDto`
- [backend/src/main/java/com/neml/badminton/dto/CricketDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/CricketDtos.java>) — records: `PlayingXiRequest`, `TossRequest`, `StartInningsRequest`, `DeliveryRequest`, `BatterStatsDto`, `BowlerStatsDto`, `DeliveryDto`, `InningsDto`, `MatchStateDto`, `AdministrativeResultRequest`, `DeliveryCorrectionRequest`, `TargetOverrideRequest`
- [backend/src/main/java/com/neml/badminton/dto/Dtos.java](<../../backend/src/main/java/com/neml/badminton/dto/Dtos.java>) — records: `TeamDto`, `PlayerDto`, `BidDto`, `AuctionStateDto`, `PlaceBidRequest`, `SellRequest`, `NextPlayerRequest`, `SetStatusRequest`, `UpdateBasePriceRequest`
- [backend/src/main/java/com/neml/badminton/dto/MatchDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/MatchDtos.java>) — records: `AdministrativeFixtureResultRequest`, `TeamRef`, `PlayerRef`, `FormatDto`, `MatchDto`, `CreateMatchRequest`, `AssignPlayersRequest`, `ReportFormatResultRequest`, `StandingDto`, `FormatLeaderDto`, `TeamStat`, `TeamAnalysisDto`, `FormatBreakdown`, `HeadToHead`, `TopPerformerDto`
- [backend/src/main/java/com/neml/badminton/dto/NavigationDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/NavigationDtos.java>) — records: `ScreenDto`, `NavigationResponse`
- [backend/src/main/java/com/neml/badminton/dto/SportConfigurationDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/SportConfigurationDtos.java>) — records: `RubberRuleDto`, `BadmintonConfigDto`, `BadmintonConfigRequest`, `RubberRuleRequest`, `CricketConfigDto`, `CricketConfigRequest`, `QuotaRuleDto`, `QuotaRuleRequest`
- [backend/src/main/java/com/neml/badminton/dto/SquadConfirmationDtos.java](<../../backend/src/main/java/com/neml/badminton/dto/SquadConfirmationDtos.java>) — records: `SquadActionRequest`, `PersonRef`, `SquadTeamDto`, `SquadSummaryDto`, `SquadEventDto`

### entity

- [backend/src/main/java/com/neml/badminton/entity/Announcement.java](<../../backend/src/main/java/com/neml/badminton/entity/Announcement.java>) — table: `announcements`
- [backend/src/main/java/com/neml/badminton/entity/AppScreen.java](<../../backend/src/main/java/com/neml/badminton/entity/AppScreen.java>) — table: `app_screens`
- [backend/src/main/java/com/neml/badminton/entity/Auction.java](<../../backend/src/main/java/com/neml/badminton/entity/Auction.java>) — table: `auctions`
- [backend/src/main/java/com/neml/badminton/entity/AuctionConfiguration.java](<../../backend/src/main/java/com/neml/badminton/entity/AuctionConfiguration.java>) — table: `auction_configurations`
- [backend/src/main/java/com/neml/badminton/entity/AuctionState.java](<../../backend/src/main/java/com/neml/badminton/entity/AuctionState.java>) — table: `auction_state`
- [backend/src/main/java/com/neml/badminton/entity/AuctionStatus.java](<../../backend/src/main/java/com/neml/badminton/entity/AuctionStatus.java>)
- [backend/src/main/java/com/neml/badminton/entity/BadmintonConfiguration.java](<../../backend/src/main/java/com/neml/badminton/entity/BadmintonConfiguration.java>) — table: `badminton_configurations`
- [backend/src/main/java/com/neml/badminton/entity/BadmintonRubberRule.java](<../../backend/src/main/java/com/neml/badminton/entity/BadmintonRubberRule.java>) — table: `badminton_rubber_rules`
- [backend/src/main/java/com/neml/badminton/entity/Bid.java](<../../backend/src/main/java/com/neml/badminton/entity/Bid.java>) — table: `bids`
- [backend/src/main/java/com/neml/badminton/entity/Championship.java](<../../backend/src/main/java/com/neml/badminton/entity/Championship.java>) — table: `championships`
- [backend/src/main/java/com/neml/badminton/entity/ChampionshipBrandingConfiguration.java](<../../backend/src/main/java/com/neml/badminton/entity/ChampionshipBrandingConfiguration.java>) — table: `championship_branding_configurations`
- [backend/src/main/java/com/neml/badminton/entity/ChampionshipPhase.java](<../../backend/src/main/java/com/neml/badminton/entity/ChampionshipPhase.java>)
- [backend/src/main/java/com/neml/badminton/entity/ChampionshipRole.java](<../../backend/src/main/java/com/neml/badminton/entity/ChampionshipRole.java>) — table: `championship_roles`
- [backend/src/main/java/com/neml/badminton/entity/ChampionshipRoleType.java](<../../backend/src/main/java/com/neml/badminton/entity/ChampionshipRoleType.java>)
- [backend/src/main/java/com/neml/badminton/entity/ChampionshipStatus.java](<../../backend/src/main/java/com/neml/badminton/entity/ChampionshipStatus.java>)
- [backend/src/main/java/com/neml/badminton/entity/CompetitionAuditEvent.java](<../../backend/src/main/java/com/neml/badminton/entity/CompetitionAuditEvent.java>) — table: `competition_audit_events`
- [backend/src/main/java/com/neml/badminton/entity/CompetitionConfiguration.java](<../../backend/src/main/java/com/neml/badminton/entity/CompetitionConfiguration.java>) — table: `competition_configurations`
- [backend/src/main/java/com/neml/badminton/entity/CompetitionQualification.java](<../../backend/src/main/java/com/neml/badminton/entity/CompetitionQualification.java>) — table: `competition_qualifications`
- [backend/src/main/java/com/neml/badminton/entity/CompetitionStage.java](<../../backend/src/main/java/com/neml/badminton/entity/CompetitionStage.java>) — table: `competition_stages`
- [backend/src/main/java/com/neml/badminton/entity/CompetitionStageStatus.java](<../../backend/src/main/java/com/neml/badminton/entity/CompetitionStageStatus.java>)
- [backend/src/main/java/com/neml/badminton/entity/CompetitionStageType.java](<../../backend/src/main/java/com/neml/badminton/entity/CompetitionStageType.java>)
- [backend/src/main/java/com/neml/badminton/entity/ConfigurationSectionState.java](<../../backend/src/main/java/com/neml/badminton/entity/ConfigurationSectionState.java>) — table: `configuration_section_states`
- [backend/src/main/java/com/neml/badminton/entity/ConfigurationStatus.java](<../../backend/src/main/java/com/neml/badminton/entity/ConfigurationStatus.java>)
- [backend/src/main/java/com/neml/badminton/entity/CricketConfiguration.java](<../../backend/src/main/java/com/neml/badminton/entity/CricketConfiguration.java>) — table: `cricket_configurations`
- [backend/src/main/java/com/neml/badminton/entity/CricketDelivery.java](<../../backend/src/main/java/com/neml/badminton/entity/CricketDelivery.java>) — table: `cricket_deliveries`
- [backend/src/main/java/com/neml/badminton/entity/CricketInnings.java](<../../backend/src/main/java/com/neml/badminton/entity/CricketInnings.java>) — table: `cricket_innings`
- [backend/src/main/java/com/neml/badminton/entity/CricketInningsStatus.java](<../../backend/src/main/java/com/neml/badminton/entity/CricketInningsStatus.java>)
- [backend/src/main/java/com/neml/badminton/entity/CricketMatchState.java](<../../backend/src/main/java/com/neml/badminton/entity/CricketMatchState.java>) — table: `cricket_match_states`
- [backend/src/main/java/com/neml/badminton/entity/CricketPlayingMember.java](<../../backend/src/main/java/com/neml/badminton/entity/CricketPlayingMember.java>) — table: `cricket_playing_members`
- [backend/src/main/java/com/neml/badminton/entity/FormatType.java](<../../backend/src/main/java/com/neml/badminton/entity/FormatType.java>)
- [backend/src/main/java/com/neml/badminton/entity/Gender.java](<../../backend/src/main/java/com/neml/badminton/entity/Gender.java>)
- [backend/src/main/java/com/neml/badminton/entity/Match.java](<../../backend/src/main/java/com/neml/badminton/entity/Match.java>) — table: `matches`
- [backend/src/main/java/com/neml/badminton/entity/MatchFormat.java](<../../backend/src/main/java/com/neml/badminton/entity/MatchFormat.java>) — table: `match_formats`
- [backend/src/main/java/com/neml/badminton/entity/MatchStatus.java](<../../backend/src/main/java/com/neml/badminton/entity/MatchStatus.java>)
- [backend/src/main/java/com/neml/badminton/entity/NavigationRole.java](<../../backend/src/main/java/com/neml/badminton/entity/NavigationRole.java>)
- [backend/src/main/java/com/neml/badminton/entity/Player.java](<../../backend/src/main/java/com/neml/badminton/entity/Player.java>) — table: `players`
- [backend/src/main/java/com/neml/badminton/entity/PlayerStatus.java](<../../backend/src/main/java/com/neml/badminton/entity/PlayerStatus.java>)
- [backend/src/main/java/com/neml/badminton/entity/PlayoffFormat.java](<../../backend/src/main/java/com/neml/badminton/entity/PlayoffFormat.java>)
- [backend/src/main/java/com/neml/badminton/entity/Role.java](<../../backend/src/main/java/com/neml/badminton/entity/Role.java>)
- [backend/src/main/java/com/neml/badminton/entity/RoleScreenMapping.java](<../../backend/src/main/java/com/neml/badminton/entity/RoleScreenMapping.java>) — table: `role_screen_mappings`
- [backend/src/main/java/com/neml/badminton/entity/RosterQuotaRule.java](<../../backend/src/main/java/com/neml/badminton/entity/RosterQuotaRule.java>) — table: `roster_quota_rules`
- [backend/src/main/java/com/neml/badminton/entity/RoundRobinMode.java](<../../backend/src/main/java/com/neml/badminton/entity/RoundRobinMode.java>)
- [backend/src/main/java/com/neml/badminton/entity/Season.java](<../../backend/src/main/java/com/neml/badminton/entity/Season.java>) — table: `seasons`
- [backend/src/main/java/com/neml/badminton/entity/SquadConfirmation.java](<../../backend/src/main/java/com/neml/badminton/entity/SquadConfirmation.java>) — table: `squad_confirmations`
- [backend/src/main/java/com/neml/badminton/entity/SquadConfirmationAction.java](<../../backend/src/main/java/com/neml/badminton/entity/SquadConfirmationAction.java>)
- [backend/src/main/java/com/neml/badminton/entity/SquadConfirmationEvent.java](<../../backend/src/main/java/com/neml/badminton/entity/SquadConfirmationEvent.java>) — table: `squad_confirmation_events`
- [backend/src/main/java/com/neml/badminton/entity/SquadConfirmationStatus.java](<../../backend/src/main/java/com/neml/badminton/entity/SquadConfirmationStatus.java>)
- [backend/src/main/java/com/neml/badminton/entity/Team.java](<../../backend/src/main/java/com/neml/badminton/entity/Team.java>) — table: `teams`
- [backend/src/main/java/com/neml/badminton/entity/TieBreaker.java](<../../backend/src/main/java/com/neml/badminton/entity/TieBreaker.java>)
- [backend/src/main/java/com/neml/badminton/entity/TournamentSettings.java](<../../backend/src/main/java/com/neml/badminton/entity/TournamentSettings.java>) — table: `tournament_settings`
- [backend/src/main/java/com/neml/badminton/entity/User.java](<../../backend/src/main/java/com/neml/badminton/entity/User.java>) — table: `users`

### repository

- [backend/src/main/java/com/neml/badminton/repository/AnnouncementRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/AnnouncementRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/AppScreenRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/AppScreenRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/AuctionConfigurationRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/AuctionConfigurationRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/AuctionRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/AuctionRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/AuctionStateRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/AuctionStateRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/BadmintonConfigurationRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/BadmintonConfigurationRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/BidRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/BidRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/ChampionshipBrandingConfigurationRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/ChampionshipBrandingConfigurationRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/ChampionshipRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/ChampionshipRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/ChampionshipRoleRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/ChampionshipRoleRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CompetitionAuditEventRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CompetitionAuditEventRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CompetitionConfigurationRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CompetitionConfigurationRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CompetitionQualificationRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CompetitionQualificationRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CompetitionStageRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CompetitionStageRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/ConfigurationSectionStateRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/ConfigurationSectionStateRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CricketConfigurationRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CricketConfigurationRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CricketDeliveryRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CricketDeliveryRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CricketInningsRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CricketInningsRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CricketMatchStateRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CricketMatchStateRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/CricketPlayingMemberRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/CricketPlayingMemberRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/MatchFormatRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/MatchFormatRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/MatchRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/MatchRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/PlayerRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/PlayerRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/RoleScreenMappingRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/RoleScreenMappingRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/RosterQuotaRuleRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/RosterQuotaRuleRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/SeasonRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/SeasonRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/SquadConfirmationEventRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/SquadConfirmationEventRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/SquadConfirmationRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/SquadConfirmationRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/TeamRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/TeamRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/TournamentSettingsRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/TournamentSettingsRepository.java>)
- [backend/src/main/java/com/neml/badminton/repository/UserRepository.java](<../../backend/src/main/java/com/neml/badminton/repository/UserRepository.java>)

### competition

- [backend/src/main/java/com/neml/badminton/competition/BadmintonCompetitionEngine.java](<../../backend/src/main/java/com/neml/badminton/competition/BadmintonCompetitionEngine.java>)
- [backend/src/main/java/com/neml/badminton/competition/CricketCompetitionEngine.java](<../../backend/src/main/java/com/neml/badminton/competition/CricketCompetitionEngine.java>)
- [backend/src/main/java/com/neml/badminton/competition/SportCompetitionEngine.java](<../../backend/src/main/java/com/neml/badminton/competition/SportCompetitionEngine.java>) — records: `CompetitionDefaults`, `RubberDefinition`, `MatchOutcome`, `SeedPair`
- [backend/src/main/java/com/neml/badminton/competition/SportEngineRegistry.java](<../../backend/src/main/java/com/neml/badminton/competition/SportEngineRegistry.java>)

### security

- [backend/src/main/java/com/neml/badminton/security/ChampionshipSecurity.java](<../../backend/src/main/java/com/neml/badminton/security/ChampionshipSecurity.java>)
- [backend/src/main/java/com/neml/badminton/security/JwtAuthFilter.java](<../../backend/src/main/java/com/neml/badminton/security/JwtAuthFilter.java>)
- [backend/src/main/java/com/neml/badminton/security/JwtService.java](<../../backend/src/main/java/com/neml/badminton/security/JwtService.java>)
- [backend/src/main/java/com/neml/badminton/security/ViewerPrincipal.java](<../../backend/src/main/java/com/neml/badminton/security/ViewerPrincipal.java>) — records: `ViewerPrincipal`

### config

- [backend/src/main/java/com/neml/badminton/config/ApplicationLifecycleLogger.java](<../../backend/src/main/java/com/neml/badminton/config/ApplicationLifecycleLogger.java>)
- [backend/src/main/java/com/neml/badminton/config/JacksonConfig.java](<../../backend/src/main/java/com/neml/badminton/config/JacksonConfig.java>)
- [backend/src/main/java/com/neml/badminton/config/RequestLoggingFilter.java](<../../backend/src/main/java/com/neml/badminton/config/RequestLoggingFilter.java>)
- [backend/src/main/java/com/neml/badminton/config/SecurityConfig.java](<../../backend/src/main/java/com/neml/badminton/config/SecurityConfig.java>)
- [backend/src/main/java/com/neml/badminton/config/StompAuthorizationInterceptor.java](<../../backend/src/main/java/com/neml/badminton/config/StompAuthorizationInterceptor.java>)
- [backend/src/main/java/com/neml/badminton/config/WebSocketConfig.java](<../../backend/src/main/java/com/neml/badminton/config/WebSocketConfig.java>)

### seed

- [backend/src/main/java/com/neml/badminton/seed/DataSeeder.java](<../../backend/src/main/java/com/neml/badminton/seed/DataSeeder.java>) — records: `ScreenSeed`
- [backend/src/main/java/com/neml/badminton/seed/SuperAdminBootstrap.java](<../../backend/src/main/java/com/neml/badminton/seed/SuperAdminBootstrap.java>)

### websocket

- [backend/src/main/java/com/neml/badminton/websocket/AuctionBroadcaster.java](<../../backend/src/main/java/com/neml/badminton/websocket/AuctionBroadcaster.java>)

## Controller route and authorization annotations

Annotations are shown in source order. Combine each class RequestMapping with method mappings; class-level PreAuthorize applies to its methods. A mapping without arguments uses the class path. See the source link for annotation-to-method association.

### AdministrativeConfigurationController.java

[backend/src/main/java/com/neml/badminton/controller/AdministrativeConfigurationController.java](<../../backend/src/main/java/com/neml/badminton/controller/AdministrativeConfigurationController.java>)

```java
@RequestMapping("/api/championships/{championshipId}/administrative-configuration")
@GetMapping("/branding")
@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")
@PutMapping("/branding")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
@GetMapping("/auction")
@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")
@PutMapping("/auction")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
```

### AuthController.java

[backend/src/main/java/com/neml/badminton/controller/AuthController.java](<../../backend/src/main/java/com/neml/badminton/controller/AuthController.java>)

```java
@RequestMapping("/api/auth")
@PostMapping("/login")
@PostMapping("/logout")
@GetMapping("/me")
```

### ChampionshipController.java

[backend/src/main/java/com/neml/badminton/controller/ChampionshipController.java](<../../backend/src/main/java/com/neml/badminton/controller/ChampionshipController.java>)

```java
@RequestMapping("/api/championships")
@PostMapping
@PreAuthorize("hasRole('SUPER_ADMIN')")
@GetMapping
@PostMapping("/join-room")
@GetMapping("/{championshipId}")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@PostMapping("/{championshipId}/roles")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@GetMapping("/{championshipId}/settings")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PutMapping("/{championshipId}/settings")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/{championshipId}/admin/teams")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
```

### CompetitionController.java

[backend/src/main/java/com/neml/badminton/controller/CompetitionController.java](<../../backend/src/main/java/com/neml/badminton/controller/CompetitionController.java>)

```java
@RequestMapping("/api/championships/{championshipId}")
@GetMapping("/competition")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@GetMapping("/competition/configuration")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@GetMapping("/competition/capabilities")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@GetMapping("/competition/audit")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@GetMapping("/dashboard")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@PutMapping("/admin/competition/configuration")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/competition/league/generate")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/competition/playoffs/generate")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PatchMapping("/admin/competition/fixtures/{fixtureId}/schedule")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@GetMapping("/configuration/workspace")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@PostMapping("/admin/configuration/validate")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/configuration/reopen")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@GetMapping("/phase-readiness")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@PostMapping("/admin/phases/{target}/advance")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
```

### CricketController.java

[backend/src/main/java/com/neml/badminton/controller/CricketController.java](<../../backend/src/main/java/com/neml/badminton/controller/CricketController.java>)

```java
@RequestMapping("/api/championships/{championshipId}/cricket/matches/{matchId}")
@PutMapping("/playing-xi/{teamId}")
@PreAuthorize("@championshipSecurity.canBid(#championshipId,#teamId,authentication)")
@PostMapping("/toss")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
@PostMapping("/innings")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
@PostMapping("/innings/{inningsId}/deliveries")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
@DeleteMapping("/innings/{inningsId}/deliveries/{deliveryId}")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
@PutMapping("/target")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
@GetMapping
@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")
@PostMapping("/administrative-result")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
```

### HealthController.java

[backend/src/main/java/com/neml/badminton/controller/HealthController.java](<../../backend/src/main/java/com/neml/badminton/controller/HealthController.java>)

```java
@RequestMapping("/api")
@GetMapping("/health")
```

### NavigationController.java

[backend/src/main/java/com/neml/badminton/controller/NavigationController.java](<../../backend/src/main/java/com/neml/badminton/controller/NavigationController.java>)

```java
@RequestMapping("/api/navigation")
@GetMapping
```

### SportConfigurationController.java

[backend/src/main/java/com/neml/badminton/controller/SportConfigurationController.java](<../../backend/src/main/java/com/neml/badminton/controller/SportConfigurationController.java>)

```java
@RequestMapping("/api/championships/{championshipId}/sport-configuration")
@GetMapping("/badminton")
@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")
@PutMapping("/badminton")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
@GetMapping("/cricket")
@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")
@PutMapping("/cricket")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
@GetMapping("/quotas")
@PreAuthorize("@championshipSecurity.canView(#championshipId,authentication)")
@PutMapping("/quotas")
@PreAuthorize("@championshipSecurity.canManage(#championshipId,authentication)")
```

### SquadConfirmationController.java

[backend/src/main/java/com/neml/badminton/controller/SquadConfirmationController.java](<../../backend/src/main/java/com/neml/badminton/controller/SquadConfirmationController.java>)

```java
@RequestMapping("/api/championships/{championshipId}/squad-confirmations")
@GetMapping
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@GetMapping("/history")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@PostMapping("/teams/{teamId}/confirm")
@PreAuthorize("@championshipSecurity.canConfirmSquad(#championshipId, #teamId, authentication)")
@PostMapping("/teams/{teamId}/lock")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/lock-all")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/teams/{teamId}/reopen")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
```

### TenantAnalyticsController.java

[backend/src/main/java/com/neml/badminton/controller/TenantAnalyticsController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantAnalyticsController.java>)

```java
@RequestMapping("/api/championships/{championshipId}/analytics")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@GetMapping("/standings")
@GetMapping("/format-leaders")
@GetMapping("/team/{id}")
@GetMapping("/top-performers")
```

### TenantAuctionController.java

[backend/src/main/java/com/neml/badminton/controller/TenantAuctionController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantAuctionController.java>)

```java
@RequestMapping("/api/championships/{championshipId}/auctions/{auctionId}")
@GetMapping("/state")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@GetMapping("/history")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@PostMapping("/bid")
@PreAuthorize("@championshipSecurity.canBid(#championshipId, #req.teamId(), authentication)")
@PostMapping("/admin/{action}")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/set-current")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/reset-unsold")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/set-status")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/coin-toss")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PutMapping("/admin/players/{playerId}/base-price")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
```

### TenantDataController.java

[backend/src/main/java/com/neml/badminton/controller/TenantDataController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantDataController.java>)

```java
@RequestMapping("/api/championships/{championshipId}")
@PreAuthorize("@championshipSecurity.canView(#championshipId, authentication)")
@GetMapping("/teams")
@GetMapping("/teams/{id}")
@GetMapping("/teams/{id}/players")
@GetMapping("/players")
@GetMapping("/players/{id}")
@GetMapping("/matches")
@GetMapping("/matches/{id}")
```

### TenantMatchController.java

[backend/src/main/java/com/neml/badminton/controller/TenantMatchController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantMatchController.java>)

```java
@RequestMapping("/api/championships/{championshipId}")
@PostMapping("/admin/matches")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/matches/formats/{formatId}/assign")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/matches/formats/{formatId}/result")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PutMapping("/admin/matches/formats/{formatId}/result")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/matches/{id}/administrative-result")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping("/admin/matches/formats/{formatId}/live-score")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@DeleteMapping("/admin/matches/{id}")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
```

### TenantPlayerAdminController.java

[backend/src/main/java/com/neml/badminton/controller/TenantPlayerAdminController.java](<../../backend/src/main/java/com/neml/badminton/controller/TenantPlayerAdminController.java>)

```java
@RequestMapping("/api/championships/{championshipId}/admin/players")
@PreAuthorize("@championshipSecurity.canManage(#championshipId, authentication)")
@PostMapping
@PutMapping("/{id}")
@DeleteMapping("/{id}")
```

## Frontend pages and layouts

- [frontend/src/app/(authed)/admin/championship-launchpad/page.tsx](<../../frontend/src/app/(authed)/admin/championship-launchpad/page.tsx>) — route `/admin/championship-launchpad`
- [frontend/src/app/(authed)/admin/championship-settings/page.tsx](<../../frontend/src/app/(authed)/admin/championship-settings/page.tsx>) — route `/admin/championship-settings`
- [frontend/src/app/(authed)/admin/championships/page.tsx](<../../frontend/src/app/(authed)/admin/championships/page.tsx>) — route `/admin/championships`
- [frontend/src/app/(authed)/admin/team-management/page.tsx](<../../frontend/src/app/(authed)/admin/team-management/page.tsx>) — route `/admin/team-management`
- [frontend/src/app/(authed)/analysis/page.tsx](<../../frontend/src/app/(authed)/analysis/page.tsx>) — route `/analysis`
- [frontend/src/app/(authed)/announcements/page.tsx](<../../frontend/src/app/(authed)/announcements/page.tsx>) — route `/announcements`
- [frontend/src/app/(authed)/auction/page.tsx](<../../frontend/src/app/(authed)/auction/page.tsx>) — route `/auction`
- [frontend/src/app/(authed)/competition/page.tsx](<../../frontend/src/app/(authed)/competition/page.tsx>) — route `/competition`
- [frontend/src/app/(authed)/dashboard/page.tsx](<../../frontend/src/app/(authed)/dashboard/page.tsx>) — route `/dashboard`
- [frontend/src/app/(authed)/format-leaders/page.tsx](<../../frontend/src/app/(authed)/format-leaders/page.tsx>) — route `/format-leaders`
- [frontend/src/app/(authed)/history/page.tsx](<../../frontend/src/app/(authed)/history/page.tsx>) — route `/history`
- [frontend/src/app/(authed)/layout.tsx](<../../frontend/src/app/(authed)/layout.tsx>) — layout `/`
- [frontend/src/app/(authed)/matches/page.tsx](<../../frontend/src/app/(authed)/matches/page.tsx>) — route `/matches`
- [frontend/src/app/(authed)/players/page.tsx](<../../frontend/src/app/(authed)/players/page.tsx>) — route `/players`
- [frontend/src/app/(authed)/scoreboard/page.tsx](<../../frontend/src/app/(authed)/scoreboard/page.tsx>) — route `/scoreboard`
- [frontend/src/app/(authed)/squad-confirmation/page.tsx](<../../frontend/src/app/(authed)/squad-confirmation/page.tsx>) — route `/squad-confirmation`
- [frontend/src/app/(authed)/teams/page.tsx](<../../frontend/src/app/(authed)/teams/page.tsx>) — route `/teams`
- [frontend/src/app/(authed)/top-performers/page.tsx](<../../frontend/src/app/(authed)/top-performers/page.tsx>) — route `/top-performers`
- [frontend/src/app/layout.tsx](<../../frontend/src/app/layout.tsx>) — layout `/`
- [frontend/src/app/login/page.tsx](<../../frontend/src/app/login/page.tsx>) — route `/login`
- [frontend/src/app/page.tsx](<../../frontend/src/app/page.tsx>) — route `/`
- [frontend/src/app/room/join/page.tsx](<../../frontend/src/app/room/join/page.tsx>) — route `/room/join`

## Shared components

- [frontend/src/components/AppShell.tsx](<../../frontend/src/components/AppShell.tsx>)
- [frontend/src/components/ComingSoon.tsx](<../../frontend/src/components/ComingSoon.tsx>)
- [frontend/src/components/CricketMatchPanel.tsx](<../../frontend/src/components/CricketMatchPanel.tsx>)
- [frontend/src/components/SplashScreen.tsx](<../../frontend/src/components/SplashScreen.tsx>)
- [frontend/src/components/auction/AuctionUnavailable.tsx](<../../frontend/src/components/auction/AuctionUnavailable.tsx>)
- [frontend/src/components/auction/BasePriceDialog.tsx](<../../frontend/src/components/auction/BasePriceDialog.tsx>)
- [frontend/src/components/auction/BidPanel.tsx](<../../frontend/src/components/auction/BidPanel.tsx>)
- [frontend/src/components/auction/CoinTossDialog.tsx](<../../frontend/src/components/auction/CoinTossDialog.tsx>)
- [frontend/src/components/players/PlayerDialog.tsx](<../../frontend/src/components/players/PlayerDialog.tsx>)
- [frontend/src/components/theme-provider.tsx](<../../frontend/src/components/theme-provider.tsx>)
- [frontend/src/components/theme-toggle.tsx](<../../frontend/src/components/theme-toggle.tsx>)

## Frontend state, transport and formatting

- [frontend/src/lib/api.ts](<../../frontend/src/lib/api.ts>)
- [frontend/src/lib/auth.ts](<../../frontend/src/lib/auth.ts>)
- [frontend/src/lib/championship.ts](<../../frontend/src/lib/championship.ts>)
- [frontend/src/lib/format.ts](<../../frontend/src/lib/format.ts>)
- [frontend/src/lib/navigation.ts](<../../frontend/src/lib/navigation.ts>)
- [frontend/src/lib/ws.ts](<../../frontend/src/lib/ws.ts>)

## Database scripts

- [docs/sql/2026-08-22-championship-admin-configuration.sql](<../../docs/sql/2026-08-22-championship-admin-configuration.sql>)
- [docs/sql/2026-08-22-strict-multi-championship.sql](<../../docs/sql/2026-08-22-strict-multi-championship.sql>)
- [docs/sql/2026-08-23-phase-2-squad-confirmation.sql](<../../docs/sql/2026-08-23-phase-2-squad-confirmation.sql>)
- [docs/sql/2026-09-01-phase-3-4-sport-competition.sql](<../../docs/sql/2026-09-01-phase-3-4-sport-competition.sql>)
- [docs/sql/2026-09-01-versioned-championship-sport-configuration.sql](<../../docs/sql/2026-09-01-versioned-championship-sport-configuration.sql>)
- [docs/sql/sports-play-complete-schema.sql](<../../docs/sql/sports-play-complete-schema.sql>)

## Backend tests

- [backend/src/test/java/com/neml/badminton/architecture/CompetitionMappingTest.java](<../../backend/src/test/java/com/neml/badminton/architecture/CompetitionMappingTest.java>)
- [backend/src/test/java/com/neml/badminton/architecture/TenantIsolationArchitectureTest.java](<../../backend/src/test/java/com/neml/badminton/architecture/TenantIsolationArchitectureTest.java>)
- [backend/src/test/java/com/neml/badminton/competition/BadmintonCompetitionEngineTest.java](<../../backend/src/test/java/com/neml/badminton/competition/BadmintonCompetitionEngineTest.java>)
- [backend/src/test/java/com/neml/badminton/competition/CricketCompetitionEngineTest.java](<../../backend/src/test/java/com/neml/badminton/competition/CricketCompetitionEngineTest.java>)
- [backend/src/test/java/com/neml/badminton/controller/ApiExceptionHandlerTest.java](<../../backend/src/test/java/com/neml/badminton/controller/ApiExceptionHandlerTest.java>)
- [backend/src/test/java/com/neml/badminton/security/ChampionshipSecurityTest.java](<../../backend/src/test/java/com/neml/badminton/security/ChampionshipSecurityTest.java>)
- [backend/src/test/java/com/neml/badminton/service/ChampionshipConfigurationInitializationTest.java](<../../backend/src/test/java/com/neml/badminton/service/ChampionshipConfigurationInitializationTest.java>)
- [backend/src/test/java/com/neml/badminton/service/CompetitionFixtureGenerationTest.java](<../../backend/src/test/java/com/neml/badminton/service/CompetitionFixtureGenerationTest.java>)
- [backend/src/test/java/com/neml/badminton/service/CricketScoringRulesTest.java](<../../backend/src/test/java/com/neml/badminton/service/CricketScoringRulesTest.java>)
- [backend/src/test/java/com/neml/badminton/service/SquadConfirmationServiceTest.java](<../../backend/src/test/java/com/neml/badminton/service/SquadConfirmationServiceTest.java>)
- [backend/src/test/java/com/neml/badminton/service/TemporaryPasswordGeneratorTest.java](<../../backend/src/test/java/com/neml/badminton/service/TemporaryPasswordGeneratorTest.java>)

## Build and configuration

- [backend/pom.xml](<../../backend/pom.xml>)
- [backend/Dockerfile](<../../backend/Dockerfile>)
- [backend/src/main/resources/application.yml](<../../backend/src/main/resources/application.yml>)
- [backend/src/main/resources/application-local.yml](<../../backend/src/main/resources/application-local.yml>)
- [backend/src/main/resources/logback-spring.xml](<../../backend/src/main/resources/logback-spring.xml>)
- [frontend/package.json](<../../frontend/package.json>)
- [frontend/package-lock.json](<../../frontend/package-lock.json>)
- [frontend/next.config.js](<../../frontend/next.config.js>)
- [frontend/tailwind.config.js](<../../frontend/tailwind.config.js>)
- [frontend/tsconfig.json](<../../frontend/tsconfig.json>)
- [frontend/.eslintrc.json](<../../frontend/.eslintrc.json>)
- [frontend/Dockerfile](<../../frontend/Dockerfile>)
- [docker-compose.yml](<../../docker-compose.yml>)
- [README.md](<../../README.md>)
- [docs/RUNBOOK.md](<../../docs/RUNBOOK.md>)
- [docs/LOCAL_POSTGRESQL.md](<../../docs/LOCAL_POSTGRESQL.md>)
- [DEPLOYMENT.md](<../../DEPLOYMENT.md>)
- [DEPLOYMENT_MANUAL.md](<../../DEPLOYMENT_MANUAL.md>)
