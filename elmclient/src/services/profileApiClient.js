/** Pure HTTP boundary: reusable by production pages and executable Node tests. */
export function createProfileApiClient(request) {
  const unwrap = (response) => {
    if (response?.success !== true)
      throw new Error(response?.message || "操作失败，请重试");
    return response.data;
  };
  const id = (value) => {
    if (
      !/^[1-9][0-9]*$/.test(String(value)) ||
      !Number.isSafeInteger(Number(value))
    )
      throw new Error("无效的记录编号");
    return String(value);
  };
  const clean = (value) => (typeof value === "string" ? value.trim() : "");
  const address = (value) => ({
    contactName: clean(value.contactName),
    contactSex: value.contactSex ?? null,
    contactTel: clean(value.contactTel),
    address: clean(value.address),
  });
  const profile = (value) =>
    Object.fromEntries(
      ["firstName", "lastName", "phone", "email", "gender"].map((key) => [
        key,
        clean(value[key]),
      ])
    );
  const verifiedProfile = (value) => {
    if (!value?.id || typeof value.username !== "string")
      throw new Error("个人信息返回异常，请重试");
    return value;
  };
  return {
    addresses: {
      list: async () => unwrap(await request.get("/api/addresses/me")),
      get: async (value) =>
        unwrap(await request.get("/api/addresses/" + id(value))),
      create: async (value) =>
        unwrap(await request.post("/api/addresses/me", address(value))),
      update: async (key, value) =>
        unwrap(await request.put("/api/addresses/" + id(key), address(value))),
      remove: async (value) =>
        unwrap(await request.delete("/api/addresses/" + id(value))),
      setDefault: async (value) =>
        unwrap(await request.put("/api/addresses/" + id(value) + "/default")),
    },
    profile: {
      get: async () => verifiedProfile(await request.get("/api/user")),
      update: async (value) =>
        verifiedProfile(await request.put("/api/user", profile(value))),
    },
    interactions: {
      list: async () =>
        unwrap(await request.get("/api/merchant/interaction/collections/me")),
      get: async (value) =>
        unwrap(
          await request.get("/api/merchant/interaction/status/me", {
            params: { merchantId: Number(id(value)) },
          })
        ),
      update: async (value) => {
        const payload = { merchantId: Number(id(value.merchantId)) };
        for (const key of ["collected", "liked"]) {
          if (value[key] !== undefined && value[key] !== null) {
            if (typeof value[key] !== "boolean")
              throw new Error("互动状态必须是布尔值");
            payload[key] = value[key];
          }
        }
        if (Object.keys(payload).length === 1)
          throw new Error("请选择要更新的互动状态");
        return unwrap(
          await request.post("/api/merchant/interaction/update", payload)
        );
      },
    },
  };
}
