import {fixtureWrite} from "./configuration-fixture";
import { test,expect,request,type APIRequestContext,type Page,type WebSocketRoute } from "@playwright/test";
import crypto from "node:crypto";
import fs from "node:fs";
const api=process.env.PHASE3_API_URL || "http://localhost:58093/api";
const evidence=process.env.UI_EVIDENCE || "../docs/phase-3/evidence";
test.skip(!process.env.PHASE3_API_URL,"Use tools/verify-phase3-ui.ps1 -Auction");
test.describe.configure({mode:"serial"});test.setTimeout(180000);
const arenaName=`NEML Championship Arena ${crypto.randomUUID().slice(0,8)}`;
const password=crypto.randomUUID(); const adminAccount={email:`arena-admin-${crypto.randomUUID()}@example.invalid`,password};
let staff:APIRequestContext,owner:APIRequestContext,cid:string,aid:string,room:string,players:any[]=[];
const captains:{account:{email:string;password:string};team:any;client:APIRequestContext}[]=[];
async function command(client:APIRequestContext,path:string,data?:unknown,expected=200,method="POST") {
 const prepared=await fixtureWrite(client,api,path,method,data,3);data=prepared.data;
  const csrf=await (await client.get(api+"/auth/csrf")).json();
  const response=await client.fetch(api+path,{method,headers:{...prepared.headers,"X-XSRF-TOKEN":csrf.token},data});
  expect(response.status(),method+" "+path).toBe(expected);return expected===204?null:response.json();
}
const auction=()=>`/championships/${cid}/auctions/${aid}`;
async function snapshot(){const response=await owner.get(api+auction()+"/state");expect(response.status()).toBe(200);return response.json();}
async function signIn(page:Page,account:{email:string;password:string}){await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill(account.email);await page.getByLabel("Password",{exact:true}).fill(account.password);await page.getByTestId("login-submit").click();await expect(page.getByRole("navigation",{name:"Role-based navigation",exact:true})).toBeVisible();await page.goto("/auction");await expect(page.getByText("Live connection",{exact:true})).toBeVisible();}
async function capture(page:Page,name:string){await page.screenshot({path:`${evidence}/${name}.png`,fullPage:false,animations:"allow"});}
async function uiBid(page:Page,amount:number){await page.getByTestId("bid-amount").fill(String(amount));const response=page.waitForResponse(r=>r.url().endsWith("/bid")&&r.request().method()==="POST");await page.getByTestId("place-bid-btn").click();expect((await response).status()).toBe(200);}
function watch(page:Page){const errors:string[]=[];page.on("pageerror",e=>errors.push(e.message));page.on("console",m=>{if(m.type()==="error"&&/hydration|server rendered|Minified React error #4(18|23|25)/i.test(m.text()))errors.push(m.text());});return errors;}

test.beforeAll(async()=>{
  fs.mkdirSync(evidence,{recursive:true});staff=await request.newContext();owner=await request.newContext();
  await command(staff,"/auth/login",{email:process.env.APP_BOOTSTRAP_EMAIL,password:process.env.APP_BOOTSTRAP_PASSWORD});
  await command(staff,"/championships",{name:"Isolated Foreign Championship",sportType:"CRICKET",isPublic:true,adminFullName:"Foreign Administrator",adminEmail:`foreign-${crypto.randomUUID()}@example.invalid`,adminMobile:"9999999999"});
  const created=await command(staff,"/championships",{name:arenaName,sportType:"BADMINTON",isPublic:true,adminFullName:"Kavya Deshmukh",adminEmail:adminAccount.email,adminMobile:"9999999999"});cid=created.championship.id;room=created.championship.roomCode;
  await command(owner,"/auth/activate",{token:created.admin.setupPath.split("=")[1],password},204);await command(owner,"/auth/login",adminAccount);
  const settings=await command(owner,`/championships/${cid}/settings`,{auctionName:"Champions Player Auction",maxSquadSize:3,purseLimit:10000000,minMale:0,minFemale:0,playerBasePrice:50000,bidIncrement:10000,timerSeconds:300,customRules:"{}"},200,"PUT");aid=settings.auctionId;
  for(const [name,shortCode,captainName] of [["Alpha Aces","ACES","Meera Rao"],["Bravo Blazers","BLAZ","Vikram Nair"],["Charlie Chargers","CHRG","Ishita Patil"]]){
    const account={email:`arena-${crypto.randomUUID()}@example.invalid`,password:crypto.randomUUID()};const team=await command(owner,`/championships/${cid}/admin/teams`,{teamName:name,shortCode,captainFullName:captainName,captainEmail:account.email,captainMobile:"9999999999"});const client=await request.newContext();await command(client,"/auth/activate",{token:team.setupPath.split("=")[1],password:account.password},204);await command(client,"/auth/login",account);captains.push({account,team,client});
  }
  for(const [i,name] of ["Ananya Deshmukh","Rohan Kulkarni","Sana Shaikh","Arjun Menon","Priya Joshi"].entries())players.push(await command(owner,`/championships/${cid}/admin/players`,{fullName:name,gender:i%2?"MALE":"FEMALE",basePrice:50000,skillLevel:"Advanced",auctionOrder:i+1}));
});
test.afterAll(async()=>{await staff?.dispose();await owner?.dispose();for(const captain of captains)await captain.client.dispose();});

test("multi-client live arena: ranks, rapid bids, undo, committed sale, duplicates and reconnect",async({browser})=>{
  const contexts=await Promise.all([0,1,2,3].map(()=>browser.newContext({viewport:{width:1440,height:960}})));
  const serverRoutes:WebSocketRoute[]=[];
  try{
    const [admin,first,second,viewer]=await Promise.all(contexts.map(context=>context.newPage()));const errors=[admin,first,second,viewer].map(watch);
    const routes:WebSocketRoute[]=[];let saleFrame:string|Buffer|null=null;
    await viewer.routeWebSocket("**/ws/**",route=>{routes.push(route);const server=route.connectToServer();serverRoutes.push(server);route.onClose((code,reason)=>server.close({code,reason}));server.onMessage(message=>{route.send(message);if(String(message).includes("PLAYER_SOLD")){saleFrame=message;route.send(message);}});});
    await Promise.all([signIn(admin,adminAccount),signIn(first,captains[0].account),signIn(second,captains[1].account)]);
    await viewer.goto("/room/join");await viewer.getByLabel("Room code").fill(room);await viewer.getByRole("button",{name:"Enter viewer room"}).click();await expect(viewer.getByRole("navigation",{name:"Role-based navigation",exact:true})).toBeVisible();await viewer.goto("/auction");await expect(viewer.getByText("Live connection",{exact:true})).toBeVisible();
    await expect(viewer.getByTestId("bid-panel")).toHaveCount(0);await expect(viewer.getByTestId("admin-controls")).toHaveCount(0);await expect(first.getByTestId("admin-controls")).toHaveCount(0);
    await admin.getByTestId("admin-start").click();await expect(first.getByTestId("current-player-name")).toHaveText(players[0].fullName);
    expect(await viewer.locator("[data-motion-id]").evaluateAll(nodes=>nodes.map(node=>node.getAttribute("data-motion-id")))).toEqual(captains.map(c=>c.team.teamId));
    await command(owner,auction()+"/admin/sell",undefined,404);await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);
    await uiBid(first,50000);await expect(viewer.getByTestId("bidder-ACES")).toHaveAttribute("data-rank","1");
    await uiBid(second,60000);await expect(first.getByTestId("bidder-BLAZ")).toHaveAttribute("data-rank","1");
    // Record actual layout positions/WAAPI transforms around a confirmed overtaking bid.
    await viewer.evaluate(()=>{const w=window as any;w.__rankFrames=[];let frame=0;const sample=()=>{w.__rankFrames.push({frame:frame++,rows:[...document.querySelectorAll<HTMLElement>("[data-motion-id]")].map(el=>({id:el.dataset.motionId,y:Math.round(el.getBoundingClientRect().y),transform:getComputedStyle(el).transform,running:el.getAnimations().filter(a=>a.playState==="running").length}))});if(frame<80)requestAnimationFrame(sample);};requestAnimationFrame(sample);});
    await command(captains[2].client,auction()+"/bid",{playerId:players[0].id,teamId:captains[2].team.teamId,amount:70000});
    await expect(viewer.getByTestId("bidder-CHRG")).toHaveAttribute("data-rank","1");await expect(viewer.getByTestId("bidder-BLAZ")).toHaveAttribute("data-rank","2");await expect(viewer.getByTestId("bidder-ACES")).toHaveAttribute("data-rank","3");
    await capture(viewer,"auction-ranking-frame-1");await capture(viewer,"auction-ranking-frame-2");await capture(viewer,"auction-ranking-frame-3");
    await uiBid(first,80000);await uiBid(second,90000);await command(captains[2].client,auction()+"/bid",{playerId:players[0].id,teamId:captains[2].team.teamId,amount:100000});
    const csrf=await Promise.all(captains.slice(0,2).map(async c=>(await (await c.client.get(api+"/auth/csrf")).json()).token));
    const concurrent=await Promise.all(captains.slice(0,2).map((c,i)=>c.client.post(api+auction()+"/bid",{headers:{"X-XSRF-TOKEN":csrf[i]},data:{playerId:players[0].id,teamId:c.team.teamId,amount:110000}})));expect(concurrent.filter(r=>r.status()===200)).toHaveLength(1);expect(concurrent.every(r=>[200,400,409].includes(r.status()))).toBe(true);
    await admin.getByTestId("admin-pause").click();await expect(admin.getByTestId("admin-resume")).toBeVisible();const beforeUndo=await snapshot();await capture(admin,"auction-paused-admin");await admin.getByTestId("admin-undo").click();await expect.poll(async()=>Number((await snapshot()).highestBid?.amount)).toBe(100000);await expect.poll(async()=> (await snapshot()).revision).toBeGreaterThan(beforeUndo.revision);const undone=await snapshot();expect(Number(undone.highestBid.amount)).toBe(100000);await expect(viewer.getByTestId("bidder-CHRG")).toHaveAttribute("data-rank","1");await admin.getByTestId("admin-resume").click();
    const frames=await viewer.evaluate(()=>(window as any).__rankFrames);expect(frames.some((frame:any)=>frame.rows.some((row:any)=>row.running>0))).toBe(true);expect(frames.some((frame:any)=>frame.rows.some((row:any)=>row.transform!=="none" && Number(row.transform.match(/,\s*(-?\d+(?:\.\d+)?)\)$/)?.[1] || 0)!==0))).toBe(true);fs.writeFileSync(`${evidence}/auction-ranking-frames.json`,JSON.stringify(frames,null,2));
    await viewer.emulateMedia({reducedMotion:"reduce"});
    let release!:()=>void;const pending=new Promise<void>(resolve=>release=resolve);await admin.route("**/admin/sell",async route=>{await pending;await route.continue();});
    await admin.getByTestId("admin-sell").click();await expect(admin.getByTestId("sold-celebration")).toHaveCount(0);await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);release();
    for(const page of [admin,first,second,viewer])await expect(page.getByTestId("sold-celebration")).toBeVisible();
    await expect(viewer.getByTestId("sold-price")).toContainText("1.0 L");await capture(admin,"auction-sold-desktop-dark");await capture(viewer,"auction-sold-reduced-motion");expect(await viewer.locator(".sold-stamp").evaluate(el=>parseFloat(getComputedStyle(el).animationDuration))).toBeLessThan(.01);
    await viewer.setViewportSize({width:375,height:812});await capture(viewer,"auction-sold-mobile-dark");await viewer.getByRole("button",{name:"Continue to auction"}).click();await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);
    // Re-deliver the same real committed frame; it must not open a second overlay.
    expect(saleFrame).toBeTruthy();routes.at(-1)!.send(saleFrame!);await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);
    const connections=routes.length;routes.at(-1)!.close({code:1001,reason:"Test transport interruption"});await expect(viewer.getByText("Connection interrupted",{exact:true})).toBeVisible();await capture(viewer,"auction-disconnected-mobile");await expect.poll(()=>routes.length).toBeGreaterThan(connections);await expect(viewer.getByText("Live connection",{exact:true})).toBeVisible();routes.at(-1)!.send(saleFrame!);await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);
    for(const page of [admin,first,second]){if(await page.getByTestId("sold-celebration").count())await page.getByRole("button",{name:"Continue to auction"}).click();}
    await admin.unroute("**/admin/sell");await expect(admin.getByTestId("current-player-name")).toHaveText(players[1].fullName);await admin.getByTestId("admin-unsold").click();await expect(viewer.getByTestId("unsold-result")).toContainText(players[1].fullName);await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);
    await expect(viewer.getByTestId("current-player-name")).toHaveText(players[2].fullName);await expect(viewer.getByTestId("bidder-CHRG")).toHaveAttribute("data-rank","none");
    for(const [width,height,size] of [[1440,960,"desktop"],[768,1024,"tablet"],[375,812,"mobile"]] as const){await viewer.setViewportSize({width,height});for(const theme of ["light","dark"]){if(await viewer.locator("html").evaluate(el=>el.classList.contains("dark"))!==(theme==="dark"))await viewer.getByRole("button",{name:"Toggle theme",exact:true}).click();expect(await viewer.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);await capture(viewer,`auction-${size}-${theme}`);}}
    await viewer.emulateMedia({reducedMotion:"reduce"});await command(captains[0].client,auction()+"/bid",{playerId:players[2].id,teamId:captains[0].team.teamId,amount:50000});await expect(viewer.getByTestId("bidder-ACES")).toHaveAttribute("data-rank","1");expect(await viewer.locator("[data-motion-id]").evaluateAll(nodes=>nodes.flatMap(node=>node.getAnimations()).filter(a=>a.playState==="running" && Number(a.effect?.getTiming().duration)>1).length)).toBe(0);
    // Reload is a baseline reconciliation and must not replay the earlier sale.
    await viewer.reload();await expect(viewer.getByText("Live connection",{exact:true})).toBeVisible();await expect(viewer.getByTestId("sold-celebration")).toHaveCount(0);
    const roster=await (await owner.get(api+`/championships/${cid}/players`)).json();expect(roster.find((p:any)=>p.id===players[0].id).status).toBe("SOLD");expect(roster.find((p:any)=>p.id===players[0].id).teamId).toBe(captains[2].team.teamId);
    expect(errors.flat()).toEqual([]);
  }finally{await Promise.allSettled(serverRoutes.map(server=>server.close()));await Promise.all(contexts.map(context=>context.close()));}
});

