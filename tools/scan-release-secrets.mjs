// Bounded secret gate for newly reachable Git objects, never prints secret values.
import {execFileSync} from 'node:child_process';
const git=(args,options={})=>execFileSync('git',args,{maxBuffer:256*1024*1024,...options});
const base=process.argv[2];
if(!base||!/^[a-f0-9]{40}$/.test(base)||/^0+$/.test(base))throw Error('Supply a valid reviewed base commit');
git(['merge-base','--is-ancestor',base,'HEAD']);
const objects=git(['rev-list','--objects',base+'..HEAD']).toString().trim().split('\n').filter(Boolean).map(x=>x.split(' ')[0]);
const data=git(['cat-file','--batch'],{input:objects.join('\n')+'\n'});
const patterns=[/-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----/,/\bgh[pousr]_[A-Za-z0-9]{30,}\b/,/\bgithub_pat_[A-Za-z0-9_]{40,}\b/,/\bAKIA[0-9A-Z]{16}\b/,/\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{15,}\b/];
let offset=0,blobs=0,findings=0;
while(offset<data.length){const end=data.indexOf(10,offset);if(end<0)break;const [,type,length]=data.subarray(offset,end).toString().split(' ');const value=data.subarray(end+1,end+1+Number(length));offset=end+2+Number(length);if(type!=='blob')continue;blobs++;if(patterns.some(p=>p.test(value.toString('utf8'))))findings++;}
console.log(JSON.stringify({newBlobs:blobs,credentialPatternMatches:findings}));
if(findings)process.exitCode=1;
