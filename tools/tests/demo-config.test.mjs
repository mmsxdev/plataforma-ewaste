import { test } from 'node:test';
import assert from 'node:assert/strict';
import { scheduleForDemo, validateDemoUrls } from '../demo-config.mjs';
test('default futuro mantém São Paulo mesmo na virada do ano', () => {
  assert.deepEqual(scheduleForDemo(undefined, new Date('2026-12-31T02:30:00Z')),
    { value: '2027-01-01T23:30', timeZone: 'America/Sao_Paulo', instant: '2027-01-02T02:30:00.000Z' });
});
test('override válido usa São Paulo e não o fuso da máquina', () => {
  assert.equal(scheduleForDemo('2027-02-01T14:00', new Date('2026-11-24T12:00Z')).instant,
    '2027-02-01T17:00:00.000Z');
});
test('rejeita data passada e exatamente agora', () => {
  for (const value of ['2026-11-23T14:00', '2026-11-24T09:00'])
    assert.throws(() => scheduleForDemo(value, new Date('2026-11-24T12:00Z')), /futuro/);
});
test('rejeita dia impossível, mês e hora inválidos', () => {
  for (const value of ['2027-02-30T10:00', '2027-13-01T10:00', '2027-01-01T25:00'])
    assert.throws(() => scheduleForDemo(value, new Date('2026-11-24T12:00Z')), /inválida/);
});
test('rejeita override com fuso/formato ambíguo', () => {
  for (const value of ['01/02/2027 14:00', '2027-02-01T14:00Z', 'amanhã'])
    assert.throws(() => scheduleForDemo(value), /YYYY/);
});
test('aceita instâncias locais distintas e rejeita rede/porta/idênticas', () => {
  assert.doesNotThrow(() => validateDemoUrls('http://127.0.0.1:18080', 'http://127.0.0.1:18081'));
  for (const [a, b] of [['http://192.168.1.2:8080', 'http://127.0.0.1:18081'],
    ['http://127.0.0.1:0', 'http://127.0.0.1:18081'],
    ['http://127.0.0.1:18080', 'http://127.0.0.1:18080'],
    ['http://127.0.0.1:18080', 'http://127.0.0.1:018080']])
    assert.throws(() => validateDemoUrls(a, b));
});
