// Supplemental, separate tenant. Never changes the full NPL championship's rules or results.
import assert from 'node:assert/strict';
import {writeFileSync,readFileSync} from 'node:fs';
import {Session,createTestChampionship} from './npl-api-client.mjs';
const credentials=JSON.parse(readFileSync(process.env.NPL_MOCK_PRIVATE_DIR+'/bootstrap.credentials.json'));
const root=new Session();await root.request('/auth/login','POST',credentials);
const {admin,cid}=await createTestChampionship(root,'DISPOSABLE cricket scoring edge cases');
const base=`/championships/${cid}`;
const draftSettings=await admin.request(base+'/settings');
await admin.request(base+'/settings','PUT',{auctionName:'Disposable edge setup',expectedVersion:draftSettings.version,maxSquadSize:11,purseLimit:10000,minMale:0,minFemale:0,playerBasePrice:5000,bidIncrement:1000,timerSeconds:30,maximumBid:15000,maximumPremiumPurchases:4,captainPreassignmentRequired:true});
const existing=await admin.request(base+'/competition/configuration');
const {championshipId,sportCode,configurationVersion,configurationStatus,resultSchemaVersion,champion,runnerUp,...fields}=existing;
await admin.request(base+'/admin/competition/configuration','PUT',{...fields,expectedConfigurationVersion:configurationVersion,configuredTeamCount:2,minimumTeamCount:2,maximumTeamCount:2,minimumRosterSize:2,maximumRosterSize:2,playingSideSize:2,qualifyingTeamCount:2,playoffFormat:'TOP_TWO_FINAL',roundRobinMode:'SINGLE',numberOfLegs:1,pointsPerWin:2,pointsPerLoss:0,pointsPerTie:1,pointsPerNoResult:3,leagueStartAt:'2026-12-01T04:00:00Z',venues:['Mock Edge Ground']});
const initial=await admin.request(base+'/settings');
const saved=await admin.request(base+'/settings','PUT',{auctionName:'Disposable edge auction',expectedVersion:initial.version,maxSquadSize:2,purseLimit:10000,minMale:0,minFemale:0,playerBasePrice:5000,bidIncrement:1000,timerSeconds:30,maximumBid:15000,maximumPremiumPurchases:4,captainPreassignmentRequired:true});
const cricket=await admin.request(base+'/sport-configuration/cricket');const {version,status,...cricketFields}=cricket;
await admin.request(base+'/sport-configuration/cricket','PUT',{...cricketFields,expectedVersion:version,presetCode:'CUSTOM',playersPerSide:2,oversPerInnings:1,maximumOversPerBowler:1,powerplayRanges:'1-1',tiePolicy:'TIE'});
const captains=[];
for(let i=0;i<2;i++){
  const c=await admin.request(base+'/admin/teams','POST',{teamName:`Mock Edge ${i}`,shortCode:`ME${i}`,captainFullName:`Mock Edge Captain ${i}`,captainEmail:`edge-${crypto.randomUUID()}@example.invalid`,captainMobile:`000000090${i}`});
  const password=`Edge-${crypto.randomUUID()}!`,session=new Session();
  await session.request('/auth/activate','POST',{token:c.setupPath.split('=')[1],password},204);
  await session.request('/auth/login','POST',{email:c.captainEmail,password});
  const player=await admin.request(`${base}/admin/teams/${c.teamId}/captains/${c.captainUserId}/player`,'PUT',{gender:'MALE',playerReference:`EDGE-CAP-${i}`});
  captains.push({...c,session,playerId:player.id});
}
const players=[];for(let i=0;i<2;i++)players.push(await admin.request(base+'/admin/players','POST',{fullName:`Mock Edge Batter ${i}`,gender:'FEMALE',basePrice:5000,playerReference:`EDGE-${i}`,auctionOrder:i+1}));
const auction=`${base}/auctions/${saved.auctionId}`;
await admin.request(auction+'/admin/start','POST');
const before=(await admin.request(auction+'/state')).teams.map(t=>t.purseRemaining);
const insufficient=await captains[0].session.request(auction+'/bid','POST',{playerId:players[0].id,teamId:captains[0].teamId,amount:11000},400);assert.match(insufficient.message,/purse/i);
await captains[0].session.request(auction+'/bid','POST',{playerId:players[0].id,teamId:captains[0].teamId,amount:5500},400);
const spectator=await createTestChampionship(root,'DISPOSABLE spectator provisioning');
await spectator.admin.request(auction+'/state','GET',undefined,403);
const profile=await spectator.admin.request('/auth/me');
await admin.request(base+'/roles','POST',{email:profile.email,role:'SPECTATOR'},204);
await spectator.admin.request(auction+'/state');
await spectator.admin.request(auction+'/bid','POST',{playerId:players[0].id,teamId:captains[1].teamId,amount:5000},403);
assert.deepEqual((await admin.request(auction+'/state')).teams.map(t=>t.purseRemaining),before);
for(let i=0;i<2;i++){
  await captains[i].session.request(auction+'/bid','POST',{playerId:players[i].id,teamId:captains[i].teamId,amount:5000});
  await admin.request(auction+'/admin/sell','POST');
  if(i===0)await captains[0].session.request(auction+'/bid','POST',{playerId:players[1].id,teamId:captains[0].teamId,amount:5000},400);
}
for(const c of captains)await c.session.request(`${base}/squad-confirmations/teams/${c.teamId}/confirm`,'POST',{});
await admin.request(base+'/squad-confirmations/lock-all','POST',{});
const overview=await admin.request(base+'/admin/competition/league/generate','POST');assert.equal(overview.fixtures.length,1);
const match=overview.fixtures[0],path=`${base}/cricket/matches/${match.id}`,roster=await admin.request(base+'/players');
for(const c of captains){const members=roster.filter(p=>p.teamId===c.teamId);await c.session.request(`${path}/playing-xi/${c.teamId}`,'PUT',{playerIds:members.map(p=>p.id),captainId:c.playerId,wicketkeeperId:members.find(p=>p.id!==c.playerId).id});}
await admin.request(path+'/toss','POST',{winnerTeamId:captains[0].teamId,decision:'BAT'});
let current=await admin.request(path+'/innings','POST',{battingTeamId:captains[0].teamId});
async function ball(values,expected=200,key=crypto.randomUUID()){
  const innings=current.innings.at(-1),batting=roster.filter(p=>p.teamId===innings.battingTeamId),bowler=roster.find(p=>p.teamId!==innings.battingTeamId);
  const body={idempotencyKey:key,strikerPlayerId:innings.nextStrikerPlayerId||batting[0].id,nonStrikerPlayerId:innings.nextNonStrikerPlayerId||batting[1].id,bowlerPlayerId:bowler.id,batterRuns:0,wides:0,noBalls:0,byes:0,legByes:0,penaltyRuns:0,wicket:false,...values};
  if(body.wicket)body.wicketPlayerId=body.strikerPlayerId;
  const result=await admin.request(`${path}/innings/${innings.id}/deliveries`,'POST',body,expected);if(expected===200)current=result;return body;
}
const wide=await ball({wides:1});assert.equal(current.innings[0].runs,1);assert.equal(current.innings[0].legalBalls,0);
await admin.request(`${path}/innings/${current.innings[0].id}/deliveries`,'POST',wide);assert.equal((await admin.request(path)).innings[0].runs,1,'idempotent duplicate wide');
await ball({noBalls:1,batterRuns:1});assert.equal(current.innings[0].runs,3);assert.equal(current.innings[0].legalBalls,0);
await ball({wicket:true,dismissalType:'BOWLED'},400);
await ball({wicket:true,dismissalType:'RUN_OUT'});assert.equal(current.innings[0].wickets,1);assert.equal(current.innings[0].legalBalls,1);assert.equal(current.innings[0].status,'COMPLETED');
current=await admin.request(path+'/innings','POST',{battingTeamId:captains[1].teamId});
await ball({byes:1,legByes:1},400);await ball({byes:1});await ball({legByes:1});await ball({batterRuns:1});
for(let i=0;i<3;i++)await ball({});
assert.equal(current.resultText,'Match tied');assert.equal(current.innings[1].runs,3);assert.equal(current.innings[1].legalBalls,6);
let standings=await admin.request(base+'/analytics/standings?penalties=false');assert.deepEqual(standings.map(s=>s.totalPoints),[1,1]);assert(standings.every(s=>s.netRunRate===0));assert.deepEqual(standings.map(s=>s.team.name),['Mock Edge 0','Mock Edge 1']);
const tieState=current;
await admin.request(path+'/administrative-result','POST',{outcome:'NO_RESULT',reason:'Synthetic verification of explicitly configured no-result points'});
standings=await admin.request(base+'/analytics/standings?penalties=false');assert.deepEqual(standings.map(s=>s.totalPoints),[3,3]);
const evidence={championshipId:cid,distinctFromFullMock:true,insufficientPurseRejected:true,offIncrementRejected:true,nonMemberAccessRejected:true,memberSpectatorCanViewButCannotBid:true,fullSquadRejected:true,unchangedRejectedBalances:true,duplicateWideIdempotent:true,freeHitDismissalRejected:true,runOutWicketRecorded:true,byesAndLegByesValidated:true,tiePoints:[1,1],noResultPoints:[3,3],tiedNetRunRates:[0,0],nameTieBreakVerified:true,tieState};
writeFileSync('docs/npl-mock/evidence/cricket-edge-results.json',JSON.stringify(evidence,null,2));
console.log('PASS separate-tenant purse/non-member/capacity rejections; wides, no-ball/free-hit/run-out, byes/leg-byes, duplicate delivery, real tie/NRR/name ordering and no-result points');

