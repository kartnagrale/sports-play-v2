import fs from 'node:fs/promises';
import http from 'node:http';
import crypto from 'node:crypto';
const base=process.env.PHASE1_API_URL || 'http://localhost:58092/api';
const out=process.env.PHASE1_EVIDENCE || 'docs/phase-1/evidence/runtime-security.json';
const results=[],sockets=[],secrets=[];
let publicRoom=null;
function check(name,pass,detail){results.push({name,pass,...(detail?{detail}: {})});if(!pass)console.error(name,detail || 'failed');}
class Session {
  cookies=new Map();csrf=null;
  constructor(bearer){this.bearer=bearer;}
  async raw(method,path,body,headers={}){
    const response=await fetch(base+path,{method,headers:{'Content-Type':'application/json',
      Cookie:[...this.cookies].map(([k,v])=>`${k}=${v}`).join(';'),...(this.bearer?{Authorization:`Bearer ${this.bearer}`} : {}),...headers},
      ...(body===undefined?{}:{body:JSON.stringify(body)}),signal:AbortSignal.timeout(15000)});
    for(const cookie of response.headers.getSetCookie()){
      const pair=cookie.split(';')[0],index=pair.indexOf('=');this.cookies.set(pair.slice(0,index),pair.slice(index+1));
    }
    return {status:response.status,data:await response.json().catch(()=>null),headers:response.headers};
  }
  async call(name,method,path,expected,body){
    const headers={};
    if(!['GET','HEAD','OPTIONS'].includes(method)){
      this.csrf = (await this.raw('GET','/auth/csrf')).data.token;
      headers['X-XSRF-TOKEN']=this.csrf;
    }
    const response=await this.raw(method,path,body,headers);
    check(name,[expected].flat().includes(response.status),{expected,status:response.status,
      ...(![expected].flat().includes(response.status)?{csrfMatchesCookie:this.csrf===this.cookies.get('XSRF-TOKEN'),cookieNames:[...this.cookies.keys()]}:{})});return response.data;
  }
  async login(name,email,password){secrets.push(password);const result=await this.call(name,'POST','/auth/login',200,{email,password});
    if(this.cookies.get('neml_auth'))secrets.push(this.cookies.get('neml_auth'));
    check(name+' uses cookie only',!result?.token&&!!this.cookies.get('neml_auth'));return result;}
}
async function socket(token){
  const ws=new WebSocket(base.replace('http','ws').replace('/api','/ws'));
  const probe={ws,frames:[],closed:false};sockets.push(ws);
  ws.onmessage=event=>probe.frames.push(String(event.data));ws.onclose=()=>probe.closed=true;
  await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=()=>reject(Error('WebSocket open failed'));});
  probe.wait=async(predicate,ms=4000)=>{const deadline=Date.now()+ms;while(Date.now()<deadline){const frame=probe.frames.find(predicate);if(frame)return frame;await new Promise(r=>setTimeout(r,30));}throw Error('Expected WebSocket frame missing');};
  probe.connect=()=>ws.send(`CONNECT\naccept-version:1.2\n${token?'Authorization:Bearer '+token+'\n':''}\n\0`);
  probe.rejected=async()=>{const deadline=Date.now()+3000;while(Date.now()<deadline){if(probe.closed||probe.frames.some(f=>f.startsWith('ERROR')))return true;await new Promise(r=>setTimeout(r,30));}return false;};
  return probe;
}
async function foreignOriginHandshake(){
  return new Promise((resolve,reject)=>{
    const url=new URL(base.replace('/api','/ws'));
    const request=http.get(url,{headers:{Origin:'https://foreign.example',Connection:'Upgrade',Upgrade:'websocket','Sec-WebSocket-Version':'13','Sec-WebSocket-Key':crypto.randomBytes(16).toString('base64')}},response=>{response.resume();resolve(response.statusCode);});
    request.on('upgrade',(response,transport)=>{transport.destroy();resolve(response.statusCode);});request.on('error',reject);
  });
}
try {
  const anonymous=new Session();
  await anonymous.call('health','GET','/health',200);
  await anonymous.call('anonymous staff list denied','GET','/championships',401);
  const missingCsrf=await anonymous.raw('POST','/auth/login',{email:process.env.APP_BOOTSTRAP_EMAIL,password:process.env.APP_BOOTSTRAP_PASSWORD});
  check('valid credentials without CSRF denied',[401,403].includes(missingCsrf.status)&&!missingCsrf.data?.user);
  check('foreign WebSocket origin denied',(await foreignOriginHandshake())===403);
  const invalid=new Session('invalid');await invalid.call('invalid JWT denied','GET','/championships',401);
  const superAdmin=new Session();const superLogin=await superAdmin.login('bootstrap login',process.env.APP_BOOTSTRAP_EMAIL,process.env.APP_BOOTSTRAP_PASSWORD);
  const encode=value=>Buffer.from(JSON.stringify(value)).toString('base64url');
  const expiredInput=encode({alg:'HS512',typ:'JWT'})+'.'+encode({sub:superLogin.user.id,cv:0,iat:Math.floor(Date.now()/1000)-120,exp:Math.floor(Date.now()/1000)-60});
  const expiredToken=expiredInput+'.'+crypto.createHmac('sha512',process.env.JWT_SECRET).update(expiredInput).digest('base64url');
  await new Session(expiredToken).call('expired signed JWT denied','GET','/championships',401);
  const navigation=await superAdmin.call('essential navigation seeded','GET','/navigation',200);
  check('platform menus available without demos',navigation.screens.some(s=>s.code==='CHAMPIONSHIP_LAUNCHPAD'));
  const championships=[];
  for(const sport of ['BADMINTON','CRICKET']){
    const suffix=crypto.randomBytes(4).toString('hex'),email=`${sport.toLowerCase()}-${suffix}@example.invalid`;
    const created=await superAdmin.call(`${sport} provisioning`,'POST','/championships',200,{name:`Phase 1 ${sport}`,sportType:sport,roomCode:`P1${suffix}`,isPublic:true,adminFullName:'Synthetic Admin',adminEmail:email,adminMobile:'9999999999'});
    check(`${sport} provisioning has no password`,!!created.admin.setupPath&&!('temporaryPassword' in created.admin));
    const cid=created.championship.id,admin=new Session(),token=created.admin.setupPath.split('=')[1],password=crypto.randomUUID();
    secrets.push(token);
    await admin.call(`${sport} inactive login denied`,'POST','/auth/login',401,{email,password});
    if(sport==='BADMINTON'){
      const replacement=await superAdmin.call('setup link renewal','POST',`/championships/${cid}/accounts/${created.admin.userId}/setup`,200);
      await admin.call('superseded setup denied','POST','/auth/activate',400,{token,password});
      created.admin.setupPath=replacement.setupPath;
    }
    const activation=created.admin.setupPath.split('=')[1];
    secrets.push(activation);
    await admin.call(`${sport} activate`,'POST','/auth/activate',204,{token:activation,password});
    await admin.call(`${sport} replay denied`,'POST','/auth/activate',400,{token:activation,password});
    await admin.login(`${sport} admin login`,email,password);
    await admin.call(`${sport} admin list`,'GET','/championships',200);
    await admin.call(`${sport} dashboard`,'GET',`/championships/${cid}/dashboard`,200);
    await admin.call(`${sport} configure auction`,'PUT',`/championships/${cid}/settings`,200,{auctionName:'Security regression',maxSquadSize:2,purseLimit:10000,minMale:0,minFemale:0,playerBasePrice:100,bidIncrement:10,timerSeconds:300,customRules:'{}'});
    const settings=await admin.call(`${sport} read settings`,'GET',`/championships/${cid}/settings`,200);
    const player=await admin.call(`${sport} manual player create`,'POST',`/championships/${cid}/admin/players`,200,{fullName:'Synthetic Player',gender:'MALE',basePrice:100,skillLevel:'Intermediate'});
    const joined=await anonymous.call(`${sport} public viewer join`,'POST','/championships/join-room',200,{roomCode:created.championship.roomCode});
    if(!publicRoom)publicRoom={roomCode:created.championship.roomCode,championshipId:cid};
    const viewer=new Session(joined.token);
    await viewer.call(`${sport} viewer staff list is controlled 403`,'GET','/championships',403);
    await viewer.call(`${sport} viewer room preserved`,'GET',`/championships/${cid}`,200);
    await viewer.call(`${sport} viewer players`,'GET',`/championships/${cid}/players`,200);
    await viewer.call(`${sport} viewer writes denied`,'POST',`/championships/${cid}/admin/players`,403,{fullName:'Forbidden',gender:'MALE'});
    await superAdmin.call(`${sport} super admin tenant write denied`,'POST',`/championships/${cid}/admin/players`,403,{fullName:'Forbidden',gender:'MALE'});
    championships.push({cid,aid:settings.auctionId,admin,viewer,viewerToken:joined.token,player,email,password});
  }
  const [a,b]=championships;
  await a.admin.call('foreign admin read denied','GET',`/championships/${b.cid}/players`,403);
  await a.viewer.call('foreign viewer read denied','GET',`/championships/${b.cid}`,403);
  const anon=await socket();anon.connect();check('anonymous CONNECT rejected',await anon.rejected());
  const anonymousSend=await socket();anonymousSend.ws.send(`SEND\ndestination:/topic/championship/${a.cid}/matches\n\nforged\0`);
  check('anonymous SEND before CONNECT rejected',await anonymousSend.rejected());
  const staffSend=await socket(superAdmin.cookies.get('neml_auth'));staffSend.connect();await staffSend.wait(f=>f.startsWith('CONNECTED'));
  staffSend.ws.send('SEND\ndestination:/queue/events\n\nforged\0');check('staff broker SEND rejected',await staffSend.rejected());
  const recipient=await socket(a.viewerToken);recipient.connect();await recipient.wait(f=>f.startsWith('CONNECTED'));
  recipient.ws.send(`SUBSCRIBE\nid:matches\ndestination:/topic/championship/${a.cid}/matches\n\n\0`);
  for(const destination of [`/topic/championship/${a.cid}/matches`,'/queue/events','/app/score']){
    const forged=await socket(a.viewerToken);forged.connect();await forged.wait(f=>f.startsWith('CONNECTED'));
    forged.ws.send(`SEND\ndestination:${destination}\n\n{"type":"PHASE1_FORGED"}\0`);
    check(`client SEND denied ${destination.startsWith('/topic')?'topic':destination}`,await forged.rejected());
  }
  const cross=await socket(a.viewerToken);cross.connect();await cross.wait(f=>f.startsWith('CONNECTED'));
  cross.ws.send(`SUBSCRIBE\nid:foreign\ndestination:/topic/championship/${b.cid}/matches\n\n\0`);
  check('cross championship subscription denied',await cross.rejected());
  check('forged championship event never delivered',!recipient.frames.some(f=>f.includes('PHASE1_FORGED')));
  const captains=[];
  for(let i=0;i<2;i++){
    const email=`captain-${crypto.randomUUID()}@example.invalid`,password=crypto.randomUUID();
    const team=await a.admin.call(`provision captain ${i+1}`,'POST',`/championships/${a.cid}/admin/teams`,200,{teamName:`Synthetic Team ${i+1}`,shortCode:`T${i+1}`,captainFullName:'Synthetic Captain',captainEmail:email,captainMobile:'9999999999'});
    secrets.push(team.setupPath.split('=')[1]);
    const session=new Session();await session.call(`activate captain ${i+1}`,'POST','/auth/activate',204,{token:team.setupPath.split('=')[1],password});
    await session.login(`captain ${i+1} login`,email,password);captains.push({session,team});
  }
  const topic=`/topic/championship/${a.cid}/auction/${a.aid}`;
  recipient.ws.send(`SUBSCRIBE\nid:auction\ndestination:${topic}\n\n\0`);
  await new Promise(r=>setTimeout(r,150));
  const started=await a.admin.call('auction starts','POST',`/championships/${a.cid}/auctions/${a.aid}/admin/start`,200);
  await recipient.wait(f=>f.startsWith('MESSAGE')&&f.includes('STATE'));
  check('authorized subscriber receives committed server broadcast',true);
  const playerId=started.currentPlayer.id;
  await captains[0].session.call('captain cannot bid for another team','POST',`/championships/${a.cid}/auctions/${a.aid}/bid`,403,{playerId,teamId:captains[1].team.teamId,amount:100});
  await a.viewer.call('viewer cannot bid','POST',`/championships/${a.cid}/auctions/${a.aid}/bid`,403,{playerId,teamId:captains[0].team.teamId,amount:100});
  await Promise.all(captains.map(async({session})=>{session.csrf=(await session.raw('GET','/auth/csrf')).data.token;}));
  const bids=await Promise.all(captains.map(({session,team})=>session.raw('POST',`/championships/${a.cid}/auctions/${a.aid}/bid`,{playerId,teamId:team.teamId,amount:100},{'X-XSRF-TOKEN':session.csrf})));
  check('simultaneous equal bids have one winner',bids.filter(r=>r.status===200).length===1&&bids.every(r=>[200,400,409].includes(r.status)),{statuses:bids.map(r=>r.status)});
  const sold=await a.admin.call('auction sale','POST',`/championships/${a.cid}/auctions/${a.aid}/admin/sell`,200);
  const roster=await a.admin.call('sold roster persists','GET',`/championships/${a.cid}/players`,200);
  check('sold player and purse remain consistent',roster.some(p=>p.id===playerId&&p.status==='SOLD'&&p.teamId)&&sold.teams.some(t=>Number(t.purseRemaining)===9900));
  const oldJwt=a.admin.cookies.get('neml_auth');
  const staffSocket=await socket(oldJwt);staffSocket.connect();await staffSocket.wait(f=>f.startsWith('CONNECTED'));
  staffSocket.ws.send(`SUBSCRIBE\nid:staff\ndestination:${topic}\n\n\0`);
  await new Promise(r=>setTimeout(r,150));
  await a.admin.call('logout','POST','/auth/logout',204);
  const replay=new Session(oldJwt);await replay.call('logged out JWT replay denied','GET','/auth/me',401);
  const before=staffSocket.frames.filter(f=>f.startsWith('MESSAGE')).length;
  const freshAdmin=new Session();await freshAdmin.login('fresh admin login after logout',a.email,a.password);
  await freshAdmin.call('valid server event after logout','POST',`/championships/${a.cid}/auctions/${a.aid}/admin/pause`,200);
  await recipient.wait(f=>f.startsWith('MESSAGE')&&f.includes('PAUSED'));
  await new Promise(r=>setTimeout(r,200));
  check('logged out subscriber receives no further events',before===staffSocket.frames.filter(f=>f.startsWith('MESSAGE')).length);
  const nextPlayer=await superAdmin.call('super admin can still observe','GET',`/championships/${a.cid}/players`,200);
  check('authenticated state remains readable after another user logout',nextPlayer.length>0);
  const serverLog=await fs.readFile('docs/phase-1/evidence/backend-runtime.log','utf8');
  check('passwords setup tokens and JWTs absent from server log',secrets.every(secret=>!serverLog.includes(secret)));
}catch(error){check('runtime harness completed',false,error.message);}
finally{for(const ws of sockets)ws.close();}
await fs.writeFile(out,JSON.stringify({at:new Date().toISOString(),base,publicRoom,results},null,2));
console.log(JSON.stringify({checks:results.length,passed:results.filter(r=>r.pass).length,failed:results.filter(r=>!r.pass)},null,2));
if(results.some(r=>!r.pass))process.exitCode=1;
