import {expect,type APIRequestContext} from "@playwright/test";

/** Keep older miniature browser fixtures consistent with the versioned configuration contract. */
export async function fixtureWrite(client:APIRequestContext,api:string,path:string,method:string,data:unknown,teamCount:number) {
  const headers:Record<string,string>={};
  if(method==="PUT"&&path.endsWith("/settings")){
    const base=path.slice(0,-"/settings".length);
    const values=data as Record<string,any>;
    const config=await(await client.get(api+base+"/competition/configuration")).json();
    const {championshipId,sportCode,configurationVersion,configurationStatus,resultSchemaVersion,champion,runnerUp,...fields}=config;
    const csrf=await(await client.get(api+"/auth/csrf")).json();
    const response=await client.put(api+base+"/admin/competition/configuration",{headers:{"X-XSRF-TOKEN":csrf.token},data:{...fields,expectedConfigurationVersion:configurationVersion,presetCode:"CUSTOM_CHAMPIONSHIP",configuredTeamCount:teamCount,minimumTeamCount:teamCount,maximumTeamCount:teamCount,minimumRosterSize:values.maxSquadSize,maximumRosterSize:values.maxSquadSize,playingSideSize:Math.min(config.playingSideSize,values.maxSquadSize),qualifyingTeamCount:2,playoffFormat:"TOP_TWO_FINAL",leagueStartAt:config.leagueStartAt||"2026-11-01T04:00:00Z"}});
    expect(response.status(),await response.text()).toBe(200);
    const settings=await(await client.get(api+path)).json();
    data={...values,expectedVersion:settings.version};
  }
  if(method==="PUT"&&path.endsWith("/sport-configuration/quotas")){
    const response=await client.get(api+path);
    headers["If-Match"]=(response.headers().etag||"").replaceAll('"',"");
  }
  return {data,headers};
}
