import { test, expect, request, type APIRequestContext, type Page } from "@playwright/test";
import crypto from "node:crypto";
import fs from "node:fs";

// Real backend, new disposable database only. Never persists credentials or setup links.
const backend = process.env.PHASE2_API_URL || "http://localhost:58093/api";
const evidence = process.env.UI_EVIDENCE || "../docs/phase-2/evidence";
test.skip(!process.env.PHASE2_API_URL, "Use tools/verify-phase2-ui.ps1 with a new disposable database");
test.setTimeout(120000);
let platform: APIRequestContext, admin: APIRequestContext;
let championship: any, other: any, captain: any, activation: any;
const accounts = {
  platform: { email: process.env.APP_BOOTSTRAP_EMAIL!, password: process.env.APP_BOOTSTRAP_PASSWORD! },
  admin: { email: `admin-${crypto.randomUUID()}@example.invalid`, password: crypto.randomUUID() },
  captain: { email: `captain-${crypto.randomUUID()}@example.invalid`, password: crypto.randomUUID() },
};
async function mutate(context: APIRequestContext, method: string, path: string, data?: unknown, status = 200) {
  const csrf = await (await context.get(backend + "/auth/csrf")).json();
  const response = await context.fetch(backend + path, { method, headers: { "X-XSRF-TOKEN": csrf.token }, data });
  expect(response.status(), `${method} ${path}`).toBe(status);
  return status === 204 ? null : response.json();
}
async function signIn(page: Page, account: { email: string; password: string }) {
  await page.goto("/login");
  await page.getByLabel("Email", { exact: true }).fill(account.email);
  await page.getByLabel("Password", { exact: true }).fill(account.password);
  await page.getByTestId("login-submit").click();
  await expect(page.getByRole("navigation", { name: "Role-based navigation" })).toBeVisible();
}
async function theme(page: Page, name: string) {
  if (await page.locator("html").evaluate(el => el.classList.contains("dark")) !== (name === "dark")) await page.getByRole("button", { name: "Toggle theme", exact: true }).click();
  await expect(page.locator("html")).toHaveClass(name);
}
async function noOverflow(page: Page) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), "No page-wide horizontal overflow").toBe(true);
}
async function screenshot(page: Page, name: string) {
  if (await page.locator("#main-content").count()) {
    await expect(page.locator("#main-content .animate-spin")).toHaveCount(0);
    await expect(page.locator('#main-content [aria-label="Loading workspace"]')).toHaveCount(0);
  }
  await page.screenshot({ path: `${evidence}/${name}.png`, fullPage: !name.includes("drawer"), animations: "disabled" });
}
function watchErrors(page: Page) {
  const errors: string[] = [];
  page.on("pageerror", error => errors.push(error.message));
  page.on("console", message => { if (message.type() === "error" && /hydration|hydrating|server rendered|Minified React error #4(18|23|25)/i.test(message.text())) errors.push(message.text()); });
  return errors;
}
test.beforeAll(async () => {
  fs.mkdirSync(evidence, { recursive: true });
  platform = await request.newContext(); admin = await request.newContext();
  await mutate(platform, "POST", "/auth/login", accounts.platform);
  for (const sport of ["BADMINTON", "CRICKET"]) {
    const created = await mutate(platform, "POST", "/championships", { name: sport === "BADMINTON" ? "NEML Champions Cup" : "NEML Cricket League", sportType: sport, isPublic: true, adminFullName: "Aarav Sharma", adminEmail: sport === "BADMINTON" ? accounts.admin.email : `other-${crypto.randomUUID()}@example.invalid`, adminMobile: "9999999999" });
    if (sport === "BADMINTON") {
      championship = created.championship;
      await mutate(admin, "POST", "/auth/activate", { token: created.admin.setupPath.split("=")[1], password: accounts.admin.password }, 204);
      await mutate(admin, "POST", "/auth/login", accounts.admin);
    } else other = created.championship;
  }
  await mutate(admin, "PUT", `/championships/${championship.id}/settings`, { auctionName: "Champions Cup Auction", maxSquadSize: 12, purseLimit: 10000000, minMale: 0, minFemale: 0, playerBasePrice: 100000, bidIncrement: 10000, timerSeconds: 30, customRules: "{}" });
  captain = await mutate(admin, "POST", `/championships/${championship.id}/admin/teams`, { teamName: "Emerald Aces", shortCode: "ACES", captainFullName: "Meera Rao", captainEmail: accounts.captain.email, captainMobile: "9999999999" });
  const temporary = await request.newContext();
  await mutate(temporary, "POST", "/auth/activate", { token: captain.setupPath.split("=")[1], password: accounts.captain.password }, 204); await temporary.dispose();
  for (const name of ["Ananya Deshmukh", "Vikram Nair", "Rohan Kulkarni"]) await mutate(admin, "POST", `/championships/${championship.id}/admin/players`, { fullName: name, gender: "MALE", basePrice: 100000, skillLevel: "Intermediate" });
  activation = await mutate(platform, "POST", "/championships", { name: "Account Setup Review", sportType: "BADMINTON", isPublic: true, adminFullName: "Setup Reviewer", adminEmail: `setup-${crypto.randomUUID()}@example.invalid`, adminMobile: "9999999999" });
});
test.afterAll(async () => { await platform?.dispose(); await admin?.dispose(); });

test("component foundation: avatars, tabs, feedback, modal and reduced motion", async ({ page }) => {
  const cssDir = ".next-local/static/css";
  const css = fs.readdirSync(cssDir).filter(file => file.endsWith(".css")).map(file => fs.readFileSync(`${cssDir}/${file}`, "utf8")).join("\n");
  const fontClasses = fs.readFileSync(".next-local/server/app/login.html", "utf8").match(/<body class="([^"]*)"/)?.[1] || "";
  await page.route("**/avatar-valid.svg", route => route.fulfill({ contentType: "image/svg+xml", body: '<svg xmlns="http://www.w3.org/2000/svg" width="128" height="128"><rect width="128" height="128" fill="#087f70"/></svg>' }));
  await page.route("**/avatar-broken.svg", route => route.fulfill({ status: 404, body: "" }));
  // A static same-origin document, without Next's application runtime or an added app route.
  await page.route("**/__phase2-component-review", route => route.fulfill({ contentType: "text/html", body: `<html class="light"><head><style>${css}</style></head><body class="${fontClasses}"><div id="root"></div></body></html>` }));
  await page.goto("/__phase2-component-review");
  const errors = watchErrors(page);
  await page.addScriptTag({ content: fs.readFileSync("../tmp/phase2-ui-harness.js", "utf8") });
  await expect(page.getByRole("heading", { name: "Shared interface foundation" })).toBeVisible();
  const photo = page.getByRole("img", { name: "Photo preview" });
  await expect(photo.locator("img")).toHaveClass(/opacity-100/);
  await page.getByRole("button", { name: "Change avatar URL" }).click(); await expect(photo.locator("img")).toHaveCount(0); await expect(photo).toContainText("PP");
  await page.getByRole("button", { name: "Change avatar URL" }).click(); await expect(photo.locator("img")).toHaveClass(/opacity-100/);
  await expect(page.getByRole("img", { name: "Player avatar", exact: true })).toBeVisible();
  await page.getByRole("tab", { name: "Player identity" }).focus(); await page.keyboard.press("ArrowRight");
  await expect(page.getByRole("tab", { name: "Form controls" })).toBeFocused(); await expect(page.getByLabel("Invalid entry")).toHaveAttribute("aria-invalid", "true");
  for (const color of ["light", "dark"]) { await page.locator("html").evaluate((el, value) => el.setAttribute("class", value), color); await screenshot(page, `components-forms-${color}`); }
  await page.getByRole("tab", { name: "Feedback", exact: true }).click(); await page.getByText("Focus for help", { exact: true }).locator("..").focus(); await expect(page.getByRole("tooltip")).toHaveCSS("opacity", "1");
  await page.getByRole("button", { name: "Open review dialog" }).click(); const dialog = page.getByRole("dialog", { name: "Review dialog" }); await expect(dialog).toBeVisible();
  await page.getByLabel("Review note").fill("Keyboard review"); await page.keyboard.press("Escape"); await expect(dialog).toHaveCount(0); await expect(page.getByRole("button", { name: "Open review dialog" })).toBeFocused();
  await page.getByRole("tab", { name: "Player identity" }).click();
  await page.getByRole("button", { name: "Reorder players" }).click(); await expect(page.locator("[data-motion-id]").first()).toHaveAttribute("data-motion-id", "Meera Rao");
  await page.emulateMedia({ reducedMotion: "reduce" }); await page.getByRole("button", { name: "Reorder players" }).click();
  await expect(page.locator("[data-motion-id]").first()).toHaveAttribute("data-motion-id", "Ananya Deshmukh");
  await page.evaluate(()=>new Promise<void>(resolve=>requestAnimationFrame(()=>resolve())));
  expect(await page.evaluate(()=>matchMedia("(prefers-reduced-motion: reduce)").matches)).toBe(true);
  expect(await page.locator("[data-motion-id]").first().evaluate(el => el.getAnimations().filter(animation=>animation.playState==="running").length)).toBe(0);
  for (const color of ["light", "dark"]) { await page.locator("html").evaluate((el, value) => el.setAttribute("class", value), color); await screenshot(page, `components-identity-${color}`); }
  expect(errors).toEqual([]);
});

test("account screens: themes, responsive layouts, labels, persistence and errors", async ({ page }) => {
  const errors = watchErrors(page);
  for (const [name, width, height] of [["desktop", 1440, 960], ["tablet", 768, 1024], ["mobile", 375, 812], ["small", 320, 740]] as const) {
    await page.setViewportSize({ width, height });
    for (const color of ["dark", "light"]) {
      for (const path of ["/login", "/activate", "/room/join"]) {
        await page.goto(path); await theme(page, color); await noOverflow(page);
        await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
        await expect(page).toHaveTitle(/Play-Neml/);
        if (name !== "small") await screenshot(page, `${path.split("/").pop()}-${name}-${color}`);
      }
    }
  }
  await page.goto("/login"); await theme(page, "light"); await page.reload(); await expect(page.locator("html")).toHaveClass("light");
  await page.getByLabel("Email", { exact: true }).fill("invalid@example.invalid"); await page.getByLabel("Password", { exact: true }).fill("incorrect-password");
  await page.getByRole("button", { name: "Show password" }).click(); await expect(page.getByLabel("Password", { exact: true })).toHaveAttribute("type", "text");
  await page.getByTestId("login-submit").click(); await expect(page.getByRole("alert")).toBeVisible();
  await page.emulateMedia({ reducedMotion: "reduce" });
  expect(await page.getByTestId("login-submit").evaluate(el => parseFloat(getComputedStyle(el).transitionDuration))).toBeLessThan(.01);
  expect(errors).toEqual([]);
});

test("activation: fragment removal, mismatch, server rejection, success and consumed-link recovery", async ({ page }) => {
  await page.goto(activation.admin.setupPath);
  await expect(page).not.toHaveURL(/#token=/);
  const password = crypto.randomUUID();
  await page.getByLabel("Password", { exact: true }).fill(password); await page.getByLabel("Confirm password", { exact: true }).fill("different-password");
  await page.getByRole("button", { name: "Set password" }).click(); await expect(page.locator(".form-error")).toContainText("Passwords must match");
  await page.getByLabel("Confirm password", { exact: true }).fill(password); await page.getByRole("button", { name: "Set password" }).click();
  await expect(page.getByRole("status")).toContainText("Your password is ready"); await screenshot(page, "activation-success");
  await page.getByRole("link", { name: "Sign in", exact: true }).click();
  await signIn(page, { email: activation.admin.email, password });
  await page.getByTestId("logout-btn").click(); await expect(page).toHaveURL(/login/);
  await page.goto(activation.admin.setupPath);
  await page.getByLabel("Password", { exact: true }).fill(password); await page.getByLabel("Confirm password", { exact: true }).fill(password); await page.getByRole("button", { name: "Set password" }).click();
  await expect(page.locator(".form-error")).toContainText("new setup link", { ignoreCase: true });
  await screenshot(page, "activation-consumed-link");
  await page.goto("/activate#token=invalid-setup-link");
  await page.getByLabel("Password", { exact: true }).fill(password); await page.getByLabel("Confirm password", { exact: true }).fill(password); await page.getByRole("button", { name: "Set password" }).click(); await expect(page.locator(".form-error")).toBeVisible();
});

for (const role of ["platform", "admin", "captain", "viewer"] as const) test(`${role}: backend navigation, both themes, drawer, account and routes`, async ({ page }) => {
  const errors = watchErrors(page);
  if (role === "viewer") { await page.goto("/room/join"); await page.getByLabel("Room code").fill(championship.roomCode); await page.getByRole("button", { name: "Enter viewer room" }).click(); }
  else await signIn(page, accounts[role]);
  const nav = page.getByRole("navigation", { name: "Role-based navigation" }); await expect(nav).toBeVisible();
  const result = await page.evaluate(async backend => {
    const cid = sessionStorage.getItem("sports_active_championship"); const token = sessionStorage.getItem("sports_viewer_token");
    return (await fetch(backend + "/navigation" + (cid ? `?championshipId=${cid}` : ""), { credentials: "include", headers: token ? { Authorization: `Bearer ${token}` } : {} })).json();
  }, backend);
  expect(result.role).toBe({ platform: "SUPER_ADMIN", admin: "CHAMPIONSHIP_ADMIN", captain: "TEAM_CAPTAIN", viewer: "SPECTATOR" }[role]);
  expect(await nav.getByRole("link").count()).toBe(result.screens.length);
  expect((await nav.getByRole("link").evaluateAll(links => links.map(link => link.getAttribute("href")))).sort()).toEqual(result.screens.map((screen: any) => screen.path).sort());
  for (const screen of result.screens) {
    await nav.locator(`a[href="${screen.path}"]`).click();
    await expect(page).toHaveURL(new RegExp(screen.path.replaceAll("/", "\\/") + "$"));
    await expect(page.locator("#main-content")).toBeVisible();
    await expect(nav.locator(`a[href="${screen.path}"]`)).toHaveAttribute("aria-current", "page");
  }
  if (role === "platform") {
    await page.getByLabel("Active championship").selectOption(other.id);
    await expect(page.getByLabel("Active championship")).toHaveValue(other.id);
    await expect(page.locator("header")).toContainText(other.name);
    await page.getByLabel("Active championship").selectOption(championship.id);
    await page.getByRole("button", { name: "Collapse navigation" }).click(); await expect(page.locator(".app-shell")).toHaveAttribute("data-collapsed", "true");
    await page.getByRole("button", { name: "Expand navigation" }).click();
  }
  const first = result.screens[0]; await nav.locator(`a[href="${first.path}"]`).click();
  for (const [name, width, height] of [["desktop", 1440, 960], ["tablet", 768, 1024], ["mobile", 375, 812]] as const) {
    await page.setViewportSize({ width, height });
    for (const color of ["dark", "light"]) {
      await theme(page, color); await noOverflow(page); await screenshot(page, `${role}-${name}-${color}`);
      if (name !== "desktop") {
        const trigger = page.getByRole("button", { name: "Open navigation" }); await trigger.click();
        await expect(page.getByRole("dialog", { name: "Navigation" })).toBeVisible();
        const dialog = page.getByRole("dialog"); await screenshot(page, `${role}-drawer-${name}-${color}`);
        // Native modal prevents background focus and returns it on Escape.
        for (let i = 0; i < 4; i++) await page.keyboard.press("Tab");
        expect(await dialog.evaluate(el => el.contains(document.activeElement))).toBe(true);
        await page.keyboard.press("Escape"); await expect(dialog).toHaveCount(0); await expect(trigger).toBeFocused();
      }
    }
  }
  await page.getByRole("button", { name: "Account menu" }).focus(); await page.keyboard.press("ArrowDown");
  await expect(page.getByRole("menuitem", { name: "Account details" })).toBeFocused(); await page.keyboard.press("Enter");
  await expect(page.getByRole("dialog", { name: "Account details" })).toBeVisible(); await page.keyboard.press("Escape"); await expect(page.getByRole("button", { name: "Account menu" })).toBeFocused();
  if (role === "viewer") { await page.reload(); await page.getByRole("button", { name: "Open navigation" }).click(); await expect(page.getByRole("navigation")).toBeVisible(); await expect(page.getByRole("link", { name: "Players", exact: true })).toHaveCount(0); }
  expect(errors).toEqual([]);
});

test("player identities, search, dialog focus and real player save", async ({ page }) => {
  await signIn(page, accounts.admin); await page.getByRole("link", { name: "Players", exact: true }).click();
  await expect(page.getByRole("table")).toBeVisible(); await expect(page.getByText("Ananya Deshmukh", { exact: true })).toBeVisible();
  await page.getByRole("searchbox", { name: "Search players" }).fill("Ananya"); await expect(page.locator("tbody tr")).toHaveCount(1); await screenshot(page, "players-identities-light");
  await page.getByRole("button", { name: "Add Player", exact: true }).click(); await expect(page.getByRole("dialog", { name: "Add Player" })).toBeVisible();
  await page.getByLabel("Full Name", { exact: true }).fill("Ishita Patil"); await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0); await expect(page.getByRole("button", { name: "Add Player", exact: true })).toBeFocused();
  await page.getByRole("searchbox").fill("Ishita"); await expect(page.getByText("Ishita Patil", { exact: true })).toBeVisible();
});

test("targeted security boundary: cookies, CSRF, scoped viewer, session revocation and WebSockets", async ({ page }) => {
  await signIn(page, accounts.captain);
  const cookie = (await page.context().cookies()).find(cookie => cookie.name === "neml_auth"); expect(cookie?.httpOnly).toBe(true); expect(cookie?.sameSite).toBe("Lax");
  const unprotected = await page.request.post(backend + "/auth/logout"); expect([401, 403]).toContain(unprotected.status());
  expect((await page.request.get(backend + "/auth/me")).status()).toBe(200);
  const joined = await mutate(platform, "POST", "/championships/join-room", { roomCode: championship.roomCode });
  const viewer = await request.newContext({ extraHTTPHeaders: { Authorization: `Bearer ${joined.token}` } });
  expect((await viewer.get(backend + "/championships")).status()).toBe(403); expect((await viewer.get(backend + `/championships/${other.id}`)).status()).toBe(403); await viewer.dispose();
  const socket = await page.evaluate(async ({ backend, token, cid }) => {
    const url = backend.replace("http", "ws").replace("/api", "/ws");
    async function connect(auth: string, send = false) { return new Promise<boolean>((resolve) => {
      const ws = new WebSocket(url); let connected = false; const timer = setTimeout(() => { ws.close(); resolve(false); }, 5000);
      ws.onopen = () => ws.send(`CONNECT\naccept-version:1.2\n${auth ? `Authorization:Bearer ${auth}\n` : ""}\n\0`);
      ws.onmessage = e => { const frame = String(e.data); if (frame.startsWith("CONNECTED")) { connected = true; if (send) ws.send(`SEND\ndestination:/topic/championship/${cid}/matches\n\nforbidden\0`); else { clearTimeout(timer); ws.close(); resolve(true); } } else if (frame.startsWith("ERROR")) { clearTimeout(timer); ws.close(); resolve(send ? connected : false); } };
      ws.onclose = () => { clearTimeout(timer); resolve(send && connected); };
    }); }
    return { authorized: await connect(token), sendDenied: await connect(token, true) };
  }, { backend, token: joined.token, cid: championship.id });
  expect(socket).toEqual({ authorized: true, sendDenied: true });
  const replay = await request.newContext({ extraHTTPHeaders: { Cookie: `neml_auth=${cookie!.value}` } });
  await page.getByTestId("logout-btn").click(); await expect(page).toHaveURL(/login/); expect((await replay.get(backend + "/auth/me")).status()).toBe(401); await replay.dispose();
});
