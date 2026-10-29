import axios, { AxiosInstance } from "axios";

const BACKEND_URL =
  process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8001";

export const API_BASE = `${BACKEND_URL}/api`;
export const WS_BASE = process.env.NEXT_PUBLIC_WS_URL ?? BACKEND_URL;

// httpOnly cookie is set by the backend on /api/auth/login. `withCredentials: true`
// sends it on subsequent requests. Viewer tokens remain tab-local bearer credentials.
export const api: AxiosInstance = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
  withCredentials: true,
});

let csrfRequest: Promise<string> | null = null;
api.interceptors.request.use(async (config) => {
  if (typeof window === "undefined") return config;
  if (!["get", "head", "options"].includes((config.method || "get").toLowerCase())) {
    // Share only concurrent fetches. Authentication transitions can rotate the
    // CSRF cookie, so a token must never be cached for the lifetime of the tab.
    csrfRequest ||= api.get<{ token: string }>("/auth/csrf").then(({ data }) => data.token)
      .finally(() => { csrfRequest = null; });
    config.headers["X-XSRF-TOKEN"] = await csrfRequest;
  }
  config.headers["X-Correlation-ID"] = crypto.randomUUID();
  const viewerToken = sessionStorage.getItem("sports_viewer_token");
  if (viewerToken) config.headers.Authorization = `Bearer ${viewerToken}`;
  const championshipId = sessionStorage.getItem("sports_active_championship");
  const auctionId = sessionStorage.getItem("sports_active_auction");
  if (!config.url) return config;
  const u = config.url;
  const requiresChampionship =
    /^(\/teams|\/players|\/matches|\/analytics|\/admin\/(players|matches))(\/|\?|$)/.test(
      u,
    );
  const requiresAuction =
    /^(\/auction\/(state|bid|history)(\/|\?|$)|\/admin\/auction(\/|\?|$))/.test(
      u,
    );
  if ((requiresChampionship || requiresAuction) && !championshipId) {
    throw new Error("Select a championship before opening this screen");
  }
  if (requiresAuction && !auctionId) {
    throw new Error(
      "The championship admin must configure its auction before opening this screen",
    );
  }
  if (!championshipId) return config;
  if (auctionId && u === "/auction/state")
    config.url = `/championships/${championshipId}/auctions/${auctionId}/state`;
  else if (auctionId && u === "/auction/bid")
    config.url = `/championships/${championshipId}/auctions/${auctionId}/bid`;
  else if (auctionId && u === "/auction/history")
    config.url = `/championships/${championshipId}/auctions/${auctionId}/history`;
  else if (auctionId && u.startsWith("/admin/auction/"))
    config.url = `/championships/${championshipId}/auctions/${auctionId}/admin/${u.slice(15)}`;
  else if (u === "/teams" || u.startsWith("/teams/"))
    config.url = `/championships/${championshipId}${u}`;
  else if (u === "/players" || u.startsWith("/players/"))
    config.url = `/championships/${championshipId}${u}`;
  else if (u === "/admin/players" || u.startsWith("/admin/players/"))
    config.url = `/championships/${championshipId}${u}`;
  else if (
    (config.method || "get").toLowerCase() === "get" &&
    (u === "/matches" || u.startsWith("/matches/"))
  )
    config.url = `/championships/${championshipId}${u}`;
  else if (u === "/admin/matches" || u.startsWith("/admin/matches/"))
    config.url = `/championships/${championshipId}${u}`;
  else if (u === "/analytics" || u.startsWith("/analytics/"))
    config.url = `/championships/${championshipId}${u}`;
  return config;
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    const details = {
      method: err.config?.method?.toUpperCase(),
      path: err.config?.url,
      status: err.response?.status,
      traceId: err.response?.headers?.["x-correlation-id"],
      message: err.response?.data?.message || err.message,
    };
    if ((err.response?.status || 0) >= 500) console.error("API request failed", details);
    else console.warn("API request rejected", details);
    return Promise.reject(err);
  },
);

export type Role = "SUPER_ADMIN" | "USER";
export type ChampionshipRole =
  "CHAMPIONSHIP_ADMIN" | "TEAM_CAPTAIN" | "SPECTATOR";
export type NavigationRole = "SUPER_ADMIN" | ChampionshipRole;

