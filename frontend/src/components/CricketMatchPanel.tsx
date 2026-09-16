"use client";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  api,
  CricketMatchStateDto,
  CricketSportConfiguration,
  MatchDto,
  PlayerDto,
} from "@/lib/api";
import { toast } from "sonner";
import { Check, Loader2, RotateCcw } from "lucide-react";

export function CricketMatchPanel({
  championshipId,
  match,
  players,
  canManage,
}: {
  championshipId: string;
  match: MatchDto;
  players: PlayerDto[];
  canManage: boolean;
}) {
  const base = `/championships/${championshipId}/cricket/matches/${match.id}`;
  const [state, setState] = useState<CricketMatchStateDto | null>(null),
    [config, setConfig] = useState<CricketSportConfiguration | null>(null),
    [busy, setBusy] = useState("");
  const [xi, setXi] = useState<Record<string, string[]>>({}),
    [captain, setCaptain] = useState<Record<string, string>>({}),
    [keepers, setKeepers] = useState<Record<string, string[]>>({});
  const [tossWinner, setTossWinner] = useState(match.teamA.id),
    [decision, setDecision] = useState<"BAT" | "BOWL">("BAT"),
    [batting, setBatting] = useState(match.teamA.id);
  const [ball, setBall] = useState({
    strikerPlayerId: "",
    nonStrikerPlayerId: "",
    bowlerPlayerId: "",
    batterRuns: 0,
    wides: 0,
    noBalls: 0,
    byes: 0,
    legByes: 0,
    penaltyRuns: 0,
    wicket: false,
    dismissalType: "",
  });
  const load = useCallback(async () => {
    try {
      const [c, s] = await Promise.all([
        api.get<CricketSportConfiguration>(
          `/championships/${championshipId}/sport-configuration/cricket`,
        ),
        api
          .get<CricketMatchStateDto>(base)
          .catch((e) =>
            e?.response?.status === 404 ? { data: null } : Promise.reject(e),
          ),
      ]);
      setConfig(c.data);
      setState(s.data);
    } catch (e: any) {
      toast.error(
        e?.response?.data?.message || "Unable to load cricket scorecard",
      );
    }
  }, [base, championshipId]);
  useEffect(() => {
    load();
  }, [load]);
  const active = state?.innings.find((i) => i.status === "LIVE"),
    battingPlayers = players.filter((p) => p.teamId === active?.battingTeamId),
    fieldingPlayers = players.filter(
      (p) =>
        p.teamId &&
        (p.teamId === match.teamA.id || p.teamId === match.teamB.id) &&
        p.teamId !== active?.battingTeamId,
    );
  useEffect(() => {
    if (!active?.nextStrikerPlayerId || !active.nextNonStrikerPlayerId) return;
    setBall((current) => ({
      ...current,
      strikerPlayerId: active.nextStrikerPlayerId || "",
      nonStrikerPlayerId: active.nextNonStrikerPlayerId || "",
    }));
  }, [active?.nextStrikerPlayerId, active?.nextNonStrikerPlayerId]);
  const names = useMemo(
    () => Object.fromEntries(players.map((p) => [p.id, p.fullName])),
    [players],
  );
  const act = async (name: string, request: () => Promise<unknown>) => {
    setBusy(name);
    try {
      await request();
      toast.success("Cricket match updated");
      await load();
    } catch (e: any) {
      toast.error(
        e?.response?.data?.message ||
          e?.response?.data ||
          "Cricket action failed",
      );
    } finally {
      setBusy("");
    }
  };
  const lockXi = (teamId: string) =>
    act(`xi-${teamId}`, () =>
      api.put(`${base}/playing-xi/${teamId}`, {
        playerIds: xi[teamId] || [],
        captainId: captain[teamId],
        wicketkeeperId: keepers[teamId]?.[0],
        wicketkeeperIds: keepers[teamId] || [],
      }),
    );
  const submitBall = async () => {
    if (!active) return;
    await act("ball", () =>
      api.post(`${base}/innings/${active.id}/deliveries`, {
        ...ball,
        idempotencyKey: crypto.randomUUID(),
        wicketPlayerId: ball.wicket ? ball.strikerPlayerId : null,
        dismissalType: ball.wicket ? ball.dismissalType || "BOWLED" : null,
      }),
    );
    setBall((current) => ({
      ...current,
      batterRuns: 0,
      wides: 0,
      noBalls: 0,
      byes: 0,
      legByes: 0,
      penaltyRuns: 0,
      wicket: false,
      dismissalType: "",
    }));
  };
  const correctLast = () => {
    if (!active) return;
    const last = active.deliveries.at(-1);
    if (!last) return;
    const reason = prompt("Reason for reversing the latest delivery?");
    if (!reason) return;
    act("correct", () =>
      api.delete(`${base}/innings/${active.id}/deliveries/${last.id}`, {
        data: { expectedInningsVersion: active.version, reason },
      }),
    );
  };
  const overrideTarget = () => {
    if (!active?.target || !config?.administrativeTargetOverrideAllowed) return;
    const targetRuns = Number(
      prompt("New target (runs)?", String(active.target)),
    );
    if (!Number.isInteger(targetRuns) || targetRuns < 1) return;
    const reason = prompt("Reason for overriding the target?");
    if (!reason) return;
    act("target", () => api.put(`${base}/target`, { targetRuns, reason }));
  };
  return (
    <div className="space-y-4">
      {!state && canManage && config && (
        <section className="card-elev rounded-2xl p-5">
          <div className="label-cap">Pre-match</div>
          <h3 className="h-heading mt-1 text-xl">Lock playing sides</h3>
          <div className="mt-4 grid gap-4 lg:grid-cols-2">
            {[match.teamA, match.teamB].map((team) => {
              const roster = players.filter((p) => p.teamId === team.id),
                selected = xi[team.id] || [];
              return (
                <div
                  key={team.id}
                  className="rounded-xl border border-white/10 p-4"
                >
                  <div className="h-heading">
                    {team.name} · {selected.length}/{config.playersPerSide}
                  </div>
                  <div className="mt-3 flex max-h-40 flex-wrap gap-1.5 overflow-y-auto">
                    {roster.map((p) => (
                      <button
                        type="button"
                        key={p.id}
                        className={`chip ${selected.includes(p.id) ? "chip-primary" : ""}`}
                        onClick={() =>
                          setXi({
                            ...xi,
                            [team.id]: selected.includes(p.id)
                              ? selected.filter((x) => x !== p.id)
                              : selected.length < config.playersPerSide
                                ? [...selected, p.id]
                                : selected,
                          })
                        }
                      >
                        {p.fullName}
                      </button>
                    ))}
                  </div>
                  <div className="mt-3 grid gap-3 sm:grid-cols-2">
                    <Select
                      label="Captain"
                      value={captain[team.id] || ""}
                      options={roster.filter((p) => selected.includes(p.id))}
                      onChange={(v) => setCaptain({ ...captain, [team.id]: v })}
                    />
                    <div>
                      <div className="label-cap mb-2">
                        Wicketkeeper
                        {config.requiredWicketkeepers === 1 ? "" : "s"} ·{" "}
                        {(keepers[team.id] || []).length}/
                        {config.requiredWicketkeepers}
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {roster
                          .filter((p) => selected.includes(p.id))
                          .map((p) => {
                            const chosen = (keepers[team.id] || []).includes(
                              p.id,
                            );
                            return (
                              <button
                                type="button"
                                key={p.id}
                                className={`chip ${chosen ? "chip-primary" : ""}`}
                                onClick={() => {
                                  const current = keepers[team.id] || [];
                                  setKeepers({
                                    ...keepers,
                                    [team.id]: chosen
                                      ? current.filter((id) => id !== p.id)
                                      : current.length <
                                          config.requiredWicketkeepers
                                        ? [...current, p.id]
                                        : current,
                                  });
                                }}
                              >
                                {p.fullName}
                              </button>
                            );
                          })}
                      </div>
                    </div>
                  </div>
                  <button
                    className="btn btn-ghost mt-3"
                    disabled={
                      Boolean(busy) ||
                      selected.length !== config.playersPerSide ||
                      !captain[team.id] ||
                      (keepers[team.id] || []).length !==
                        config.requiredWicketkeepers
                    }
                    onClick={() => lockXi(team.id)}
                  >
                    {busy === `xi-${team.id}` ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : (
                      <Check size={14} />
                    )}
                    Lock XI
                  </button>
                </div>
              );
            })}
          </div>
          <div className="mt-5 flex flex-wrap items-end gap-3">
            <Select
              label="Toss winner"
              value={tossWinner}
              options={
                [match.teamA, match.teamB].map((t) => ({
                  id: t.id,
                  fullName: t.name,
                })) as PlayerDto[]
              }
              onChange={setTossWinner}
            />
            <label>
              <span className="label-cap mb-2 block">Decision</span>
              <select
                className="input"
                value={decision}
                onChange={(e) => setDecision(e.target.value as "BAT" | "BOWL")}
              >
                <option value="BAT">Bat</option>
                <option value="BOWL">Bowl</option>
              </select>
            </label>
            <button
              className="btn btn-primary"
              disabled={Boolean(busy)}
              onClick={() =>
                act("toss", () =>
                  api.post(`${base}/toss`, {
                    winnerTeamId: tossWinner,
                    decision,
                  }),
                )
              }
            >
              Record toss
            </button>
            {!config.tossRequired && (
              <>
                <Select
                  label="Batting first"
                  value={batting}
                  options={[match.teamA, match.teamB].map((team) => ({
                    id: team.id,
                    fullName: team.name,
                  }))}
                  onChange={setBatting}
                />
                <button
                  className="btn btn-ghost"
                  disabled={Boolean(busy)}
                  onClick={() =>
                    act("innings", () =>
                      api.post(`${base}/innings`, { battingTeamId: batting }),
                    )
                  }
                >
                  Start without toss
                </button>
              </>
            )}
          </div>
        </section>
      )}
      {state && (
        <>
          <section className="card-elev rounded-2xl p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="label-cap">Live scorecard</div>
                <h3 className="h-heading mt-1 text-2xl">
                  {state.resultText ||
                    `${match.teamA.shortCode} vs ${match.teamB.shortCode}`}
                </h3>
              </div>
              <span className="chip">
                {state.tossWinnerTeamId && state.tossDecision
                  ? `Toss · ${state.tossWinnerTeamId === match.teamA.id ? match.teamA.shortCode : match.teamB.shortCode} ${state.tossDecision.toLowerCase()}`
                  : "Toss not required"}
              </span>
              {canManage &&
                active?.target &&
                config?.administrativeTargetOverrideAllowed && (
                  <button
                    className="btn btn-ghost"
                    disabled={Boolean(busy)}
                    onClick={overrideTarget}
                  >
                    Override target
                  </button>
                )}
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {state.innings.map((i) => (
                <div
                  key={i.id}
                  className="rounded-xl border border-white/10 p-4"
                >
                  <div className="label-cap">
                    Innings {i.inningsNumber} ·{" "}
                    {i.battingTeamId === match.teamA.id
                      ? match.teamA.shortCode
                      : match.teamB.shortCode}
                  </div>
                  <div className="stat-num mt-2 text-3xl">
                    {i.runs}/{i.wickets}{" "}
                    <span className="text-base text-white/40">({i.overs})</span>
                  </div>
                  {i.target && (
                    <div className="mt-1 text-xs text-white/50">
                      Target {i.target}
                      {i.requiredRunRate != null
                        ? ` · RRR ${i.requiredRunRate.toFixed(2)}`
                        : ""}
                    </div>
                  )}
                  <div className="mt-3 text-xs text-white/40">
                    {i.batters
                      .map(
                        (b) =>
                          `${names[b.playerId] || "Batter"} ${b.runs} (${b.balls})`,
                      )
                      .join(" · ") || "No deliveries"}
                  </div>
                </div>
              ))}
            </div>
          </section>
          {canManage && !active && match.status !== "COMPLETED" && (
            <section className="card-elev rounded-xl p-5">
              <div className="flex flex-wrap items-end gap-3">
                <Select
                  label="Batting team"
                  value={batting}
                  options={
                    [match.teamA, match.teamB].map((t) => ({
                      id: t.id,
                      fullName: t.name,
                    })) as PlayerDto[]
                  }
                  onChange={setBatting}
                />
                <button
                  className="btn btn-primary"
                  disabled={Boolean(busy)}
                  onClick={() =>
                    act("innings", () =>
                      api.post(`${base}/innings`, { battingTeamId: batting }),
                    )
                  }
                >
                  Start next innings
                </button>
              </div>
            </section>
          )}
          {canManage && active && (
            <section className="card-elev rounded-xl p-5">
              <div className="label-cap">Delivery entry</div>
              <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                <Select
                  label="Striker"
                  value={ball.strikerPlayerId}
                  options={battingPlayers}
                  onChange={(v) => setBall({ ...ball, strikerPlayerId: v })}
                />
                <Select
                  label="Non-striker"
                  value={ball.nonStrikerPlayerId}
                  options={battingPlayers}
                  onChange={(v) => setBall({ ...ball, nonStrikerPlayerId: v })}
                />
                <Select
                  label="Bowler"
                  value={ball.bowlerPlayerId}
                  options={fieldingPlayers}
                  onChange={(v) => setBall({ ...ball, bowlerPlayerId: v })}
                />
                {(
                  [
                    "batterRuns",
                    "wides",
                    "noBalls",
                    "byes",
                    "legByes",
                    "penaltyRuns",
                  ] as const
                ).map((key) => (
                  <label key={key}>
                    <span className="label-cap mb-2 block">
                      {key.replace(/([A-Z])/g, " $1")}
                    </span>
                    <input
                      className="input"
                      type="number"
                      min={0}
                      max={key === "batterRuns" ? 6 : undefined}
                      value={ball[key]}
                      onChange={(e) =>
                        setBall({ ...ball, [key]: Number(e.target.value) })
                      }
                    />
                  </label>
                ))}
              </div>
              <label className="mt-3 flex items-center gap-2 text-sm text-white/60">
                <input
                  type="checkbox"
                  checked={ball.wicket}
                  onChange={(e) =>
                    setBall({ ...ball, wicket: e.target.checked })
                  }
                />
                Wicket
              </label>
              {ball.wicket && (
                <label className="mt-3 block">
                  <span className="label-cap mb-2 block">Dismissal type</span>
                  <select
                    className="input max-w-xs"
                    value={ball.dismissalType}
                    onChange={(e) =>
                      setBall({ ...ball, dismissalType: e.target.value })
                    }
                  >
                    <option value="BOWLED">Bowled</option>
                    <option value="CAUGHT">Caught</option>
                    <option value="LBW">LBW</option>
                    <option value="RUN_OUT">Run out</option>
                    <option value="STUMPED">Stumped</option>
                  </select>
                </label>
              )}
              <div className="mt-4 flex gap-2">
                <button
                  className="btn btn-primary"
                  disabled={
                    Boolean(busy) ||
                    !ball.strikerPlayerId ||
                    !ball.nonStrikerPlayerId ||
                    !ball.bowlerPlayerId
                  }
                  onClick={submitBall}
                >
                  {busy === "ball" ? (
                    <Loader2 className="animate-spin" size={14} />
                  ) : (
                    <Check size={14} />
                  )}
                  Submit delivery
                </button>
                {active.deliveries.length > 0 && (
                  <button
                    className="btn btn-ghost"
                    disabled={Boolean(busy)}
                    onClick={correctLast}
                  >
                    <RotateCcw size={14} />
                    Reverse latest
                  </button>
                )}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  );
}

function Select({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: { id: string; fullName: string }[];
  onChange: (value: string) => void;
}) {
  return (
    <label>
      <span className="label-cap mb-2 block">{label}</span>
      <select
        className="input min-w-48"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">Select…</option>
        {options.map((p) => (
          <option key={p.id} value={p.id}>
            {p.fullName}
          </option>
        ))}
      </select>
    </label>
  );
}
