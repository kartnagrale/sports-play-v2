"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  api,
  AuctionConfiguration,
  BadmintonSportConfiguration,
  BrandingConfiguration,
  CompetitionAuditDto,
  CompetitionOverviewDto,
  ConfigurationWorkspaceDto,
  CricketSportConfiguration,
  DashboardCapabilitiesDto,
  MatchDto,
  QuotaRuleDto,
} from "@/lib/api";
import { useChampionship } from "@/lib/championship";
import { toast } from "sonner";
import {
  CalendarDays,
  CheckCircle2,
  Clock3,
  GitBranch,
  Loader2,
  MapPin,
  Settings2,
  Trophy,
  Users,
} from "lucide-react";

type FormState = {
  expectedConfigurationVersion: number;
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
  timezone: string;
  presetCode: string;
  roundRobinMode: "SINGLE" | "DOUBLE" | "CUSTOM";
  pointsPerWin: number;
  pointsPerLoss: number;
  pointsPerTie: number;
  pointsPerNoResult: number;
  tieBreakerPriority: string;
  qualifyingTeamCount: number;
  venues: string;
  leagueStartAt: string;
  fixtureSpacingDays: number;
  manualDateEditing: boolean;
  allowedWeekdays: string;
  dailyStartTime: string;
  dailyEndTime: string;
  fixtureSlotMinutes: number;
  maximumFixturesPerDay: number;
  blackoutDates: string;
  minimumRestHours: number;
  simultaneousFixturesAllowed: boolean;
  teamMayPlayTwicePerDay: boolean;
};

