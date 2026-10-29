// End-to-end integration against real Docker gateway + PostgreSQL. No mocked HTTP or fabricated results.
import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import {Session,createTestChampionship} from './npl-api-client.mjs';
const root=new Session();
await root.request('/auth/login','POST',{email:process.env.NPL_TEST_EMAIL,password:process.env.NPL_TEST_PASSWORD});
const {admin,cid}=await createTestChampionship(root);
const base=`/championships/${cid}`;
const initial=await admin.request(base+'/settings');
const settings={auctionName:'NPL real integration',maxSquadSize:11,purseLimit:100000,minMale:0,minFemale:3,playerBasePrice:5000,bidIncrement:1000,timerSeconds:300,customRules:'Disposable synthetic integration',expectedVersion:initial.version,maximumBid:15000,maximumPremiumPurchases:4,captainPreassignmentRequired:true};
const configured=await admin.request(base+'/settings','PUT',settings);
assert.equal((await admin.request(base+'/settings')).maximumBid,15000);
await admin.request(base+'/settings','PUT',settings,409);
const existing=await admin.request(base+'/competition/configuration');
const competition={expectedConfigurationVersion:existing.configurationVersion,configuredTeamCount:4,minimumTeamCount:4,maximumTeamCount:4,minimumRosterSize:11,maximumRosterSize:11,playingSideSize:11,maximumSubstitutes:0,numberOfLegs:1,playoffFormat:'TOP_TWO_FINAL',roundRobinMode:'SINGLE',pointsPerWin:2,pointsPerLoss:0,pointsPerTie:1,pointsPerNoResult:1,tieBreakerPriority:['POINTS','NET_RUN_RATE','HEAD_TO_HEAD','NAME'],qualifyingTeamCount:2,venues:['Disposable Test Ground'],leagueStartAt:'2026-11-01T04:00:00Z',fixtureSpacingDays:1,manualDateEditing:true,timezone:'Asia/Kolkata',presetCode:'CUSTOM_CHAMPIONSHIP'};
await admin.request(base+'/admin/competition/configuration','PUT',competition);
// One-over custom cricket is used only to exercise complete, real delivery scoring cheaply.
const cricket=await admin.request(base+'/sport-configuration/cricket');
const {version,status,...cricketFields}=cricket;
await admin.request(base+'/sport-configuration/cricket','PUT',{...cricketFields,expectedVersion:version,presetCode:'CUSTOM',playersPerSide:11,oversPerInnings:1,maximumOversPerBowler:1,powerplayRanges:'1-1',tiePolicy:'TIE'});
const aid=configured.auctionId,auction=`${base}/auctions/${aid}`;
await admin.request(auction+'/admin/start','POST',undefined,409);
const captains=[],captainPlayers=[];
for(let n=0;n<4;n++){
  const created=await admin.request(base+'/admin/teams','POST',{teamName:`Test Team ${n}`,shortCode:`NT${n}`,captainFullName:`Test Captain ${n}`,captainEmail:`captain-${crypto.randomUUID()}@example.invalid`,captainMobile:'9999999999'});
  const session=new Session(),password=`Npl-captain-${crypto.randomUUID()}!`;
  await session.request('/auth/activate','POST',{token:created.setupPath.split('=')[1],password},204);
  await session.request('/auth/login','POST',{email:created.captainEmail,password});
  const path=`${base}/admin/teams/${created.teamId}/captains/${created.captainUserId}/player`;
  const request={gender:n===3?'FEMALE':'MALE',playerReference:`CAP-${n}`};
  const player=await admin.request(path,'PUT',request);
  assert.equal((await admin.request(path,'PUT',request)).id,player.id,'repeat assignment is idempotent');
  await session.request(path,'PUT',request,403);
  assert.equal(player.status,'PREASSIGNED');assert.equal(player.soldPrice,undefined);
  captainPlayers.push(player);captains.push({...created,session});
}
let teams=await admin.request(base+'/teams');
assert(teams.every(t=>t.totalPlayers===1&&t.purseRemaining===100000));
assert.equal((await admin.request(auction+'/history')).length,0,'no fake captain bid');
await captains[0].session.request(`/championships/${crypto.randomUUID()}/players`,'GET',undefined,403);
const fixtureDir=process.env.NPL_FIXTURE_DIR;
async function preview(filename){const form=new FormData();form.set('file',new Blob([readFileSync(`${fixtureDir}/${filename}`)],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}),filename);return admin.request(base+'/admin/players/imports','POST',form);}
const duplicate=await preview('captain-duplicate.xlsx');assert(duplicate.invalid>0,'captain name collision must be flagged');
const imported=await preview('forty-players.xlsx');assert.equal(imported.invalid,0,JSON.stringify(imported.rows));
const confirmed=await admin.request(`${base}/admin/players/imports/${imported.importId}/confirm`,'POST',{checksum:imported.checksum,allowUnreferenced:false});assert.equal(confirmed.imported,40);
const all=await admin.request(base+'/players');assert.equal(all.length,44);assert.equal(all.filter(p=>p.status==='AVAILABLE').length,40);
console.log('PASS configuration, 4 captain sessions, preassignment/idempotency/authorization, Excel 40 import, 44 identities and untouched purses.');
let state=await admin.request(auction+'/admin/start','POST');
await admin.request(base+'/settings','PUT',{...settings,expectedVersion:(await admin.request(base+'/settings')).version},409);
const before=state.teams.map(t=>t.purseRemaining);
for(const amount of [15001,16000])await captains[0].session.request(auction+'/bid','POST',{playerId:state.currentPlayer.id,teamId:captains[0].teamId,amount},400);
assert.deepEqual((await admin.request(auction+'/state')).teams.map(t=>t.purseRemaining),before);
for(let team=0;team<4;team++)for(let n=0;n<10;n++){
  state=await admin.request(auction+'/state');
  const player=all.find(p=>p.playerReference===`EMP-${team}-${n}`);
  if(state.currentPlayer.id!==player.id)state=await admin.request(auction+'/admin/set-current','POST',{playerId:player.id});
  const amount=n<4?15000:5000;
  if(team===0&&n===0){
    await captains[1].session.request(auction+'/bid','POST',{playerId:player.id,teamId:captains[1].teamId,amount:5000});
    const bids=await Promise.all([0,1].map(()=>captains[0].session.raw(auction+'/bid','POST',{playerId:player.id,teamId:captains[0].teamId,amount})));
    assert.deepEqual(bids.map(r=>r.status).sort(),[200,400]);
    await admin.request(auction+'/admin/undo','POST');
  }
  if(n===4)await captains[team].session.request(auction+'/bid','POST',{playerId:player.id,teamId:captains[team].teamId,amount:15000},400);
  if(team===1&&n===0)await captains[0].session.request(auction+'/bid','POST',{playerId:player.id,teamId:captains[0].teamId,amount:5000},400);
  await captains[team].session.request(auction+'/bid','POST',{playerId:player.id,teamId:captains[team].teamId,amount});
  if(team===0&&n===0){
    const sales=await Promise.all([0,1].map(()=>admin.raw(auction+'/admin/sell','POST')));
    assert.deepEqual(sales.map(r=>r.status).sort(),[200,404]);
    assert.equal((await admin.request(auction+'/state')).teams.find(t=>t.id===captains[0].teamId).purseRemaining,85000);
  }else await admin.request(auction+'/admin/sell','POST');
}
state=await admin.request(auction+'/state');assert.equal(state.status,'COMPLETED');
assert(state.teams.every(t=>t.totalPlayers===11&&t.purseRemaining===10000));
assert(state.teamEligibility.every(t=>t.premiumPurchases===4&&t.femalePlayers>=3&&t.squadSlotsRemaining===0));
for(const c of captains)await c.session.request(`${base}/squad-confirmations/teams/${c.teamId}/confirm`,'POST',{});
const locked=await admin.request(base+'/squad-confirmations/lock-all','POST',{});assert(locked.allSquadsLocked);
console.log('PASS ceiling, premium cap, losing/undone bids, concurrent bids/sales, 40 real sales, purse reconciliation and 11-member squad confirmations.');
let overview=await admin.request(base+'/admin/competition/league/generate','POST');
assert.equal(overview.fixtures.length,6);assert.equal(new Set(overview.fixtures.map(f=>[f.teamA.id,f.teamB.id].sort().join(':'))).size,6);
for(const c of captains)assert.equal(overview.fixtures.filter(f=>f.teamA.id===c.teamId||f.teamB.id===c.teamId).length,3);
await admin.request(base+'/admin/competition/playoffs/generate','POST',undefined,409);
const roster=await admin.request(base+'/players');
async function score(match){
  const sides=[match.teamA,match.teamB].sort((a,b)=>a.name.localeCompare(b.name));const [winner,loser]=sides;
  for(const team of sides){const members=roster.filter(p=>p.teamId===team.id);const captain=members.find(p=>p.status==='PREASSIGNED');
    await admin.request(`${base}/cricket/matches/${match.id}/playing-xi/${team.id}`,'PUT',{playerIds:members.map(p=>p.id),captainId:captain.id,wicketkeeperId:members[1].id});}
  const path=`${base}/cricket/matches/${match.id}`;
  await admin.request(path+'/toss','POST',{winnerTeamId:loser.id,decision:'BAT'});
  let current;
  for(const team of [loser,winner]){
    current=await admin.request(path+'/innings','POST',{battingTeamId:team.id});let innings=current.innings.at(-1);
    const batting=roster.filter(p=>p.teamId===team.id);const bowling=roster.find(p=>p.teamId!==(team.id)&&[winner.id,loser.id].includes(p.teamId));
    while(innings.status==='LIVE'){
      current=await admin.request(`${path}/innings/${innings.id}/deliveries`,'POST',{idempotencyKey:crypto.randomUUID(),strikerPlayerId:innings.nextStrikerPlayerId||batting[0].id,nonStrikerPlayerId:innings.nextNonStrikerPlayerId||batting[1].id,bowlerPlayerId:bowling.id,batterRuns:team.id===loser.id?1:6,wides:0,noBalls:0,byes:0,legByes:0,penaltyRuns:0,wicket:false});
      innings=current.innings.at(-1);
    }
  }
  assert(current.resultText);return current;
}
for(const match of overview.fixtures)await score(match);
const standings=await admin.request(base+'/analytics/standings?penalties=false');
assert.deepEqual(standings.map(s=>s.totalPoints),[6,4,2,0]);assert(standings.every(s=>s.played===3&&Number.isFinite(s.netRunRate)));
overview=await admin.request(base+'/admin/competition/playoffs/generate','POST');
assert.equal(overview.fixtures.length,7);assert.equal(overview.fixtures.filter(f=>f.stageType==='SEMI_FINAL').length,0);
const final=overview.fixtures.find(f=>f.stageType==='FINAL');assert(final);
assert.deepEqual([final.teamA.id,final.teamB.id].sort(),standings.slice(0,2).map(s=>s.team.id).sort());
const finalState=await score(final);
overview=await admin.request(base+'/competition');assert.equal(overview.configuration.champion.id,captains[0].teamId);assert.equal(overview.configuration.runnerUp.id,captains[1].teamId);
const approval=await admin.request(base+'/admin/certificates/approvals','POST',{justification:'Real delivery scoring completed in disposable integration test'});
const eligibility=await admin.request(base+'/admin/certificates/eligibility?category=PARTICIPATION&recipientType=PLAYER');
assert.equal(eligibility.recipients?.length??eligibility.eligible?.length,44);
console.log('PASS six unique league fixtures, three matches per team, real scoring, standings/NRR, top-two final, champion/runner-up and captain certificate eligibility.');
if(process.env.NPL_TEST_STATE)writeFileSync(process.env.NPL_TEST_STATE,JSON.stringify({cid,aid,adminCookies:[...admin.cookies],final,finalState,approval}));
