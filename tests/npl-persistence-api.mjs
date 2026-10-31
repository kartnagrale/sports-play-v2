import assert from 'node:assert/strict';
import {readFileSync,writeFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {Session} from './npl-api-client.mjs';
import {observeAuction} from './npl-websocket.mjs';
const file=process.env.NPL_TEST_STATE;
const state=JSON.parse(readFileSync(file));
const admin=new Session();admin.cookies=new Map(state.adminCookies);
if(process.env.NPL_FULL_MOCK==='true'){
  await admin.request('/auth/login','POST',{email:state.admin.email,password:state.admin.password});
  state.adminCookies=[...admin.cookies];
}
const base=`/championships/${state.cid}`,auction=`${base}/auctions/${state.aid}`;
if(process.env.NPL_FULL_MOCK==='true'){
  const certificateBase=base+'/admin/certificates';
  const snapshot=async()=>({players:await admin.request(base+'/players'),auction:await admin.request(auction+'/state'),competition:await admin.request(base+'/competition'),squads:await admin.request(base+'/squad-confirmations'),certificates:await admin.request(certificateBase)});
  const digest=value=>createHash('sha256').update(JSON.stringify(value)).digest('hex');
  const save=()=>writeFileSync(file,JSON.stringify(state,null,2),{mode:0o600});
  if(!['after','baseline'].includes(process.env.NPL_PERSISTENCE_MODE)){
    if(state.completed.includes('persistence')){console.log('PASS persistence checkpoint already complete; use after mode to recheck persisted state');process.exit(0);}
    if(!state.frozenLeagueVerified){
      const match=(await admin.request(base+'/competition')).fixtures.find(f=>f.stageType==='LEAGUE');
      const path=`${base}/cricket/matches/${match.id}`,current=await admin.request(path),innings=current.innings.at(-1),last=innings.deliveries.at(-1);
      const attempted=await admin.raw(`${path}/innings/${innings.id}/deliveries/${last.id}`,'DELETE',{expectedInningsVersion:innings.version,reason:'Synthetic negative test: frozen qualifiers must prevent league corrections'});
      if(attempted.status===200){
        const {id,sequence,legalBall,totalRuns,...delivery}=last;
        await admin.request(`${path}/innings/${innings.id}/deliveries`,'POST',{...delivery,idempotencyKey:crypto.randomUUID()});
      }
      writeFileSync('docs/npl-mock/evidence/frozen-league-correction.json',JSON.stringify({httpStatus:attempted.status,expectedStatus:409,restoredIfAccepted:true},null,2));
      assert.equal(attempted.status,409,'frozen league results must not change after the final exists');state.frozenLeagueVerified=true;save();
    }
    const [one,two]=await Promise.all([observeAuction(admin,state.cid,state.aid),observeAuction(admin,state.cid,state.aid)]);
    try{
      const player=(await admin.request(base+'/players')).find(p=>p.playerReference==='MOCK-EMP-0-2');
      const form=new FormData();form.set('file',new Blob([readFileSync(process.env.NPL_TEST_PHOTO)],{type:'image/png'}),'synthetic-avatar.png');
      const photo=await admin.request(`${base}/admin/players/${player.id}/photo`,'PUT',form);state.photoUrl=photo.photoUrl;
      await Promise.all([one.waitFor(e=>e.type==='PLAYER_PHOTO_UPDATED'),two.waitFor(e=>e.type==='PLAYER_PHOTO_UPDATED')]);
    }finally{one.close();two.close();}
    const reconnected=await observeAuction(admin,state.cid,state.aid);reconnected.close();
    const path=`${base}/cricket/matches/${state.final.id}`;
    if(!state.pendingCorrection){
      const current=await admin.request(path),innings=current.innings.at(-1),delivery=innings.deliveries.at(-1);
      state.pendingCorrection={inningsId:innings.id,version:innings.version,delivery};save();
      await admin.request(`${path}/innings/${innings.id}/deliveries/${delivery.id}`,'DELETE',{expectedInningsVersion:innings.version,reason:'Synthetic regression: correct the final delivery, then restore it through delivery scoring'});
    }
    const during=await admin.request(base+'/competition'),championship=await admin.request(base);
    const winnerCertificate=state.certificates.find(c=>c.certificate.category==='WINNER');
    assert.equal((await admin.request(`${certificateBase}/${winnerCertificate.certificate.id}`)).status,'REVOKED','database source trigger revokes affected awards');
    await admin.request(certificateBase,'POST',{requestId:crypto.randomUUID(),approvalId:state.approval.id,category:'PARTICIPATION',recipientType:'PLAYER',recipientId:state.captains[0].playerId},409);
    const observed={finalStatus:during.fixtures.find(f=>f.id===state.final.id).status,champion:during.configuration.champion??null,runnerUp:during.configuration.runnerUp??null,status:championship.status,lifecyclePhase:championship.lifecyclePhase,finalStageStatus:during.stages.find(s=>(s.stageType??s.type)==='FINAL')?.status,staleApprovalRejected:true,certificateAutomaticallyRevoked:true};
    state.correctionObservation=observed;save();
    // Restore even if an invariant is broken, so inspection always retains a completed tournament.
    const {id,sequence,legalBall,totalRuns,...delivery}=state.pendingCorrection.delivery;
    state.finalState=await admin.request(`${path}/innings/${state.pendingCorrection.inningsId}/deliveries`,'POST',{...delivery,idempotencyKey:crypto.randomUUID()});
    assert(state.finalState.resultText);delete state.pendingCorrection;save();
    state.approval=await admin.request(certificateBase+'/approvals','POST',{justification:'Synthetic final correction verified and decisive delivery restored through the scoring engine'});
    state.certificateHistory??=[];state.certificateHistory.push(...state.certificates);const replacements=[];
    for(const previous of state.certificates){
      const c=previous.certificate;
      const issued=await admin.request(certificateBase,'POST',{requestId:crypto.randomUUID(),approvalId:state.approval.id,category:c.category,recipientType:c.recipient.type,recipientId:c.recipient.id,reissuedFrom:c.id,reason:'Reissued after a verified final delivery correction and fresh result approval'});
      assert.equal(issued.status,'ISSUED');replacements.push(issued);
    }
    state.certificates=replacements;state.tournament=await admin.request(base+'/competition');state.matches[state.final.id]=state.finalState;save();
    writeFileSync('docs/npl-mock/evidence/correction-result.json',JSON.stringify(observed,null,2));
    assert.equal(observed.champion,null,'a reopened final must clear the authoritative champion');
    assert.equal(observed.runnerUp,null);assert.equal(observed.status,'ACTIVE');assert.equal(observed.lifecyclePhase,'FINAL');assert.equal(observed.finalStageStatus,'ACTIVE');
    state.completed.push('persistence');
  }
  const photo=await admin.raw(state.photoUrl);assert.equal(photo.status,200);const hash=createHash('sha256').update(Buffer.from(await photo.arrayBuffer())).digest('hex');
  const persisted=await snapshot();assert.equal(persisted.players.length,44);assert.equal(persisted.auction.status,'COMPLETED');assert(persisted.auction.teams.every(t=>t.totalPlayers===11&&t.purseRemaining===10000));assert(persisted.squads.allSquadsLocked);assert.equal(persisted.competition.fixtures.length,7);assert(persisted.competition.fixtures.every(f=>f.status==='COMPLETED'));assert.equal(persisted.competition.configuration.champion.id,state.captains[0].teamId);
  for(const c of state.certificates)assert.equal((await admin.request(`${certificateBase}/${c.certificate.id}`)).status,'ISSUED');
  if(process.env.NPL_PERSISTENCE_MODE==='after'){
    assert.equal(hash,state.photoHash);assert.equal(digest(persisted),state.persistenceDigest,'full API state must survive recreation');
    writeFileSync('docs/npl-mock/evidence/persistence-result.json',JSON.stringify({project:state.project,championshipId:state.cid,photoSha256:hash,stateSha256:state.persistenceDigest,players:44,purchases:40,lockedSquads:4,completedFixtures:7,issuedCertificates:state.certificates.length},null,2));
  }else{state.photoHash=hash;state.persistenceDigest=digest(persisted);save();}
  console.log('PASS full-state persistence, completed tournament, photo authorization/reconnect, correction/invalidation, fresh approval and certificate reissue');
  process.exit(0);
}
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
