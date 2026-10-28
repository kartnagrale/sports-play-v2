import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFile} from 'node:child_process';
import {promisify} from 'node:util';
const execute=promisify(execFile),contention=[];
const mode=process.argv[2]||'baseline',base='http://localhost:58096/api';
const config=JSON.parse(await fs.readFile('tmp/phase6-runtime.json','utf8'));
const evidence='docs/phase-6/evidence';await fs.mkdir(evidence,{recursive:true});
class Session {
 cookies=new Map();csrf='';constructor(bearer){this.bearer=bearer;}
 async raw(method,path,body,headers={}){
  const response=await fetch(base+path,{method,headers:{Cookie:[...this.cookies].map(([k,v])=>`${k}=${v}`).join(';'),...(this.bearer?{Authorization:`Bearer ${this.bearer}`} : {}),...(body instanceof FormData?{}:{'Content-Type':'application/json'}),...headers},...(body===undefined?{}:{body:body instanceof FormData?body:JSON.stringify(body)}),signal:AbortSignal.timeout(30000)});
  for(const cookie of response.headers.getSetCookie()){const pair=cookie.split(';')[0],i=pair.indexOf('=');this.cookies.set(pair.slice(0,i),pair.slice(i+1));}
  return {status:response.status,data:await response.json().catch(()=>null)};
 }
 async call(method,path,body){if(method!=='GET')this.csrf=(await this.raw('GET','/auth/csrf')).data.token;const r=await this.raw(method,path,body,method==='GET'?{}:{'X-XSRF-TOKEN':this.csrf});assert.ok([200,204].includes(r.status),`${method} ${path}: ${r.status} ${r.data?.message||''}`);return r.data;}
}
const sockets=[];let admin,cid,aid,sampler;const latencies={state:[],dashboard:[],bids:[],delivery:[]};let errors=0,expectedRejections=0;
const summary=values=>{const s=[...values].sort((a,b)=>a-b);return {count:s.length,p50Ms:+(s[Math.floor(s.length*.5)]||0).toFixed(2),p95Ms:+(s[Math.min(s.length-1,Math.floor(s.length*.95))]||0).toFixed(2),maxMs:+(s.at(-1)||0).toFixed(2)}};
async function connect(token){
 const ws=new WebSocket(base.replace('http','ws').replace('/api','/ws'));const probe={ws,events:[],connectMs:performance.now()};sockets.push(probe);
 await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('WebSocket connect timeout')),10000);ws.onerror=()=>{clearTimeout(timer);reject(Error('WebSocket error'));};ws.onopen=()=>ws.send(`CONNECT\naccept-version:1.2\nAuthorization:Bearer ${token}\n\n\0`);ws.onmessage=e=>{const raw=String(e.data);if(raw.startsWith('CONNECTED')){probe.connectMs=performance.now()-probe.connectMs;ws.send(`SUBSCRIBE\nid:auction\ndestination:/topic/championship/${cid}/auction/${aid}\n\n\0`);clearTimeout(timer);resolve();}if(raw.startsWith('MESSAGE')){const start=raw.indexOf('\n\n');try{probe.events.push({...JSON.parse(raw.slice(start+2).replace(/\0+$/,'')),receivedAt:performance.now()});}catch{errors++;}}};});return probe;
}
try{
 const platform=new Session();await platform.call('POST','/auth/login',{email:config.bootstrapEmail,password:config.bootstrapPassword});
 const email=`load-${crypto.randomUUID()}@example.invalid`,password=crypto.randomUUID();
 const created=await platform.call('POST','/championships',{name:`Phase 6 ${mode} measurement`,sportType:'BADMINTON',isPublic:true,adminFullName:'Synthetic Load Admin',adminEmail:email,adminMobile:'9999999999'});cid=created.championship.id;admin=new Session();await admin.call('POST','/auth/activate',{token:created.admin.setupPath.split('=')[1],password});await admin.call('POST','/auth/login',{email,password});
 const root=`/championships/${cid}`;aid=(await admin.call('PUT',root+'/settings',{auctionName:'Bounded performance scenario',maxSquadSize:3,purseLimit:10000000,minMale:0,minFemale:0,playerBasePrice:100,bidIncrement:10,timerSeconds:300,customRules:'{}'})).auctionId;
 const teams=[];for(const [name,code]of [['Load Aces','LDA'],['Load Blazers','LDB']])teams.push(await admin.call('POST',root+'/admin/teams',{teamName:name,shortCode:code,captainFullName:'Synthetic Captain',captainEmail:`load-captain-${crypto.randomUUID()}@example.invalid`,captainMobile:'9999999999'}));
 const form=new FormData();form.set('file',new Blob([await fs.readFile('docs/phase-6/fixtures/load-players.xlsx')]),'load-players.xlsx');const preview=await admin.call('POST',root+'/admin/players/imports',form);assert.equal(preview.invalid,0);const imported=await admin.call('POST',root+`/admin/players/imports/${preview.importId}/confirm`,{checksum:preview.checksum});assert.equal(imported.imported,500);
 let state=await admin.call('POST',root+`/auctions/${aid}/admin/start`);const player=state.currentPlayer.id;
 const viewerSession=new Session();const viewer=await viewerSession.call('POST','/championships/join-room',{roomCode:created.championship.roomCode});
 await Promise.all(Array.from({length:12},()=>connect(viewer.token)));await new Promise(r=>setTimeout(r,200));
 let sampling=false;
 sampler=setInterval(async()=>{if(sampling)return;sampling=true;try{const {stdout}=await execute('docker',['exec',config.container,'psql','-U','phase6','-d','phase6','-Atc',"select count(*) filter(where wait_event_type='Lock'),count(*) filter(where state='active') from pg_stat_activity where datname='phase6'"]);contention.push(stdout.trim().split('|').map(Number));}finally{sampling=false;}},200);
 // Warm both read paths before capturing bounded request samples.
 for(let i=0;i<3;i++){await admin.call('GET',root+`/auctions/${aid}/state`);await admin.call('GET',root+'/dashboard');}
 for(let round=0;round<10;round++)await Promise.all(Array.from({length:4},async()=>{const start=performance.now();const r=await admin.raw('GET',root+`/auctions/${aid}/state`);latencies.state.push(performance.now()-start);if(r.status!==200)errors++;}));
 for(let round=0;round<3;round++)await Promise.all(Array.from({length:4},async()=>{const start=performance.now();const r=await admin.raw('GET',root+'/dashboard');latencies.dashboard.push(performance.now()-start);if(r.status!==200)errors++;}));
 let revision=state.revision;
 for(let round=0;round<4;round++){
  const amount=100+round*10;const start=performance.now();const csrf=(await admin.raw('GET','/auth/csrf')).data.token;
  const responses=await Promise.all(Array.from({length:8},async(_,i)=>{const began=performance.now();const r=await admin.raw('POST',root+`/auctions/${aid}/bid`,{playerId:player,teamId:teams[i%2].teamId,amount},{'X-XSRF-TOKEN':csrf});latencies.bids.push(performance.now()-began);return r;}));
  const accepted=responses.filter(r=>r.status===200);assert.equal(accepted.length,1);assert.ok(responses.every(r=>[200,400,409].includes(r.status)));expectedRejections+=7;
  assert.ok(accepted[0].data.revision>revision);revision=accepted[0].data.revision;
  const until=Date.now()+10000;while(sockets.some(p=>!p.events.some(e=>e.type==='STATE'&&e.data.revision===revision))&&Date.now()<until)await new Promise(r=>setTimeout(r,10));
  for(const probe of sockets){const event=probe.events.find(e=>e.type==='STATE'&&e.data.revision===revision);assert.ok(event,'Every connected viewer receives committed revision');latencies.delivery.push(event.receivedAt-start);}
 }
 const snapshot=await admin.call('GET',root+`/auctions/${aid}/state`);const winner=snapshot.highestBid.teamId,price=Number(snapshot.highestBid.amount);await admin.call('POST',root+`/auctions/${aid}/admin/sell`);
 const sold=await admin.call('GET',root+`/players/${player}`);assert.equal(sold.teamId,winner);assert.equal(Number(sold.soldPrice),price);const balance=await admin.call('GET',root+`/teams/${winner}`);assert.equal(Number(balance.purseRemaining),10000000-price);
 await new Promise(resolve=>{sockets[0].ws.onclose=resolve;sockets[0].ws.close();});
 await connect(viewer.token);await new Promise(r=>setTimeout(r,100));const recovered=await viewerSession.raw('GET',root+`/auctions/${aid}/state`,undefined,{Authorization:`Bearer ${viewer.token}`});assert.equal(recovered.status,200);assert.ok(recovered.data.revision>=revision);
 const result={mode,environment:{java:'17',heapMiB:512,postgres:'15',transport:'native WebSocket STOMP',host:'Windows local Docker Desktop',players:500,teams:2,viewerConnections:12,reconnects:1,readConcurrency:4,bidBurstConcurrency:8,bidBursts:4,warmups:3},latency:{state:summary(latencies.state),dashboard:summary(latencies.dashboard),bids:summary(latencies.bids),deliveryFromRequestStart:summary(latencies.delivery)},databaseContention:{sampleIntervalMs:200,samples:contention.length,maxObservedLockWaiters:Math.max(0,...contention.map(s=>s[0])),maxObservedActive:Math.max(0,...contention.map(s=>s[1]))},unexpectedErrors:errors,expectedBidRejections:expectedRejections,correctness:{exactlyOneEqualBidAccepted:true,monotonicAcceptedRevisions:true,allViewersReceivedRevisions:true,purseDeductedOnce:true,reconnectSnapshotRecovered:true},connect:summary(sockets.map(p=>p.connectMs)),measuredAt:new Date().toISOString()};
 await fs.writeFile(`${evidence}/performance-${mode}.json`,JSON.stringify(result,null,2));console.log(JSON.stringify(result));assert.equal(errors,0);
}finally{clearInterval(sampler);for(const p of sockets)p.ws.close();}
