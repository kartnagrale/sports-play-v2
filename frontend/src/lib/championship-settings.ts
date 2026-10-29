export interface ChampionshipSettings {
  auctionName: string; maxSquadSize: number; purseLimit: number; minMale: number; minFemale: number;
  playerBasePrice: number; bidIncrement: number; timerSeconds: number; customRules: string; version: number;
}
export type SettingsForm = { [K in keyof ChampionshipSettings]: K extends 'auctionName' | 'customRules' ? string : number | '' };

export function settingsError(form: SettingsForm): string | null {
  if (!form.auctionName.trim()) return 'Auction name is required';
  for (const key of ['purseLimit', 'playerBasePrice', 'bidIncrement'] as const) {
    const value=form[key];
    if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0 || value > 999999999999 || Math.abs(value*100-Math.round(value*100)) > 0.0001)
      return `${key}: enter a positive rupee amount with at most two decimal places`;
  }
  for (const key of ['maxSquadSize', 'minMale', 'minFemale', 'timerSeconds'] as const) {
    const value=form[key];
    if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < (key==='minMale'||key==='minFemale'?0:1)) return `${key}: enter a valid whole number`;
  }
  if (Number(form.timerSeconds)<5 || Number(form.timerSeconds)>300) return 'Bid timer must be between 5 and 300 seconds';
  if (Number(form.minMale)+Number(form.minFemale)>Number(form.maxSquadSize)) return 'Gender minimums cannot exceed squad size';
  if (!Number.isSafeInteger(form.version)) return 'Reload settings before saving';
  return null;
}

/** Explicit write contract: response metadata must never be sent back to a strict parser. */
export function settingsRequest(form: SettingsForm) {
  const error=settingsError(form); if(error) throw new Error(error);
  return {auctionName:form.auctionName.trim(),maxSquadSize:form.maxSquadSize,purseLimit:form.purseLimit,
    minMale:form.minMale,minFemale:form.minFemale,playerBasePrice:form.playerBasePrice,
    bidIncrement:form.bidIncrement,timerSeconds:form.timerSeconds,customRules:form.customRules,expectedVersion:form.version};
}

export function inputNumber(value: string): number | '' {
  if (!value.trim()) return '';
  const parsed=Number(value);return Number.isFinite(parsed)?parsed:'';
}
