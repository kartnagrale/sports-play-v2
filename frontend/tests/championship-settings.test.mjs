import {test} from 'node:test';
import assert from 'node:assert/strict';
import {settingsRequest,settingsError,inputNumber} from '../src/lib/championship-settings.ts';
const form={auctionName:'NPL',maxSquadSize:11,purseLimit:100000,minMale:0,minFemale:3,playerBasePrice:5000,bidIncrement:1000,timerSeconds:30,customRules:'',version:1};
test('valid rupees retain exact units and exclude response metadata',()=>{
  const payload=settingsRequest({...form,championshipId:'read-only',status:'ACTIVE',auctionId:'read-only'});
  assert.equal(payload.purseLimit,100000);assert.equal(payload.playerBasePrice,5000);assert.equal(payload.bidIncrement,1000);
  assert.equal(payload.championshipId,undefined);assert.equal(payload.version,undefined);assert.equal(payload.expectedVersion,1);
});
test('keyboard clearing permits an empty draft and prevents submission',()=>{
  assert.equal(inputNumber(''),'');assert.equal(inputNumber('NaN'),'');
  assert.match(settingsError({...form,purseLimit:inputNumber('')}),/purseLimit/);
  assert.equal(inputNumber('5000'),5000);assert.equal(settingsError(form),null);
});
test('negative, nonfinite, too precise and missing values fail before serialization',()=>{
  for(const purseLimit of [-1,NaN,Infinity,'',0,1.001])assert.throws(()=>settingsRequest({...form,purseLimit}));
  assert.equal(settingsRequest({...form,purseLimit:100000.50}).purseLimit,100000.50);
});