export interface ScreenDto {
  code: string;
  label: string;
  path: string;
  icon: string;
  section: string;
  displayOrder: number;
}

export interface NavigationResponse {
  role: NavigationRole;
  screens: ScreenDto[];
}

export interface ChampionshipDto {
  id: string;
  name: string;
  sportType: string;
  roomCode: string;
  isPublic: boolean;
  status: "DRAFT" | "ACTIVE" | "COMPLETED" | "ARCHIVED";
  lifecyclePhase: ChampionshipPhase;
  defaultAuctionId?: string | null;
}

export type ChampionshipPhase =
  | "CREATION"
  | "CONFIGURATION"
  | "TEAM_PROVISIONING"
  | "PLAYER_REGISTRATION"
  | "AUCTION"
  | "SQUAD_CONFIRMATION"
  | "LEAGUE_STAGE"
  | "PLAYOFFS"
  | "FINAL"
  | "COMPLETED"
  | "ARCHIVED";
export type CompetitionStageType =
  "LEAGUE" | "SEMI_FINAL" | "THIRD_PLACE" | "FINAL";

export interface UserInfo {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  championships: {
    championshipId: string;
    role: ChampionshipRole;
    teamId?: string | null;
  }[];
}

export interface TeamDto {
  id: string;
  name: string;
  shortCode: string;
  logoUrl: string;
  primaryColor: string;
  purseTotal: number;
  purseRemaining: number;
  maleCount: number;
  femaleCount: number;
  totalPlayers: number;
  matchPoints: number;
}

export interface PlayerDto {
  photoUrl?: string | null;
  playerReference?: string | null;
  id: string;
  fullName: string;
  gender: "MALE" | "FEMALE";
  basePrice: number;
  soldPrice?: number | null;
  status: "AVAILABLE" | "ON_BLOCK" | "SOLD" | "UNSOLD" | "PREASSIGNED";
  captainUserId?: string | null;
  teamId?: string | null;
  teamName?: string | null;
  skillLevel?: string;
  auctionOrder?: number;
}

export interface BidDto {
  playerPhotoUrl?: string | null;
  id: string;
  playerId: string;
  playerName: string;
  teamId: string;
  teamName: string;
  amount: number;
  createdAt: string;
  active: boolean;
}

export interface TeamAuctionEligibility {
  teamId:string;premiumPurchases:number;premiumLimit?:number|null;squadSlotsRemaining:number;
  femalePlayers:number;minimumFemale:number;malePlayers:number;minimumMale:number;
  maximumAffordableBid:number;unavailableReason?:string|null;
}
export interface AuctionStateDto {
  minimumBid?:number;maximumBid?:number|null;maximumPremiumPurchases?:number|null;teamEligibility?:TeamAuctionEligibility[];
  revision: number;
  teamBids: BidDto[];
  status: "NOT_STARTED" | "RUNNING" | "PAUSED" | "COMPLETED";
  currentPlayer?: PlayerDto | null;
  highestBid?: BidDto | null;
  bidHistory: BidDto[];
  teams: TeamDto[];
  remainingPlayers: number;
  bidDeadline?: string | null;
  timerSeconds?: number | null;
  bidIncrement: number;
  maxSquadSize: number;
  minMale: number;
  minFemale: number;
}

export interface StandingDto {
  team: { id: string; name: string; shortCode: string; primaryColor: string };
  played: number;
  won: number;
  lost: number;
  formatWins: number;
  formatLosses: number;
  formatDiff: number;
  basePoints: number;
  penalty: number;
  totalPoints: number;
  netRunRate?: number | null;
  rank: number;
  unplayedPlayerIds: string[];
}

export interface FormatLeaderDto {
  formatType: string;
  leadingTeam?: {
    id: string;
    name: string;
    shortCode: string;
    primaryColor: string;
  } | null;
  wins: number;
  allTeams: {
    team: { id: string; name: string; shortCode: string; primaryColor: string };
    wins: number;
  }[];
}

