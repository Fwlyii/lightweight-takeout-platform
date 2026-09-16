import test from 'node:test';
import assert from 'node:assert/strict';
import { merchantIndicator, pinMerchantCard, releaseMerchantCard } from '../src/utils/merchantMotion.js';
test('indicator follows actual active-tab dimensions without selecting a different tab', () => {
  let active = { offsetLeft: 12, offsetWidth: 74 };
  const values = {};
  const element = { querySelector: () => active, style: { setProperty: (key, value) => { values[key] = value; } } };
  merchantIndicator.mounted(element);
  assert.equal(values['--merchant-indicator-x'], '12px');
  active = { offsetLeft: 104, offsetWidth: 88 };
  merchantIndicator.updated(element);
  assert.equal(values['--merchant-indicator-x'], '104px');
  assert.equal(values['--merchant-indicator-width'], '88px');
  merchantIndicator.unmounted(element);
});
test('leaving cards retain their measured geometry and cleanup removes only motion styles', () => {
  const element = { offsetLeft: 10, offsetTop: 25, offsetWidth: 340, offsetHeight: 510, style: { color: 'red' } };
  pinMerchantCard(element);
  assert.equal(element.style.width, '340px'); assert.equal(element.style.top, '25px');
  releaseMerchantCard(element);
  assert.equal(element.style.width, ''); assert.equal(element.style.color, 'red');
});
