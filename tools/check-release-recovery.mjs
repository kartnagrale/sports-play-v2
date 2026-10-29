import fs from 'node:fs/promises';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
const hash=x=>crypto.createHash('sha256').update(x).digest('hex');
if(process.argv[2]==='database'){
 const [project,database]=process.argv.slice(3);
 assert.match(project,/^playneml65-[a-f0-9]{8}$/);assert.match(database,/^recovery_[a-f0-9]{8}$/);
 const before=JSON.parse(await fs.readFile('tmp/docker-persistence-before.json','utf8'));
 for(const [table,expected]of Object.entries(before.rows)){
  assert.match(table,/^[a-z_]+$/);
  const value=execFileSync('docker',['compose','--env-file','tmp/docker-test.env','-p',project,'exec','-T','db','psql','-X','-U','sports_play_owner','-d',database,'-At','-v','ON_ERROR_STOP=1','-c',`select coalesce(jsonb_agg(to_jsonb(t) order by to_jsonb(t)::text),'[]') from play_neml."${table}" t`],{encoding:'utf8'}).trim();
  assert.deepEqual({count:JSON.parse(value).length,sha256:hash(value)},expected,'Restored table differs');
 }
 console.log('All 43 restored table hashes match.');
}else{
 const base=process.argv[3]+'/api';
 const before=JSON.parse(await fs.readFile('/evidence/docker-persistence-before.json','utf8'));
 const accounts=JSON.parse(await fs.readFile('/evidence/phase6-demo-accounts.json','utf8'));
 const cookies=new Map();
 async function call(path,body){
  const csrf=body?await call('/auth/csrf'):null;
  const r=await fetch(base+path,{method:body?'POST':'GET',headers:{Cookie:[...cookies].map(([k,v])=>`${k}=${v}`).join('; '),...(body?{'Content-Type':'application/json','X-XSRF-TOKEN':csrf.token}:{})},...(body?{body:JSON.stringify(body)}:{})});
  for(const cookie of r.headers.getSetCookie()){const pair=cookie.split(';')[0],i=pair.indexOf('=');cookies.set(pair.slice(0,i),pair.slice(i+1));}
  assert.equal(r.status,200,'Recovered authorized request failed');
  return r.headers.get('content-type')?.includes('application/json')?r.json():Buffer.from(await r.arrayBuffer());
 }
 await call('/auth/login',accounts.admin);await call('/auth/me');
 const root='/championships/'+accounts.cid;
 const players=await call(root+'/players');let count=0;
 for(const p of players)if(p.photoUrl){assert.equal(hash(await call(p.photoUrl)),before.photos[p.id]);count++;}
 assert.ok(count>0);
 const history=await call(root+'/admin/certificates');
 const valid=history.items.find(x=>x.certificate.reference===before.valid),revoked=history.items.find(x=>x.certificate.reference===before.revoked);
 assert.equal(valid.status,'ISSUED');assert.equal(revoked.status,'REVOKED');
 const pdf=await call(root+'/admin/certificates/'+valid.certificate.id+'/pdf');assert.equal(pdf.subarray(0,5).toString(),'%PDF-');
 assert.equal((await call('/certificate-verification/'+before.valid)).status,'INVALID');
 console.log('Recovered login, authorized photos, certificate states and PDF passed; public verification disabled.');
}
