import {fixtureWrite} from "./configuration-fixture";
import {test,expect,request,type APIRequestContext,type Page,type WebSocketRoute} from "@playwright/test";
import crypto from "node:crypto";
import fs from "node:fs";
const api=process.env.PHASE4_API_URL||"http://localhost:58094/api";
const evidence=process.env.UI_EVIDENCE||"../docs/phase-4/evidence";
const fixtures="../backend/target/phase4-fixtures/";
test.skip(!process.env.PHASE4_API_URL,"Use tools/verify-phase4.ps1");test.describe.configure({mode:"serial"});test.setTimeout(180000);
let platform:APIRequestContext,admin:APIRequestContext,foreign:APIRequestContext,cid:string,aid:string,otherCid:string,room:string,foreignSetup:string;
const account={email:`import-${crypto.randomUUID()}@example.invalid`,password:crypto.randomUUID()};
const foreignAccount={email:`foreign-${crypto.randomUUID()}@example.invalid`,password:crypto.randomUUID()},passcode=crypto.randomUUID();
async function command(client:APIRequestContext,path:string,data?:unknown,status=200,method="POST"){
 const prepared=await fixtureWrite(client,api,path,method,data,2);data=prepared.data;
 const csrf=await(await client.get(api+"/auth/csrf")).json();const response=await client.fetch(api+path,{method,headers:{...prepared.headers,"X-XSRF-TOKEN":csrf.token},data});expect(response.status(),`${method} ${path}: ${await response.text()}`).toBe(status);return status===204?null:response.json();
}
async function upload(file:string,client=admin,champ=cid,status=200){const csrf=await(await client.get(api+"/auth/csrf")).json();const response=await client.post(api+`/championships/${champ}/admin/players/imports`,{headers:{"X-XSRF-TOKEN":csrf.token},multipart:{file:{name:file,mimeType:"application/octet-stream",buffer:fs.readFileSync(fixtures+file)}}});expect(response.status(),await response.text()).toBe(status);return response.status()===401?null:response.json();}
const base=()=>`/championships/${cid}/admin/players`;
const confirm=(p:any,status=200,checksum=p.checksum)=>command(admin,base()+`/imports/${p.importId}/confirm`,{checksum},status);
async function all(){return(await admin.get(api+`/championships/${cid}/players`)).json();}
test.beforeAll(async()=>{
 fs.mkdirSync(evidence,{recursive:true});platform=await request.newContext();admin=await request.newContext();foreign=await request.newContext();
 await command(platform,"/auth/login",{email:process.env.APP_BOOTSTRAP_EMAIL,password:process.env.APP_BOOTSTRAP_PASSWORD});
 const created=await command(platform,"/championships",{name:"Phase 4 Registration",sportType:"BADMINTON",isPublic:false,passcode,adminFullName:"Championship Administrator",adminEmail:account.email,adminMobile:"9999999999"});cid=created.championship.id;room=created.championship.roomCode;
 await command(admin,"/auth/activate",{token:created.admin.setupPath.split("=")[1],password:account.password},204);await command(admin,"/auth/login",account);
 const other=await command(platform,"/championships",{name:"Isolated Championship",sportType:"CRICKET",isPublic:false,adminFullName:"Foreign Administrator",adminEmail:foreignAccount.email,adminMobile:"9999999999"});otherCid=other.championship.id;foreignSetup=other.admin.setupPath;
 const s=await command(admin,`/championships/${cid}/settings`,{auctionName:"Phase 4 Auction",maxSquadSize:3,purseLimit:10000000,minMale:0,minFemale:0,playerBasePrice:50000,bidIncrement:10000,timerSeconds:300,customRules:"{}"},200,"PUT");aid=s.auctionId;
 if(process.env.PHASE4_PHOTO_ONLY==="true"){const p=await upload("valid.xlsx");await confirm(p);}
});
test.afterAll(async()=>{await platform?.dispose();await admin?.dispose();await foreign?.dispose();});
test("Excel API: validation, shared manual defaults, atomicity, retries, concurrency and tenant checks",async()=>{
 const template=await admin.get(api+base()+"/imports/template");expect(template.status()).toBe(200);expect((await template.body()).subarray(0,2).toString()).toBe("PK");
 const invalid=await upload("invalid.xlsx");expect(invalid.invalid).toBe(1);await confirm(invalid,409);expect(await all()).toHaveLength(0);
 const dup=await upload("duplicates.xlsx");expect(dup.invalid).toBe(2);
 const preview=await upload("valid.xlsx");expect(preview.status).toBe("PREVIEW");expect(await all()).toHaveLength(0);
 await confirm(preview,409,"wrong-checksum");expect(await all()).toHaveLength(0);
 await upload("valid.xlsx",foreign,cid,401);await upload("valid.xlsx",platform,cid,403);await upload("valid.xlsx",admin,otherCid,403);
 await command(admin,`/championships/${otherCid}/admin/players/imports/${preview.importId}/confirm`,{checksum:preview.checksum},403);
 // Concurrent confirmations of the same validated content are one committed result.
 const results=await Promise.all([confirm(preview),confirm(preview)]);expect(results.map(x=>x.imported)).toEqual([2,2]);expect(await all()).toHaveLength(2);
 const saved=await all();expect(saved[0].fullName).toBe(saved[1].fullName);expect(new Set(saved.map((p:any)=>p.auctionOrder)).size).toBe(2);expect(saved.find((p:any)=>p.auctionOrder!==10).basePrice).toBe(50000);
 expect((await upload("valid.xlsx")).status).toBe("COMPLETED");expect((await confirm(preview)).imported).toBe(2);
 const manual=await command(admin,base(),{fullName:"Manual Player",gender:"MALE",playerReference:"manual-1"});expect(manual.basePrice).toBe(50000);expect(manual.auctionOrder).toBe(11);
 await command(admin,base()+`/${manual.id}`,{fullName:"Manual Edited",gender:"MALE",basePrice:70000},200,"PUT");
 await command(admin,base(),{fullName:"Duplicate order",gender:"MALE",auctionOrder:10},409);
 const orders=await upload("orders.xlsx");expect(orders.invalid).toBe(0);
 await command(admin,base(),{fullName:"Concurrent registration",gender:"MALE",auctionOrder:100});
 await confirm(orders,409);expect((await all()).some((p:any)=>p.fullName==="New")).toBe(false);
 const revalidated=await upload("orders.xlsx");expect(revalidated.invalid).toBe(1);
 const noRef=await upload("noref.xlsx");await confirm(noRef,409);const accepted=await command(admin,base()+`/imports/${noRef.importId}/confirm`,{checksum:noRef.checksum,allowUnreferenced:true});expect(accepted.imported).toBe(1);
 const missingCsrf=await admin.post(api+base()+"/imports",{multipart:{file:{name:"valid.xlsx",mimeType:"application/octet-stream",buffer:fs.readFileSync(fixtures+"valid.xlsx")}}});expect([401,403]).toContain(missingCsrf.status());
});
test("Excel browser: invalid preview, correction, confirmation and actual registered players",async({page})=>{
 await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill(account.email);await page.getByLabel("Password",{exact:true}).fill(account.password);await page.getByTestId("login-submit").click();await expect(page.getByRole("navigation")).toBeVisible();await page.goto("/players");
 await page.getByRole("button",{name:"Add Player",exact:true}).click();await page.getByLabel("Full Name",{exact:true}).fill("Browser Manual Player");await page.getByLabel("Player Reference",{exact:true}).fill("BROWSER-MANUAL");await page.getByRole("button",{name:"Save",exact:true}).click();await expect(page.getByRole("dialog")).toHaveCount(0);expect((await all()).some((p:any)=>p.fullName==="Browser Manual Player")).toBe(true);
 const download=page.waitForEvent("download");await page.getByRole("button",{name:"Download Excel Template",exact:true}).click();expect((await download).suggestedFilename()).toBe("play-neml-players.xlsx");
 await page.getByRole("button",{name:"Import Players",exact:true}).click();await page.getByLabel("Excel workbook (.xlsx)").setInputFiles(fixtures+"invalid.xlsx");await page.getByRole("button",{name:"Validate workbook",exact:true}).click();await expect(page.getByRole("dialog")).toContainText("Gender must be MALE or FEMALE");await expect(page.getByRole("button",{name:"Review confirmation"})).toBeDisabled();await page.screenshot({path:`${evidence}/excel-invalid.png`});
 // Large real workbook exercises 2,000-row streaming import and complete UI flow.
 await page.getByLabel("Excel workbook (.xlsx)").setInputFiles(fixtures+"large.xlsx");await page.getByRole("button",{name:"Validate workbook",exact:true}).click();await expect(page.getByRole("dialog")).toContainText("2000 rows");await page.screenshot({path:`${evidence}/excel-preview.png`});
 await page.getByRole("button",{name:"Review confirmation"}).click();await expect(page.getByRole("dialog")).toContainText("Phase 4 Registration");await page.getByRole("button",{name:"Confirm import",exact:true}).click();await expect(page.getByRole("dialog")).toContainText("2000 imported",{timeout:60000});await page.screenshot({path:`${evidence}/excel-result.png`});
 await page.getByRole("button",{name:"Return to player management"}).click();await page.getByRole("searchbox",{name:"Search players"}).fill("Player 1999");await expect(page.locator("tbody")).toContainText("Player 1999");
 for(const theme of ["light","dark"]){if(await page.locator("html").evaluate(el=>el.classList.contains("dark"))!==(theme==="dark"))await page.getByRole("button",{name:"Toggle theme",exact:true}).click();await page.setViewportSize({width:375,height:812});await page.screenshot({path:`${evidence}/players-mobile-${theme}.png`});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);}
});