export default function CompetitionPage() {
  const active = useChampionship((state) => state.active);
  const [overview, setOverview] = useState<CompetitionOverviewDto | null>(null);
  const [dashboard, setDashboard] = useState<DashboardCapabilitiesDto | null>(
    null,
  );
  const [audit, setAudit] = useState<CompetitionAuditDto[]>([]);
  const [form, setForm] = useState<FormState | null>(null);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState("");
  const [workspace, setWorkspace] = useState<ConfigurationWorkspaceDto | null>(
    null,
  );
  const [sportConfig, setSportConfig] = useState<
    BadmintonSportConfiguration | CricketSportConfiguration | null
  >(null);
  const [branding, setBranding] = useState<BrandingConfiguration | null>(null);
  const [auctionConfig, setAuctionConfig] =
    useState<AuctionConfiguration | null>(null);
  const [quotaVersion,setQuotaVersion]=useState<string>("");
  const [quotas, setQuotas] = useState<QuotaRuleDto[]>([]);

  const load = useCallback( () => {
    if (!active) return;
    const base = `/championships/${active.id}`;
    const sport=active.sportType.toLowerCase()==="cricket"?"cricket":"badminton";
    return Promise.all([
        api.get<CompetitionOverviewDto>(`${base}/competition`),
        api.get<DashboardCapabilitiesDto>(`${base}/dashboard`),
        api.get<CompetitionAuditDto[]>(`${base}/competition/audit`),
        api.get<ConfigurationWorkspaceDto>(`${base}/configuration/workspace`),
        api.get<BadmintonSportConfiguration | CricketSportConfiguration>(
          `${base}/sport-configuration/${sport}`,
        ),
        api.get<BrandingConfiguration>(
          `${base}/administrative-configuration/branding`,
        ),
        api.get<AuctionConfiguration>(
          `${base}/administrative-configuration/auction`,
        ),
        api.get<QuotaRuleDto[]>(`${base}/sport-configuration/quotas`),
      ]).then(([
        competition,
        capabilities,
        history,
        workspaceResponse,
        sportResponse,
        brandingResponse,
        auctionResponse,
        quotaResponse,
      ]) => {
setOverview(competition.data);
      setDashboard(capabilities.data);
      setAudit(history.data);
      setWorkspace(workspaceResponse.data);
      setSportConfig(sportResponse.data);
      setBranding(brandingResponse.data);
      setAuctionConfig(auctionResponse.data);
      setQuotas(quotaResponse.data);
      setQuotaVersion(String(quotaResponse.headers.etag||"").replaceAll('"',""));
      const c = competition.data.configuration;
      setForm({
        expectedConfigurationVersion: c.configurationVersion,
        configuredTeamCount: c.configuredTeamCount,
        minimumTeamCount: c.minimumTeamCount,
        maximumTeamCount: c.maximumTeamCount,
        minimumRosterSize: c.minimumRosterSize,
        maximumRosterSize: c.maximumRosterSize,
        playingSideSize: c.playingSideSize,
        maximumSubstitutes: c.maximumSubstitutes,
        numberOfLegs: c.numberOfLegs,
        playoffFormat: c.playoffFormat,
        timezone: c.timezone,
        presetCode: c.presetCode || "CUSTOM",
        roundRobinMode: c.roundRobinMode,
        pointsPerWin: c.pointsPerWin,
        pointsPerLoss: c.pointsPerLoss,
        pointsPerTie: c.pointsPerTie,
        pointsPerNoResult: c.pointsPerNoResult,
        tieBreakerPriority: c.tieBreakerPriority.join(", "),
        qualifyingTeamCount: c.qualifyingTeamCount,
        venues: c.venues.join("\n"),
        leagueStartAt: toLocal(c.leagueStartAt),
        fixtureSpacingDays: c.fixtureSpacingDays,
        manualDateEditing: c.manualDateEditing,
        allowedWeekdays: c.allowedWeekdays.join(","),
        dailyStartTime: c.dailyStartTime,
        dailyEndTime: c.dailyEndTime,
        fixtureSlotMinutes: c.fixtureSlotMinutes,
        maximumFixturesPerDay: c.maximumFixturesPerDay,
        blackoutDates: c.blackoutDates.join("\n"),
        minimumRestHours: c.minimumRestHours,
        simultaneousFixturesAllowed: c.simultaneousFixturesAllowed,
        teamMayPlayTwicePerDay: c.teamMayPlayTwicePerDay,
      });

}).catch((error: any) => {

      toast.error(
        error?.response?.data?.message ||
          "Unable to load competition management",
      );

}).finally(() => {
      setLoading(false);
    });
  }, [active]);
  useEffect(() => {
    load();
  }, [load]);

  const mutate = async (
    kind: string,
    path: string,
    body?: unknown,
    method: "post" | "put" = "post",
  ) => {
    if (!active) return;
    setWorking(kind);
    try {
      await api[method](`/championships/${active.id}${path}`, body);
      toast.success(
        kind === "config"
          ? "League configuration saved"
          : kind === "league"
            ? "League fixtures generated"
            : "Playoff bracket generated",
      );
      await load();
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message || "Competition action failed",
      );
    } finally {
      setWorking("");
    }
  };
  const save = () =>
    form &&
    mutate(
      "config",
      "/admin/competition/configuration",
      {
        ...form,
        leagueStartAt: new Date(form.leagueStartAt).toISOString(),
        tieBreakerPriority: form.tieBreakerPriority
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        venues: form.venues
          .split("\n")
          .map((item) => item.trim())
          .filter(Boolean),
        allowedWeekdays: form.allowedWeekdays
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        blackoutDates: form.blackoutDates
          .split("\n")
          .map((item) => item.trim())
          .filter(Boolean),
      },
      "put",
    );
  const saveSport = async () => {
    if (!active || !sportConfig) return;
    setWorking("sport");
    try {
      const sport =
        active.sportType.toLowerCase() === "cricket" ? "cricket" : "badminton";
      const payload = { ...sportConfig, expectedVersion: sportConfig.version };
      delete (payload as any).version;
      delete (payload as any).status;
      await api.put(
        `/championships/${active.id}/sport-configuration/${sport}`,
        payload,
      );
      toast.success("Sport rules saved as a new version");
      await load();
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message || "Unable to save sport rules",
      );
    } finally {
      setWorking("");
    }
  };
  const saveAdministrative = async (kind: "branding" | "auction") => {
    if (!active) return;
    const value = kind === "branding" ? branding : auctionConfig;
    if (!value) return;
    setWorking(kind);
    try {
      const payload = { ...value, expectedVersion: value.version };
      delete (payload as any).version;
      delete (payload as any).status;
      await api.put(
        `/championships/${active.id}/administrative-configuration/${kind}`,
        payload,
      );
      toast.success(
        `${kind === "branding" ? "Branding" : "Auction policy"} saved as a new version`,
      );
      await load();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || `Unable to save ${kind}`);
    } finally {
      setWorking("");
    }
  };
  const saveQuotas = async () => {
    if (!active) return;
    setWorking("quotas");
    try {
      await api.put(
        `/championships/${active.id}/sport-configuration/quotas`,
        quotas,
        {headers:{"If-Match":quotaVersion}},
      );
      toast.success("Roster quota rules saved");
      await load();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Unable to save quotas");
    } finally {
      setWorking("");
    }
  };

  const actions = dashboard?.permittedActions || [];
  const semis =
    overview?.fixtures.filter((f) => f.stageType === "SEMI_FINAL") || [];
  const final = overview?.fixtures.find((f) => f.stageType === "FINAL");
  const league =
    overview?.fixtures.filter((f) => f.stageType === "LEAGUE") || [];

  if (loading)
    return (
      <div className="grid min-h-[60vh] place-items-center">
        <Loader2 className="animate-spin text-primary" size={30} />
      </div>
    );
  if (!overview || !form || !dashboard)
    return (
      <div className="portal-page">
        <div className="card-elev rounded-2xl p-10 text-center text-white/50">
          Competition data is unavailable.
        </div>
      </div>
    );

  return (
    <div className="p-5 sm:p-10">
      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <div className="label-cap">
            {dashboard.sportCode} · Competition control
          </div>
          <h1 className="h-heading mt-1 text-4xl font-bold sm:text-5xl">
            League & Playoffs
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-white/50">
            Configure the league, generate isolated fixtures, freeze qualifiers
            and follow the championship bracket.
          </p>
        </div>
        <div className="flex gap-2">
          <span className="chip">
            v{overview.configuration.configurationVersion} ·{" "}
            {overview.configuration.configurationStatus}
          </span>
          <span className="chip chip-primary">
            {phaseLabel(dashboard.currentPhase)}
          </span>
        </div>
      </div>

      {overview.configuration.champion && (
        <section className="mb-8 overflow-hidden rounded-3xl border border-primary/30 bg-primary/[.08] p-8 text-center">
          <Trophy className="mx-auto text-primary" size={38} />
          <div className="label-cap mt-3">Championship winner</div>
          <h2 className="h-heading mt-2 text-4xl text-primary">
            {overview.configuration.champion.name}
          </h2>
          <p className="mt-2 text-sm text-white/50">
            Runner-up · {overview.configuration.runnerUp?.name}
          </p>
        </section>
      )}

      <div className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Kpi
          icon={<GitBranch />}
          label="Lifecycle"
          value={phaseLabel(dashboard.currentPhase)}
        />
        <Kpi
          icon={<Users />}
          label="Locked squads"
          value={`${dashboard.lockedSquads} / ${dashboard.totalSquads}`}
        />
        <Kpi
          icon={<CalendarDays />}
          label="League fixtures"
          value={`${league.filter((f) => f.status === "COMPLETED").length} / ${league.length}`}
        />
        <Kpi
          icon={<Trophy />}
          label="Qualifiers"
          value={String(overview.configuration.qualifyingTeamCount)}
        />
      </div>

      {dashboard.blockersForNextPhase.length > 0 && (
        <div className="mb-8 rounded-2xl border border-warning/25 bg-warning/[.07] p-5">
          <div className="label-cap text-warning">Next phase blockers</div>
          <div className="mt-2 flex flex-wrap gap-2">
            {dashboard.blockersForNextPhase.map((item) => (
              <span key={item} className="chip">
                {item}
              </span>
            ))}
          </div>
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(320px,.8fr)]">
        <section className="card-elev rounded-2xl p-6">
          <div className="mb-5 flex items-center gap-3">
            <Settings2 className="text-primary" />
            <div>
              <div className="label-cap">Championship configuration</div>
              <h2 className="h-heading text-2xl">
                Teams, rosters & competition
              </h2>
            </div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Championship preset">
              <select
                className="input"
                value={form.presetCode}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, presetCode: e.target.value })
                }
              >
                <option value="SMALL_CORPORATE_LEAGUE">
                  Small Corporate League
                </option>
                <option value="STANDARD_CORPORATE_LEAGUE">
                  Standard Corporate League
                </option>
                <option value="CUSTOM_CHAMPIONSHIP">Custom Championship</option>
              </select>
            </Field>
            <Field label="Target teams">
              <input
                type="number"
                min={2}
                className="input"
                value={form.configuredTeamCount}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    configuredTeamCount: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Maximum teams">
              <input
                type="number"
                min={2}
                className="input"
                value={form.maximumTeamCount}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, maximumTeamCount: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Roster size (minimum)">
              <input
                type="number"
                min={1}
                className="input"
                value={form.minimumRosterSize}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    minimumRosterSize: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Roster size (maximum)">
              <input
                type="number"
                min={1}
                className="input"
                value={form.maximumRosterSize}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    maximumRosterSize: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Playing side">
              <input
                type="number"
                min={1}
                className="input"
                value={form.playingSideSize}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, playingSideSize: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Timezone">
              <input
                className="input"
                value={form.timezone}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) => setForm({ ...form, timezone: e.target.value })}
              />
            </Field>
            <Field label="Round robin">
              <select
                className="input"
                value={form.roundRobinMode}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, roundRobinMode: e.target.value as any })
                }
              >
                <option value="SINGLE">Single round robin</option>
                <option value="DOUBLE">Double round robin</option>
                <option value="CUSTOM">Custom legs</option>
              </select>
            </Field>
            <Field label="Playoff format">
              <select
                className="input"
                value={form.playoffFormat}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, playoffFormat: e.target.value as any })
                }
              >
                <option value="NONE">No playoffs</option>
                <option value="TOP_TWO_FINAL">Top two final</option>
                <option value="SEMI_FINALS_AND_FINAL">
                  Semifinals & final
                </option>
                <option value="SEMI_FINALS_THIRD_PLACE_AND_FINAL">
                  Semifinals, third place & final
                </option>
              </select>
            </Field>
            <Field label="League start">
              <input
                type="datetime-local"
                className="input"
                value={form.leagueStartAt}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, leagueStartAt: e.target.value })
                }
              />
            </Field>
            <Field label="Points for win">
              <input
                type="number"
                min={0}
                className="input"
                value={form.pointsPerWin}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, pointsPerWin: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Points for loss">
              <input
                type="number"
                min={0}
                className="input"
                value={form.pointsPerLoss}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, pointsPerLoss: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Points for tie">
              <input
                type="number"
                min={0}
                className="input"
                value={form.pointsPerTie}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, pointsPerTie: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Points for no result">
              <input
                type="number"
                min={0}
                className="input"
                value={form.pointsPerNoResult}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    pointsPerNoResult: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Qualifying teams">
              <input
                type="number"
                min={2}
                step={2}
                className="input"
                value={form.qualifyingTeamCount}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    qualifyingTeamCount: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Fixture spacing (days)">
              <input
                type="number"
                min={0}
                max={30}
                className="input"
                value={form.fixtureSpacingDays}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    fixtureSpacingDays: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Allowed weekdays">
              <input
                className="input"
                value={form.allowedWeekdays}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    allowedWeekdays: e.target.value.toUpperCase(),
                  })
                }
              />
            </Field>
            <Field label="Daily window start">
              <input
                type="time"
                className="input"
                value={form.dailyStartTime}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, dailyStartTime: e.target.value })
                }
              />
            </Field>
            <Field label="Daily window end">
              <input
                type="time"
                className="input"
                value={form.dailyEndTime}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, dailyEndTime: e.target.value })
                }
              />
            </Field>
            <Field label="Fixture slot (minutes)">
              <input
                type="number"
                min={15}
                className="input"
                value={form.fixtureSlotMinutes}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    fixtureSlotMinutes: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Maximum fixtures per day">
              <input
                type="number"
                min={1}
                className="input"
                value={form.maximumFixturesPerDay}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({
                    ...form,
                    maximumFixturesPerDay: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Minimum rest (hours)">
              <input
                type="number"
                min={0}
                className="input"
                value={form.minimumRestHours}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, minimumRestHours: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Blackout dates (yyyy-mm-dd)">
              <textarea
                className="input min-h-24"
                value={form.blackoutDates}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, blackoutDates: e.target.value })
                }
              />
            </Field>
            <Field label="Tie-breaker priority">
              <input
                className="input"
                value={form.tieBreakerPriority}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) =>
                  setForm({ ...form, tieBreakerPriority: e.target.value })
                }
              />
            </Field>
            <Field label="Venues (one per line)">
              <textarea
                className="input min-h-24"
                value={form.venues}
                disabled={!actions.includes("CONFIGURE_COMPETITION")}
                onChange={(e) => setForm({ ...form, venues: e.target.value })}
              />
            </Field>
          </div>
          <label className="mt-4 flex items-center gap-2 text-sm text-white/60">
            <input
              type="checkbox"
              className="accent-primary"
              checked={form.manualDateEditing}
              disabled={!actions.includes("CONFIGURE_COMPETITION")}
              onChange={(e) =>
                setForm({ ...form, manualDateEditing: e.target.checked })
              }
            />
            Allow manual fixture date edits
          </label>
          <label className="mt-3 flex items-center gap-2 text-sm text-white/60">
            <input
              type="checkbox"
              className="accent-primary"
              checked={form.simultaneousFixturesAllowed}
              disabled={!actions.includes("CONFIGURE_COMPETITION")}
              onChange={(e) =>
                setForm({
                  ...form,
                  simultaneousFixturesAllowed: e.target.checked,
                })
              }
            />
            Allow simultaneous fixtures across venues
          </label>
          <label className="mt-3 flex items-center gap-2 text-sm text-white/60">
            <input
              type="checkbox"
              className="accent-primary"
              checked={form.teamMayPlayTwicePerDay}
              disabled={!actions.includes("CONFIGURE_COMPETITION")}
              onChange={(e) =>
                setForm({ ...form, teamMayPlayTwicePerDay: e.target.checked })
              }
            />
            Allow one team to play twice in a day
          </label>
          <div className="mt-5 flex flex-wrap gap-3">
            {actions.includes("CONFIGURE_COMPETITION") && (
              <button
                className="btn btn-ghost"
                disabled={Boolean(working) || !form.leagueStartAt}
                onClick={save}
              >
                {working === "config" ? (
                  <Loader2 className="animate-spin" size={14} />
                ) : (
                  <CheckCircle2 size={14} />
                )}
                Save configuration
              </button>
            )}
            {actions.includes("GENERATE_LEAGUE") && (
              <button
                className="btn btn-primary"
                disabled={Boolean(working)}
                onClick={() =>
                  mutate("league", "/admin/competition/league/generate")
                }
              >
                {working === "league" ? (
                  <Loader2 className="animate-spin" size={14} />
                ) : (
                  <CalendarDays size={14} />
                )}
                Generate league fixtures
              </button>
            )}
            {actions.includes("GENERATE_PLAYOFFS") && (
              <button
                className="btn btn-primary"
                disabled={Boolean(working)}
                onClick={() =>
                  mutate("playoffs", "/admin/competition/playoffs/generate")
                }
              >
                {working === "playoffs" ? (
                  <Loader2 className="animate-spin" size={14} />
                ) : (
                  <GitBranch size={14} />
                )}
                Freeze table & generate playoffs
              </button>
            )}
          </div>
        </section>

        <section className="card-elev rounded-2xl p-6">
          <div className="label-cap">Competition stages</div>
          <div className="mt-4 space-y-3">
            {overview.stages.length ? (
              overview.stages.map((stage) => (
                <div
                  key={stage.id}
                  className="flex items-center justify-between rounded-xl border border-white/10 p-4"
                >
                  <div>
                    <div className="h-heading">{stageLabel(stage.type)}</div>
                    <div className="mt-1 text-xs text-white/40">
                      {stage.startedAt
                        ? new Date(stage.startedAt).toLocaleString()
                        : "Not started"}
                    </div>
                  </div>
                  <span
                    className={`chip ${stage.status === "FROZEN" || stage.status === "COMPLETED" ? "chip-primary" : ""}`}
                  >
                    {stage.status}
                  </span>
                </div>
              ))
            ) : (
              <p className="text-sm text-white/40">
                Stages appear after league generation.
              </p>
            )}
          </div>
        </section>
      </div>

      {workspace && (
        <section className="mt-8 card-elev rounded-2xl p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="label-cap">Configuration workspace</div>
              <h2 className="h-heading mt-1 text-2xl">Validation & lock map</h2>
            </div>
            <span className="chip">Version {workspace.version}</span>
          </div>
          <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-5">
            {workspace.sections.map((section) => (
              <div
                key={section.code}
                className={`rounded-xl border p-4 ${section.editable ? "border-primary/20 bg-primary/[.04]" : "border-white/10 bg-black/20"}`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="label-cap text-[9px]">
                    {section.code.replaceAll("_", " ")}
                  </span>
                  <span className="chip">
                    v{section.version} · {section.status}
                  </span>
                </div>
                <div className="mt-3 text-xs text-white/40">
                  {section.dependencies.length
                    ? `Depends on ${section.dependencies.join(", ")}`
                    : "No dependencies"}
                </div>
                <div className="mt-2 text-[11px] text-white/30">
                  {section.lastChangedBy
                    ? `Changed by ${section.lastChangedBy}`
                    : "No changes recorded"}
                  {section.lastChangedAt
                    ? ` · ${new Date(section.lastChangedAt).toLocaleString()}`
                    : ""}
                </div>
                {section.validationMessages.map((message) => (
                  <div key={message} className="mt-2 text-xs text-warning">
                    {message}
                  </div>
                ))}
              </div>
            ))}
          </div>
        </section>
      )}

      {branding && (
        <section className="mt-8 card-elev rounded-2xl p-6">
          <div className="flex items-center justify-between">
            <div>
              <div className="label-cap">Branding</div>
              <h2 className="h-heading mt-1 text-2xl">
                Corporate identity & visibility
              </h2>
            </div>
            <span className="chip">
              v{branding.version} · {branding.status}
            </span>
          </div>
          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Field label="Display name">
              <input
                className="input"
                value={branding.displayName}
                onChange={(e) =>
                  setBranding({ ...branding, displayName: e.target.value })
                }
              />
            </Field>
            <Field label="Short name">
              <input
                className="input"
                value={branding.shortName}
                onChange={(e) =>
                  setBranding({ ...branding, shortName: e.target.value })
                }
              />
            </Field>
            <Field label="Logo reference">
              <input
                className="input"
                value={branding.logoReference || ""}
                onChange={(e) =>
                  setBranding({ ...branding, logoReference: e.target.value })
                }
              />
            </Field>
            <Field label="Primary color">
              <input
                className="input"
                type="color"
                value={branding.primaryColor}
                onChange={(e) =>
                  setBranding({ ...branding, primaryColor: e.target.value })
                }
              />
            </Field>
            <Field label="Secondary color">
              <input
                className="input"
                type="color"
                value={branding.secondaryColor}
                onChange={(e) =>
                  setBranding({ ...branding, secondaryColor: e.target.value })
                }
              />
            </Field>
            <Field label="Sponsor">
              <input
                className="input"
                value={branding.sponsorName || ""}
                onChange={(e) =>
                  setBranding({ ...branding, sponsorName: e.target.value })
                }
              />
            </Field>
            <Field label="Contact information">
              <input
                className="input"
                value={branding.contactInformation || ""}
                onChange={(e) =>
                  setBranding({
                    ...branding,
                    contactInformation: e.target.value,
                  })
                }
              />
            </Field>
            <Field label="Public description">
              <textarea
                className="input min-h-24"
                value={branding.publicDescription || ""}
                onChange={(e) =>
                  setBranding({
                    ...branding,
                    publicDescription: e.target.value,
                  })
                }
              />
            </Field>
          </div>
          <label className="mt-4 flex gap-2 text-sm text-white/60">
            <input
              type="checkbox"
              checked={branding.publicVisibility}
              onChange={(e) =>
                setBranding({ ...branding, publicVisibility: e.target.checked })
              }
            />
            Public championship visibility
          </label>
          {actions.includes("CONFIGURE_COMPETITION") && (
            <button
              className="btn btn-primary mt-5"
              disabled={Boolean(working)}
              onClick={() => saveAdministrative("branding")}
            >
              Save branding version
            </button>
          )}
        </section>
      )}

      {active?.sportType.toUpperCase()==="CRICKET"&&form&&<div className="card-elev mb-5 rounded-xl p-4"><button className="btn btn-secondary" type="button" onClick={()=>setForm({...form,presetCode:"CUSTOM_CHAMPIONSHIP",configuredTeamCount:4,minimumTeamCount:4,maximumTeamCount:4,minimumRosterSize:11,maximumRosterSize:11,playingSideSize:11,maximumSubstitutes:0,roundRobinMode:"SINGLE",numberOfLegs:1,playoffFormat:"TOP_TWO_FINAL",qualifyingTeamCount:2,pointsPerWin:2,pointsPerLoss:0})}>Fill proposed four-team NPL cricket format</button><p className="mt-2 text-sm text-muted">Save the competition form above: six league matches, then one top-two final. Win 2 / loss 0 are examples; tie and no-result points remain configurable. Configure Cricket with eleven players per side.</p></div>}
      {auctionConfig && (
        <section className="mt-8 card-elev rounded-2xl p-6">
          <div className="flex items-center justify-between">
            <div>
              <div className="label-cap">Auction policy</div>
              <h2 className="h-heading mt-1 text-2xl">
                Budget, bidding & retention
              </h2>
            </div>
            <span className="chip">
              v{auctionConfig.version} · {auctionConfig.status}
            </span>
          </div>
          <div className="my-4 grid gap-4 sm:grid-cols-2"><label className="block text-sm">Maximum bid (₹, optional)<input className="input mt-2" type="number" min="0.01" step="0.01" value={auctionConfig.maximumBid??""} onChange={e=>setAuctionConfig({...auctionConfig,maximumBid:e.target.value===""?null:Number(e.target.value)})}/></label><label className="block text-sm">Purchases allowed at maximum bid<input className="input mt-2" type="number" min="0" step="1" value={auctionConfig.maximumPremiumPurchases??""} onChange={e=>setAuctionConfig({...auctionConfig,maximumPremiumPurchases:e.target.value===""?null:Number(e.target.value)})}/></label><label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={!!auctionConfig.captainPreassignmentRequired} onChange={e=>setAuctionConfig({...auctionConfig,captainPreassignmentRequired:e.target.checked})}/>Require preassigned captains</label></div>
          <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Field label="Auction type">
              <select
                className="input"
                value={auctionConfig.auctionType}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    auctionType: e.target
                      .value as AuctionConfiguration["auctionType"],
                  })
                }
              >
                <option value="OPEN_ASCENDING">Open ascending</option>
              </select>
            </Field>
            <Field label="Team budget">
              <input
                className="input"
                type="number"
                min={1}
                value={auctionConfig.teamBudget}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    teamBudget: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Minimum bid">
              <input
                className="input"
                type="number"
                min={1}
                value={auctionConfig.minimumBid}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    minimumBid: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Bid increment">
              <input
                className="input"
                type="number"
                min={1}
                value={auctionConfig.bidIncrement}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    bidIncrement: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Timer seconds">
              <input
                className="input"
                type="number"
                min={5}
                max={600}
                value={auctionConfig.timerSeconds}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    timerSeconds: Number(e.target.value),
                  })
                }
              />
            </Field>
            <Field label="Maximum retained">
              <input
                className="input"
                type="number"
                min={0}
                value={auctionConfig.maximumRetainedPlayers}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    maximumRetainedPlayers: Number(e.target.value),
                  })
                }
              />
            </Field>
          </div>
          <div className="mt-4 flex flex-wrap gap-5 text-sm text-white/60">
            <label className="flex gap-2">
              <input
                type="checkbox"
                checked={auctionConfig.enabled}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    enabled: e.target.checked,
                  })
                }
              />
              Auction enabled
            </label>
            <label className="flex gap-2">
              <input
                type="checkbox"
                checked={auctionConfig.retainedPlayersAllowed}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    retainedPlayersAllowed: e.target.checked,
                  })
                }
              />
              Retained players allowed
            </label>
            <label className="flex gap-2">
              <input
                type="checkbox"
                checked={auctionConfig.unsoldPlayerReauctionAllowed}
                onChange={(e) =>
                  setAuctionConfig({
                    ...auctionConfig,
                    unsoldPlayerReauctionAllowed: e.target.checked,
                  })
                }
              />
              Unsold reauction allowed
            </label>
          </div>
          {actions.includes("CONFIGURE_COMPETITION") && (
            <button
              className="btn btn-primary mt-5"
              disabled={Boolean(working)}
              onClick={() => saveAdministrative("auction")}
            >
              Save auction-policy version
            </button>
          )}
        </section>
      )}

      <section className="mt-8 card-elev rounded-2xl p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="label-cap">Roster policy</div>
            <h2 className="h-heading mt-1 text-2xl">
              Squad & playing-side quotas
            </h2>
          </div>
          {actions.includes("CONFIGURE_COMPETITION") && (
            <button
              className="btn btn-ghost"
              onClick={() =>
                setQuotas([
                  ...quotas,
                  {
                    ruleCode: `RULE_${quotas.length + 1}`,
                    categoryType: "GENDER",
                    categoryValue: "FEMALE",
                    minimumCount: 1,
                    maximumCount: null,
                    appliesTo: "SQUAD",
                    enabled: true,
                    validationMessage: "",
                  },
                ])
              }
            >
              Add quota
            </button>
          )}
        </div>
        <div className="mt-5 space-y-3">
          {quotas.length === 0 && (
            <p className="text-sm text-white/40">
              No additional gender or player-category quotas configured.
            </p>
          )}
          {quotas.map((quota, index) => (
            <div
              key={`${quota.ruleCode}-${index}`}
              className="grid gap-3 rounded-xl border border-white/10 p-4 md:grid-cols-3 xl:grid-cols-7"
            >
              <Field label="Rule code">
                <input
                  className="input"
                  value={quota.ruleCode}
                  onChange={(e) =>
                    setQuotas(
                      quotas.map((item, i) =>
                        i === index
                          ? { ...item, ruleCode: e.target.value.toUpperCase() }
                          : item,
                      ),
                    )
                  }
                />
              </Field>
              <Field label="Category">
                <select
                  className="input"
                  value={quota.categoryType}
                  onChange={(e) =>
                    setQuotas(
                      quotas.map((item, i) =>
                        i === index
                          ? {
                              ...item,
                              categoryType: e.target
                                .value as QuotaRuleDto["categoryType"],
                            }
                          : item,
                      ),
                    )
                  }
                >
                  <option value="GENDER">Gender</option>
                  <option value="SKILL_LEVEL">Skill level</option>
                  <option value="PLAYER_CATEGORY">Player category</option>
                </select>
              </Field>
              <Field label="Value">
                <input
                  className="input"
                  value={quota.categoryValue}
                  onChange={(e) =>
                    setQuotas(
                      quotas.map((item, i) =>
                        i === index
                          ? { ...item, categoryValue: e.target.value }
                          : item,
                      ),
                    )
                  }
                />
              </Field>
              <Field label="Minimum">
                <input
                  className="input"
                  type="number"
                  min={0}
                  value={quota.minimumCount ?? ""}
                  onChange={(e) =>
                    setQuotas(
                      quotas.map((item, i) =>
                        i === index
                          ? {
                              ...item,
                              minimumCount: e.target.value
                                ? Number(e.target.value)
                                : null,
                            }
                          : item,
                      ),
                    )
                  }
                />
              </Field>
              <Field label="Maximum">
                <input
                  className="input"
                  type="number"
                  min={0}
                  value={quota.maximumCount ?? ""}
                  onChange={(e) =>
                    setQuotas(
                      quotas.map((item, i) =>
                        i === index
                          ? {
                              ...item,
                              maximumCount: e.target.value
                                ? Number(e.target.value)
                                : null,
                            }
                          : item,
                      ),
                    )
                  }
                />
              </Field>
              <Field label="Applies to">
                <select
                  className="input"
                  value={quota.appliesTo}
                  onChange={(e) =>
                    setQuotas(
                      quotas.map((item, i) =>
                        i === index
                          ? {
                              ...item,
                              appliesTo: e.target
                                .value as QuotaRuleDto["appliesTo"],
                            }
                          : item,
                      ),
                    )
                  }
                >
                  <option value="SQUAD">Squad</option>
                  <option value="PLAYING_SIDE">Playing side</option>
                </select>
              </Field>
              <button
                className="btn btn-ghost self-end"
                onClick={() => setQuotas(quotas.filter((_, i) => i !== index))}
              >
                Remove
              </button>
            </div>
          ))}
        </div>
        {actions.includes("CONFIGURE_COMPETITION") && (
          <button
            className="btn btn-primary mt-5"
            disabled={Boolean(working)}
            onClick={saveQuotas}
          >
            {working === "quotas" ? "Saving…" : "Save quota rules"}
          </button>
        )}
      </section>

      {sportConfig && (
        <section className="mt-8 card-elev rounded-2xl p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="label-cap">{dashboard.sportCode} engine</div>
              <h2 className="h-heading mt-1 text-2xl">Sport-specific rules</h2>
            </div>
            <span className="chip">
              {sportConfig.presetCode} · v{sportConfig.version}
            </span>
          </div>
          {dashboard.sportCode === "CRICKET" &&
          "oversPerInnings" in sportConfig ? (
            <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Field label="Preset">
                <select
                  className="input"
                  value={sportConfig.presetCode}
                  onChange={(e) => {
                    const preset = e.target
                      .value as CricketSportConfiguration["presetCode"];
                    const values =
                      preset === "T10"
                        ? [10, 2]
                        : preset === "T20"
                          ? [20, 4]
                          : preset === "ODI"
                            ? [50, 10]
                            : [
                                sportConfig.oversPerInnings,
                                sportConfig.maximumOversPerBowler,
                              ];
                    setSportConfig({
                      ...sportConfig,
                      presetCode: preset,
                      playersPerSide:
                        preset === "CUSTOM" ? sportConfig.playersPerSide : 11,
                      oversPerInnings: values[0],
                      maximumOversPerBowler: values[1],
                    });
                  }}
                >
                  <option>T10</option>
                  <option>T20</option>
                  <option>ODI</option>
                  <option>CUSTOM</option>
                </select>
              </Field>
              <Field label="Players per side">
                <input
                  className="input"
                  type="number"
                  min={2}
                  max={11}
                  value={sportConfig.playersPerSide}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      playersPerSide: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Field label="Overs per innings">
                <input
                  className="input"
                  type="number"
                  min={1}
                  max={50}
                  value={sportConfig.oversPerInnings}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      oversPerInnings: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Field label="Overs per bowler">
                <input
                  className="input"
                  type="number"
                  min={1}
                  value={sportConfig.maximumOversPerBowler}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      maximumOversPerBowler: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Field label="Required wicketkeepers">
                <input
                  className="input"
                  type="number"
                  min={1}
                  max={sportConfig.playersPerSide}
                  value={sportConfig.requiredWicketkeepers}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      requiredWicketkeepers: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Field label="Tie policy">
                <select
                  className="input"
                  value={sportConfig.tiePolicy}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      tiePolicy: e.target
                        .value as CricketSportConfiguration["tiePolicy"],
                    })
                  }
                >
                  <option value="SUPER_OVER">Super over</option>
                  <option value="TIE">Declare tie</option>
                  <option value="ADMINISTRATIVE">
                    Administrative decision
                  </option>
                </select>
              </Field>
              <Field label="Maximum super overs">
                <input
                  className="input"
                  type="number"
                  min={0}
                  value={sportConfig.maximumSuperOvers}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      maximumSuperOvers: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Field label="Powerplay ranges">
                <input
                  className="input"
                  value={sportConfig.powerplayRanges}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      presetCode: "CUSTOM",
                      powerplayRanges: e.target.value,
                    })
                  }
                />
              </Field>
              <Field label="Wide penalty runs">
                <input
                  className="input"
                  type="number"
                  min={1}
                  value={sportConfig.wideBallRuns}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      wideBallRuns: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Field label="No-ball penalty runs">
                <input
                  className="input"
                  type="number"
                  min={1}
                  value={sportConfig.noBallRuns}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      noBallRuns: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <label className="flex items-center gap-2 text-sm text-white/60">
                <input
                  type="checkbox"
                  checked={sportConfig.freeHitEnabled}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      freeHitEnabled: e.target.checked,
                    })
                  }
                />
                Free hit after a no-ball
              </label>
              <label className="flex items-center gap-2 text-sm text-white/60">
                <input
                  type="checkbox"
                  checked={sportConfig.tossRequired}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      tossRequired: e.target.checked,
                    })
                  }
                />
                Toss required
              </label>
              <Field label="Abandonment policy">
                <select
                  className="input"
                  value={sportConfig.abandonmentPolicy}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      abandonmentPolicy: e.target
                        .value as CricketSportConfiguration["abandonmentPolicy"],
                    })
                  }
                >
                  <option value="ADMINISTRATIVE_NO_RESULT">
                    Administrative no result
                  </option>
                  <option value="ADMINISTRATIVE_RESULT">
                    Administrative result
                  </option>
                </select>
              </Field>
              <label className="flex items-center gap-2 text-sm text-white/60">
                <input
                  type="checkbox"
                  checked={sportConfig.administrativeTargetOverrideAllowed}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      administrativeTargetOverrideAllowed: e.target.checked,
                    })
                  }
                />
                Allow audited target override
              </label>
              <label className="flex items-center gap-2 text-sm text-white/60">
                <input
                  type="checkbox"
                  checked={sportConfig.administrativeResultOverrideAllowed}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      administrativeResultOverrideAllowed: e.target.checked,
                    })
                  }
                />
                Allow audited result override
              </label>
            </div>
          ) : "rubbers" in sportConfig ? (
            <>
              <Field label="Rubber preset">
                <select
                  className="input mt-4 max-w-sm"
                  value={sportConfig.presetCode}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      presetCode: e.target.value,
                    })
                  }
                >
                  <option value="BADMINTON_STANDARD_FIVE">
                    Standard five rubbers
                  </option>
                  <option value="BADMINTON_COMPACT_THREE">
                    Compact three rubbers
                  </option>
                  <option value="CUSTOM">Custom program</option>
                </select>
              </Field>
              <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                {sportConfig.rubbers.map((rubber, index) => (
                  <div
                    key={rubber.rubberCode}
                    className="rounded-xl border border-white/10 p-4"
                  >
                    <div className="flex items-center justify-between">
                      <span className="h-heading">{rubber.displayLabel}</span>
                      <input
                        type="checkbox"
                        checked={rubber.enabled}
                        onChange={(e) =>
                          setSportConfig({
                            ...sportConfig,
                            presetCode: "CUSTOM",
                            rubbers: sportConfig.rubbers.map((r, i) =>
                              i === index
                                ? { ...r, enabled: e.target.checked }
                                : r,
                            ),
                          })
                        }
                      />
                    </div>
                    <div className="mt-2 text-xs text-white/40">
                      {rubber.participantCountPerSide} per side ·{" "}
                      {rubber.genderRule} · {rubber.pointsPerGame} points, win
                      by {rubber.winBy}, cap {rubber.scoreCap}
                    </div>
                    <div className="mt-3 grid grid-cols-2 gap-2">
                      <input
                        className="input"
                        aria-label={`${rubber.displayLabel} label`}
                        value={rubber.displayLabel}
                        onChange={(e) =>
                          setSportConfig({
                            ...sportConfig,
                            presetCode: "CUSTOM",
                            rubbers: sportConfig.rubbers.map((r, i) =>
                              i === index
                                ? { ...r, displayLabel: e.target.value }
                                : r,
                            ),
                          })
                        }
                      />
                      <input
                        className="input"
                        aria-label={`${rubber.displayLabel} order`}
                        type="number"
                        min={1}
                        value={rubber.displayOrder}
                        onChange={(e) =>
                          setSportConfig({
                            ...sportConfig,
                            presetCode: "CUSTOM",
                            rubbers: sportConfig.rubbers.map((r, i) =>
                              i === index
                                ? { ...r, displayOrder: Number(e.target.value) }
                                : r,
                            ),
                          })
                        }
                      />
                      <select
                        className="input"
                        aria-label={`${rubber.displayLabel} gender rule`}
                        value={rubber.genderRule}
                        onChange={(e) =>
                          setSportConfig({
                            ...sportConfig,
                            presetCode: "CUSTOM",
                            rubbers: sportConfig.rubbers.map((r, i) =>
                              i === index
                                ? { ...r, genderRule: e.target.value }
                                : r,
                            ),
                          })
                        }
                      >
                        <option value="MALE">Male</option>
                        <option value="FEMALE">Female</option>
                        <option value="MIXED">Mixed</option>
                        <option value="OPEN">Open</option>
                      </select>
                      {(
                        [
                          "participantCountPerSide",
                          "bestOfGames",
                          "pointsPerGame",
                          "winBy",
                          "scoreCap",
                        ] as const
                      ).map((field) => (
                        <input
                          key={field}
                          className="input"
                          aria-label={`${rubber.displayLabel} ${field}`}
                          type="number"
                          min={1}
                          max={
                            field === "participantCountPerSide" ? 2 : undefined
                          }
                          value={rubber[field]}
                          onChange={(e) =>
                            setSportConfig({
                              ...sportConfig,
                              presetCode: "CUSTOM",
                              rubbers: sportConfig.rubbers.map((r, i) =>
                                i === index
                                  ? { ...r, [field]: Number(e.target.value) }
                                  : r,
                              ),
                            })
                          }
                        />
                      ))}
                    </div>
                  </div>
                ))}
              </div>
              <Field label="Maximum appearances per fixture">
                <input
                  className="input mt-4 max-w-xs"
                  type="number"
                  min={1}
                  value={sportConfig.maximumAppearancesPerPlayer}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      maximumAppearancesPerPlayer: Number(e.target.value),
                    })
                  }
                />
              </Field>
              <Field label="Tie handling">
                <select
                  className="input mt-4 max-w-xs"
                  value={sportConfig.tieHandling}
                  onChange={(e) =>
                    setSportConfig({
                      ...sportConfig,
                      tieHandling: e.target
                        .value as BadmintonSportConfiguration["tieHandling"],
                    })
                  }
                >
                  <option value="LEAGUE_TIE">League tie points</option>
                  <option value="ADMINISTRATIVE_WIN">
                    Administrative decision
                  </option>
                </select>
              </Field>
              <div className="mt-4 flex flex-wrap gap-5 text-sm text-white/60">
                <label className="flex gap-2">
                  <input
                    type="checkbox"
                    checked={sportConfig.multipleRubberCategoriesAllowed}
                    onChange={(e) =>
                      setSportConfig({
                        ...sportConfig,
                        multipleRubberCategoriesAllowed: e.target.checked,
                      })
                    }
                  />
                  Allow players in multiple rubber categories
                </label>
                <label className="flex gap-2">
                  <input
                    type="checkbox"
                    checked={sportConfig.lineupChangesAfterStartAllowed}
                    onChange={(e) =>
                      setSportConfig({
                        ...sportConfig,
                        lineupChangesAfterStartAllowed: e.target.checked,
                      })
                    }
                  />
                  Allow lineup corrections after scoring starts
                </label>
              </div>
            </>
          ) : null}
          {actions.includes("CONFIGURE_COMPETITION") && (
            <button
              className="btn btn-primary mt-5"
              disabled={Boolean(working)}
              onClick={saveSport}
            >
              {working === "sport" ? (
                <Loader2 className="animate-spin" size={14} />
              ) : (
                <CheckCircle2 size={14} />
              )}
              Save sport-rule version
            </button>
          )}
        </section>
      )}

      <section className="mt-8">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <div className="label-cap">Calendar</div>
            <h2 className="h-heading text-2xl">Fixtures</h2>
          </div>
          <span className="chip">{overview.fixtures.length} total</span>
        </div>
        {overview.fixtures.length ? (

          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {overview.fixtures.map((f) => (
              <Fixture key={f.id} fixture={f} />
            ))}
          </div>
        ) : (
          <div className="card-elev rounded-2xl p-10 text-center text-sm text-white/40">
            Complete the locked-squad gate and generate league fixtures.
          </div>
        )}
      </section>

      {(semis.length > 0 || final) && (
        <section className="mt-8 card-elev rounded-2xl p-6">
          <div className="label-cap">Phase 4</div>
          <h2 className="h-heading mt-1 text-2xl">Playoff bracket</h2>
          <div className="mt-6 grid items-center gap-6 lg:grid-cols-[1fr_auto_1fr]">
            <div className="space-y-4">
              {semis.map((f, index) => (
                <BracketMatch
                  key={f.id}
                  title={`Semifinal ${index + 1}`}
                  fixture={f}
                />
              ))}
            </div>
            <GitBranch className="mx-auto rotate-90 text-white/20 lg:rotate-0" />
            <div>
              {final ? (
                <BracketMatch title="Final" fixture={final} />
              ) : (
                <div className="rounded-2xl border border-dashed border-white/15 p-8 text-center text-white/35">
                  Final is generated automatically after both semifinals finish.
                </div>
              )}
            </div>
          </div>
        </section>
      )}

      <section className="mt-8 card-elev rounded-2xl p-6">
        <div className="label-cap">Immutable history</div>
        <h2 className="h-heading mt-1 text-2xl">Competition audit</h2>
        <div className="mt-4 divide-y divide-white/5">
          {audit.length ? (
            audit.slice(0, 20).map((item) => (
              <div
                key={item.id}
                className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"
              >
                <div>
                  <div className="h-heading">
                    {item.eventType.replaceAll("_", " ")}
                  </div>
                  <div className="mt-1 text-xs text-white/40">
                    {item.actorName} · revision {item.revision}
                  </div>
                </div>
                <time className="text-xs text-white/35">
                  {new Date(item.occurredAt).toLocaleString()}
                </time>
              </div>
            ))
          ) : (
            <p className="py-6 text-sm text-white/40">
              Configuration and result events will appear here.
            </p>
          )}
        </div>
      </section>
    </div>
  );
}

