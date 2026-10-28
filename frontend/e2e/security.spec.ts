import { test, expect } from "@playwright/test";
import fs from "node:fs";
import crypto from "node:crypto";
const backend=process.env.PHASE1_API_URL || "http://localhost:58092/api";
test.skip(!process.env.APP_BOOTSTRAP_PASSWORD,"Run through the isolated Phase 1 runtime runner");

test("staff login uses hardened cookie and provisioning activation works",async ({page})=>{
  const errors:string[]=[];page.on("pageerror",error=>errors.push(error.message));
  await page.goto("/login");
  await page.getByTestId("login-email").fill(process.env.APP_BOOTSTRAP_EMAIL!);
  await page.getByTestId("login-password").fill(process.env.APP_BOOTSTRAP_PASSWORD!);
  const login=page.waitForResponse(response=>response.url().endsWith("/auth/login")&&response.request().method()==="POST");
  await page.getByTestId("login-submit").click();
  const response=await login;expect(response.status()).toBe(200);expect(await response.json()).not.toHaveProperty("token");
  await expect(page.getByRole("navigation")).toBeVisible();
  const cookie=(await page.context().cookies()).find(c=>c.name==="neml_auth");
  expect(cookie?.httpOnly).toBe(true);expect(cookie?.sameSite).toBe("Lax");
  const csrf=await (await page.request.get(backend+"/auth/csrf")).json();
  const email=crypto.randomUUID()+"@example.invalid";
  const provisioned=await page.request.post(backend+"/championships",{headers:{"X-XSRF-TOKEN":csrf.token},data:{
    name:"Browser Security Test",sportType:"BADMINTON",isPublic:true,adminFullName:"Browser Admin",adminEmail:email,adminMobile:"9999999999"
  }});
  expect(provisioned.status()).toBe(200);
  const created=await provisioned.json();expect(created.admin).not.toHaveProperty("temporaryPassword");
  const logoutCsrf=await (await page.request.get(backend+"/auth/csrf")).json();
  expect((await page.request.post(backend+"/auth/logout",{headers:{"X-XSRF-TOKEN":logoutCsrf.token}})).status()).toBe(204);
  await page.goto(created.admin.setupPath);
  await expect(page).not.toHaveURL(/#token=/);
  const password=crypto.randomUUID();
  await page.getByLabel("Password",{exact:true}).fill(password);
  await page.getByLabel("Confirm password",{exact:true}).fill(password);
  await page.getByRole("button",{name:"Set password"}).click();
  await expect(page.getByRole("link",{name:"Sign in",exact:true})).toBeVisible();
  await page.getByRole("link",{name:"Sign in",exact:true}).click();
  await page.getByTestId("login-email").fill(email);await page.getByTestId("login-password").fill(password);
  await page.getByTestId("login-submit").click();
  await expect(page.getByRole("navigation")).toBeVisible();
  await expect(page.getByTestId("user-name")).toHaveText("Browser Admin");
  expect(errors).toEqual([]);
});

test("viewer room reload preserves read-only access and authorized socket",async({page})=>{
  const fixture=JSON.parse(fs.readFileSync("../docs/phase-1/evidence/runtime-security.json","utf8")).publicRoom;
  await page.goto("/room/join");
  await page.getByLabel("Room code").fill(fixture.roomCode);
  await page.getByRole("button",{name:"Enter viewer room"}).click();
  await expect(page.getByRole("navigation")).toBeVisible();
  await page.reload();
  await expect(page.getByRole("navigation")).toBeVisible();
  await expect(page.getByRole("link",{name:"Players",exact:true})).toHaveCount(0);
  const socket=await page.evaluate(async({backend,cid})=>{
    const ws=new WebSocket(backend.replace("http","ws").replace("/api","/ws"));
    return new Promise<boolean>((resolve,reject)=>{
      const timeout=setTimeout(()=>{ws.close();reject(Error("Viewer socket timed out"));},5000);
      ws.onopen=()=>ws.send(`CONNECT\naccept-version:1.2\nAuthorization:Bearer ${sessionStorage.getItem("sports_viewer_token")}\n\n\0`);
      ws.onmessage=event=>{if(String(event.data).startsWith("CONNECTED")){
        ws.send(`SUBSCRIBE\nid:browser\ndestination:/topic/championship/${cid}/matches\n\n\0`);
        clearTimeout(timeout);ws.close();resolve(true);
      }else if(String(event.data).startsWith("ERROR")){clearTimeout(timeout);ws.close();reject(Error("Viewer socket rejected"));}};
    });
  },{backend,cid:fixture.championshipId});
  expect(socket).toBe(true);
});

test("anonymous browser socket cannot connect",async({page})=>{
  await page.goto("/login");
  const denied=await page.evaluate(async backend=>{
    const ws=new WebSocket(backend.replace("http","ws").replace("/api","/ws"));
    return new Promise<boolean>(resolve=>{
      const timer=setTimeout(()=>{ws.close();resolve(false);},5000);
      ws.onopen=()=>ws.send("CONNECT\naccept-version:1.2\n\n\0");
      ws.onmessage=event=>{if(String(event.data).startsWith("ERROR")){clearTimeout(timer);ws.close();resolve(true);}};
      ws.onclose=()=>{clearTimeout(timer);resolve(true);};
    });
  },backend);
  expect(denied).toBe(true);
});