async function putPhoto(file:string,pid:string,client=admin,champ=cid,expected=200){const csrf=await(await client.get(api+"/auth/csrf")).json();const response=await client.put(api+`/championships/${champ}/admin/players/${pid}/photo`,{headers:{"X-XSRF-TOKEN":csrf.token},multipart:{file:{name:"../../untrusted-name.exe",mimeType:"application/octet-stream",buffer:fs.readFileSync(fixtures+file)}}});expect(response.status(),await response.text()).toBe(expected);return response.status()===401?null:response.json();}
test("Photograph APIs: private retrieval, content checks, replacement, removal and championship isolation",async()=>{
 const player=(await all()).find((p:any)=>p.playerReference==="NEML-001");expect(player.photoUrl??null).toBeNull();
 await putPhoto("invalid.jpg",player.id,admin,cid,400);await putPhoto("unsupported.gif",player.id,admin,cid,400);await putPhoto("portrait.png",player.id,foreign,cid,401);
 await command(foreign,"/auth/activate",{token:foreignSetup.split("=")[1],password:foreignAccount.password},204);await command(foreign,"/auth/login",foreignAccount);
 await putPhoto("portrait.png",player.id,foreign,cid,403);await putPhoto("portrait.png",player.id,admin,otherCid,403);await putPhoto("portrait.png",crypto.randomUUID(),admin,cid,404);
 const csrf=await(await admin.get(api+"/auth/csrf")).json();const large=await admin.put(api+base()+`/${player.id}/photo`,{headers:{"X-XSRF-TOKEN":csrf.token},multipart:{file:{name:"huge.jpg",mimeType:"image/jpeg",buffer:Buffer.alloc(5242881)}}});expect([401,413]).toContain(large.status());
 const one=await putPhoto("portrait.png",player.id);const response=await admin.get(api+one.photoUrl);expect(response.status()).toBe(200);expect(response.headers()["content-type"]).toContain("image/jpeg");expect(response.headers()["cache-control"]).toContain("no-store");expect((await response.body())[0]).toBe(255);
 const anonymous=await request.newContext();try{expect((await anonymous.get(api+one.photoUrl)).status()).toBe(401);}finally{await anonymous.dispose();}
 expect((await foreign.get(api+one.photoUrl)).status()).toBe(403);expect((await admin.get(api+one.photoUrl.replace(cid,otherCid))).status()).toBe(403);
 const joined=await command(platform,"/championships/join-room",{roomCode:room,passcode});const viewerClient=await request.newContext({extraHTTPHeaders:{Authorization:`Bearer ${joined.token}`}});
 try{expect((await viewerClient.get(api+one.photoUrl)).status()).toBe(200);expect((await viewerClient.get(api+one.photoUrl.replace(cid,otherCid))).status()).toBe(403);await putPhoto("portrait.png",player.id,viewerClient,cid,403);}finally{await viewerClient.dispose();}
 const cookies=await admin.storageState();const loggedOut=await request.newContext({storageState:cookies}),revoked=await request.newContext({storageState:cookies});
 try{await command(loggedOut,"/auth/logout",undefined,204);expect((await loggedOut.get(api+one.photoUrl)).status()).toBe(401);expect((await revoked.get(api+one.photoUrl)).status()).toBe(401);await putPhoto("portrait.png",player.id,revoked,cid,401);}finally{await loggedOut.dispose();await revoked.dispose();await command(admin,"/auth/login",account);}
 expect((await upload("valid.xlsx")).status).toBe("COMPLETED");expect((await all()).find((p:any)=>p.id===player.id).photoUrl).toBe(one.photoUrl);
 const two=await putPhoto("replacement.jpg",player.id);expect(two.photoUrl).not.toBe(one.photoUrl);expect((await admin.get(api+one.photoUrl)).status()).toBe(404);expect((await admin.get(api+two.photoUrl)).status()).toBe(200);
 const removed=await command(admin,base()+`/${player.id}/photo`,undefined,200,"DELETE");expect(removed.photoUrl??null).toBeNull();expect((await admin.get(api+two.photoUrl)).status()).toBe(404);
 for(const field of ["status","teamId","basePrice","soldPrice","auctionOrder","playerReference"])expect(removed[field]).toEqual(player[field]);
});