function Kpi({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="card-elev rounded-2xl p-5">
      <div className="flex items-center justify-between text-white/35">
        <span className="label-cap">{label}</span>
        {icon}
      </div>
      <div className="stat-num mt-3 text-2xl">{value}</div>
    </div>
  );
}
function Field({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <label>
      <span className="label-cap mb-2 block text-[9px]">{label}</span>
      {children}
    </label>
  );
}
function Fixture({ fixture }: { fixture: MatchDto }) {
  return (
    <div className="card-elev rounded-xl p-5">
      <div className="flex justify-between">
        <span className="chip">
          {stageLabel(fixture.stageType)} · R{fixture.roundNumber}
          {fixture.legNumber > 1 ? ` · L${fixture.legNumber}` : ""}
        </span>
        <span
          className={`chip ${fixture.status === "COMPLETED" ? "chip-primary" : ""}`}
        >
          {fixture.status}
        </span>
      </div>
      <div className="h-heading mt-4 flex items-center justify-between gap-3">
        <span>{fixture.teamA.shortCode}</span>
        <span className="text-white/25">
          {fixture.status === "COMPLETED"
            ? `${fixture.teamAFormatWins} – ${fixture.teamBFormatWins}`
            : "vs"}
        </span>
        <span>{fixture.teamB.shortCode}</span>
      </div>
      <div className="mt-4 flex flex-wrap gap-3 text-xs text-white/40">
        <span className="flex items-center gap-1">
          <Clock3 size={12} />
          {new Date(fixture.scheduledAt).toLocaleString()}
        </span>
        {fixture.venue && (
          <span className="flex items-center gap-1">
            <MapPin size={12} />
            {fixture.venue}
          </span>
        )}
      </div>
    </div>
  );
}
function BracketMatch({
  title,
  fixture,
}: {
  title: string;
  fixture: MatchDto;
}) {
  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 p-5">
      <div className="label-cap">{title}</div>
      <div className="mt-3 space-y-2">
        {[fixture.teamA, fixture.teamB].map((team) => (
          <div
            key={team.id}
            className={`flex justify-between rounded-lg border px-3 py-2 ${fixture.winner?.id === team.id ? "border-primary/40 bg-primary/10" : "border-white/10"}`}
          >
            <span>{team.name}</span>
            {fixture.winner?.id === team.id && (
              <Trophy size={15} className="text-primary" />
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
function stageLabel(stage: string) {
  return stage
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}
function phaseLabel(phase: string) {
  return stageLabel(phase);
}
function toLocal(value?: string | null) {
  const date = value ? new Date(value) : new Date(Date.now() + 86400000);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}
