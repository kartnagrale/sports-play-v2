import {createRequire} from 'node:module';
const require=createRequire(new URL('../frontend/package.json',import.meta.url));
const WebSocket=require('next/dist/compiled/ws');

export async function observeAuction(session,cid,aid){
  const messages=[],commands=[];
  const socket=new WebSocket('ws://localhost:18090/ws',{headers:{Origin:'http://localhost:18090',Cookie:[...session.cookies].map(([k,v])=>`${k}=${v}`).join('; ')}});
  await new Promise((resolve,reject)=>{
    const timeout=setTimeout(()=>{socket.close();reject(Error('STOMP subscription timeout; frames='+commands.join(',')));},10000);
    socket.on('error',error=>{clearTimeout(timeout);reject(error);});
    socket.on('open',()=>socket.send('CONNECT\naccept-version:1.2\nhost:localhost\nheart-beat:0,0\n\n\0'));
    socket.on('message',data=>{
      const frame=data.toString();commands.push(frame.split('\n')[0]);
      if(frame.startsWith('ERROR')){clearTimeout(timeout);reject(Error('STOMP rejected the test subscription'));}
      if(frame.startsWith('CONNECTED')){socket.send(`SUBSCRIBE\nid:npl\ndestination:/topic/championship/${cid}/auction/${aid}\n\n\0`);clearTimeout(timeout);setTimeout(resolve,100);}
      if(frame.startsWith('RECEIPT')){clearTimeout(timeout);resolve();}
      if(frame.startsWith('MESSAGE'))messages.push(JSON.parse(frame.slice(frame.indexOf('\n\n')+2).replace(/\0$/,'')));
    });
  });
  return {messages,close:()=>socket.close(),async waitFor(predicate){
    for(let attempt=0;attempt<100;attempt++){const match=messages.find(predicate);if(match)return match;await new Promise(r=>setTimeout(r,50));}
    throw Error('Expected authoritative WebSocket event was not received');
  }};
}
