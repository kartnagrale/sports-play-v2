import {test,expect} from "@playwright/test";
import {Session,createTestChampionship} from "../../tests/npl-api-client.mjs";

// Explicit opt-in. This suite must use the disposable stack, never a company installation.
test.skip(process.env.NPL_BROWSER_ENABLED!=="true","Requires authorized browser automation and disposable NPL stack");
test("settings keyboard editing, real submission, contract and persisted reload",async({page})=>{
  const root=new Session();
  await root.request("/auth/login","POST",{email:process.env.NPL_TEST_EMAIL,password:process.env.NPL_TEST_PASSWORD});
  const {admin,cid}=await createTestChampionship(root,"NPL browser regression");
  await page.context().addCookies([...admin.cookies].map(([name,value])=>({name,value,domain:"localhost",path:"/",httpOnly:name==="neml_auth",secure:false,sameSite:"Lax" as const})));
  await page.addInitScript(id=>sessionStorage.setItem("sports_active_championship",id),cid);
  await page.goto("http://localhost:18090/admin/championship-settings");
  await expect(page.getByRole("heading",{name:"Auction & roster settings"})).toBeVisible();
  await page.getByLabel("Auction name",{exact:true}).fill("NPL browser auction");
  await page.getByLabel("Maximum squad size",{exact:true}).fill("11");
  await page.getByLabel("Minimum male",{exact:true}).fill("0");
  await page.getByLabel("Minimum female",{exact:true}).fill("3");
  const purse=page.getByLabel("Team purse",{exact:true});
  await purse.focus();await purse.press("ControlOrMeta+A");await purse.press("Backspace");
  await expect(purse).toHaveValue("");await expect(page.getByRole("button",{name:"Save & activate"})).toBeDisabled();
  await purse.pressSequentially("100000");
  await page.getByLabel("Player base price",{exact:true}).fill("5000");
  await page.getByLabel("Bid increment",{exact:true}).fill("1000");
  expect(await purse.evaluate((input:HTMLInputElement)=>input.validity.stepMismatch)).toBe(false);
  const [response]=await Promise.all([page.waitForResponse(r=>r.url().endsWith(`/championships/${cid}/settings`)&&r.request().method()==="PUT"),page.getByRole("button",{name:"Save & activate"}).click()]);
  expect(response.status()).toBe(200);
  const payload=response.request().postDataJSON();
  expect(payload.purseLimit).toBe(100000);expect(payload.playerBasePrice).toBe(5000);expect(payload.bidIncrement).toBe(1000);
  expect(payload).not.toHaveProperty("championshipId");expect(payload).not.toHaveProperty("status");expect(payload).not.toHaveProperty("auctionId");
  await page.reload();await expect(purse).toHaveValue("100000");
  await expect(page.getByLabel("Player base price",{exact:true})).toHaveValue("5000");
  await purse.fill("-1");await expect(page.getByRole("button",{name:"Save & activate"})).toBeDisabled();
});