export interface TeamAnalysisDto {
  team: { id: string; name: string; shortCode: string; primaryColor: string };
  played: number;
  won: number;
  lost: number;
  winPct: number;
  participationCount: number;
  squadSize: number;
  participationPct: number;
  unplayedPlayerIds: string[];
  strongestFormat?: string | null;
  weakestFormat?: string | null;
  formatBreakdown: { formatType: string; won: number; lost: number }[];
  headToHead: {
    opponent: {
      id: string;
      name: string;
      shortCode: string;
      primaryColor: string;
    };
    played: number;
    won: number;
    lost: number;
  }[];
}

export interface TopPerformerDto {
  player: { id: string; fullName: string; gender: string; teamId: string; photoUrl?: string | null };
  team: { id: string; name: string; shortCode: string; primaryColor: string };
  matchesPlayed: number;
  wins: number;
  losses: number;
  winPct: number;
  longestStreak: number;
  totalPointsWon: number;
  avgMargin: number;
}

export interface MatchFormatDto {
  id: string;
  formatType: string;
  formatOrder: number;
  label: string;
  shortLabel: string;
  playersPerSide: number;
  genderRule: "MALE" | "FEMALE" | "MIXED";
  sideAPlayers: {
    id: string;
    fullName: string;
    gender: string;
    teamId: string;
  }[];
  sideBPlayers: {
    id: string;
    fullName: string;
    gender: string;
    teamId: string;
  }[];
  scoreA: number;
  scoreB: number;
  winner?: {
    id: string;
    name: string;
    shortCode: string;
    primaryColor: string;
  } | null;
  completed: boolean;
}

export interface MatchDto {
  id: string;
  matchNumber: number;
  stageType: CompetitionStageType;
  roundNumber: number;
  legNumber: number;
  fixtureKey: string;
  seedA?: number | null;
  seedB?: number | null;
  teamA: { id: string; name: string; shortCode: string; primaryColor: string };
  teamB: { id: string; name: string; shortCode: string; primaryColor: string };
  scheduledAt: string;
  status: "SCHEDULED" | "LIVE" | "COMPLETED";
  winner?: {
    id: string;
    name: string;
    shortCode: string;
    primaryColor: string;
  } | null;
  teamAFormatWins: number;
  teamBFormatWins: number;
  teamAPoints: number;
  teamBPoints: number;
  resultRevision: number;
  venue?: string;
  formats: MatchFormatDto[];
}

