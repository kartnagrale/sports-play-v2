// Run only through verify-dockerization.ps1 against its private, isolated state.
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import http from 'node:http';
import {execFileSync} from 'node:child_process';
import {createRequire} from 'node:module';
const require=createRequire(new URL('../frontend/package.json',import.meta.url));
const {request}=require('@playwright/test');
const config=Object.fromEntries((await fs.readFile('tmp/docker-test.env','utf8')).split(/\r?\n/).filter(x=>x&&!x.startsWith('#')).map(x=>[x.slice(0,x.indexOf('=')),x.slice(x.indexOf('=')+1)]));
const project=(await fs.readFile('tmp/docker-project.txt','utf8')).trim();
assert.match(project,/^playneml65-[a-f0-9]{8}$/);
assert.equal(project,config.COMPOSE_PROJECT_NAME);
const origin=config.PUBLIC_ORIGIN,base=origin+'/api',mode=process.argv[2];
const compose=['compose','--env-file','tmp/docker-test.env','-p',project];
const docker=(...args)=>execFileSync('docker',[...compose,...args],{encoding:'utf8',maxBuffer:32*1024*1024});
const sql=(q,database=config.POSTGRES_DB)=>docker('exec','-T','db','psql','-X','-U',config.POSTGRES_USER,'-d',database,'-At','-v','ON_ERROR_STOP=1','-c',q).trim();
const hash=x=>crypto.createHash('sha256').update(x).digest('hex');
const summaryPath='tmp/docker-verification.json';
let summary=JSON.parse(await fs.readFile(summaryPath,'utf8').catch(()=>'{}'));
async function command(client,path,data,method='POST'){
 const csrf=await(await client.get(base+'/auth/csrf')).json();
 const response=await client.fetch(base+path,{method,headers:{'X-XSRF-TOKEN':csrf.token},data});
 assert.ok([200,204].includes(response.status()),'Authorized command failed: '+response.status());
 return response.status()===204?null:response.json();
}
async function login(account){
 const client=await request.newContext();
 await command(client,'/auth/login',account);
 return client;
}
async function snapshot(){
 const accounts=JSON.parse(await fs.readFile('tmp/phase6-demo-accounts.json','utf8'));
 const client=await login(accounts.admin);
 const root='/championships/'+accounts.cid;
 const rows={};
 const tables=sql("select tablename from pg_tables where schemaname='play_neml' order by tablename").split('\n');
 for(const table of tables){
  assert.match(table,/^[a-z_]+$/);
  const data=sql(`select coalesce(jsonb_agg(to_jsonb(t) order by to_jsonb(t)::text),'[]') from play_neml."${table}" t`);
  rows[table]={count:JSON.parse(data).length,sha256:hash(data)};
 }
 const players=await(await client.get(base+root+'/players')).json();
 const photos={};
 for(const player of players){
  if(!player.photoUrl)continue;
  const response=await client.get(base+player.photoUrl);
  assert.equal(response.status(),200,'Versioned private photo must remain readable');
  assert.match(response.headers()['cache-control'],/private/);
  photos[player.id]=hash(await response.body());
 }
 assert.ok(Object.keys(photos).length>0,'Expected a persisted private photo');
 const manifest=docker('exec','-T','backend','sh','-c',"find /app/var/player-photos -type f -name '*.jpg' -exec sha256sum {} + | sort").trim();
 const volumePhotos=Object.fromEntries(manifest.split('\n').filter(Boolean).map(line=>[line.slice(line.lastIndexOf('/')+1),line.split(/\s+/)[0]]));
 const certs=(await(await client.get(base+root+'/admin/certificates')).json()).items;
 const valid=certs.find(x=>x.status==='ISSUED'),revoked=certs.find(x=>x.status==='REVOKED');
 assert.ok(valid&&revoked,'Need valid and revoked certificate persistence');
 for(const item of [valid,revoked]){
  const verification=await(await fetch(base+'/certificate-verification/'+item.certificate.reference)).json();
  assert.equal(verification.status,item.status==='ISSUED'?'VALID':'REVOKED');
 }
 const pdf=await client.get(base+root+'/admin/certificates/'+valid.certificate.id+'/pdf');
 assert.equal(pdf.status(),200);assert.equal((await pdf.body()).subarray(0,5).toString(),'%PDF-');
 await client.dispose();
 return {rows,photos,volumePhotos,valid:valid.certificate.reference,revoked:revoked.certificate.reference};
}
if(mode==='security'){
 const ids=docker('ps','-q').trim().split(/\s+/);
 const containers=JSON.parse(execFileSync('docker',['inspect',...ids],{encoding:'utf8'}));
 for(const container of containers){
  const service=container.Config.Labels['com.docker.compose.service'];
  if(service==='gateway'){
   assert.equal(container.HostConfig.PortBindings['8080/tcp'][0].HostIp,'127.0.0.1');
  }else assert.ok(Object.keys(container.HostConfig.PortBindings||{}).length===0,'Internal service publishes a host port');
  if(['backend','frontend','gateway'].includes(service)){
   assert.ok(container.Config.User&&!['root','0','0:0'].includes(container.Config.User));
   assert.equal(container.HostConfig.ReadonlyRootfs,true);
   const image=JSON.parse(execFileSync('docker',['image','inspect',container.Config.Image],{encoding:'utf8'}))[0];
   assert.ok(!image.Config.Env.some(x=>/^(DB_PASSWORD|POSTGRES_PASSWORD|JWT_SECRET|APP_BOOTSTRAP_PASSWORD)=/.test(x)),'Runtime secret baked into image');
  }
 }
 for(const network of ['application','database']){
  const details=JSON.parse(execFileSync('docker',['network','inspect',project+'_'+network],{encoding:'utf8'}))[0];
  assert.equal(details.Internal,true);
 }
 const anonymous=await request.newContext();
 assert.equal((await anonymous.get(base+'/auth/me')).status(),401);
 assert.ok([401,403].includes((await anonymous.post(base+'/auth/login',{data:{email:config.APP_BOOTSTRAP_EMAIL,password:config.APP_BOOTSTRAP_PASSWORD}})).status()));
 const staff=await login({email:config.APP_BOOTSTRAP_EMAIL,password:config.APP_BOOTSTRAP_PASSWORD});
 const cookies=(await staff.storageState()).cookies;
 const auth=cookies.find(x=>x.name==='neml_auth'),csrf=cookies.find(x=>x.name==='XSRF-TOKEN');
 assert.equal(auth.httpOnly,true);assert.equal(auth.sameSite,'Lax');assert.equal(csrf.httpOnly,true);
 assert.equal((await staff.get(base+'/auth/me')).status(),200);
 // CSRF runs before JWT authentication; the existing error dispatch can return 401.
 assert.ok([401,403].includes((await staff.post(base+'/championships',{data:{}})).status()));
 assert.ok([401,403].includes((await staff.post(base+'/championships',{headers:{'X-XSRF-TOKEN':'invalid'},data:{}})).status()));
 // Use the loopback alias so this is actually cross-origin; same-origin needs no CORS header.
 const allowed=await anonymous.fetch(base.replace('localhost','127.0.0.1')+'/auth/me',{method:'OPTIONS',headers:{Origin:origin,'Access-Control-Request-Method':'GET'}});
 assert.equal(allowed.headers()['access-control-allow-origin'],origin);
 const denied=await anonymous.fetch(base+'/auth/me',{method:'OPTIONS',headers:{Origin:'https://untrusted.example.invalid','Access-Control-Request-Method':'GET'}});
 assert.equal(denied.status(),403);assert.equal(denied.headers()['access-control-allow-origin'],undefined);
 const handshake=await new Promise((resolve,reject)=>{
  const req=http.request(origin+'/ws',{headers:{Origin:'https://untrusted.example.invalid',Connection:'Upgrade',Upgrade:'websocket','Sec-WebSocket-Key':crypto.randomBytes(16).toString('base64'),'Sec-WebSocket-Version':'13'}});
  req.on('response',r=>{r.resume();resolve(r.statusCode);});
  req.on('upgrade',(_r,s)=>{s.destroy();reject(Error('Foreign WebSocket origin was accepted'));});
  req.on('error',reject);req.setTimeout(10000,()=>req.destroy(Error('Handshake timeout')));req.end();
 });
 assert.equal(handshake,403);
 const account=JSON.parse(await fs.readFile('tmp/phase6-demo-accounts.json','utf8'));
 const admin=await login(account.admin),root='/championships/'+account.cid;
 const players=await(await admin.get(base+root+'/players')).json();
 for(const p of players)if(p.photoUrl)assert.equal((await anonymous.get(base+p.photoUrl)).status(),401);
 for(const path of ['/var/player-photos/','/.env','/player-photos/'])assert.ok((await anonymous.get(origin+path)).status()>=400);
 assert.equal(sql("select rolsuper or rolcreatedb or rolcreaterole from pg_roles where rolname='sports_play_app'"),'f');
 assert.equal(sql("select has_schema_privilege('sports_play_app','play_neml','CREATE')"),'f');
 assert.equal(sql("select has_table_privilege('sports_play_app','play_neml.schema_migrations','UPDATE')"),'f');
 assert.equal(sql('select count(*) from play_neml.schema_migrations'),'7');
 const currentCertificates=(await(await admin.get(base+root+'/admin/certificates')).json()).items;
 if(!currentCertificates.some(x=>x.status==='ISSUED')){
  const approval=await command(admin,root+'/admin/certificates/approvals',{justification:'Container persistence verification after final score review'});
  const eligible=await(await admin.get(base+root+'/admin/certificates/eligibility?category=PARTICIPATION&recipientType=PLAYER')).json();
  await command(admin,root+'/admin/certificates',{requestId:crypto.randomUUID(),approvalId:approval.id,category:'PARTICIPATION',recipientType:'PLAYER',recipientId:eligible.recipients[0].id});
 }
 summary.security={onlyLoopbackGatewayPublished:true,internalNetworks:true,nonRootReadOnlyApplicationContainers:true,noRuntimeSecretsInImages:true,missingAndInvalidCsrfRejected:true,httpOnlySameSiteCookies:true,foreignCorsAndWebSocketOriginsRejected:true,privatePhotosRequireAuthentication:true,noPublicStorageRoute:true,restrictedDatabaseRole:true,migrationCount:7};
 await Promise.all([anonymous.dispose(),staff.dispose(),admin.dispose()]);
}else if(mode==='migrations'){
 assert.equal(sql('select count(*) from play_neml.schema_migrations'),'7');
 docker('run','--rm','--no-deps','migrate');
 assert.equal(sql('select count(*) from play_neml.schema_migrations'),'7');
 const name='upgrade_'+crypto.randomBytes(4).toString('hex');
 const script=`set -eu
 createdb -U "$POSTGRES_USER" ${name}
 psql -X -U "$POSTGRES_USER" -d ${name} -v ON_ERROR_STOP=1 -f /opt/play-neml/schema.sql
 checksum=$(sha256sum /opt/play-neml-migrations/2026-10-27-phase1-account-security.sql | cut -d ' ' -f 1)
 psql -X -U "$POSTGRES_USER" -d ${name} -v ON_ERROR_STOP=1 -v "checksum=$checksum" -v version=2026-10-27-phase1-account-security -v migration_file=2026-10-27-phase1-account-security.sql -f /opt/play-neml-migrations/phase1-apply.psql
 psql -X -U "$POSTGRES_USER" -d ${name} -v ON_ERROR_STOP=1 -c "create table play_neml.docker_sentinel (value text); insert into play_neml.docker_sentinel values ('preserved');"
 POSTGRES_DB=${name} sh /opt/play-neml-migrations/phase1-init.sh
 POSTGRES_DB=${name} sh /opt/play-neml-migrations/phase1-init.sh
 test "$(psql -X -U "$POSTGRES_USER" -d ${name} -Atc 'select count(*) from play_neml.schema_migrations')" = 7
 test "$(psql -X -U "$POSTGRES_USER" -d ${name} -Atc 'select value from play_neml.docker_sentinel')" = preserved
 `;
 execFileSync('docker',[...compose,'exec','-T','db','sh'],{input:script,encoding:'utf8',maxBuffer:8*1024*1024});
 const original=await fs.readFile('docs/sql/2026-10-28-phase5-certificate-batches.sql');
 await fs.writeFile('tmp/docker-tampered-migration.sql',Buffer.concat([original,Buffer.from('\n-- checksum rejection probe\n')]));
 let rejected=false;
 try{
  docker('run','--rm','--no-deps','--volume',process.cwd().replaceAll('\\','/')+'/tmp/docker-tampered-migration.sql:/opt/play-neml-migrations/2026-10-28-phase5-certificate-batches.sql:ro','migrate');
 }catch(error){rejected=String(error.stderr).includes('Migration checksum mismatch');}
 assert.equal(rejected,true,'Migration checksum changes must block startup');
 assert.equal(sql('select count(*) from play_neml.schema_migrations'),'7');
 summary.migrations={freshSeven:true,repeatedWithoutDuplication:true,populatedEarlierSchemaUpgraded:true,sentinelPreserved:true,tamperedChecksumRejected:true};
}else if(mode==='snapshot'){
 await fs.writeFile('tmp/docker-persistence-before.json',JSON.stringify(await snapshot()));
}else if(mode==='persistence'){
 const before=JSON.parse(await fs.readFile('tmp/docker-persistence-before.json','utf8')),after=await snapshot();
 assert.deepEqual(after,before,'Database content or private photo bytes changed across restart/recreation');
 summary[process.argv[3]||'persistence']={allTablesAndRowsUnchanged:true,tableCount:Object.keys(after.rows).length,photoCount:Object.keys(after.volumePhotos).length,photoBytesUnchanged:true,validAndRevokedCertificatesPreserved:true,pdfStillDownloads:true};
}else if(mode==='backup'){
 const before=JSON.parse(await fs.readFile('tmp/docker-persistence-before.json','utf8'));
 const database='restore_'+crypto.randomBytes(4).toString('hex');
 docker('exec','-T','db','pg_dump','-U',config.POSTGRES_USER,'-d',config.POSTGRES_DB,'-Fc','-f','/tmp/dockerization.dump');
 docker('exec','-T','db','createdb','-U',config.POSTGRES_USER,database);
 docker('exec','-T','db','pg_restore','--exit-on-error','--no-owner','--no-privileges','-U',config.POSTGRES_USER,'-d',database,'/tmp/dockerization.dump');
 for(const [table,expected]of Object.entries(before.rows)){
  const data=sql(`select coalesce(jsonb_agg(to_jsonb(t) order by to_jsonb(t)::text),'[]') from play_neml."${table}" t`,database);
  assert.deepEqual({count:JSON.parse(data).length,sha256:hash(data)},expected,'Restored table differs');
 }
 const backupDirectory='tmp/docker-backup-'+crypto.randomBytes(4).toString('hex');
 await fs.mkdir(backupDirectory,{recursive:true});
 const dbId=docker('ps','-q','db').trim(),backendId=docker('ps','-q','backend').trim();
 execFileSync('docker',['cp',dbId+':/tmp/dockerization.dump',backupDirectory+'/database.dump'],{stdio:'pipe'});
 execFileSync('docker',['cp',backendId+':/app/var/player-photos',backupDirectory+'/player-photos'],{stdio:'pipe'});
 const photoFiles=await fs.readdir(backupDirectory+'/player-photos');
 const photoHashes=await Promise.all(photoFiles.map(async f=>hash(await fs.readFile(backupDirectory+'/player-photos/'+f))));
 for(const expected of Object.values(before.photos))assert.ok(photoHashes.includes(expected));
 assert.equal(photoFiles.length,Object.keys(before.volumePhotos).length);
 for(const [filename,expected]of Object.entries(before.volumePhotos))assert.equal(hash(await fs.readFile(backupDirectory+'/player-photos/'+filename)),expected);
 summary.backup={customDumpCopiedWithoutShellRedirection:true,restoreIntoSeparateEmptyDatabase:true,allRestoredTableHashesMatch:true,privatePhotoBackupHashesMatch:true};
}else if(mode==='secure-cookie'){
 // Raw requests deliberately carry the Secure CSRF cookie over the loopback test link.
 const response=await fetch(base+'/auth/csrf'),csrf=await response.json();
 const cookie=response.headers.getSetCookie().map(x=>x.split(';')[0]).join('; ');
 const login=await fetch(base+'/auth/login',{method:'POST',headers:{Cookie:cookie,'X-XSRF-TOKEN':csrf.token,'Content-Type':'application/json'},body:JSON.stringify({email:config.APP_BOOTSTRAP_EMAIL,password:config.APP_BOOTSTRAP_PASSWORD})});
 assert.equal(login.status,200);
 const auth=login.headers.getSetCookie().find(x=>x.startsWith('neml_auth='));
 assert.match(auth,/; Secure/);assert.match(auth,/; HttpOnly/);assert.match(auth,/SameSite=Lax/);
 assert.ok(response.headers.getSetCookie().every(x=>/; Secure/.test(x)));
 summary.secureCookie={secureFlagVerifiedWithProductionSetting:true,tlsTransportTested:false};
}else throw Error('Unknown verification mode');
await fs.writeFile(summaryPath,JSON.stringify(summary,null,2)+'\n');
console.log('Container check passed: '+mode);
