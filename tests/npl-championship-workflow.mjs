// Full-mode extension of npl-championship-api.mjs. Real gateway/API/WS only.
// Run through tests/run-npl-full-mock.ps1, which verifies the Compose project first.
import assert from 'node:assert/strict';
import {readFileSync,writeFileSync,existsSync,mkdirSync,renameSync} from 'node:fs';
import {resolve,dirname,join} from 'node:path';
import {randomBytes,randomUUID,createHash} from 'node:crypto';
import {execFileSync} from 'node:child_process';
import {Session,base as apiUrl} from './npl-api-client.mjs';
import {observeAuction} from './npl-websocket.mjs';

const privateDir=resolve(process.env.NPL_MOCK_PRIVATE_DIR||'');
const project=process.env.NPL_MOCK_ISOLATED_PROJECT;
assert(['playneml-npl-mock','playneml-npl-mock-verified'].includes(project),'Use the guarded PowerShell runner');
assert(privateDir!==process.cwd()&&!privateDir.startsWith(resolve('.')+'\\'),'Keep runtime secrets outside Git');
assert(existsSync(join(privateDir,'bootstrap.credentials.json')),'Missing protected bootstrap credentials');
const stateFile=process.env.NPL_MOCK_STATE_FILE?resolve(process.env.NPL_MOCK_STATE_FILE):join(privateDir,'full-mock.credentials.json');
assert.equal(dirname(stateFile).toLowerCase(),privateDir.toLowerCase(),'Checkpoint must stay in protected storage');
const state=existsSync(stateFile)?JSON.parse(readFileSync(stateFile)): {project,completed:[],captains:[],checks:[],sales:[]};
assert.equal(state.project,project);
const evidenceDir=resolve('docs/npl-mock/evidence');mkdirSync(evidenceDir,{recursive:true});
const secret=()=>`Mock-${randomBytes(24).toString('base64url')}!`;
function save(){
  writeFileSync(stateFile+'.tmp',JSON.stringify(state,null,2),{mode:0o600});
  // Windows readers/scanners can briefly hold a non-delete-sharing handle. Preserve atomic
  // replacement and retry that transient condition rather than truncating the valid checkpoint.
  for(let attempt=0;;attempt++){
    try{renameSync(stateFile+'.tmp',stateFile);return;}
    catch(error){if(attempt>=199||!['EPERM','EBUSY','EACCES'].includes(error.code))throw error;Atomics.wait(new Int32Array(new SharedArrayBuffer(4)),0,0,25);}
  }
}
function check(name,details={}){state.checks.push({name,...details});save();console.log(`PASS ${name}`);}
function checkpoint(name){if(!state.completed.includes(name))state.completed.push(name);save();console.log(`CHECKPOINT ${name}`);if(process.env.NPL_MOCK_STOP_AFTER===name)process.exit(0);}
const root=new Session();const bootstrap=JSON.parse(readFileSync(join(privateDir,'bootstrap.credentials.json')));
assert.equal((await fetch(apiUrl+'/health')).status,200);
assert.equal((await fetch(apiUrl.replace(/\/api$/,'')+'/login')).status,200);
assert.equal((await fetch(apiUrl+'/championships')).status,401);
assert([401,403].includes((await fetch(apiUrl+'/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(bootstrap)})).status),'missing CSRF must be rejected');
await root.request('/auth/login','POST',bootstrap);
assert([401,403].includes((await fetch(apiUrl+'/auth/logout',{method:'POST',headers:{Cookie:[...root.cookies].map(([k,v])=>`${k}=${v}`).join('; ')}})).status),'authenticated mutation without CSRF must fail');
if(!state.completed.includes('health')){check('gateway/frontend health, anonymous denial, CSRF enforcement and bootstrap API login');checkpoint('health');}
async function login(account){const s=new Session();await s.request('/auth/login','POST',{email:account.email,password:account.password});return s;}
async function activate(account){const s=new Session();await s.request('/auth/activate','POST',{token:account.setupPath.split('=')[1],password:account.password},204);delete account.setupPath;account.activated=true;save();}

if(!state.cid){
  const created=await root.request('/championships','POST',{name:'NPL MOCK 2026',sportType:'CRICKET',adminFullName:'Mock Aster Vale',adminEmail:`administrator-${randomUUID()}@example.invalid`,adminMobile:'0000000100'});
  assert.equal(created.championship.status,'DRAFT');
  state.cid=created.championship.id;state.admin={...created.admin,password:secret()};save();
}
if(!state.admin.activated)await activate(state.admin);
const admin=await login(state.admin),base=`/championships/${state.cid}`;
state.adminCookies=[...admin.cookies];save();
if(!state.completed.includes('configured')){
  // The browser uses these same serializers and preset values; UI coverage is separate evidence.
  const initial=await admin.request(base+'/settings');
  const settings={auctionName:'NPL Mock Auction',maxSquadSize:11,purseLimit:100000,minMale:0,minFemale:3,playerBasePrice:5000,bidIncrement:1000,timerSeconds:30,customRules:'Synthetic one-over verification; not a historical NPL format',maximumBid:15000,maximumPremiumPurchases:4,captainPreassignmentRequired:true,expectedVersion:initial.version};
  const configured=await admin.request(base+'/settings','PUT',settings);state.aid=configured.auctionId;save();
  await admin.request(base+'/settings','PUT',settings,409);
  const beforeInvalid=await admin.request(base+'/settings');
  const invalid=await admin.request(base+'/settings','PUT',{...settings,minFemale:12,expectedVersion:beforeInvalid.version},400);assert(invalid.message);
  assert.deepEqual(await admin.request(base+'/settings'),beforeInvalid);
  const existing=await admin.request(base+'/competition/configuration');
  const competition={expectedConfigurationVersion:existing.configurationVersion,configuredTeamCount:4,minimumTeamCount:4,maximumTeamCount:4,minimumRosterSize:11,maximumRosterSize:11,playingSideSize:11,maximumSubstitutes:0,numberOfLegs:1,playoffFormat:'TOP_TWO_FINAL',roundRobinMode:'SINGLE',pointsPerWin:2,pointsPerLoss:0,pointsPerTie:1,pointsPerNoResult:1,tieBreakerPriority:['POINTS','NET_RUN_RATE','HEAD_TO_HEAD','NAME'],qualifyingTeamCount:2,venues:['Mock Aurora Cricket Ground'],leagueStartAt:'2026-11-15T04:00:00Z',fixtureSpacingDays:1,manualDateEditing:true,timezone:'Asia/Kolkata',presetCode:'CUSTOM_CHAMPIONSHIP'};
  await admin.request(base+'/admin/competition/configuration','PUT',competition);
  const cricket=await admin.request(base+'/sport-configuration/cricket');const {version,status,...fields}=cricket;
  await admin.request(base+'/sport-configuration/cricket','PUT',{...fields,expectedVersion:version,presetCode:'CUSTOM',playersPerSide:11,oversPerInnings:1,maximumOversPerBowler:1,powerplayRanges:'1-1',tiePolicy:'TIE'});
  const loaded=await admin.request(base+'/settings');
  for(const [key,value] of Object.entries(settings))if(key!=='expectedVersion')assert.equal(loaded[key],value,key);
  const policy=await admin.request(base+'/administrative-configuration/auction');assert.equal(policy.teamBudget,100000);assert.equal(policy.maximumBid,15000);
  assert.equal((await admin.request(base)).status,'ACTIVE');
  check('settings persistence, invalid transaction rollback, stale version and competition separation');checkpoint('configured');
}
const auction=`${base}/auctions/${state.aid}`;
if(!state.completed.includes('teams')){
  const teamDefinitions=[['NPL Thunder','NPT','#2563EB','Mock Arlo Quill'],['NPL Warriors','NPW','#DC2626','Mock Bram Vireo'],['NPL Strikers','NPS','#16A34A','Mock Cyra Flint'],['NPL Titans','NTT','#9333EA','Mock Daro Wren']];
  for(let i=0;i<4;i++){
    if(!state.captains[i]){
      const [teamName,shortCode,primaryColor,captainFullName]=teamDefinitions[i];
      const created=await admin.request(base+'/admin/teams','POST',{teamName,shortCode,primaryColor,captainFullName,captainEmail:`captain-${randomUUID()}@example.invalid`,captainMobile:`000000020${i}`});
      state.captains[i]={...created,email:created.captainEmail,password:secret()};save();
    }
    const c=state.captains[i];if(!c.activated)await activate(c);
    const path=`${base}/admin/teams/${c.teamId}/captains/${c.captainUserId}/player`;
    const request={gender:'MALE',playerReference:`MOCK-CAP-${i}`};
    const player=await admin.request(path,'PUT',request);c.playerId=player.id;save();
    assert.equal(player.status,'PREASSIGNED');assert.equal(player.teamId,c.teamId);assert(!player.soldPrice);
    assert.equal((await admin.request(path,'PUT',request)).id,player.id);
  }
  const teams=await admin.request(base+'/teams');assert.equal(teams.length,4);assert(teams.every(t=>t.purseRemaining===100000&&t.totalPlayers===1));
  check('four activated captains, male preassigned players, idempotent assignment and untouched opening purses');checkpoint('teams');
}
const captains=await Promise.all(state.captains.map(async c=>({...c,session:await login(c)})));
if(!state.completed.includes('imported')){
  // Packaged Windows Python virtualizes LocalAppData. Synthetic XLSX fixtures use ignored tmp;
  // credentials and activation/session state remain in the protected private directory.
  const fixtureDir=resolve('tmp/npl-mock/fixtures');mkdirSync(fixtureDir,{recursive:true});
  const template=await admin.raw(base+'/admin/players/imports/template');assert.equal(template.status,200);
  writeFileSync(join(fixtureDir,'downloaded-template.xlsx'),Buffer.from(await template.arrayBuffer()));
  execFileSync('python',[resolve('tests/npl-workbook.py'),fixtureDir,'--full'],{stdio:'pipe'});
  async function preview(file){const form=new FormData();form.set('file',new Blob([readFileSync(join(fixtureDir,file))],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}),file);return admin.request(base+'/admin/players/imports','POST',form);}
  for(const file of ['invalid-row.xlsx','duplicate-reference.xlsx']){
    const invalid=await preview(file);assert(invalid.invalid>0);await admin.request(`${base}/admin/players/imports/${invalid.importId}/confirm`,'POST',{checksum:invalid.checksum,allowUnreferenced:false},409);
  }
  const previewed=state.importPreview??await preview('forty-players.xlsx');assert.equal(previewed.invalid,0,JSON.stringify(previewed.rows));
  state.importPreview={importId:previewed.importId,checksum:previewed.checksum,invalid:previewed.invalid};save();
  const imported=await admin.request(`${base}/admin/players/imports/${previewed.importId}/confirm`,'POST',{checksum:previewed.checksum,allowUnreferenced:false});assert.equal(imported.imported,40);
  const retry=await admin.request(`${base}/admin/players/imports/${previewed.importId}/confirm`,'POST',{checksum:previewed.checksum,allowUnreferenced:false});assert.equal(retry.imported,40);
  const players=await admin.request(base+'/players');assert.equal(players.length,44);assert.equal(players.filter(p=>p.status==='AVAILABLE').length,40);assert.equal(players.filter(p=>p.status==='AVAILABLE'&&p.gender==='FEMALE').length,12);assert.equal(new Set(players.map(p=>p.playerReference)).size,44);
  check('downloaded XLSX template, preview/confirm/retry, invalid rows and duplicates, 44 identities and 12 imported women');checkpoint('imported');
}
if(!state.photoUrl){
  const fixtureDir=resolve('tmp/npl-mock/fixtures');
  if(!existsSync(join(fixtureDir,'synthetic-avatar.png')))execFileSync('python',[resolve('tests/npl-workbook.py'),fixtureDir,'--full'],{stdio:'pipe'});
  const player=(await admin.request(base+'/players')).find(p=>p.playerReference==='MOCK-EMP-0-2');
  const form=new FormData();form.set('file',new Blob([readFileSync(join(fixtureDir,'synthetic-avatar.png'))],{type:'image/png'}),'synthetic-avatar.png');
  const photo=await admin.request(`${base}/admin/players/${player.id}/photo`,'PUT',form);
  const bytes=await admin.raw(photo.photoUrl);assert.equal(bytes.status,200);state.photoUrl=photo.photoUrl;state.photoHash=createHash('sha256').update(Buffer.from(await bytes.arrayBuffer())).digest('hex');save();
  assert.equal((await new Session().raw(photo.photoUrl)).status,401);
  assert.equal((await captains[0].session.raw(photo.photoUrl)).status,200);
  check('synthetic player photograph normalized, authorized and anonymous access rejected');
}
if(!state.completed.includes('auction')){
  const all=await admin.request(base+'/players');
  const expectedLiveSales=all.filter(p=>p.status!=='SOLD'&&p.status!=='PREASSIGNED').length;
  let current=await admin.request(auction+'/state');
  // Five independent authenticated subscriptions; rebuild subscriptions after a resumed run.
  const observers=await Promise.all([admin,...captains.map(c=>c.session)].map(s=>observeAuction(s,state.cid,state.aid)));
  try{
    if(!state.completed.includes('auction-preflight')){
      await captains[0].session.request(base+'/admin/teams','POST',{teamName:'Unauthorized Mock Team',shortCode:'NOPE',captainFullName:'Mock Unauthorized',captainEmail:'unauthorized@example.invalid',captainMobile:'0000000999'},403);
      await captains[0].session.request(`/championships/${randomUUID()}/players`,'GET',undefined,403);
      await captains[0].session.request(`${base}/squad-confirmations/teams/${captains[1].teamId}/confirm`,'POST',{},403);
      await admin.request(base+'/admin/competition/league/generate','POST',undefined,409);
      await admin.request(`${base}/squad-confirmations/teams/${captains[0].teamId}/lock`,'POST',{},409);
      await captains[0].session.request(`${base}/squad-confirmations/teams/${captains[0].teamId}/confirm`,'POST',{},409);
      if(current.status!=='RUNNING')current=await admin.request(auction+'/admin/start','POST');
      assert.equal(current.status,'RUNNING');assert.equal(current.timerSeconds,30);assert(Date.parse(current.bidDeadline)>Date.now());
      const playerId=current.currentPlayer.id,teamId=captains[0].teamId;
      for(const amount of [4000,15001,16000,5500])await captains[0].session.request(auction+'/bid','POST',{playerId,teamId,amount},400);
      await captains[0].session.request(auction+'/bid','POST',{playerId,teamId:captains[1].teamId,amount:5000},403);
      // Real pending bid and undo; neither deducts money.
      await captains[0].session.request(auction+'/bid','POST',{playerId,teamId,amount:5000});
      const concurrent=await Promise.all([0,1].map(()=>captains[0].session.raw(auction+'/bid','POST',{playerId,teamId,amount:6000})));
      assert.deepEqual(concurrent.map(r=>r.status).sort(),[200,400]);
      await admin.request(auction+'/admin/undo','POST');
      await admin.request(auction+'/admin/undo','POST');
      assert.equal((await admin.request(auction+'/state')).teams.find(t=>t.id===teamId).purseRemaining,100000);
      await admin.request(auction+'/admin/unsold','POST');assert.equal((await admin.request(base+'/players')).find(p=>p.id===playerId).status,'UNSOLD');
      await admin.request(auction+'/admin/reset-unsold','POST');
      await admin.request(auction+'/admin/pause','POST');
      check('roles, wrong championship, fixture/squad prerequisites, timer, illegal prices, team binding, pending undo, unsold/reauction');checkpoint('auction-preflight');
    }
    current=await admin.request(auction+'/state');if(current.status!=='RUNNING'&&current.status!=='COMPLETED')await admin.request(auction+'/admin/resume','POST');
    for(let team=0;team<4;team++)for(let n=0;n<10;n++){
      const player=(await admin.request(base+'/players')).find(p=>p.playerReference===`MOCK-EMP-${team}-${n}`);
      if(player.status==='SOLD'){assert.equal(player.teamId,captains[team].teamId);assert.equal(player.soldPrice,n<4?15000:5000);if(!state.sales.some(s=>s.playerId===player.id)){state.sales.push({playerId:player.id,reference:player.playerReference,teamId:player.teamId,amount:player.soldPrice});save();}continue;}
      current=await admin.request(auction+'/state');
      if(current.currentPlayer?.id!==player.id)current=await admin.request(auction+'/admin/set-current','POST',{playerId:player.id});
      if(current.status!=='RUNNING')await admin.request(auction+'/admin/resume','POST');
      const amount=n<4?15000:5000,teamId=captains[team].teamId;
      if(n===4)await captains[team].session.request(auction+'/bid','POST',{playerId:player.id,teamId,amount:15000},400);
      if(team>0&&n===0)await captains[0].session.request(auction+'/bid','POST',{playerId:player.id,teamId:captains[0].teamId,amount:5000},400);
      if(n===7){
        const male=all.find(p=>p.playerReference===`MOCK-EMP-${(team+1)%4}-0`&&p.status!=='SOLD');
        if(male&&(await admin.request(base+'/players')).find(p=>p.id===male.id).status!=='SOLD'){
          await admin.request(auction+'/admin/set-current','POST',{playerId:male.id});
          const rejected=await captains[team].session.request(auction+'/bid','POST',{playerId:male.id,teamId,amount:5000},400);assert.match(rejected.message,/gender/);
          await admin.request(auction+'/admin/set-current','POST',{playerId:player.id});
        }
      }
      // Use every legal increment, alternating two captains. Losing bids do not spend purse.
      const latest=await admin.request(auction+'/state');
      for(let bid=latest.highestBid?latest.highestBid.amount+1000:5000;bid<=amount;bid+=1000){
        const bidder=bid===amount||((bid-5000)/1000)%2===0?team:(team+1)%4;
        // The other team may be full/premium-capped near the end; self raises are legal.
        const other=current.teams.find(t=>t.id===captains[bidder].teamId);
        const chosen=bidder!==team&&other.totalPlayers<11?bidder:team;
        await captains[chosen].session.request(auction+'/bid','POST',{playerId:player.id,teamId:captains[chosen].teamId,amount:bid});
      }
      if(process.env.NPL_MOCK_STOP_AFTER===`bid-${team*10+n+1}`){await admin.request(auction+'/admin/pause','POST');checkpoint(`bid-${team*10+n+1}`);}
      if(team===0&&n===0){
        const sales=await Promise.all([admin.raw(auction+'/admin/sell','POST'),admin.raw(auction+'/admin/sell','POST')]);assert.deepEqual(sales.map(r=>r.status).sort(),[200,404]);
      }else await admin.request(auction+'/admin/sell','POST');
      const sold=(await admin.request(base+'/players')).find(p=>p.id===player.id);assert.equal(sold.status,'SOLD');assert.equal(sold.soldPrice,amount);assert.equal(sold.teamId,teamId);
      if(team===0&&n===0){
        await admin.request(auction+'/admin/set-current','POST',{playerId:player.id},400);
        await captains[0].session.request(auction+'/bid','POST',{playerId:player.id,teamId,amount:5000},400);
      }
      if(!state.sales.some(s=>s.playerId===player.id))state.sales.push({playerId:player.id,reference:player.playerReference,teamId,amount});save();
      console.log(`SALE ${state.sales.length}/40 ${player.playerReference} ${captains[team].shortCode} INR ${amount}`);
      if(process.env.NPL_MOCK_STOP_AFTER===`sale-${state.sales.length}`){await admin.request(auction+'/admin/pause','POST');checkpoint(`sale-${state.sales.length}`);}
    }
    current=await admin.request(auction+'/state');assert.equal(current.status,'COMPLETED');
    assert(current.teams.every(t=>t.totalPlayers===11&&t.purseRemaining===10000&&t.femaleCount===3));
    assert(current.teamEligibility.every(t=>t.premiumPurchases===4&&t.squadSlotsRemaining===0));
    const roster=await admin.request(base+'/players');assert.equal(roster.filter(p=>p.status==='SOLD').length,40);assert.equal(roster.filter(p=>p.status==='PREASSIGNED').length,4);
    await captains[0].session.request(auction+'/bid','POST',{playerId:roster[0].id,teamId:captains[0].teamId,amount:5000},400);
    for(const observer of observers){await observer.waitFor(e=>e.type==='STATE'&&e.data.status==='COMPLETED');await observer.waitFor(()=>observer.messages.filter(e=>e.type==='PLAYER_SOLD').length>=expectedLiveSales);const sales=observer.messages.filter(e=>e.type==='PLAYER_SOLD');assert.equal(sales.length,expectedLiveSales);assert.equal(new Set(sales.map(e=>e.data.saleId)).size,expectedLiveSales);const revisions=observer.messages.filter(e=>e.type==='STATE').map(e=>e.data.revision);assert(revisions.every((r,i)=>i===0||r>=revisions[i-1]));}
    state.websocket={subscribers:5,eventCounts:observers.map(o=>o.messages.length),saleEventCounts:observers.map(o=>o.messages.filter(e=>e.type==='PLAYER_SOLD').length),duplicateSaleEvents:0};
    check('40 purchases, 16 premium/24 regular, four exact quotas/purses and five live WebSocket subscribers',state.websocket);checkpoint('auction');
  }finally{for(const o of observers)o.close();}
}
if(!state.completed.includes('locked')){
  for(const c of captains){const summary=await admin.request(base+'/squad-confirmations');const squad=summary.teams.find(t=>t.teamId===c.teamId||t.team?.id===c.teamId||t.id===c.teamId);if(!squad||!['CONFIRMED','LOCKED'].includes(squad.status))await c.session.request(`${base}/squad-confirmations/teams/${c.teamId}/confirm`,'POST',{});}
  const locked=await admin.request(base+'/squad-confirmations/lock-all','POST',{});assert(locked.allSquadsLocked);assert.equal(locked.lockedTeams,4);
  check('four captain confirmations and administrator squad locks');checkpoint('locked');
}
let overview=await admin.request(base+'/competition');
if(!overview.fixtures.length)overview=await admin.request(base+'/admin/competition/league/generate','POST');
assert.equal(overview.fixtures.filter(f=>f.stageType!=='FINAL').length,6);
assert.equal(new Set(overview.fixtures.filter(f=>f.stageType!=='FINAL').map(f=>[f.teamA.id,f.teamB.id].sort().join(':'))).size,6);
const roster=await admin.request(base+'/players');
async function score(match){
  const path=`${base}/cricket/matches/${match.id}`;
  const loaded=await admin.raw(path);
  assert([200,404].includes(loaded.status));
  // The engine creates its state on toss/start, so an unstarted fixture returns 404.
  let current=loaded.status===404?{innings:[]}:await loaded.json();
  if(current.resultText&&current.innings.length===2&&current.innings.at(-1).status!=='LIVE')return current;
  // A > B > C > D by declared team order, independently of alphabetic names.
  const [winner,loser]=[match.teamA,match.teamB].sort((a,b)=>captains.findIndex(c=>c.teamId===a.id)-captains.findIndex(c=>c.teamId===b.id));
  if(!current.innings.length){
    for(const team of [winner,loser]){
      const members=roster.filter(p=>p.teamId===team.id),captain=members.find(p=>p.status==='PREASSIGNED');assert(captain);
      state.lockedXis??={};state.lockedXis[match.id]??=[];
      if(!state.lockedXis[match.id].includes(team.id)){
        await admin.request(`${path}/playing-xi/${team.id}`,'PUT',{playerIds:members.map(p=>p.id),captainId:captain.id,wicketkeeperId:members.find(p=>p.id!==captain.id).id});
        state.lockedXis[match.id].push(team.id);save();
      }
    }
    await admin.request(path+'/toss','POST',{winnerTeamId:loser.id,decision:'BAT'});
  }
  for(let i=0;i<2;i++){
    const team=i===0?loser:winner;
    current=await admin.request(path);
    if(!current.innings[i])current=await admin.request(path+'/innings','POST',{battingTeamId:team.id});
    let innings=current.innings[i];
    const batting=roster.filter(p=>p.teamId===team.id),bowling=roster.find(p=>p.teamId===(i===0?winner:loser).id);
    while(innings.status==='LIVE'){
      // Persist the idempotency key before sending; an uncertain response can safely retry.
      const key=`${match.id}:${innings.id}:${innings.deliveries.length}`;
      state.deliveryKeys??={};state.deliveryKeys[key]??=randomUUID();save();
      current=await admin.request(`${path}/innings/${innings.id}/deliveries`,'POST',{idempotencyKey:state.deliveryKeys[key],strikerPlayerId:innings.nextStrikerPlayerId||batting[0].id,nonStrikerPlayerId:innings.nextNonStrikerPlayerId||batting[1].id,bowlerPlayerId:bowling.id,batterRuns:i===0?1:6,wides:0,noBalls:0,byes:0,legByes:0,penaltyRuns:0,wicket:false});
      innings=current.innings[i];
    }
  }
  assert(current.resultText);state.matches??={};state.matches[match.id]=current;save();return current;
}
if(!state.completed.includes('league')){
  await admin.request(base+'/admin/competition/playoffs/generate','POST',undefined,409);
  for(const c of captains)assert.equal(overview.fixtures.filter(f=>f.stageType!=='FINAL'&&(f.teamA.id===c.teamId||f.teamB.id===c.teamId)).length,3);
  for(const match of overview.fixtures.filter(f=>f.stageType!=='FINAL')){await score(match);console.log(`MATCH league ${match.matchNumber} completed`);}
  const standings=await admin.request(base+'/analytics/standings?penalties=false');assert.deepEqual(standings.map(s=>s.totalPoints),[6,4,2,0]);assert(standings.every(s=>s.played===3&&Number.isFinite(s.netRunRate)));
  for(const [i,nrr]of [30,54/7,-54/7,-30].entries())assert(Math.abs(standings[i].netRunRate-Math.round(nrr*1000)/1000)<1e-7);
  state.standings=standings;check('six completed league matches, unique pairings, 6/4/2/0 points and independently calculated NRR');checkpoint('league');
}
if(!state.completed.includes('final')){
  overview=await admin.request(base+'/competition');
  if(!overview.fixtures.some(f=>f.stageType==='FINAL'))overview=await admin.request(base+'/admin/competition/playoffs/generate','POST');
  assert.equal(overview.fixtures.length,7);assert.equal(overview.fixtures.filter(f=>f.stageType==='FINAL').length,1);assert(!overview.fixtures.some(f=>f.stageType==='SEMI_FINAL'||f.stageType==='THIRD_PLACE'));
  state.final=overview.fixtures.find(f=>f.stageType==='FINAL');save();
  assert.deepEqual([state.final.teamA.id,state.final.teamB.id].sort(),state.standings.slice(0,2).map(s=>s.team.id).sort());
  state.finalState=await score(state.final);save();
  overview=await admin.request(base+'/competition');assert.equal(overview.configuration.champion.id,captains[0].teamId);assert.equal(overview.configuration.runnerUp.id,captains[1].teamId);
  state.tournament=overview;check('top-two final completed; NPL Thunder champion and NPL Warriors runner-up');checkpoint('final');
}
if(!state.completed.includes('certificates')){
  const certificateBase=base+'/admin/certificates';
  state.approval=await admin.request(certificateBase+'/approvals','POST',{justification:'Seven synthetic cricket matches completed through actual delivery scoring'});save();
  const eligible=await admin.request(certificateBase+'/eligibility?category=PARTICIPATION&recipientType=PLAYER');assert.equal(eligible.recipients.length,44);
  state.certificates??=[];
  for(const [category,recipientType,recipientId] of [['PARTICIPATION','PLAYER',captains[0].playerId],['WINNER','TEAM',captains[0].teamId],['RUNNER_UP','TEAM',captains[1].teamId]]){
    const eligibility=await admin.request(`${certificateBase}/eligibility?category=${category}&recipientType=${recipientType}`);assert(eligibility.recipients.some(r=>r.id===recipientId));
    const issue={requestId:randomUUID(),approvalId:state.approval.id,category,recipientType,recipientId};
    const issued=await admin.request(certificateBase,'POST',issue);assert.equal(issued.status,'ISSUED');
    assert.equal((await admin.request(certificateBase,'POST',issue)).certificate.id,issued.certificate.id);
    const pdf=await admin.raw(`${certificateBase}/${issued.certificate.id}/pdf`);assert.equal(pdf.status,200);const bytes=Buffer.from(await pdf.arrayBuffer());assert.equal(bytes.subarray(0,5).toString(),'%PDF-');
    writeFileSync(join(evidenceDir,`mock-${category.toLowerCase()}.pdf`),bytes);state.certificates.push(issued);save();
    await captains[0].session.request(`${certificateBase}/${issued.certificate.id}/pdf`,'GET',undefined,403);
    const anonymous=new Session();await anonymous.request(`${certificateBase}/${issued.certificate.id}/pdf`,'GET',undefined,401);
  }
  await admin.request(certificateBase,'POST',{requestId:randomUUID(),approvalId:state.approval.id,category:'WINNER',recipientType:'TEAM',recipientId:captains[3].teamId},409);
  const revoked=await admin.request(`${certificateBase}/${state.certificates[0].certificate.id}/revoke`,'POST',{reason:'Synthetic verification of the certificate revocation workflow'});assert.equal(revoked.status,'REVOKED');
  state.certificates[0]=revoked;check('44 participant eligibilities, winner/runner-up certificates, PDF bytes, idempotency, authorization and revocation');checkpoint('certificates');
}
if(process.env.NPL_MOCK_SUPPLEMENTAL==='true'&&!state.completed.includes('supplemental')){
  process.env.NPL_TEST_EMAIL=bootstrap.email;process.env.NPL_TEST_PASSWORD=bootstrap.password;
  await import('./npl-settings-api.mjs');await import('./npl-reserve-api.mjs');
  await import('./npl-cricket-edge-api.mjs');
  delete process.env.NPL_TEST_EMAIL;delete process.env.NPL_TEST_PASSWORD;
  checkpoint('supplemental');
}
// Idempotent lifecycle verification preserves the original qualification snapshot and seeds.
state.tournament=await admin.request(base+'/admin/competition/playoffs/generate','POST');
assert.equal(state.tournament.stages.find(s=>s.type==='LEAGUE').status,'FROZEN');
assert.equal(state.tournament.stages.find(s=>s.type==='FINAL').status,'COMPLETED');
assert.equal(state.tournament.qualifications.length,2);
assert.equal((await admin.request(base)).status,'COMPLETED');
state.standings=await admin.request(base+'/analytics/standings?penalties=false');
for(const match of state.tournament.fixtures){state.matches[match.id]=await admin.request(`${base}/cricket/matches/${match.id}`);}
state.finalState=state.matches[state.final.id];save();
// Only sanitized application state is committed. Credentials and sessions stay in privateDir.
for(const issued of state.certificates){
  const pdf=await admin.raw(`${base}/admin/certificates/${issued.certificate.id}/pdf`);assert.equal(pdf.status,200);
  writeFileSync(join(evidenceDir,`mock-${issued.certificate.category.toLowerCase()}.pdf`),Buffer.from(await pdf.arrayBuffer()));
}
const {adminCookies,admin:privateAdmin,captains:privateCaptains,deliveryKeys,importPreview,...publicState}=state;
writeFileSync(join(evidenceDir,'full-mock-result.json'),JSON.stringify({...publicState,captains:privateCaptains.map(({password,setupPath,...c})=>c)},null,2));
console.log('PASS full NPL auction/tournament checkpoints and final API reconciliation.');
