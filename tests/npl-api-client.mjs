import assert from 'node:assert/strict';
export const base=process.env.NPL_TEST_URL || 'http://localhost:18090/api';
if(!/^http:\/\/(localhost|127\.0\.0\.1):18090\/api$/.test(base))throw Error('Disposable NPL stack only');
export class Session {
  cookies=new Map();
  csrfRequest=null;
  async raw(path,method='GET',body,extraHeaders={}) {
    if(method!=='GET'){this.csrfRequest ||= this.request('/auth/csrf').then(data=>data.token).finally(()=>{this.csrfRequest=null;});extraHeaders={...extraHeaders,'X-XSRF-TOKEN':await this.csrfRequest};}
    const headers={Cookie:[...this.cookies].map(([k,v])=>`${k}=${v}`).join('; '),...extraHeaders};
    if(body!==undefined&&!(body instanceof FormData)){headers['Content-Type']='application/json';body=JSON.stringify(body);}
    const response=await fetch(base+path,{method,headers,body});
    for(const cookie of response.headers.getSetCookie()){const [key,value]=cookie.split(';')[0].split('=');this.cookies.set(key,value);}
    return response;
  }
  async request(path,method='GET',body,expected=200,headers={}){
    const response=await this.raw(path,method,body,headers);const text=await response.text();
    assert.equal(response.status,expected,`${method} ${path}: ${text.slice(0,1000)}`);
    return text?JSON.parse(text):null;
  }
}
export async function createTestChampionship(root,label='NPL integration'){
  const created=await root.request('/championships','POST',{name:label,sportType:'CRICKET',adminFullName:'NPL Test Administrator',adminEmail:`npl-${crypto.randomUUID()}@example.invalid`,adminMobile:'9999999999'});
  const admin=new Session(),password=`Npl-test-${crypto.randomUUID()}!`;
  await admin.request('/auth/activate','POST',{token:created.admin.setupPath.split('=')[1],password},204);
  await admin.request('/auth/login','POST',{email:created.admin.email,password});
  return {admin,cid:created.championship.id};
}
