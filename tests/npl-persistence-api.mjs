import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {Session} from './npl-api-client.mjs';
import {observeAuction} from './npl-websocket.mjs';
const file=process.env.NPL_TEST_STATE;
const state=JSON.parse(readFileSync(file));
const admin=new Session();admin.cookies=new Map(state.adminCookies);
const base=`/championships/${state.cid}`,auction=`${base}/auctions/${state.aid}`;
if(process.env.NPL_PERSISTENCE_MODE==='after'){
  const response=await admin.raw(state.photoUrl);assert.equal(response.status,200);
  assert.equal(createHash('sha256').update(Buffer.from(await response.arrayBuffer())).digest('hex'),state.photoHash);
  const current=await admin.request(auction+'/state');
  assert(current.teams.every(t=>t.totalPlayers===11&&t.purseRemaining===10000));
  assert.equal((await admin.request(base+'/players')).length,44);
  assert.equal((await admin.request(base+'/competition')).fixtures.length,7);
  console.log('PASS photo bytes, 44 player identities, team purses and seven fixtures persist across container recreation.');
}else{
  const players=await admin.request(base+'/players');
  const [one,two]=await Promise.all([observeAuction(admin,state.cid,state.aid),observeAuction(admin,state.cid,state.aid)]);
  const form=new FormData();form.set('file',new Blob([readFileSync(process.env.NPL_TEST_PHOTO)],{type:'image/png'}),'test-player.png');
  const player=players.find(p=>p.status==='SOLD');
  const photo=await admin.request(`${base}/admin/players/${player.id}/photo`,'PUT',form);
  await Promise.all([one.waitFor(e=>e.type==='PLAYER_PHOTO_UPDATED'),two.waitFor(e=>e.type==='PLAYER_PHOTO_UPDATED')]);
  one.close();two.close();
  const reconnected=await observeAuction(admin,state.cid,state.aid);reconnected.close();
  const before=await admin.request(auction+'/state');assert(Number.isSafeInteger(before.revision));
  const image=await admin.raw(photo.photoUrl);assert.equal(image.status,200);
  state.photoUrl=photo.photoUrl;state.photoHash=createHash('sha256').update(Buffer.from(await image.arrayBuffer())).digest('hex');
  const captain=players.find(p=>p.status==='PREASSIGNED');
  const certificateBase=base+'/admin/certificates';
  const issue={requestId:crypto.randomUUID(),approvalId:state.approval.id,category:'PARTICIPATION',recipientType:'PLAYER',recipientId:captain.id};
  const issued=await admin.request(certificateBase,'POST',issue);assert.equal(issued.status,'ISSUED');
  const pdf=await admin.raw(`${certificateBase}/${issued.certificate.id}/pdf`);assert.equal(pdf.status,200);assert.equal(Buffer.from(await pdf.arrayBuffer()).subarray(0,5).toString(),'%PDF-');
  // Correct an actual delivery in the final, then ensure prior approval cannot issue a new award.
  const innings=state.finalState.innings.at(-1),delivery=innings.deliveries.at(-1);
  await admin.request(`${base}/cricket/matches/${state.final.id}/innings/${innings.id}/deliveries/${delivery.id}`,'DELETE',{expectedInningsVersion:innings.version,reason:'Disposable regression: reverse the final delivery for correction'});
  await admin.request(certificateBase,'POST',{...issue,requestId:crypto.randomUUID(),recipientId:players.find(p=>p.id!==captain.id).id},409);
  const revoked=await admin.request(`${certificateBase}/${issued.certificate.id}/revoke`,'POST',{reason:'Final delivery corrected; prior participation issuance revoked for regression'});assert.equal(revoked.status,'REVOKED');
  writeFileSync(file,JSON.stringify(state));
  console.log('PASS two authenticated WebSocket subscribers/reconnect, player photo storage, captain certificate PDF, stale approval rejection and revocation after a real delivery correction.');
}