test("complete team bids survive the 50-row history window; scoped reads and commands remain protected",async()=>{
  let current=await snapshot();const pid=current.currentPlayer.id;
  // One early bidder falls outside the recent 50 bids; its valid rank must remain available.
  let amount=Number(current.highestBid.amount)+10000;
  await command(captains[1].client,auction()+"/bid",{playerId:pid,teamId:captains[1].team.teamId,amount});
  for(let i=0;i<55;i++){amount+=10000;await command(captains[2].client,auction()+"/bid",{playerId:pid,teamId:captains[2].team.teamId,amount});}
  current=await snapshot();expect(current.bidHistory).toHaveLength(50);expect(current.teamBids).toHaveLength(3);expect(current.teamBids.find((b:any)=>b.teamId===captains[0].team.teamId).amount).toBe(50000);
  await command(captains[0].client,auction()+"/bid",{playerId:pid,teamId:captains[1].team.teamId,amount:amount+10000},403);
  const joined=await command(staff,"/championships/join-room",{roomCode:room});const viewer=await request.newContext({extraHTTPHeaders:{Authorization:`Bearer ${joined.token}`}});
  try{await command(viewer,auction()+"/admin/sell",undefined,403);expect((await viewer.get(api+"/championships")).status()).toBe(403);const foreign=await (await staff.get(api+"/championships")).json();const other=foreign.find((c:any)=>c.id!==cid);expect((await viewer.get(api+`/championships/${other.id}`)).status()).toBe(403);}finally{await viewer.dispose();}
  await command(owner,auction()+"/admin/sell");await command(owner,auction()+"/admin/unsold");await command(owner,auction()+"/admin/unsold");expect((await snapshot()).status).toBe("COMPLETED");
});

