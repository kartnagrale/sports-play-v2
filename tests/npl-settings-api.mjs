// Real gateway + PostgreSQL regression. Run only against the disposable NPL stack.
import assert from 'node:assert/strict';
import {writeFileSync} from 'node:fs';
const base=process.env.NPL_TEST_URL || 'http://localhost:18090/api';
if (!/^http:\/\/(localhost|127\.0\.0\.1):18090\/api$/.test(base)) throw Error('Disposable NPL stack only');
import {Session} from './npl-api-client.mjs';
const root=new Session();
await root.request('/auth/login','POST',{email:process.env.NPL_TEST_EMAIL,password:process.env.NPL_TEST_PASSWORD});
const created=await root.request('/championships','POST',{name:'NPL disposable settings test',sportType:'CRICKET',adminFullName:'Settings Test Admin',adminEmail:`settings-${Date.now()}@example.invalid`,adminMobile:'9999999999'});
const cid=created.championship.id;
const admin=new Session();
const password='Disposable-Settings-Test-7924!';
await admin.request('/auth/activate','POST',{token:created.admin.setupPath.split('=')[1],password},204);
await admin.request('/auth/login','POST',{email:created.admin.email,password});
const path=`/championships/${cid}/settings`;
const loaded=await admin.request(path);
const values={auctionName:'NPL Auction',maxSquadSize:11,purseLimit:100000,minMale:0,minFemale:3,playerBasePrice:5000,bidIncrement:1000,timerSeconds:30,customRules:'',expectedVersion:loaded.version};
if(process.env.NPL_REPRODUCE==='true') {
  // Exactly what the original page sends: spread response retains response-only fields.
  const error=await admin.request(path,'PUT',{...loaded,...values,expectedVersion:undefined},400);
  assert.equal(error.message,'Required request fields are missing or invalid');
  console.log('Reproduced original GET-to-PUT payload HTTP 400 with real gateway, CSRF and PostgreSQL.');
} else {
  const saved=await admin.request(path,'PUT',values);
  const reloaded=await admin.request(path);
  for(const [key,value] of Object.entries(values)) if(key!=='expectedVersion'&&key!=='customRules') assert.equal(reloaded[key],value,key);
  assert.equal(reloaded.version,saved.version);
  await admin.request(path,'PUT',values,409);
  const policy=await admin.request(`/championships/${cid}/administrative-configuration/auction`);
  assert.equal(policy.teamBudget,100000);assert.equal(policy.minimumBid,5000);assert.equal(policy.bidIncrement,1000);assert.equal(policy.maximumRosterSize,11);
  const policyRequest={expectedVersion:policy.version,enabled:policy.enabled,auctionType:policy.auctionType,teamBudget:110000,minimumBid:policy.minimumBid,bidIncrement:policy.bidIncrement,minimumRosterSize:policy.minimumRosterSize,maximumRosterSize:policy.maximumRosterSize,retainedPlayersAllowed:policy.retainedPlayersAllowed,maximumRetainedPlayers:policy.maximumRetainedPlayers,unsoldPlayerReauctionAllowed:policy.unsoldPlayerReauctionAllowed,purseCarryForwardPolicy:policy.purseCarryForwardPolicy,auctionOrderPolicy:policy.auctionOrderPolicy,timerSeconds:policy.timerSeconds};
  await admin.request(`/championships/${cid}/administrative-configuration/auction`,'PUT',policyRequest);
  assert.equal((await admin.request(path)).purseLimit,110000);
  await admin.request(path,'PUT',{...values,expectedVersion:saved.version},409);
  const latest=await admin.request(path);
  await admin.request(path,'PUT',{...values,maxSquadSize:5,expectedVersion:latest.version},400);
  assert.equal((await admin.request(path)).purseLimit,110000,'invalid transaction rolls back all changes');
  const revision=(await admin.request(path)).version;
  const attempts=await Promise.all([100000,105000].map(purseLimit=>admin.raw(path,'PUT',{...values,expectedVersion:revision,purseLimit})));
  assert.deepEqual(attempts.map(r=>r.status).sort(),[200,409]);
  const winner=await attempts.find(r=>r.status===200).json();
  assert.equal((await admin.request(path)).purseLimit,winner.purseLimit);
  const quotaPath=`/championships/${cid}/sport-configuration/quotas`;
  const quotaResponse=await admin.raw(quotaPath);assert.equal(quotaResponse.status,200);
  const quotaVersion=quotaResponse.headers.get('etag')?.replaceAll('"','');assert(quotaVersion,'quota snapshot revision header');
  const quotaRules=await quotaResponse.json();
  await admin.request(quotaPath,'PUT',quotaRules,200,{'If-Match':quotaVersion});
  await admin.request(quotaPath,'PUT',quotaRules,409,{'If-Match':quotaVersion});
  await admin.request(path,'PUT',{...values,expectedVersion:winner.version},409);
  const synced=await admin.request(path);assert.equal(synced.minFemale,3);assert.equal(synced.minMale,0);
  console.log('PASS settings PUT/reload, cross-screen versions, simultaneous saves, quota snapshot/If-Match, synchronization and rollback.');
}
if(process.env.NPL_TEST_STATE)writeFileSync(process.env.NPL_TEST_STATE,JSON.stringify({cid,email:created.admin.email,password,cookies:[...admin.cookies]}));