export interface CompetitionConfigurationDto {
  championshipId: string;
  sportCode: string;
  configurationVersion: number;
  configurationStatus: "DRAFT" | "VALID" | "LOCKED" | "SUPERSEDED";
  configuredTeamCount: number;
  minimumTeamCount: number;
  maximumTeamCount: number;
  minimumRosterSize: number;
  maximumRosterSize: number;
  playingSideSize: number;
  maximumSubstitutes: number;
  numberOfLegs: number;
  playoffFormat:
    | "NONE"
    | "TOP_TWO_FINAL"
    | "SEMI_FINALS_AND_FINAL"
    | "SEMI_FINALS_THIRD_PLACE_AND_FINAL";
  roundRobinMode: "SINGLE" | "DOUBLE" | "CUSTOM";
  pointsPerWin: number;
  pointsPerLoss: number;
  tieBreakerPriority: string[];
  qualifyingTeamCount: number;
  venues: string[];
  leagueStartAt?: string | null;
  fixtureSpacingDays: number;
  manualDateEditing: boolean;
  resultSchemaVersion: number;
  pointsPerTie: number;
  pointsPerNoResult: number;
  timezone: string;
  presetCode?: string | null;
  allowedWeekdays: string[];
  dailyStartTime: string;
  dailyEndTime: string;
  fixtureSlotMinutes: number;
  maximumFixturesPerDay: number;
  blackoutDates: string[];
  minimumRestHours: number;
  simultaneousFixturesAllowed: boolean;
  teamMayPlayTwicePerDay: boolean;
  champion?: MatchDto["teamA"] | null;
  runnerUp?: MatchDto["teamA"] | null;
}
export interface CompetitionStageDto {
  id: string;
  type: CompetitionStageType;
  order: number;
  status: "NOT_STARTED" | "ACTIVE" | "COMPLETED" | "FROZEN";
  startedAt?: string | null;
  completedAt?: string | null;
  frozenAt?: string | null;
}
export interface CompetitionOverviewDto {
  configuration: CompetitionConfigurationDto;
  stages: CompetitionStageDto[];
  qualifications: {
    seed: number;
    team: MatchDto["teamA"];
    sourceStage: CompetitionStageType;
  }[];
  fixtures: MatchDto[];
  leagueComplete: boolean;
  playoffsGenerated: boolean;
  finalGenerated: boolean;
}
export interface CompetitionAuditDto {
  id: string;
  eventType: string;
  fixtureId?: string | null;
  actorName: string;
  revision: number;
  configurationVersion?: number | null;
  entityType: string;
  entityId: string;
  reason?: string | null;
  correlationId: string;
  payload: string;
  occurredAt: string;
}
export interface DashboardCapabilitiesDto {
  sportCode: string;
  currentPhase: ChampionshipPhase;
  permittedActions: string[];
  supportedWidgets: string[];
  terminology: Record<string, string>;
  metrics: Record<string, unknown>;
  configurationComplete: boolean;
  blockersForNextPhase: string[];
  teamCount: number;
  playerCount: number;
  lockedSquads: number;
  totalSquads: number;
  upcomingFixtures: MatchDto[];
  recentFixtures: MatchDto[];
  standings: StandingDto[];
  champion?: MatchDto["teamA"] | null;
  runnerUp?: MatchDto["teamA"] | null;
}
export interface ConfigurationWorkspaceDto {
  version: number;
  supportedPresets: string[];
  permittedActions: string[];
  blockersForNextPhase: string[];
  sections: {
    code: string;
    status: "DRAFT" | "VALID" | "LOCKED" | "SUPERSEDED";
    editable: boolean;
    version: number;
    lastChangedBy?: string | null;
    lastChangedAt?: string | null;
    lockedAt?: string | null;
    dependencies: string[];
    validationMessages: string[];
    fields: {
      name: string;
      type: string;
      minimum?: unknown;
      maximum?: unknown;
      allowedValues: string[];
      dependency?: string | null;
    }[];
  }[];
}
export interface BadmintonSportConfiguration {
  version: number;
  status: string;
  presetCode: string;
  maximumAppearancesPerPlayer: number;
  multipleRubberCategoriesAllowed: boolean;
  lineupChangesAfterStartAllowed: boolean;
  tieHandling: "LEAGUE_TIE" | "ADMINISTRATIVE_WIN";
  walkoverPolicy: string;
  retirementPolicy: string;
  lineupVisibilityPolicy: string;
  rubbers: {
    rubberCode: string;
    displayLabel: string;
    shortLabel: string;
    displayOrder: number;
    participantCountPerSide: number;
    genderRule: string;
    bestOfGames: number;
    pointsPerGame: number;
    winBy: number;
    scoreCap: number;
    enabled: boolean;
  }[];
}
export interface CricketSportConfiguration {
  version: number;
  status: string;
  presetCode: "T10" | "T20" | "ODI" | "CUSTOM";
  playersPerSide: number;
  oversPerInnings: number;
  maximumOversPerBowler: number;
  requiredWicketkeepers: number;
  tiePolicy: "SUPER_OVER" | "TIE" | "ADMINISTRATIVE";
  maximumSuperOvers: number;
  powerplayRanges: string;
  wideBallRuns: number;
  noBallRuns: number;
  freeHitEnabled: boolean;
  tossRequired: boolean;
  abandonmentPolicy: "ADMINISTRATIVE_NO_RESULT" | "ADMINISTRATIVE_RESULT";
  administrativeTargetOverrideAllowed: boolean;
  administrativeResultOverrideAllowed: boolean;
  playingXiLockPolicy: "LOCK_ON_SUBMISSION";
  netRunRatePolicy: "STANDARD";
}
export interface CricketDeliveryDto {
  id: string;
  sequence: number;
  strikerPlayerId: string;
  nonStrikerPlayerId: string;
  bowlerPlayerId: string;
  batterRuns: number;
  wides: number;
  noBalls: number;
  byes: number;
  legByes: number;
  penaltyRuns: number;
  wicket: boolean;
  dismissalType?: string | null;
  legalBall: boolean;
  totalRuns: number;
}
export interface CricketInningsDto {
  id: string;
  version: number;
  inningsNumber: number;
  battingTeamId: string;
  runs: number;
  wickets: number;
  legalBalls: number;
  overs: string;
  status: "LIVE" | "COMPLETED";
  target?: number | null;
  requiredRunRate?: number | null;
  nextStrikerPlayerId?: string | null;
  nextNonStrikerPlayerId?: string | null;
  batters: {
    playerId: string;
    runs: number;
    balls: number;
    fours: number;
    sixes: number;
    out: boolean;
  }[];
  bowlers: {
    playerId: string;
    runsConceded: number;
    wickets: number;
    legalBalls: number;
    overs: string;
  }[];
  deliveries: CricketDeliveryDto[];
}
export interface CricketMatchStateDto {
  matchId: string;
  tossWinnerTeamId?: string | null;
  tossDecision?: "BAT" | "BOWL" | null;
  resultText?: string | null;
  innings: CricketInningsDto[];
}
export interface QuotaRuleDto {
  ruleCode: string;
  categoryType: "GENDER" | "SKILL_LEVEL" | "PLAYER_CATEGORY";
  categoryValue: string;
  minimumCount?: number | null;
  maximumCount?: number | null;
  appliesTo: "SQUAD" | "PLAYING_SIDE";
  enabled: boolean;
  validationMessage?: string | null;
}
export interface BrandingConfiguration {
  version: number;
  status: string;
  displayName: string;
  shortName: string;
  logoReference?: string | null;
  primaryColor: string;
  secondaryColor: string;
  sponsorName?: string | null;
  sponsorLogoReference?: string | null;
  publicDescription?: string | null;
  contactInformation?: string | null;
  publicVisibility: boolean;
}
export interface AuctionConfiguration {
  maximumBid?:number|null;maximumPremiumPurchases?:number|null;captainPreassignmentRequired?:boolean;
  version: number;
  status: string;
  enabled: boolean;
  auctionType: "OPEN_ASCENDING";
  teamBudget: number;
  minimumBid: number;
  bidIncrement: number;
  minimumRosterSize: number;
  maximumRosterSize: number;
  retainedPlayersAllowed: boolean;
  maximumRetainedPlayers: number;
  unsoldPlayerReauctionAllowed: boolean;
  purseCarryForwardPolicy: string;
  auctionOrderPolicy: string;
  timerSeconds: number;
}