test("portfolio and operational pages: real metrics, filters, rosters and responsive presentation",async({page})=>{
  const errors=watch(page);
  await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill(process.env.APP_BOOTSTRAP_EMAIL!);await page.getByLabel("Password",{exact:true}).fill(process.env.APP_BOOTSTRAP_PASSWORD!);await page.getByTestId("login-submit").click();await expect(page.getByRole("navigation",{name:"Role-based navigation",exact:true})).toBeVisible();await page.goto("/admin/championships");await expect(page.getByRole("heading",{name:"Every championship. One view."})).toBeVisible();
  await page.getByRole("searchbox",{name:"Search championships"}).fill(arenaName);await expect(page.getByRole("button").filter({hasText:arenaName})).toHaveCount(1);
  for(const [width,height,size] of [[1440,960,"desktop"],[768,1024,"tablet"],[375,812,"mobile"]] as const){await page.setViewportSize({width,height});for(const theme of ["light","dark"]){if(await page.locator("html").evaluate(el=>el.classList.contains("dark"))!==(theme==="dark"))await page.getByRole("button",{name:"Toggle theme",exact:true}).click();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);await capture(page,`portfolio-${size}-${theme}`);}}
  await page.getByRole("button").filter({hasText:arenaName}).click();await expect(page.getByRole("dialog",{name:arenaName})).toBeVisible();await page.keyboard.press("Escape");
  // New browser context avoids staff/viewer credential carry-over.
  const context=await page.context().browser()!.newContext({viewport:{width:1440,height:960}});
  try{const admin=await context.newPage();const adminErrors=watch(admin);await signIn(admin,adminAccount);await expect(admin.getByRole("heading",{name:"Auction completed",exact:true})).toBeVisible();await capture(admin,"auction-completed");for(const [path,name] of [["/dashboard","operations"],["/players","players"],["/teams","teams"],["/matches","fixtures"],["/scoreboard","standings"],["/analysis","analytics"],["/competition","competition"],["/squad-confirmation","squads"],["/history","history"]]){await admin.goto(path);await expect(admin.locator("#main-content h1").first()).toBeVisible();await capture(admin,`portal-${name}-desktop`);}
    await admin.goto("/players");await admin.getByRole("searchbox",{name:"Search players"}).fill(players[0].fullName);await admin.getByLabel("Filter player status").selectOption("SOLD");await expect(admin.locator("tbody tr")).toHaveCount(1);
    await admin.goto("/teams");await admin.getByTestId("team-tab-CHRG").click();await expect(admin.getByText(players[0].fullName,{exact:true})).toBeVisible();await capture(admin,"portal-winning-roster");
    await admin.goto("/history");await admin.getByRole("searchbox",{name:"Search auction history"}).fill(players[2].fullName);await expect(admin.locator("tbody tr")).toHaveCount(50);expect(adminErrors).toEqual([]);
  }finally{await context.close();}
  expect(errors).toEqual([]);
});
