import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
const runtime=JSON.parse(await fs.readFile('tmp/phase6-runtime.json','utf8'));
assert.equal(execFileSync('docker',['inspect','--format','{{ index .Config.Labels "purpose" }}',runtime.container],{encoding:'utf8'}).trim(),'play-neml-phase6','Only the isolated Phase 6 environment is permitted');
const api='http://localhost:58096/api';
class Session{
 cookies=new Map();
 async request(method,path,body){let headers={Cookie:[...this.cookies].map(([k,v])=>`${k}=${v}`).join(';')};
  if(method!=='GET'){const csrf=await this.request('GET','/auth/csrf');headers={Cookie:[...this.cookies].map(([k,v])=>`${k}=${v}`).join(';'),'X-XSRF-TOKEN':csrf.token};}
  const response=await fetch(api+path,{method,headers:{...headers,...(body instanceof FormData?{}:{'Content-Type':'application/json'})},...(body===undefined?{}:{body:body instanceof FormData?body:JSON.stringify(body)}),signal:AbortSignal.timeout(60000)});
  for(const c of response.headers.getSetCookie()){const pair=c.split(';')[0],i=pair.indexOf('=');this.cookies.set(pair.slice(0,i),pair.slice(i+1));}
  const data=await response.json().catch(()=>null);assert.ok([200,204].includes(response.status),`${method} ${path}: ${response.status} ${data?.message||''}`);return data;
 }
}
const archive=JSON.parse(await fs.readFile('tmp/phase6-demo-accounts.json','utf8'));
const archiveAdmin=new Session();await archiveAdmin.request('POST','/auth/login',archive.admin);
const archiveRoot=`/championships/${archive.cid}`;
assert.match((await archiveAdmin.request('GET',archiveRoot)).name,/^Phase 6 /);
const approval=await archiveAdmin.request('POST',archiveRoot+'/admin/certificates/approvals',{justification:'Synthetic demonstration results reviewed for leadership walkthrough'});
const history=await archiveAdmin.request('GET',archiveRoot+'/admin/certificates');const certificates=[];
for(const [category,type]of [['WINNER','TEAM'],['RUNNER_UP','TEAM'],['PARTICIPATION','PLAYER']]){
 const eligible=await archiveAdmin.request('GET',archiveRoot+`/admin/certificates/eligibility?category=${category}&recipientType=${type}`);
 for(const recipient of eligible.recipients){const existing=history.items.find(c=>c.status==='ISSUED'&&c.certificate.category===category&&c.certificate.recipient.id===recipient.id);const issued=existing||await archiveAdmin.request('POST',archiveRoot+'/admin/certificates',{requestId:crypto.randomUUID(),approvalId:approval.id,category,recipientType:type,recipientId:recipient.id});certificates.push({reference:issued.certificate.reference,category,url:issued.certificate.verificationUrl});}
}
let live;
try{live=JSON.parse(await fs.readFile('tmp/phase6-demo-access.json','utf8')).live;}catch{}
if(!live){
 const platform=new Session();await platform.request('POST','/auth/login',{email:runtime.bootstrapEmail,password:runtime.bootstrapPassword});
 const account={email:`demo-${crypto.randomUUID()}@example.invalid`,password:crypto.randomUUID()};
 const created=await platform.request('POST','/championships',{name:'Phase 6 Leadership Live Arena',sportType:'BADMINTON',isPublic:true,adminFullName:'Demo Championship Administrator',adminEmail:account.email,adminMobile:'9999999999'});
 const admin=new Session();await admin.request('POST','/auth/activate',{token:created.admin.setupPath.split('=')[1],password:account.password});await admin.request('POST','/auth/login',account);
 const root=`/championships/${created.championship.id}`;const settings=await admin.request('PUT',root+'/settings',{auctionName:'Leadership demonstration auction',maxSquadSize:1,purseLimit:10000,minMale:0,minFemale:0,playerBasePrice:100,bidIncrement:10,timerSeconds:300,customRules:'{}'});
 const config=await admin.request('GET',root+'/competition/configuration');
 await admin.request('PUT',root+'/admin/competition/configuration',{expectedConfigurationVersion:config.configurationVersion,presetCode:'CUSTOM',configuredTeamCount:2,minimumTeamCount:2,maximumTeamCount:2,minimumRosterSize:1,maximumRosterSize:1,playingSideSize:1,maximumSubstitutes:0,numberOfLegs:1,playoffFormat:'TOP_TWO_FINAL',roundRobinMode:'SINGLE',pointsPerWin:3,pointsPerLoss:0,qualifyingTeamCount:2,tieBreakerPriority:['MATCHES_WON','NAME'],venues:['NEML Demo Court'],leagueStartAt:'2026-10-29T10:00:00Z',fixtureSpacingDays:1,manualDateEditing:true});
 const rules=await admin.request('GET',root+'/sport-configuration/badminton');
 await admin.request('PUT',root+'/sport-configuration/badminton',{expectedVersion:rules.version,presetCode:'CUSTOM',maximumAppearancesPerPlayer:1,multipleRubberCategoriesAllowed:false,lineupChangesAfterStartAllowed:false,tieHandling:'ADMINISTRATIVE_WIN',walkoverPolicy:rules.walkoverPolicy,retirementPolicy:rules.retirementPolicy,lineupVisibilityPolicy:rules.lineupVisibilityPolicy,rubbers:[{rubberCode:'MENS_SINGLES',displayLabel:'Open Singles',shortLabel:'OS',displayOrder:1,participantCountPerSide:1,genderRule:'OPEN',bestOfGames:1,pointsPerGame:21,winBy:2,scoreCap:30,enabled:true}]});
 await admin.request('PUT',root+'/sport-configuration/quotas',[]);
 const captains=[];
 for(const [name,code]of [['Demo Aces','DACE'],['Demo Blazers','DBLZ']]){const captainAccount={email:`demo-captain-${crypto.randomUUID()}@example.invalid`,password:crypto.randomUUID()};const team=await admin.request('POST',root+'/admin/teams',{teamName:name,shortCode:code,captainFullName:`${name} Captain`,captainEmail:captainAccount.email,captainMobile:'9999999999'});const captain=new Session();await captain.request('POST','/auth/activate',{token:team.setupPath.split('=')[1],password:captainAccount.password});captains.push({account:captainAccount,teamId:team.teamId,name});}
 const form=new FormData();form.set('file',new Blob([await fs.readFile('docs/phase-5/fixtures/players.xlsx')]),'demo-players.xlsx');const preview=await admin.request('POST',root+'/admin/players/imports',form);assert.equal(preview.invalid,0);await admin.request('POST',root+`/admin/players/imports/${preview.importId}/confirm`,{checksum:preview.checksum});
 const players=await admin.request('GET',root+'/players');const photo=new FormData();photo.set('file',new Blob([await fs.readFile('docs/phase-6/fixtures/portrait.png')],{type:'image/png'}),'portrait.png');await admin.request('PUT',root+`/admin/players/${players[0].id}/photo`,photo);
 live={admin:account,cid:created.championship.id,aid:settings.auctionId,roomCode:created.championship.roomCode,captains};
}
await fs.writeFile('tmp/phase6-demo-access.json',JSON.stringify({archive:{admin:archive.admin,captain:archive.captain,cid:archive.cid,roomCode:archive.roomCode,certificates},live},null,2));
console.log('Synthetic archive certificates and a separate interactive live arena are ready. Credentials remain only in ignored tmp/phase6-demo-access.json.');
console.log(JSON.stringify({archiveCertificates:certificates.length,liveArenaPrepared:true,privateAccountFile:'tmp/phase6-demo-access.json'}));