test("Photograph browser and live auction: preview/save, fallback, viewer access, rankings and SOLD image",async({page,browser})=>{
 const errors:string[]=[];page.on("pageerror",e=>errors.push(e.message));
 await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill(account.email);await page.getByLabel("Password",{exact:true}).fill(account.password);await page.getByTestId("login-submit").click();await expect(page.getByRole("navigation")).toBeVisible();await page.goto("/players");
 await page.getByRole("searchbox",{name:"Search players"}).fill("Asha Rao");const row=page.locator("tbody tr").filter({hasText:"NEML-001"});await row.getByRole("button",{name:"Add Photo for Asha Rao"}).click();await page.getByLabel("Photograph",{exact:true}).setInputFiles(fixtures+"portrait.png");await expect(page.getByAltText("Preview for Asha Rao")).toBeVisible();await page.screenshot({path:`${evidence}/photo-preview.png`});await page.getByRole("button",{name:"Save Changes"}).click();await expect(page.getByRole("dialog")).toHaveCount(0);await expect(row.locator("img")).toBeVisible();await expect.poll(()=>row.locator("img").evaluate((img:HTMLImageElement)=>img.complete&&img.naturalWidth>0)).toBe(true);await page.screenshot({path:`${evidence}/photo-player-table.png`});
 await mobileThemes(page,"photo-players");
 await page.getByLabel("Players without photographs",{exact:true}).check();await expect(page.locator("tbody tr")).toHaveCount(1);await page.getByLabel("Players without photographs",{exact:true}).uncheck();
 await row.getByRole("button",{name:"Change Photo for Asha Rao"}).click();await page.getByLabel("Photograph",{exact:true}).setInputFiles(fixtures+"replacement.jpg");await page.getByRole("button",{name:"Save Changes"}).click();await expect(page.getByRole("dialog")).toHaveCount(0);await expect(row.locator("img")).toBeVisible();
 await row.getByRole("button",{name:"Change Photo for Asha Rao"}).click();await page.getByRole("button",{name:"Remove Photo",exact:true}).click();await page.getByRole("button",{name:"Save Changes"}).click();await expect(page.getByRole("dialog")).toHaveCount(0);await expect(row.locator("img")).toHaveCount(0);
 const player=(await all()).find((p:any)=>p.playerReference==="NEML-001");await putPhoto("portrait.png",player.id);
 const teams:any[]=[];
 for(const [name,code] of [["Phase Aces","PHA"],["Phase Blazers","PHB"]])teams.push(await command(admin,`/championships/${cid}/admin/teams`,{teamName:name,shortCode:code,captainFullName:"Test Captain",captainEmail:`photo-captain-${crypto.randomUUID()}@example.invalid`,captainMobile:"9999999999"}));
 const captain=await request.newContext();const captainPassword=crypto.randomUUID();await command(captain,"/auth/activate",{token:teams[0].setupPath.split("=")[1],password:captainPassword},204);await command(captain,"/auth/login",{email:teams[0].captainEmail,password:captainPassword});await putPhoto("portrait.png",player.id,captain,cid,403);await upload("valid.xlsx",captain,cid,403);
 const viewerContext=await browser.newContext({viewport:{width:1440,height:960}});
 try{
  const viewer=await viewerContext.newPage();await viewer.emulateMedia({reducedMotion:"no-preference"});viewer.on("pageerror",e=>errors.push(e.message));const routes:WebSocketRoute[]=[];let saleFrame:string|Buffer|null=null;
  await viewer.routeWebSocket("**/ws/**",route=>{routes.push(route);const server=route.connectToServer();server.onMessage(message=>{route.send(message);if(String(message).includes("PLAYER_SOLD")){saleFrame=message;route.send(message);}});});
  await viewer.goto("/room/join");await viewer.getByLabel("Room code",{exact:true}).fill(room);await viewer.getByLabel("Passcode",{exact:true}).fill(passcode);await viewer.getByRole("button",{name:"Enter viewer room"}).click();await expect(viewer.getByRole("navigation")).toBeVisible();await viewer.goto("/auction");await page.goto("/auction");await expect(page.getByText("Live connection",{exact:true})).toBeVisible();await expect(viewer.getByText("Live connection",{exact:true})).toBeVisible();
  const a=`/championships/${cid}/auctions/${aid}`;await command(admin,a+"/admin/start");await expect(page.getByTestId("current-player-name")).toHaveText(player.fullName);await expect(viewer.locator(".spotlight-identity img")).toBeVisible();await expect.poll(()=>viewer.locator(".spotlight-identity img").evaluate((img:HTMLImageElement)=>img.complete&&img.naturalWidth>0)).toBe(true);
  const beforeSrc=await viewer.locator(".spotlight-identity img").getAttribute("src");const beforePhoto=(await all()).find((p:any)=>p.id===player.id).photoUrl;await putPhoto("replacement.jpg",player.id);await expect.poll(()=>viewer.locator(".spotlight-identity img").getAttribute("src")).not.toBe(beforeSrc);await expect(viewer.locator(".spotlight-identity img")).toBeVisible();await expect.poll(()=>viewer.locator(".spotlight-identity img").evaluate((img:HTMLImageElement)=>img.complete&&img.naturalWidth>0)).toBe(true);const changed=(await all()).find((p:any)=>p.id===player.id).photoUrl;expect(changed).not.toBe(beforePhoto);
  await command(captain,a+"/bid",{playerId:player.id,teamId:teams[0].teamId,amount:50000});await expect(viewer.getByTestId("bidder-PHA")).toHaveAttribute("data-rank","1");await viewer.evaluate(()=>{const frames:unknown[]=[];(window as any).__photoRankMotion=frames;const end=performance.now()+1500;const sample=()=>{frames.push(Array.from(document.querySelectorAll("[data-motion-id]")).map(el=>({id:el.getAttribute("data-motion-id"),top:el.getBoundingClientRect().top,transform:getComputedStyle(el).transform,animations:el.getAnimations().filter(a=>a.playState==="running").length})));if(performance.now()<end)requestAnimationFrame(sample);};requestAnimationFrame(sample);});await command(admin,a+"/bid",{playerId:player.id,teamId:teams[1].teamId,amount:60000});await expect.poll(()=>viewer.evaluate(()=>(window as any).__photoRankMotion.some((frame:any[])=>frame.some(row=>row.animations>0)))).toBe(true);fs.writeFileSync(`${evidence}/photo-ranking-motion.json`,JSON.stringify(await viewer.evaluate(()=>(window as any).__photoRankMotion),null,2));await expect(viewer.getByTestId("bidder-PHB")).toHaveAttribute("data-rank","1");await viewer.screenshot({path:`${evidence}/auction-photo-spotlight.png`});
  await mobileThemes(viewer,"auction-photo-spotlight");
  await command(admin,a+"/admin/sell");await expect(viewer.getByTestId("sold-celebration")).toBeVisible();await expect(viewer.getByTestId("sold-celebration").locator("img")).toBeVisible();await expect.poll(()=>viewer.getByTestId("sold-celebration").locator("img").evaluate((img:HTMLImageElement)=>img.complete&&img.naturalWidth>0&&Number(getComputedStyle(img).opacity)>=.99)).toBe(true);await viewer.screenshot({path:`${evidence}/auction-photo-sold.png`});await viewer.setViewportSize({width:375,height:812});await viewer.screenshot({path:`${evidence}/auction-photo-sold-mobile.png`});await page.keyboard.press("Escape");await viewer.keyboard.press("Escape");
  await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);expect(saleFrame).not.toBeNull();expect(String(saleFrame)).toContain(changed);expect(String(saleFrame)).toContain(player.id);routes[0].send(saleFrame!);await viewer.waitForTimeout(350);await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);
  await expect(viewer.locator(".spotlight-identity img")).toHaveCount(0);await expect(viewer.getByTestId("current-player-name")).toHaveText("Asha Rao");
  const sold=(await all()).find((p:any)=>p.id===player.id);expect(sold.status).toBe("SOLD");expect(sold.soldPrice).toBe(60000);expect(sold.teamId).toBe(teams[1].teamId);
  const cosmetic=await putPhoto("portrait.png",player.id);expect(cosmetic.status).toBe("SOLD");expect(cosmetic.teamId).toBe(sold.teamId);expect(cosmetic.soldPrice).toBe(sold.soldPrice);
  await page.goto("/teams");await page.getByTestId("team-tab-PHB").click();await expect(page.getByTestId(`row-player-${player.id}`).locator("img")).toBeVisible();await page.screenshot({path:`${evidence}/photo-team-roster.png`});
  await page.goto("/history");await expect(page.locator("tbody img").first()).toBeVisible();await page.screenshot({path:`${evidence}/photo-auction-history.png`});await viewer.goto("/players");await expect(viewer.getByRole("button",{name:"Import Players",exact:true})).toHaveCount(0);await expect(viewer.getByRole("button",{name:/Photo for/})).toHaveCount(0);
 }finally{await viewerContext.close();await captain.dispose();}
 expect(errors).toEqual([]);
});
async function mobileThemes(page:Page,prefix:string){await page.setViewportSize({width:375,height:812});for(const theme of ["light","dark"]){if(await page.locator("html").evaluate(el=>el.classList.contains("dark"))!==(theme==="dark"))await page.getByRole("button",{name:"Toggle theme",exact:true}).click();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);await page.screenshot({path:`${evidence}/${prefix}-mobile-${theme}.png`});}await page.setViewportSize({width:1440,height:960});}
