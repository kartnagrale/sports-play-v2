// Export the exact BuildKit COPY context and prove private canaries are excluded.
import fs from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import crypto from 'node:crypto';
const root=process.cwd(),token='private-context-probe-'+crypto.randomUUID();
await fs.mkdir('tmp',{recursive:true});
const auditFile=path.join(root,'tmp/context-audit.Dockerfile');
await fs.writeFile(auditFile,'FROM scratch\nCOPY . /context/\n');
const result={};
async function walk(dir){
 let files=[];
 for(const entry of await fs.readdir(dir,{withFileTypes:true})){
  const location=path.join(dir,entry.name);
  files.push(...entry.isDirectory()?await walk(location):[location]);
 }
 return files;
}
for(const [name,context]of [['database','.'],['backend','backend'],['frontend','frontend'],['gateway','docker/gateway']]){
 const probes=['.env.context-probe','private-demo-context-probe.txt','credentials-context-probe.json','private-photo-context-probe.jpg'];
 const created=[];
 try{
  for(const probe of probes){
   const location=path.join(root,context,probe);
   await fs.writeFile(location,token,{flag:'wx'});created.push(location);
  }
  const destination=path.join(root,'tmp','context-audit-'+name+'-'+crypto.randomBytes(4).toString('hex'));
  execFileSync('docker',['build','-f',auditFile,'--output','type=local,dest='+destination,path.join(root,context)],{encoding:'utf8',stdio:'pipe',maxBuffer:8*1024*1024});
  const files=await walk(path.join(destination,'context'));
  let bytes=0;
  for(const file of files){
   const relative=path.relative(path.join(destination,'context'),file).replaceAll('\\','/');
   assert.ok(!/(^|\/)(\.env[^/]*|node_modules|\.next[^/]*|target|e2e|logs|tmp|var|evidence|player-photos)(\/|$)/.test(relative),'Unnecessary/private context path');
   const contents=await fs.readFile(file);bytes+=contents.length;
   assert.ok(!contents.includes(Buffer.from(token)),'Private canary entered the build context');
  }
  assert.ok(files.length>0);
  result[name]={files:files.length,bytes,privateCanariesExcluded:true};
 }finally{
  for(const file of created)await fs.unlink(file);
 }
}
await fs.writeFile('tmp/docker-context-verification.json',JSON.stringify(result,null,2)+'\n');
console.log('All four BuildKit contexts exclude private canaries, env files and unnecessary evidence.');
