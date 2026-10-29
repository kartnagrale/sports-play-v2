import fs from 'node:fs/promises';
import crypto from 'node:crypto';
const directory='docs/phase-7/evidence';await fs.mkdir(directory,{recursive:true});
const images=[];
for(const service of ['backend','frontend','db','gateway']){
 const bytes=await fs.readFile(`tmp/trivy-${service}.json`),report=JSON.parse(bytes);
 const findings=(report.Results||[]).flatMap(result=>(result.Vulnerabilities||[]).map(v=>({id:v.VulnerabilityID,package:v.PkgName,installed:v.InstalledVersion,fixed:v.FixedVersion||null,severity:v.Severity,status:v.Status,type:result.Type,target:result.Target,primaryUrl:v.PrimaryURL,applicability:v.VulnerabilityID==='CVE-2026-47890'?'SSE view-fragment path not configured; affected package retained':v.VulnerabilityID==='CVE-2026-47884'?'XSLT view path not configured; affected package retained':'Package match; reachability not cleared. Owner triage required.'})));
 const counts=findings.reduce((a,v)=>(a[v.severity]=(a[v.severity]||0)+1,a),{});
 images.push({service,image:report.ArtifactName,imageId:report.Metadata?.ImageID,os:report.Metadata?.OS,scanCreatedAt:report.CreatedAt,rawReportSha256:crypto.createHash('sha256').update(bytes).digest('hex'),counts,findings});
 const sbom=JSON.parse(await fs.readFile(`tmp/sbom-${service}.json`));
 await fs.writeFile(`${directory}/sbom-${service}.cdx.json`,JSON.stringify(sbom,null,2)+'\n');
}
const maven=JSON.parse(await fs.readFile('tmp/maven-osv.json'));
const npm=JSON.parse(await fs.readFile('tmp/npm-audit.json'));
const record={date:'2026-10-29',scanner:(await fs.readFile('tmp/trivy-image.txt','utf8')).trim(),maven:{source:maven.source,packageCount:maven.packages.length,matches:maven.packages.filter(p=>p.vulnerabilities.length)},npm:{source:'npm audit --package-lock-only',counts:npm.metadata.vulnerabilities},images,productionRelease:'BLOCKED',securityOwnerApproval:'PENDING',pushPermitted:false};
await fs.writeFile(`${directory}/security-inventory.json`,JSON.stringify(record,null,2)+'\n');
await fs.copyFile('tmp/maven-sbom.json',`${directory}/sbom-maven.cdx.json`);
await fs.copyFile('tmp/npm-sbom.json',`${directory}/sbom-npm.cdx.json`);
console.log(JSON.stringify({mavenMatches:record.maven.matches.length,npm:record.npm.counts,images:images.map(i=>({service:i.service,counts:i.counts}))}));
