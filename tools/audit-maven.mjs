import fs from 'node:fs/promises';
const [treeFile='docs/phase-1/evidence/backend-dependency-tree.txt',output='docs/phase-1/evidence/backend-osv-audit.json']=process.argv.slice(2);
const tree=await fs.readFile(treeFile,'utf8');
const packages=[...new Map(tree.split(/\r?\n/).map(line=>{
  const match=line.match(/([\w.-]+):([\w.-]+):jar:([^:\s]+):(compile|runtime|test)/);
  return match?[`${match[1]}:${match[2]}@${match[3]}`,{name:`${match[1]}:${match[2]}`,version:match[3],scope:match[4]}]:null;
}).filter(Boolean)).values()];
if(!packages.length)throw Error('No resolved Maven packages found');
const results=[];
for(let index=0;index<packages.length;index+=50){
  const batch=packages.slice(index,index+50);
  const response=await fetch('https://api.osv.dev/v1/querybatch',{method:'POST',headers:{'Content-Type':'application/json'},
    body:JSON.stringify({queries:batch.map(p=>({package:{name:p.name,ecosystem:'Maven'},version:p.version}))}),signal:AbortSignal.timeout(30000)});
  if(!response.ok)throw Error(`OSV query failed: ${response.status}`);
  const data=await response.json();
  results.push(...batch.map((p,i)=>({...p,vulnerabilities:data.results[i]?.vulns || []})));
}
await fs.writeFile(output,JSON.stringify({scannedAt:new Date().toISOString(),source:'https://api.osv.dev/v1/querybatch',
  scope:'Resolved Maven compile/runtime/test dependencies; excludes build plugins, JDK and container OS. Matches require reachability triage.',packages:results},null,2));
console.log(JSON.stringify({scanned:results.length,matched:results.filter(p=>p.vulnerabilities.length).length,
  advisoryIds:[...new Set(results.flatMap(p=>p.vulnerabilities.map(v=>v.id)))]}));
if(results.some(p=>p.vulnerabilities.length))process.exitCode=1;
