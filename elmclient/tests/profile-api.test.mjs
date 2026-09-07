import test from "node:test";
import assert from "node:assert/strict";
import { createProfileApiClient } from "../src/services/profileApiClient.js";
import { formatRating, formatMoney } from "../src/utils/formatters.js";

test("shared rating formatter preserves unrated state and consistent rounding", () => {
  for (const value of [null, undefined, "", 0, "bad"])
    assert.equal(formatRating(value), null);
  assert.equal(formatRating(4.67), "4.7");
  assert.equal(formatMoney(3), "3.00");
});
function fixture(response = { success: true, data: { id: 1 } }) {
  const calls = [];
  const request = Object.fromEntries(
    ["get", "post", "put", "delete"].map((method) => [
      method,
      async (...args) => {
        calls.push({ method, args });
        return response;
      },
    ])
  );
  return { calls, api: createProfileApiClient(request) };
}
test("address requests do not submit owner, audit or default flags", async () => {
  const { api, calls } = fixture();
  await api.addresses.create({
    contactName: " 李 ",
    contactTel: "13900000000",
    address: " 学校 ",
    userId: 99,
    deleted: true,
    isDefault: true,
  });
  assert.deepEqual(calls[0], {
    method: "post",
    args: [
      "/api/addresses/me",
      {
        contactName: "李",
        contactSex: null,
        contactTel: "13900000000",
        address: "学校",
      },
    ],
  });
});
test("default selection is a server request, not a browser-only preference", async () => {
  const { api, calls } = fixture();
  await api.addresses.setDefault(7);
  assert.deepEqual(calls[0], {
    method: "put",
    args: ["/api/addresses/7/default"],
  });
});
test("reject malformed ids before making requests", async () => {
  for (const value of [
    "../user",
    "https://bad.test",
    0,
    -1,
    1.2,
    "9007199254740993",
  ]) {
    const { api, calls } = fixture();
    await assert.rejects(api.addresses.get(value));
    assert.equal(calls.length, 0);
  }
});
test("profile payload contains editable fields only and supports clearing optional values", async () => {
  const { api, calls } = fixture({ id: 1, username: "owner" });
  await api.profile.update({
    phone: "13900000000",
    id: 99,
    authorities: ["ADMIN"],
    password: "bad",
    photo: "bad",
  });
  assert.deepEqual(calls[0].args[1], {
    firstName: "",
    lastName: "",
    phone: "13900000000",
    email: "",
    gender: "",
  });
});
test("invalid profile responses are not accepted as a new session profile", async () => {
  const { api } = fixture({ success: false, message: "failed" });
  await assert.rejects(api.profile.get());
});
test("unfavorite sends false explicitly and discards request identity", async () => {
  const { api, calls } = fixture();
  await api.interactions.update({
    merchantId: 8,
    collected: false,
    userId: 99,
  });
  assert.deepEqual(calls[0].args[1], { merchantId: 8, collected: false });
});
test("string false must not be coerced to a true interaction", async () => {
  const { api, calls } = fixture();
  await assert.rejects(
    api.interactions.update({ merchantId: 8, collected: "false" })
  );
  await assert.rejects(api.interactions.update({ merchantId: 8 }));
  assert.equal(calls.length, 0);
});
test("business failure is surfaced instead of returning an empty successful list", async () => {
  const { api } = fixture({ success: false, message: "无权访问" });
  await assert.rejects(api.interactions.list(), /无权访问/);
});