export type SquadConfirmationStatus = "DRAFT" | "CONFIRMED" | "LOCKED";

export interface SquadTeamDto {
  teamId: string;
  teamName: string;
  shortCode: string;
  primaryColor: string;
  status: SquadConfirmationStatus;
  rosterValid: boolean;
  validationIssues: string[];
  playerCount: number;
  maleCount: number;
  femaleCount: number;
  requiredPlayers: number;
  requiredMale: number;
  requiredFemale: number;
  confirmedBy?: { id: string; fullName: string } | null;
  confirmedAt?: string | null;
  lockedBy?: { id: string; fullName: string } | null;
  lockedAt?: string | null;
  reopenedBy?: { id: string; fullName: string } | null;
  reopenedAt?: string | null;
  notes?: string | null;
  updatedAt?: string | null;
}

export interface SquadSummaryDto {
  phaseStatus: "NOT_READY" | "READY" | "IN_PROGRESS" | "COMPLETED";
  auctionCompleted: boolean;
  allSquadsLocked: boolean;
  totalTeams: number;
  readyTeams: number;
  confirmedTeams: number;
  lockedTeams: number;
  teams: SquadTeamDto[];
}

export interface SquadEventDto {
  id: string;
  teamId: string;
  teamName: string;
  action: "CONFIRMED" | "LOCKED" | "REOPENED";
  actor: { id: string; fullName: string };
  occurredAt: string;
  notes?: string | null;
}

export const FORMAT_LABEL: Record<string, string> = {
  MENS_SINGLES: "Men's Singles",
  WOMENS_SINGLES: "Women's Singles",
  MENS_DOUBLES: "Men's Doubles",
  MIXED_DOUBLES: "Mixed Doubles",
  MENS_DOUBLES_TWO: "Men's Doubles II",
};

export const FORMAT_SHORT: Record<string, string> = {
  MENS_SINGLES: "MS",
  WOMENS_SINGLES: "WS",
  MENS_DOUBLES: "MD",
  MIXED_DOUBLES: "XD",
  MENS_DOUBLES_TWO: "MD2",
};
